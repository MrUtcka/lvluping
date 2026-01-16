package org.mrutcka.lvluping;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.command.LevelCommand;
import org.mrutcka.lvluping.command.RaceCommand;
import org.mrutcka.lvluping.command.StarCommand;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.handler.AttributeHandler;
import org.mrutcka.lvluping.network.S2CSyncTalents;
import org.slf4j.Logger;

@Mod(LvlupingMod.MODID)
public class LvlupingMod {
    public static final String MODID = "lvluping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LvlupingMod(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Lvluping Mod Setup");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LevelCommand.register(event.getDispatcher());
        StarCommand.register(event.getDispatcher());
        RaceCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            AttributeHandler.applyStats(newPlayer, true);
        }
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeHandler.applyStats(player, false);

            PacketDistributor.sendToPlayer(player, new S2CSyncTalents(
                    PlayerLevels.getLevel(player),
                    PlayerLevels.getStars(player.getUUID()),
                    PlayerLevels.getPlayerTalents(player.getUUID()),
                    PlayerLevels.getPlayerStatsMap(player.getUUID()),
                    PlayerLevels.getRace(player.getUUID()).id
            ));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.setStoredHealth(player.getUUID(), player.getHealth());
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        PlayerLevels.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PlayerLevels.save(event.getServer());
    }

    @SubscribeEvent
    public void onLevelSave(LevelEvent.Save event) {
        if (event.getLevel().getServer() != null) {
            PlayerLevels.save(event.getLevel().getServer());
        }
    }
}