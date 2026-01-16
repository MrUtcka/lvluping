package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mrutcka.lvluping.LvlupingMod;

public class ClientEvents {

    public static final KeyMapping TALENT_KEY = new KeyMapping(
            "key.lvluping.talents",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.lvluping"
    );

    @Mod.EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TALENT_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                while (TALENT_KEY.consumeClick()) {
                    Minecraft.getInstance().setScreen(new TalentScreen());
                }
            }
        }
    }
}