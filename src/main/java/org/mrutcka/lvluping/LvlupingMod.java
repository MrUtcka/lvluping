package org.mrutcka.lvluping;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.mrutcka.lvluping.command.LevelCommand;
import org.mrutcka.lvluping.command.RaceCommand;
import org.mrutcka.lvluping.command.StarCommand;
import org.slf4j.Logger;

@Mod(LvlupingMod.MODID)
public class LvlupingMod {
    public static final String MODID = "lvluping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LvlupingMod(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Lvluping Mod Setup");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        LevelCommand.register(event.getDispatcher());
        StarCommand.register(event.getDispatcher());
        RaceCommand.register(event.getDispatcher());
    }
}