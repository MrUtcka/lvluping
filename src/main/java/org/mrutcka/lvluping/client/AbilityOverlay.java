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

        int combo = player.getPersistentData().getInt("lvluping_combo");
        int lastHit = player.getPersistentData().getInt("lvluping_last_hit");

        if (combo > 0 && (player.level().getGameTime() - lastHit < 40)) {
            guiGraphics.drawString(mc.font, "КОМБО: x" + combo, x, y, 0xFFAA00, true);
            y += 12;
        }

        if (TalentScreen.clientTalents.contains("as_slide")) {
            renderAbility(guiGraphics, mc, player, "cd_slide", "Подкат", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("as_smoke")) {
            renderAbility(guiGraphics, mc, player, "cd_smoke", "Смок", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("a_dash")) {
            renderAbility(guiGraphics, mc, player, "cd_dash", "Отскок", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("w_parry")) {
            renderAbility(guiGraphics, mc, player, "cd_parry", "Парирование", x, y);
            y += 12;
        }
        if (TalentScreen.clientTalents.contains("m_barrier")) {
            renderAbility(guiGraphics, mc, player, "cd_buff", "Усиление", x, y);
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

            graphics.drawString(mc.font, text, (int)scaledX, (int)scaledY, 0xFF5555, true);
        } else {
            graphics.drawString(mc.font, label + ": ГОТОВО", (int)scaledX, (int)scaledY, 0x55FF55, true);
        }

        graphics.pose().popPose();
    }
}