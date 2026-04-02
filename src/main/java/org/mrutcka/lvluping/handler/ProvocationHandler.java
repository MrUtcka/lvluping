package org.mrutcka.lvluping.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.S2CProvocationHint;

import java.util.Set;

public final class ProvocationHandler {

    private static final String TEAM_NAME = "lvluping_provoker";
    private static final double AGGRO_RANGE = 12.0;

    public static void setProvokerTeam(ServerPlayer player, boolean add) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            Scoreboard sb = server.getScoreboard();
            if (sb == null) return;
            String name = player.getScoreboardName();
            if (name == null || name.isEmpty()) return;

            PlayerTeam team = sb.getPlayerTeam(TEAM_NAME);
            if (team == null) {
                team = sb.addPlayerTeam(TEAM_NAME);
                if (team != null) team.setColor(ChatFormatting.RED);
            }
            if (team == null) return;

            if (add) {
                if (!team.getPlayers().contains(name)) sb.addPlayerToTeam(name, team);
            } else {
                sb.removePlayerFromTeam(name, team);
            }
        } catch (Exception ignored) {
        }
    }

    public static void tickProvocation(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            long until = player.getPersistentData().getLong("lvluping_provocation_until");
            if (until > 0 && until <= time) {
                var pd = player.getPersistentData();
                pd.remove("lvluping_provocation_until");
                float beforeAbs = pd.contains("lvluping_prov_absorb_before") ? pd.getFloat("lvluping_prov_absorb_before") : 0f;
                pd.remove("lvluping_prov_absorb_before");
                Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
                int plvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_provocation", talents);
                float want = (float) (player.getMaxHealth() * AbilityUpgradeConfig.getDouble("w_provocation", "absorption_max_hp_ratio", plvl, 0.2));
                float ourPart = Math.max(0f, want - beforeAbs);
                float cur = player.getAbsorptionAmount();
                float remove = Math.min(cur, ourPart);
                player.setAbsorptionAmount(Math.max(0f, cur - remove));
                player.removeEffect(MobEffects.GLOWING);
                setProvokerTeam(player, false);
                for (ServerPlayer p : level.players()) {
                    PacketDistributor.sendToPlayer(p, new S2CProvocationHint(false));
                }
                continue;
            }
            if (until <= time) continue;

            setProvokerTeam(player, true);

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(AGGRO_RANGE))) {
                if (mob.distanceTo(player) <= AGGRO_RANGE) {
                    mob.setTarget(player);
                }
            }
        }
    }

    public static boolean isProvoker(Player player) {
        if (player.level().isClientSide()) return false;
        long until = player.getPersistentData().getLong("lvluping_provocation_until");
        return until > 0 && player.level().getGameTime() < until;
    }

    public static ServerPlayer getActiveProvoker(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer p : level.players()) {
            long until = p.getPersistentData().getLong("lvluping_provocation_until");
            if (until > time) return p;
        }
        return null;
    }
}
