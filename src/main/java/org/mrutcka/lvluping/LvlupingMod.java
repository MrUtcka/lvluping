package org.mrutcka.lvluping;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.command.LevelCommand;
import org.mrutcka.lvluping.command.StarCommand;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.handler.AttributeHandler;
import org.mrutcka.lvluping.network.ModNetworking;
import org.mrutcka.lvluping.network.S2CSyncTalents;
import org.slf4j.Logger;

import java.util.UUID;

@Mod(LvlupingMod.MODID)
public class LvlupingMod {
    public static final String MODID = "lvluping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LvlupingMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::register);
        LOGGER.info("Lvluping: Common setup complete.");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("Lvluping: Client setup complete.");
    }

    private void onServerStart(ServerStartingEvent event) {
        LOGGER.info("Lvluping: Server starting!");
        PlayerLevels.load(event.getServer());
    }

    private void onServerStop(ServerStoppingEvent event) {
        LOGGER.info("Lvluping: Server stopping!");
        PlayerLevels.save(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LevelCommand.register(event.getDispatcher());
        StarCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            UUID oldId = event.getOriginal().getUUID();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            AttributeHandler.applyStats(newPlayer, true);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLevels.setStoredHealth(player.getUUID(), player.getHealth());
        }
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeHandler.applyStats(player, false);

            ModNetworking.CHANNEL.send(new S2CSyncTalents(
                    PlayerLevels.getLevel(player),
                    PlayerLevels.getStars(player.getUUID()),
                    PlayerLevels.getPlayerTalents(player.getUUID()),
                    PlayerLevels.getPlayerStatsMap(player.getUUID())
            ), PacketDistributor.PLAYER.with(player));
        }
    }
}