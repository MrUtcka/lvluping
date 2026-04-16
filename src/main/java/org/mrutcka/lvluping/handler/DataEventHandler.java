package org.mrutcka.lvluping.handler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.LvlupingServerData;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.data.PlayerStatTrainingData;
import org.mrutcka.lvluping.data.StatTrainingConfig;
import org.mrutcka.lvluping.network.S2CProvocationHint;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class DataEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.applyStartingBonus(player);

            AttributeHandler.applyStats(player, false);
            syncPlayer(player);
            TalentAbilityHandler.syncAllCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
            if (!PlayerStatTrainingData.consumePendingTrainDeathPenaltyAndApply(oldPlayer, newPlayer)) {
                AttributeHandler.applyStats(newPlayer, true);
            }
            syncPlayer(newPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.setStoredHealth(player.getUUID(), player.getHealth());
            if (player.getPersistentData().getLong("lvluping_provocation_until") > 0 && player.level() instanceof ServerLevel sl) {
                for (ServerPlayer p : sl.players()) {
                    PacketDistributor.sendToPlayer(p, new S2CProvocationHint(false));
                }
            }
        }
    }

    private static void syncPlayer(ServerPlayer player) {
        PlayerLevels.syncTalentsToClient(player);
        PlayerStatTrainingData.syncToClient(player);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        var server = event.getServer();
        LvlupingServerData.migratePresetIfNeeded(server);
        PlayerLevels.load(server);
        LvlupingMod.LOGGER.info("LVLuping: данные игроков и пресет — {}", LvlupingServerData.root(server));
        AbilityUpgradeConfig.load();
        StatTrainingConfig.load(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) { PlayerLevels.save(event.getServer()); }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().getServer() != null) PlayerLevels.save(event.getLevel().getServer());
    }
}