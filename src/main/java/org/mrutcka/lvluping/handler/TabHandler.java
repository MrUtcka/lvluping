package org.mrutcka.lvluping.handler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.PlayerLevels;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class TabHandler {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                updatePlayerScoreboard(player);
            }
        }
    }

    public static void updatePlayerScoreboard(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        int stars = PlayerLevels.getStars(player.getUUID());
        int level = PlayerLevels.getLevel(player);

        String teamName = "lvl_tab_" + stars;
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        String starIcon = stars > 0 ? "§e" + "★".repeat(stars) + " " : "";
        Component prefix = Component.literal(starIcon + "§r");
        Component suffix = Component.literal(" §7[§b" + level + "§7]");

        team.setPlayerPrefix(prefix);
        team.setPlayerSuffix(suffix);

        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }
}