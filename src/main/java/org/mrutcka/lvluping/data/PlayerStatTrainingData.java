package org.mrutcka.lvluping.data;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.network.S2CSyncStatTraining;

import java.util.*;

public final class PlayerStatTrainingData {
    private static final Map<UUID, int[]> PROGRESS = new HashMap<>();
    private static final Map<UUID, Integer> GLOBAL_FATIGUE = new HashMap<>();
    private static final Map<UUID, Integer> TRAIN_MASK = new HashMap<>();
    private static final Map<UUID, Integer> APPROACH_WARNED = new HashMap<>();
    private static final Map<UUID, Long> MELEE_BOW_LOCKOUT_UNTIL = new HashMap<>();

    private static final Set<UUID> TRAINED_THIS_SECOND = Collections.newSetFromMap(new HashMap<>());

    public static final int MASK_BRAWN = 1;
    public static final int MASK_SPEED = 2;

    private static final int I_DMG = 0, I_SPD = 1, I_HP = 2;

    private static final int APPROACH_1 = 1;
    private static final int APPROACH_2 = 2;
    private static final int APPROACH_3 = 4;
    private static final int APPROACH_4 = 8;

    private static final ResourceKey<MobEffect> MINING_FATIGUE_KEY = ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.withDefaultNamespace("mining_fatigue"));

    private static MobEffectInstance trainingMiningFatigue(int durationTicks, int amplifier) {
        Holder<MobEffect> h = BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(MINING_FATIGUE_KEY);
        return new MobEffectInstance(h, durationTicks, amplifier, false, true, true);
    }

    private static int[] prog(UUID u) {
        return PROGRESS.computeIfAbsent(u, k -> new int[3]);
    }

    private static int unitsNeededForNextLevel(UUID uuid, AttributeStat stat, int baseNeed) {
        if (baseNeed <= 0) return 0;
        int max = Math.max(1, stat.maxLevel);
        int cur = Math.max(0, Math.min(PlayerLevels.getStatLevel(uuid, stat.id), max - 1));
        if (max <= 1) return baseNeed;
        double t = (double) cur / (double) (max - 1);
        double mult = 1.0 + t;
        return Math.max(1, (int) Math.round(baseNeed * mult));
    }

    private static int unitsNeededForNextLevel(UUID uuid, AttributeStat stat) {
        int baseNeed = switch (stat) {
            case DAMAGE -> StatTrainingConfig.damageUnitsPerLevel;
            case SPEED -> StatTrainingConfig.speedUnitsPerLevel;
            case HEALTH -> StatTrainingConfig.healthUnitsPerLevel;
            default -> 0;
        };
        return unitsNeededForNextLevel(uuid, stat, baseNeed);
    }

    public static void syncToClient(ServerPlayer player) {
        UUID u = player.getUUID();
        int[] p = prog(u);
        int dmgNeed = unitsNeededForNextLevel(u, AttributeStat.DAMAGE);
        int spdNeed = unitsNeededForNextLevel(u, AttributeStat.SPEED);
        int hpNeed = unitsNeededForNextLevel(u, AttributeStat.HEALTH);
        int gf = getGlobalFatigue(u);
        int cap = Math.max(1, StatTrainingConfig.tfThreshold4);
        PacketDistributor.sendToPlayer(player, new S2CSyncStatTraining(
                p[I_DMG], Math.max(1, dmgNeed),
                p[I_SPD], Math.max(1, spdNeed),
                p[I_HP], Math.max(1, hpNeed),
                PlayerLevels.getStatLevel(u, AttributeStat.DAMAGE.id) >= AttributeStat.DAMAGE.maxLevel,
                PlayerLevels.getStatLevel(u, AttributeStat.SPEED.id) >= AttributeStat.SPEED.maxLevel,
                PlayerLevels.getStatLevel(u, AttributeStat.HEALTH.id) >= AttributeStat.HEALTH.maxLevel,
                gf,
                fatigueTier(gf),
                cap
        ));
    }

    public static void appendTag(CompoundTag pData, UUID uuid) {
        CompoundTag t = new CompoundTag();
        int[] p = prog(uuid);
        t.putInt("dmg", p[I_DMG]);
        t.putInt("spd", p[I_SPD]);
        t.putInt("hp", p[I_HP]);
        t.putInt("gf", getGlobalFatigue(uuid));
        t.putInt("tm", getTrainMask(uuid));
        t.putInt("wa", APPROACH_WARNED.getOrDefault(uuid, 0));
        t.putLong("nmjGT", MELEE_BOW_LOCKOUT_UNTIL.getOrDefault(uuid, 0L));
        pData.put("stat_training", t);
    }

    public static void readTag(CompoundTag pData, UUID uuid) {
        if (!pData.contains("stat_training")) return;
        CompoundTag t = pData.getCompound("stat_training");
        int[] p = prog(uuid);
        p[I_DMG] = t.getInt("dmg");
        p[I_SPD] = t.getInt("spd");
        p[I_HP] = t.getInt("hp");
        if (t.contains("gf")) {
            GLOBAL_FATIGUE.put(uuid, Math.max(0, t.getInt("gf")));
            TRAIN_MASK.put(uuid, t.getInt("tm"));
            APPROACH_WARNED.put(uuid, t.getInt("wa"));
            if (t.contains("nmjGT")) {
                long until = t.getLong("nmjGT");
                if (until > 0) MELEE_BOW_LOCKOUT_UNTIL.put(uuid, until);
                else MELEE_BOW_LOCKOUT_UNTIL.remove(uuid);
            }
        } else {
            migrateLegacyFatigueNbt(t, uuid);
        }
    }

    private static void migrateLegacyFatigueNbt(CompoundTag t, UUID uuid) {
        int sum = Math.max(t.getInt("fMelee"), Math.max(t.getInt("fBow"), Math.max(t.getInt("fSpd"), t.getInt("fPvp"))));
        if (sum > 0) GLOBAL_FATIGUE.put(uuid, Math.min(sum, StatTrainingConfig.tfThreshold4 + 10_000));
        APPROACH_WARNED.put(uuid, t.getInt("warned"));
    }

    public static Collection<UUID> extraSaveUuids() {
        HashSet<UUID> out = new HashSet<>();
        out.addAll(PROGRESS.keySet());
        out.addAll(GLOBAL_FATIGUE.keySet());
        out.addAll(TRAIN_MASK.keySet());
        out.addAll(APPROACH_WARNED.keySet());
        out.addAll(MELEE_BOW_LOCKOUT_UNTIL.keySet());
        return out;
    }

    public static void markTrainingThisSecond(ServerPlayer player) {
        TRAINED_THIS_SECOND.add(player.getUUID());
    }


    public static boolean tryConsumeOneLevelTrainingProgress(ServerPlayer player, AttributeStat stat) {
        int need = unitsNeededForNextLevel(player.getUUID(), stat);
        if (need <= 0) return false;
        UUID u = player.getUUID();
        if (PlayerLevels.getStatLevel(u, stat.id) >= stat.maxLevel) return false;
        int idx = switch (stat) {
            case DAMAGE -> I_DMG;
            case SPEED -> I_SPD;
            case HEALTH -> I_HP;
            default -> -1;
        };
        if (idx < 0) return false;
        int[] p = prog(u);
        if (p[idx] < need) return false;
        p[idx] -= need;
        return true;
    }

    public static void tickRestSecond(ServerPlayer player) {
        UUID u = player.getUUID();
        if (TRAINED_THIS_SECOND.remove(u)) return;
        decayGlobalFatigueOneSecond(player);
    }

    private static void decayGlobalFatigueOneSecond(ServerPlayer player) {
        int d = StatTrainingConfig.tfDecayPerSecondWhenIdle;
        if (d <= 0) return;
        UUID u = player.getUUID();
        int prev = getGlobalFatigue(u);
        if (prev <= 0) return;
        int next = Math.max(0, prev - d);
        if (next == prev) return;
        setGlobalFatigue(u, next);
        updateApproachFlagsAfterDecay(u, next);
        if (next == 0) {
            TRAIN_MASK.remove(u);
            APPROACH_WARNED.remove(u);
        }
        syncToClient(player);
    }

    private static int getGlobalFatigue(UUID u) {
        return GLOBAL_FATIGUE.getOrDefault(u, 0);
    }

    private static void setGlobalFatigue(UUID u, int v) {
        if (v <= 0) GLOBAL_FATIGUE.remove(u);
        else GLOBAL_FATIGUE.put(u, v);
    }

    private static int getTrainMask(UUID u) {
        return TRAIN_MASK.getOrDefault(u, 0);
    }

    private static void addTrainMask(UUID u, int maskBit) {
        TRAIN_MASK.merge(u, maskBit, (a, b) -> a | b);
    }

    private static int fatigueTier(int fatigue) {
        if (fatigue < StatTrainingConfig.tfThreshold1) return 0;
        if (fatigue < StatTrainingConfig.tfThreshold2) return 1;
        if (fatigue < StatTrainingConfig.tfThreshold3) return 2;
        if (fatigue < StatTrainingConfig.tfThreshold4) return 3;
        return 4;
    }

    private static boolean addTrainingStrain(ServerPlayer player, int amount, int maskBit) {
        if (amount <= 0) return false;
        markTrainingThisSecond(player);
        UUID u = player.getUUID();
        addTrainMask(u, maskBit);
        int prev = getGlobalFatigue(u);
        int next = Math.min(prev + amount, StatTrainingConfig.tfThreshold4 + 1_000_000);
        setGlobalFatigue(u, next);
        checkApproachMessages(player, prev, next);
        int oldTier = fatigueTier(prev);
        int newTier = fatigueTier(next);
        if (newTier > oldTier) {
            int tApply = Math.min(newTier, 3);
            if (tApply > oldTier) {
                if (tApply == 3 && player.getRandom().nextDouble() >= StatTrainingConfig.tfTier3ApplyChance) {
                } else {
                    applyTierEffects(player, tApply);
                    player.sendSystemMessage(Component.translatable("message.lvluping.training_fatigue.cross_tier" + tApply));
                }
            }
        }
        if (newTier >= 4) {
            syncToClient(player);
            killFromOvertraining(player);
            return true;
        }
        return false;
    }

    private static void killFromOvertraining(ServerPlayer player) {
        UUID u = player.getUUID();
        setGlobalFatigue(u, 0);
        TRAIN_MASK.remove(u);
        APPROACH_WARNED.remove(u);
        MELEE_BOW_LOCKOUT_UNTIL.remove(u);
        player.getPersistentData().putBoolean("lvluping_train_death_penalty_pending", true);
        player.sendSystemMessage(Component.translatable("message.lvluping.training_fatigue.cross_tier4"));
        syncToClient(player);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
    }


    public static boolean consumePendingTrainDeathPenaltyAndApply(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        var o = oldPlayer.getPersistentData();
        var n = newPlayer.getPersistentData();
        if (!o.getBoolean("lvluping_train_death_penalty_pending") && !n.getBoolean("lvluping_train_death_penalty_pending")) {
            return false;
        }
        o.remove("lvluping_train_death_penalty_pending");
        n.remove("lvluping_train_death_penalty_pending");
        applyPostTrainingDeathPenaltyOnRespawn(newPlayer);
        return true;
    }

    private static void applyPostTrainingDeathPenaltyOnRespawn(ServerPlayer player) {
        if (player.getServer() == null) return;
        int ticks = StatTrainingConfig.tfPostTrainDeathPenaltyTicks();
        long until = player.getServer().overworld().getGameTime() + ticks;
        player.getPersistentData().putLong("lvluping_train_death_penalty_until", until);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1, false, true, true));
        player.addEffect(trainingMiningFatigue(ticks, 1));
        org.mrutcka.lvluping.handler.AttributeHandler.applyStats(player, true);
    }

    public static void tickTrainDeathPenaltyExpire(ServerPlayer player) {
        long until = player.getPersistentData().getLong("lvluping_train_death_penalty_until");
        if (until <= 0 || player.getServer() == null) return;
        if (player.getServer().overworld().getGameTime() >= until) {
            player.getPersistentData().remove("lvluping_train_death_penalty_until");
            org.mrutcka.lvluping.handler.AttributeHandler.applyStats(player, false);
        }
    }

    private static void applyTier3MeleeBowLockout(ServerPlayer player, int durationTicks) {
        if (durationTicks <= 0 || player.getServer() == null) return;
        long until = player.getServer().overworld().getGameTime() + durationTicks;
        MELEE_BOW_LOCKOUT_UNTIL.put(player.getUUID(), until);
    }

    public static boolean isMeleeBowBlockedByTraining(ServerPlayer player) {
        if (player.getServer() == null) return false;
        long now = player.getServer().overworld().getGameTime();
        return MELEE_BOW_LOCKOUT_UNTIL.getOrDefault(player.getUUID(), 0L) > now;
    }

    private static void applyTierEffects(ServerPlayer player, int tier) {
        int ticks = StatTrainingConfig.tfEffectTicksForTier(tier);
        int mask = getTrainMask(player.getUUID());
        boolean brawn = (mask & MASK_BRAWN) != 0;
        boolean spd = (mask & MASK_SPEED) != 0;

        switch (tier) {
            case 1 -> {
                if (brawn) player.addEffect(trainingMiningFatigue(ticks, 0));
                if (spd) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, false, true, true));
            }
            case 2 -> {
                if (brawn) player.addEffect(trainingMiningFatigue(ticks, 2));
                if (spd) player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0, false, true, true));
            }
            case 3 -> {
                if (brawn) player.addEffect(trainingMiningFatigue(ticks, 3));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 1, false, true, true));
                if (!spd) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, false, true, true));
                }
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0, false, true, true));
                applyTier3MeleeBowLockout(player, ticks);
            }
            default -> {
            }
        }
    }

    private static void checkApproachMessages(ServerPlayer player, int prev, int next) {
        tryApproach(player, prev, next, StatTrainingConfig.tfThreshold1, StatTrainingConfig.tfApproachFraction1, APPROACH_1, "message.lvluping.training_fatigue.approach_tier1");
        tryApproach(player, prev, next, StatTrainingConfig.tfThreshold2, StatTrainingConfig.tfApproachFraction2, APPROACH_2, "message.lvluping.training_fatigue.approach_tier2");
        tryApproach(player, prev, next, StatTrainingConfig.tfThreshold3, StatTrainingConfig.tfApproachFraction3, APPROACH_3, "message.lvluping.training_fatigue.approach_tier3");
        tryApproach(player, prev, next, StatTrainingConfig.tfThreshold4, StatTrainingConfig.tfApproachFraction4, APPROACH_4, "message.lvluping.training_fatigue.approach_tier4");
    }

    private static void tryApproach(ServerPlayer player, int prev, int next, int threshold, double fraction, int bit, String langKey) {
        if (threshold <= 0 || fraction <= 0 || fraction >= 1.0) return;
        int line = Math.max(1, (int) Math.floor(threshold * fraction));
        UUID u = player.getUUID();
        int w = APPROACH_WARNED.getOrDefault(u, 0);
        if (prev < line && next >= line && (w & bit) == 0) {
            APPROACH_WARNED.put(u, w | bit);
            player.sendSystemMessage(Component.translatable(langKey));
        }
    }

    private static void updateApproachFlagsAfterDecay(UUID u, int fatigue) {
        int w = APPROACH_WARNED.getOrDefault(u, 0);
        int nw = w;
        if (fatigue < lineForApproach(StatTrainingConfig.tfThreshold1, StatTrainingConfig.tfApproachFraction1)) nw &= ~APPROACH_1;
        if (fatigue < lineForApproach(StatTrainingConfig.tfThreshold2, StatTrainingConfig.tfApproachFraction2)) nw &= ~APPROACH_2;
        if (fatigue < lineForApproach(StatTrainingConfig.tfThreshold3, StatTrainingConfig.tfApproachFraction3)) nw &= ~APPROACH_3;
        if (fatigue < lineForApproach(StatTrainingConfig.tfThreshold4, StatTrainingConfig.tfApproachFraction4)) nw &= ~APPROACH_4;
        if (nw != w) {
            if (nw == 0) APPROACH_WARNED.remove(u);
            else APPROACH_WARNED.put(u, nw);
        }
    }

    private static int lineForApproach(int threshold, double fraction) {
        return Math.max(1, (int) Math.floor(threshold * fraction));
    }

    public static void addDamageProgress(ServerPlayer player, int units, int fatigueCost) {
        if (units <= 0) return;
        AttributeStat st = AttributeStat.DAMAGE;
        if (PlayerLevels.getStatLevel(player.getUUID(), st.id) >= st.maxLevel) {
            syncToClient(player);
            return;
        }
        int[] p = prog(player.getUUID());
        p[I_DMG] += units;
        if (addTrainingStrain(player, fatigueCost, MASK_BRAWN)) return;
        tryLevel(player, st, p, I_DMG);
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
        if (addTrainingStrain(player, units / 2 + 1, MASK_SPEED)) return;
        tryLevel(player, st, p, I_SPD);
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
        if (addTrainingStrain(player, units + 2, MASK_BRAWN)) return;
        tryLevel(player, st, p, I_HP);
        syncToClient(player);
    }

    private static void tryLevel(ServerPlayer player, AttributeStat stat, int[] pArr, int idx) {
        UUID uuid = player.getUUID();
        int prog = pArr[idx];
        boolean leveled = false;
        while (PlayerLevels.getStatLevel(uuid, stat.id) < stat.maxLevel) {
            int need = unitsNeededForNextLevel(uuid, stat);
            if (need <= 0 || prog < need) break;
            prog -= need;
            PlayerLevels.upgradeStat(uuid, stat.id);
            leveled = true;
            player.sendSystemMessage(Component.literal(
                    "§aТренировка: §f" + stat.label + " §7+1 §8(" + PlayerLevels.getStatLevel(uuid, stat.id) + "/" + stat.maxLevel + ")"));
        }
        pArr[idx] = prog;
        if (leveled) {
            org.mrutcka.lvluping.handler.AttributeHandler.applyStats(player, false);
            PlayerLevels.syncTalentsToClient(player);
        }
    }

    private PlayerStatTrainingData() {}
}
