package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.network.C2SUseAbility;

public class ClientEvents {

    public static final KeyMapping TALENT_KEY = new KeyMapping(
            "key.lvluping.talents",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY = new KeyMapping(
            "key.lvluping.ability",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "key.categories.lvluping"
    );

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TALENT_KEY);
            event.register(ABILITY_KEY);
        }
    }

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
            float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
            AbilityOverlay.render(event.getGuiGraphics(), partialTick);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (TALENT_KEY.consumeClick() && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new TalentScreen());
            }
            while (ABILITY_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility());
            }
        }
    }
}