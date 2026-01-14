package org.mrutcka.lvluping.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.mrutcka.lvluping.LvlupingMod;

@Mod.EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT)
public class ClientEvents {
    public static final KeyMapping TALENT_KEY = new KeyMapping(
            "key.lvluping.talents", GLFW.GLFW_KEY_K, "key.categories.lvluping");

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(TALENT_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (TALENT_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new TalentScreen());
        }
    }
}