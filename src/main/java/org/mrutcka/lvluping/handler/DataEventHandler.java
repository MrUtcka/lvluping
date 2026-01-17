package org.mrutcka.lvluping.handler;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.S2CSyncTalents;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class DataEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.applyStartingBonus(player);

            AttributeHandler.applyStats(player, false);
            syncPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            AttributeHandler.applyStats(newPlayer, true);
            syncPlayer(newPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.setStoredHealth(player.getUUID(), player.getHealth());
        }
    }

    private static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2CSyncTalents(
                PlayerLevels.getLevel(player),
                PlayerLevels.getStars(player.getUUID()),
                PlayerLevels.getPlayerTalents(player.getUUID()),
                PlayerLevels.getPlayerStatsMap(player.getUUID()),
                PlayerLevels.getRace(player.getUUID()).id
        ));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { PlayerLevels.load(event.getServer()); }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) { PlayerLevels.save(event.getServer()); }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().getServer() != null) PlayerLevels.save(event.getLevel().getServer());
    }
}