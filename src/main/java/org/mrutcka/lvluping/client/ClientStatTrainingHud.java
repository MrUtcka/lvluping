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

    public static int globalFatigue;
    public static int fatigueTier;
    public static int fatigueCap = 1;

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
        globalFatigue = Math.max(0, msg.globalFatigue());
        fatigueTier = Mth.clamp(msg.fatigueTier(), 0, 4);
        fatigueCap = Math.max(1, msg.fatigueCap());
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

        int h = mc.getWindow().getGuiScaledHeight();
        int w = mc.getWindow().getGuiScaledWidth();
        int xpBarTop = h - HudLayout.VANILLA_XP_BAR_TOP_OFFSET;
        int gap = HudLayout.STAT_TRAINING_GAP_ABOVE_XP;
        int trainH = HudLayout.STAT_TRAINING_BAR_HEIGHT;
        int fatH = HudLayout.STAT_FATIGUE_BAR_HEIGHT;
        int barW = 182;
        int left = (w - barW) / 2;

        long now = Util.getMillis();
        boolean showTraining = now <= hideAfterMs && activeLane != ActiveLane.NONE;

        int prog = 0;
        int need = 1;
        boolean maxed = false;
        int fillCol = 0xFFFFFFFF;
        if (showTraining) {
            switch (activeLane) {
                case DMG -> {
                    prog = dmgProg;
                    need = dmgNeed;
                    maxed = dmgMaxed;
                    fillCol = dmgMaxed ? 0xFFC9A04A : 0xFFCC6644;
                }
                case SPD -> {
                    prog = spdProg;
                    need = spdNeed;
                    maxed = spdMaxed;
                    fillCol = spdMaxed ? 0xFFC9A04A : 0xFF44AACC;
                }
                case HP -> {
                    prog = hpProg;
                    need = hpNeed;
                    maxed = hpMaxed;
                    fillCol = hpMaxed ? 0xFFC9A04A : 0xFF55BB66;
                }
                default -> showTraining = false;
            }
        }

        if (showTraining && maxed) {
            showTraining = false;
        }

        int yTrainTop = xpBarTop - gap - trainH;
        int yFatTop;
        if (showTraining) {
            yFatTop = yTrainTop - fatH;
        } else {
            yFatTop = xpBarTop - gap - fatH;
        }

        RenderSystem.enableBlend();

        if (globalFatigue > 0) {
            float fFill = Mth.clamp((float) globalFatigue / (float) fatigueCap, 0f, 1f);
            int fCol = fatigueTierColor(fatigueTier);
            drawBarStrip(gui, left, yFatTop, barW, fatH, fFill, 0xE0282828, fCol);
        }

        if (showTraining) {
            float fill = Mth.clamp((float) prog / (float) need, 0f, 1f);
            drawBarStrip(gui, left, yTrainTop, barW, trainH, fill, 0xE0282828, fillCol);
        }

        RenderSystem.disableBlend();
    }

    private static int fatigueTierColor(int tier) {
        return switch (Mth.clamp(tier, 0, 4)) {
            case 0 -> 0xFF7aab8e;
            case 1 -> 0xFFd4b84a;
            case 2 -> 0xFFe88838;
            case 3 -> 0xFFe84830;
            default -> 0xFFc02828;
        };
    }

    private static void drawBarStrip(GuiGraphics g, int x, int y, int w, int h, float fill, int bg, int fillCol) {
        g.fill(x, y, x + w, y + h, bg);
        int fw = Mth.ceil(w * fill);
        if (fill > 0f && fw < 1) fw = 1;
        fw = Math.min(fw, w);
        if (fw > 0) g.fill(x, y, x + fw, y + h, fillCol);
        g.renderOutline(x, y, w, h, 0xFF101010);
    }

    private ClientStatTrainingHud() {}
}
