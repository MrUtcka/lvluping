package org.mrutcka.lvluping;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.mrutcka.lvluping.commands.LevelCommand;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.slf4j.Logger;

@Mod(LvlupingMod.MOD_ID)
public class LvlupingMod {
    public static final String MOD_ID = "lvluping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LvlupingMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStop);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void setup(final FMLCommonSetupEvent event) {
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
    }
}
