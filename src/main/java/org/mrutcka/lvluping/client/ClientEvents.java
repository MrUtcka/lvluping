package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.mrutcka.lvluping.LvlupingMod;

public class ClientEvents {

    public static final KeyMapping TALENT_KEY = new KeyMapping(
            "key.lvluping.talents",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.lvluping"
    );

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TALENT_KEY);
        }
    }

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (TALENT_KEY.consumeClick() && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new TalentScreen());
            }
        }
    }
}