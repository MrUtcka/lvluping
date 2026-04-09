package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.mrutcka.lvluping.network.S2CSyncStatTraining;


public final class ClientStatTrainingHud {

    private static final long IDLE_HIDE_MS = 5_000L;

    private enum ActiveLane {
        NONE, DMG, SPD, HP
    }

    public static int dmgProg;
    public static int dmgNeed = 1;
    public static int spdProg;
    public static int spdNeed = 1;
    public static int hpProg;
    public static int hpNeed = 1;
    public static boolean dmgMaxed;
    public static boolean spdMaxed;
    public static boolean hpMaxed;
    public static boolean hasData;

    private static boolean havePriorSync;
    private static ActiveLane activeLane = ActiveLane.NONE;
    private static long hideAfterMs;

    public static void applyPacket(S2CSyncStatTraining msg) {
        int prevD = dmgProg;
        int prevS = spdProg;
        int prevH = hpProg;

        dmgProg = msg.dmgProg();
        dmgNeed = Math.max(1, msg.dmgNeed());
        spdProg = msg.spdProg();
        spdNeed = Math.max(1, msg.spdNeed());
        hpProg = msg.hpProg();
        hpNeed = Math.max(1, msg.hpNeed());
        dmgMaxed = msg.dmgMaxed();
        spdMaxed = msg.spdMaxed();
        hpMaxed = msg.hpMaxed();
        hasData = true;

        if (!havePriorSync) {
            havePriorSync = true;
            return;
        }

        int dD = (!msg.dmgMaxed() && msg.dmgProg() > prevD) ? msg.dmgProg() - prevD : 0;
        int dS = (!msg.spdMaxed() && msg.spdProg() > prevS) ? msg.spdProg() - prevS : 0;
        int dH = (!msg.hpMaxed() && msg.hpProg() > prevH) ? msg.hpProg() - prevH : 0;

        if (dD > 0 || dS > 0 || dH > 0) {
            long now = Util.getMillis();
            if (dD >= dS && dD >= dH) {
                activeLane = ActiveLane.DMG;
            } else if (dS >= dH) {
                activeLane = ActiveLane.SPD;
            } else {
                activeLane = ActiveLane.HP;
            }
            hideAfterMs = now + IDLE_HIDE_MS;
        }

        if (activeLane == ActiveLane.DMG && dmgMaxed) activeLane = ActiveLane.NONE;
        if (activeLane == ActiveLane.SPD && spdMaxed) activeLane = ActiveLane.NONE;
        if (activeLane == ActiveLane.HP && hpMaxed) activeLane = ActiveLane.NONE;
    }

    public static void render(GuiGraphics gui, Minecraft mc) {
        if (!hasData || mc.options.hideGui || mc.player == null) return;
        if (mc.player.isSpectator()) return;
        long now = Util.getMillis();
        if (now > hideAfterMs) {
            activeLane = ActiveLane.NONE;
            return;
        }
        if (activeLane == ActiveLane.NONE) return;

        int prog;
        int need;
        boolean maxed;
        int fillCol;
        String mark;
        switch (activeLane) {
            case DMG -> {
                prog = dmgProg;
                need = dmgNeed;
                maxed = dmgMaxed;
                fillCol = dmgMaxed ? 0xFFC9A04A : 0xFFCC6644;
                mark = "С";
            }
            case SPD -> {
                prog = spdProg;
                need = spdNeed;
                maxed = spdMaxed;
                fillCol = spdMaxed ? 0xFFC9A04A : 0xFF44AACC;
                mark = "↔";
            }
            case HP -> {
                prog = hpProg;
                need = hpNeed;
                maxed = hpMaxed;
                fillCol = hpMaxed ? 0xFFC9A04A : 0xFF55BB66;
                mark = "+";
            }
            default -> {
                return;
            }
        }

        if (maxed) return;

        int h = mc.getWindow().getGuiScaledHeight();
        int w = mc.getWindow().getGuiScaledWidth();

        int xpBarTop = h - 32 + 3;
        int y = xpBarTop - 11;
        int barW = 182;
        int left = (w - barW) / 2;
        int barH = 4;

        float fill = Mth.clamp((float) prog / (float) need, 0f, 1f);

        RenderSystem.enableBlend();
        drawSegment(gui, left, y, barW, barH, fill, 0xE0282828, fillCol, mark, prog, maxed);
        RenderSystem.disableBlend();
    }


    private static void drawSegment(GuiGraphics g, int x, int y, int w, int h, float fill, int bg, int fillCol, String mark, int prog, boolean maxed) {
        g.fill(x, y, x + w, y + h, bg);
        int fw = maxed ? w : Mth.ceil(w * fill);
        if (!maxed && prog > 0 && fw < 1) fw = 1;
        fw = Math.min(fw, w);
        if (fw > 0) g.fill(x, y, x + fw, y + h, fillCol);
        g.renderOutline(x, y, w, h, 0xFF101010);
        Minecraft mc = Minecraft.getInstance();
        int mw = mc.font.width(mark);
        g.drawString(mc.font, mark, x + (w - mw) / 2, y - 9, 0xFFE0E0E0, false);
    }

    private ClientStatTrainingHud() {}
}
