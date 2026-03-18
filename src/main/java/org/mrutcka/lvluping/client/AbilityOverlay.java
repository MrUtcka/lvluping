package org.mrutcka.lvluping.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class AbilityOverlay {
    public static void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || mc.options.hideGui || TalentScreen.clientTalents == null) return;

        int x = 10;
        int y = 10;

        if (ProvocationHintClient.isProvocationActive()) {
            int w = mc.getWindow().getGuiScaledWidth();
            int h = mc.getWindow().getGuiScaledHeight();
            String msg = net.minecraft.network.chat.Component.translatable("message.lvluping.hit_provoker").getString();
            int tw = mc.font.width(msg);
            guiGraphics.drawString(mc.font, msg, (w - tw) / 2, h / 2 - 30, 0xFF4444, true);
            guiGraphics.drawString(mc.font, msg, (w - tw) / 2, h / 2 - 30, 0xFFFFFF, false);
        }

        int combo = player.getPersistentData().getInt("lvluping_combo");
        int lastHit = player.getPersistentData().getInt("lvluping_last_hit");

        if (combo > 0 && (player.level().getGameTime() - lastHit < 40)) {
            guiGraphics.drawString(mc.font, "КОМБО: x" + combo, x, y, 0xFFAA00, true);
            y += 12;
        }

        if (TalentScreen.clientTalents.contains("as_slide")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_slide", "Подкат [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_shield_strike")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_shield", "Удар щитом [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_smoke")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_smoke", "Смок [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_dash")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_dash", "Отскок [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_seismic")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_seismic", "Сейсмический удар [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_spin")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_spin", "Круговой удар [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_parry")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_parry", "Парирование [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_barrier")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_buff", "Усиление [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_fire_lightning")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_fire", "Фаербол/Молния [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_ice_arrow")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_ice", "Ледяная стрела [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_teleport")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_teleport", "Телепорт [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_servant")) {
            String key = ClientEvents.ABILITY_KEY_1.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_summon", "Призыв слуги [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_sacrifice")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_sacrifice", "Жертва [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_summon_command")) {
            String key = ClientEvents.ABILITY_KEY_3.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_m_command", "Команда [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_iron_skin")) {
            String key = ClientEvents.ABILITY_KEY_4.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_iron_skin", "Железная кожа [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_heavy_step")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_heavy_step", "Тяжёлая поступь [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_unbreakable")) {
            renderAbility(guiGraphics, mc, player, "cd_w_unbreakable", "Несокрушимый", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_armor_breaker")) {
            renderAbility(guiGraphics, mc, player, "cd_w_armor_breaker", "Разрушитель доспехов", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_provocation")) {
            String key = ClientEvents.ABILITY_KEY_2.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_provocation", "Провокация [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_berserk")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_berserk", "Берсерк [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_brotherhood")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_brotherhood", "Братство [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_final_countdown")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_final_countdown", "Судный молот [" + key + "]", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_ult_invulnerability")) {
            String key = ClientEvents.ABILITY_KEY_5.getTranslatedKeyMessage().getString();
            renderAbility(guiGraphics, mc, player, "cd_w_ult_invulnerability", "Неуязвимость [" + key + "]", x, y);
            y += 12;
        }
    }

    private static void renderAbility(GuiGraphics graphics, Minecraft mc, Player player, String nbtKey, String label, int x, int y) {
        int currentTicks = player.getPersistentData().getInt(nbtKey);

        graphics.pose().pushPose();

        float scale = 0.6f;
        graphics.pose().scale(scale, scale, scale);

        float scaledX = x / scale;
        float scaledY = y / scale;

        if (currentTicks > 0) {
            float secondsLeft = currentTicks / 20.0f;
            String text = String.format("%s: %.1f сек", label, secondsLeft);
            graphics.drawString(mc.font, text, (int) scaledX, (int) scaledY, 0xFF5555, true);
        } else {
            graphics.drawString(mc.font, label + ": ГОТОВО", (int) scaledX, (int) scaledY, 0x55FF55, true);
        }

        graphics.pose().popPose();
    }
}