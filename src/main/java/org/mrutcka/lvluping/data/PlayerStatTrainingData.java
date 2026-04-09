package org.mrutcka.lvluping.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.network.S2CSyncStatTraining;

import java.util.*;

public final class PlayerStatTrainingData {
    private static final Map<UUID, int[]> PROGRESS = new HashMap<>();
    private static final Map<UUID, int[]> FATIGUE = new HashMap<>();
    private static final Map<UUID, Integer> FATIGUE_WARNED = new HashMap<>();

    private static final int I_DMG = 0, I_SPD = 1, I_HP = 2;
    private static final int F_MELEE = 0, F_BOW = 1, F_SPD = 2, F_PVP = 3;

    private static int[] prog(UUID u) {
        return PROGRESS.computeIfAbsent(u, k -> new int[3]);
    }

    private static int[] fat(UUID u) {
        return FATIGUE.computeIfAbsent(u, k -> new int[4]);
    }

    public static void syncToClient(ServerPlayer player) {
        UUID u = player.getUUID();
        int[] p = prog(u);
        PacketDistributor.sendToPlayer(player, new S2CSyncStatTraining(
                p[I_DMG], Math.max(1, StatTrainingConfig.damageUnitsPerLevel),
                p[I_SPD], Math.max(1, StatTrainingConfig.speedUnitsPerLevel),
                p[I_HP], Math.max(1, StatTrainingConfig.healthUnitsPerLevel),
                PlayerLevels.getStatLevel(u, AttributeStat.DAMAGE.id) >= AttributeStat.DAMAGE.maxLevel,
                PlayerLevels.getStatLevel(u, AttributeStat.SPEED.id) >= AttributeStat.SPEED.maxLevel,
                PlayerLevels.getStatLevel(u, AttributeStat.HEALTH.id) >= AttributeStat.HEALTH.maxLevel
        ));
    }

    public static void appendTag(CompoundTag pData, UUID uuid) {
        CompoundTag t = new CompoundTag();
        int[] p = prog(uuid);
        t.putInt("dmg", p[I_DMG]);
        t.putInt("spd", p[I_SPD]);
        t.putInt("hp", p[I_HP]);
        int[] f = fat(uuid);
        t.putInt("fMelee", f[F_MELEE]);
        t.putInt("fBow", f[F_BOW]);
        t.putInt("fSpd", f[F_SPD]);
        t.putInt("fPvp", f[F_PVP]);
        t.putInt("warned", FATIGUE_WARNED.getOrDefault(uuid, 0));
        pData.put("stat_training", t);
    }

    public static void readTag(CompoundTag pData, UUID uuid) {
        if (!pData.contains("stat_training")) return;
        CompoundTag t = pData.getCompound("stat_training");
        int[] p = prog(uuid);
        p[I_DMG] = t.getInt("dmg");
        p[I_SPD] = t.getInt("spd");
        p[I_HP] = t.getInt("hp");
        int[] f = fat(uuid);
        f[F_MELEE] = t.getInt("fMelee");
        f[F_BOW] = t.getInt("fBow");
        f[F_SPD] = t.getInt("fSpd");
        f[F_PVP] = t.getInt("fPvp");
        FATIGUE_WARNED.put(uuid, t.getInt("warned"));
    }

    public static Collection<UUID> extraSaveUuids() {
        HashSet<UUID> out = new HashSet<>();
        out.addAll(PROGRESS.keySet());
        out.addAll(FATIGUE.keySet());
        out.addAll(FATIGUE_WARNED.keySet());
        return out;
    }

    public static void decayFatigue(UUID uuid) {
        int[] f = fat(uuid);
        int d = StatTrainingConfig.fatigueDecayPerSecond;
        f[F_MELEE] = Math.max(0, f[F_MELEE] - d);
        f[F_BOW] = Math.max(0, f[F_BOW] - d);
        f[F_SPD] = Math.max(0, f[F_SPD] - d);
        f[F_PVP] = Math.max(0, f[F_PVP] - d);
        int w = FATIGUE_WARNED.getOrDefault(uuid, 0);
        if (f[F_MELEE] < StatTrainingConfig.fatigueWarnMelee) w &= ~W_MELEE;
        if (f[F_BOW] < StatTrainingConfig.fatigueWarnBow) w &= ~W_BOW;
        if (f[F_SPD] < StatTrainingConfig.fatigueWarnSpeed) w &= ~W_SPD;
        if (f[F_PVP] < StatTrainingConfig.fatigueWarnPvp) w &= ~W_PVP;
        FATIGUE_WARNED.put(uuid, w);
    }

    private static final int W_MELEE = 1, W_BOW = 2, W_SPD = 4, W_PVP = 8;

    private static boolean wasWarned(UUID u, int bit) {
        return (FATIGUE_WARNED.getOrDefault(u, 0) & bit) != 0;
    }

    private static void setWarned(UUID u, int bit) {
        FATIGUE_WARNED.merge(u, bit, (a, b) -> a | b);
    }

    private static void clearWarnBit(UUID u, int bit) {
        FATIGUE_WARNED.computeIfPresent(u, (k, v) -> v & ~bit);
    }

    public enum FatigueTrack { MELEE, BOW, SPEED, PVP }

    public static void addFatigue(ServerPlayer player, FatigueTrack track, int amount) {
        UUID u = player.getUUID();
        int[] f = fat(u);
        int idx = switch (track) {
            case MELEE -> F_MELEE;
            case BOW -> F_BOW;
            case SPEED -> F_SPD;
            case PVP -> F_PVP;
        };
        f[idx] = Math.min(50_000_000, f[idx] + amount);
        int warn;
        int deb;
        int wbit;
        switch (track) {
            case MELEE -> {
                warn = StatTrainingConfig.fatigueWarnMelee;
                deb = StatTrainingConfig.fatigueDebuffMelee;
                wbit = W_MELEE;
            }
            case BOW -> {
                warn = StatTrainingConfig.fatigueWarnBow;
                deb = StatTrainingConfig.fatigueDebuffBow;
                wbit = W_BOW;
            }
            case SPEED -> {
                warn = StatTrainingConfig.fatigueWarnSpeed;
                deb = StatTrainingConfig.fatigueDebuffSpeed;
                wbit = W_SPD;
            }
            default -> {
                warn = StatTrainingConfig.fatigueWarnPvp;
                deb = StatTrainingConfig.fatigueDebuffPvp;
                wbit = W_PVP;
            }
        }

        if (f[idx] >= warn && !wasWarned(u, wbit)) {
            setWarned(u, wbit);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eВы устали от тренировки. Сделайте перерыв."));
        }
        if (f[idx] >= deb) {
            f[idx] = deb / 2;
            clearWarnBit(u, wbit);
            applyFatigueDebuff(player, track);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cПеретренированность: отдых обязателен."));
        }
    }

    private static void applyFatigueDebuff(ServerPlayer player, FatigueTrack track) {
        var rng = player.getRandom();
        switch (track) {
            case SPEED -> {
                int amp = StatTrainingConfig.slownessAmpMin;
                if (StatTrainingConfig.slownessAmpMax > amp && rng.nextBoolean()) amp = StatTrainingConfig.slownessAmpMax;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                        StatTrainingConfig.slownessTicks, amp, false, true, true));
            }
            case MELEE, BOW, PVP -> {
                int dur = track == FatigueTrack.BOW || track == FatigueTrack.MELEE
                        ? StatTrainingConfig.weaknessTicksLong
                        : StatTrainingConfig.weaknessTicksShort;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS,
                        dur, 0, false, true, true));
            }
        }
    }

    public static void addDamageProgress(ServerPlayer player, int units, FatigueTrack fatigueTrack, int fatigueCost) {
        if (units <= 0) return;
        AttributeStat st = AttributeStat.DAMAGE;
        if (PlayerLevels.getStatLevel(player.getUUID(), st.id) >= st.maxLevel) {
            syncToClient(player);
            return;
        }
        int[] p = prog(player.getUUID());
        p[I_DMG] += units;
        addFatigue(player, fatigueTrack, fatigueCost);
        tryLevel(player, st, p, I_DMG, StatTrainingConfig.damageUnitsPerLevel);
        syncToClient(player);
    }

    public static void addSpeedProgress(ServerPlayer player, int units) {
        if (units <= 0) return;
        AttributeStat st = AttributeStat.SPEED;
        if (PlayerLevels.getStatLevel(player.getUUID(), st.id) >= st.maxLevel) {
            syncToClient(player);
            return;
        }
        int[] p = prog(player.getUUID());
        p[I_SPD] += units;
        addFatigue(player, FatigueTrack.SPEED, units / 2 + 1);
        tryLevel(player, st, p, I_SPD, StatTrainingConfig.speedUnitsPerLevel);
        syncToClient(player);
    }

    public static void addHealthProgress(ServerPlayer player, int units) {
        if (units <= 0) return;
        AttributeStat st = AttributeStat.HEALTH;
        if (PlayerLevels.getStatLevel(player.getUUID(), st.id) >= st.maxLevel) {
            syncToClient(player);
            return;
        }
        int[] p = prog(player.getUUID());
        p[I_HP] += units;
        addFatigue(player, FatigueTrack.PVP, units + 2);
        tryLevel(player, st, p, I_HP, StatTrainingConfig.healthUnitsPerLevel);
        syncToClient(player);
    }

    private static void tryLevel(ServerPlayer player, AttributeStat stat, int[] pArr, int idx, int perLevel) {
        if (perLevel <= 0) return;
        UUID uuid = player.getUUID();
        int prog = pArr[idx];
        while (prog >= perLevel && PlayerLevels.getStatLevel(uuid, stat.id) < stat.maxLevel) {
            prog -= perLevel;
            PlayerLevels.upgradeStat(uuid, stat.id);
            org.mrutcka.lvluping.handler.AttributeHandler.applyStats(player, false);
            PlayerLevels.syncTalentsToClient(player);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§aТренировка: §f" + stat.label + " §7+1 §8(" + PlayerLevels.getStatLevel(uuid, stat.id) + "/" + stat.maxLevel + ")"));
        }
        pArr[idx] = prog;
    }

    private PlayerStatTrainingData() {}
}
