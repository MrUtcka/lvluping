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

    public static final KeyMapping ABILITY_KEY_1 = new KeyMapping(
            "key.lvluping.ability1",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_2 = new KeyMapping(
            "key.lvluping.ability2",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_3 = new KeyMapping(
            "key.lvluping.ability3",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_4 = new KeyMapping(
            "key.lvluping.ability4",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            "key.categories.lvluping"
    );

    public static final KeyMapping ABILITY_KEY_5 = new KeyMapping(
            "key.lvluping.ability5",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "key.categories.lvluping"
    );

    @EventBusSubscriber(modid = LvlupingMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TALENT_KEY);
            event.register(ABILITY_KEY_1);
            event.register(ABILITY_KEY_2);
            event.register(ABILITY_KEY_3);
            event.register(ABILITY_KEY_4);
            event.register(ABILITY_KEY_5);
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
            while (ABILITY_KEY_1.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(0));
            }
            while (ABILITY_KEY_2.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(1));
            }
            while (ABILITY_KEY_3.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(2));
            }
            while (ABILITY_KEY_4.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(3));
            }
            while (ABILITY_KEY_5.consumeClick()) {
                PacketDistributor.sendToServer(new C2SUseAbility(4));
            }

            // Локальное уменьшение КД для отображения (сервер остаётся авторитетным)
            var player = Minecraft.getInstance().player;
            if (player != null) {
                tickCooldownDisplay(player, "cd_slide");
                tickCooldownDisplay(player, "cd_smoke");
                tickCooldownDisplay(player, "cd_dash");
                tickCooldownDisplay(player, "cd_parry");
                tickCooldownDisplay(player, "cd_buff");
                tickCooldownDisplay(player, "cd_w_shield");
                tickCooldownDisplay(player, "cd_w_seismic");
                tickCooldownDisplay(player, "cd_w_iron_skin");
                tickCooldownDisplay(player, "cd_w_spin");
                tickCooldownDisplay(player, "cd_w_heavy_step");
                tickCooldownDisplay(player, "cd_w_unbreakable");
                tickCooldownDisplay(player, "cd_w_armor_breaker");
                tickCooldownDisplay(player, "cd_w_provocation");
                tickCooldownDisplay(player, "cd_w_ult_berserk");
                tickCooldownDisplay(player, "cd_w_ult_brotherhood");
                tickCooldownDisplay(player, "cd_w_ult_final_countdown");
                tickCooldownDisplay(player, "cd_w_ult_invulnerability");
                tickCooldownDisplay(player, "cd_m_fire");
                tickCooldownDisplay(player, "cd_m_ice");
                tickCooldownDisplay(player, "cd_m_teleport");
                tickCooldownDisplay(player, "cd_m_summon");
                tickCooldownDisplay(player, "cd_m_sacrifice");
                tickCooldownDisplay(player, "cd_m_command");
            }
            JudgementHammerClient.tick();
        }

        private static void tickCooldownDisplay(net.minecraft.world.entity.player.Player player, String key) {
            int val = player.getPersistentData().getInt(key);
            if (val > 0) {
                player.getPersistentData().putInt(key, val - 1);
            }
        }
    }
}