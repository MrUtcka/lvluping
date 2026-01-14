package org.mrutcka.lvluping.handler;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.PlayerLevels;

@Mod.EventBusSubscriber(modid = LvlupingMod.MODID)
public class TabHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            // Опрашиваем всех игроков раз в 2 секунды
            if (event.getServer().getTickCount() % 40 == 0) {
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    updatePlayerScoreboard(player);
                }
            }
        }
    }

    public static void updatePlayerScoreboard(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        int stars = PlayerLevels.getStars(player.getUUID());
        int level = PlayerLevels.getLevel(player);

        // Название команды зависит от количества звезд, чтобы игроки распределялись правильно
        // lvl_0, lvl_1, lvl_2 и т.д.
        String teamName = "lvl_tab_" + stars;
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);

        // Если такой команды еще нет на сервере — создаем её
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        // Формируем префикс (Звезды) и суффикс (Уровень)
        String starIcon = stars > 0 ? "§e" + "★".repeat(stars) + " " : "";
        Component prefix = Component.literal(starIcon + "§r");
        Component suffix = Component.literal(" §7[§b" + level + "§7]");

        // Устанавливаем визуальные части команды
        team.setPlayerPrefix(prefix);
        team.setPlayerSuffix(suffix);

        // Добавляем игрока в эту команду, если он еще не в ней
        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }
}