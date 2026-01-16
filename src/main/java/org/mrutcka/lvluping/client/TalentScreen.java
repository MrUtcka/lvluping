package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.*;
import org.mrutcka.lvluping.network.*;
import com.mojang.math.Axis; // Проверьте этот импорт

import java.util.*;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");
    public static int clientLevel = 0, clientStars = 2;
    public static Set<String> clientTalents = new HashSet<>();
    public static Map<String, Integer> clientStats = new HashMap<>();

    private float scrollX = 0, scrollY = 0, zoom = 0.3f;
    private boolean isStatsTab = false;

    public TalentScreen() { super(Component.literal("Меню Развития")); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int availablePoints = getAvailablePoints();
        long talentCount = getTalentCount();
        int talentLimit = PlayerLevels.getTalentLimit(clientStars);

        renderRepeatingBackground(gui);

        gui.pose().pushPose();
        gui.pose().translate(width / 2f + scrollX, height / 2f + scrollY, 0);
        gui.pose().scale(zoom, zoom, 1.0f);

        if (isStatsTab) {
            renderStatsArea(gui, availablePoints);
        } else {
            renderTalentsArea(gui, talentCount, talentLimit);
        }
        gui.pose().popPose();

        renderHUD(gui, availablePoints, talentCount, talentLimit);
        drawTab(gui, "ТАЛАНТЫ", 10, 10, !isStatsTab);
        drawTab(gui, "ХАРАКТЕРИСТИКИ", 115, 10, isStatsTab);

        renderTooltips(gui, mouseX, mouseY, talentCount, talentLimit);
    }

    private void renderRepeatingBackground(GuiGraphics gui) {
        RenderSystem.setShaderTexture(0, BG);

        int textureSize = 16;

        int u = Math.round(-scrollX) % textureSize;
        int v = Math.round(-scrollY) % textureSize;

        gui.blit(BG, 0, 0, (float)u, (float)v, width, height, textureSize, textureSize);
    }

    private void drawOptimizedLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float angle = (float) Math.atan2(dy, dx);
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        gui.pose().pushPose();
        gui.pose().translate(x1, y1, 0);
        gui.pose().mulPose(Axis.ZP.rotation(angle));
        gui.fill(0, -2, (int)length, 2, color);
        gui.pose().popPose();
    }

    private void renderTalentsArea(GuiGraphics gui, long currentCount, int limit) {
        for (Talent t : Talent.values()) {
            if (t.parent != null) {
                int color = clientTalents.contains(t.parent.id) ? 0xFFFFAA00 : 0xFF444444;
                drawOptimizedLine(gui, t.x, t.y, t.parent.x, t.parent.y, color);
            }
        }

        for (Talent t : Talent.values()) {
            boolean unlocked = clientTalents.contains(t.id);
            boolean parentUnlocked = (t.parent == null || clientTalents.contains(t.parent.id));
            boolean branchBlocked = isBranchBlocked(t);
            boolean canPurchase = parentUnlocked && !branchBlocked && (!unlocked && currentCount < limit);

            int bgColor = unlocked ? 0xFF00AA00 : (branchBlocked || !parentUnlocked ? 0xFF330000 : 0xFF444444);
            int halfSize = 74;

            gui.fill(t.x - halfSize, t.y - halfSize, t.x + halfSize, t.y + halfSize, bgColor);
            int outlineColor = unlocked ? 0xFFAAFF00 : (canPurchase ? 0xFFFFFFFF : 0xFF555555);
            gui.renderOutline(t.x - halfSize, t.y - halfSize, halfSize * 2, halfSize * 2, outlineColor);

            if (!unlocked && (branchBlocked || !parentUnlocked)) {
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
            }

            gui.blit(t.icon, t.x - 64, t.y - 64, 0, 0, 128, 128, 128, 128);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderStatsArea(GuiGraphics gui, int points) {
        int startY = -220;
        for (AttributeStat s : AttributeStat.values()) {
            int level = clientStats.getOrDefault(s.id, 0);
            int x = -500;
            int y = startY;
            gui.fill(x, y, x + 1000, y + 150, 0xAA000000);
            gui.renderOutline(x, y, 1000, 150, 0xFFFFFFFF);
            if (s.icon != null) gui.blit(s.icon, x + 15, y + 15, 0, 0, 120, 120, 120, 120);
            gui.pose().pushPose();
            gui.pose().translate(x + 150, y + 55, 0);
            gui.pose().scale(2.5f, 2.5f, 2.5f);
            gui.drawString(font, s.label + " [" + level + "/" + s.maxLevel + "]", 0, 0, 0xFFFFFF);
            gui.pose().popPose();
            int btnX = x + 850;
            int btnY = y + 15;
            boolean canUpgrade = points > 0 && level < s.maxLevel;
            gui.fill(btnX, btnY, btnX + 120, btnY + 120, canUpgrade ? 0xFF00AA00 : 0xFF555555);
            gui.renderOutline(btnX, btnY, 120, 120, 0xFFFFFFFF);
            gui.pose().pushPose();
            gui.pose().translate(btnX + 40, btnY + 30, 0);
            gui.pose().scale(4.0f, 4.0f, 4.0f);
            gui.drawString(font, "+", 0, 0, 0xFFFFFF);
            gui.pose().popPose();
            startY += 170;
        }
    }

    private void renderHUD(GuiGraphics gui, int points, long count, int limit) {
        gui.fill(0, height - 35, width, height, 0xDD000000);
        gui.renderOutline(0, height - 35, width, 1, 0xFFAAAAAA);
        gui.drawString(font, "Уровень: §f" + clientLevel + " §e" + "★".repeat(clientStars), 15, height - 25, 0xFFFFFF);
        String pts = "Очки: §b" + points;
        gui.drawString(font, pts, width / 2 - font.width(pts) / 2, height - 25, 0xFFFFFF);
        String lim = "Лимит: " + count + "/" + limit;
        gui.drawString(font, lim, width - font.width(lim) - 15, height - 25, 0xFFFFFF);
    }

    private int getAvailablePoints() {
        int spentOnTalents = clientTalents.stream().map(Talent::getById).filter(Objects::nonNull).mapToInt(t -> t.cost).sum();
        int spentOnStats = clientStats.values().stream().mapToInt(Integer::intValue).sum();
        return clientLevel - (spentOnTalents + spentOnStats);
    }

    private long getTalentCount() { return clientTalents.stream().filter(id -> !id.equals("start")).count(); }

    private void renderTooltips(GuiGraphics gui, int mx, int my, long currentCount, int limit) {
        float rx = (mx - width / 2f - scrollX) / zoom;
        float ry = (my - height / 2f - scrollY) / zoom;
        if (isStatsTab) {
            int startY = -220;
            for (AttributeStat s : AttributeStat.values()) {
                if (rx >= -500 && rx <= 500 && ry >= startY && ry <= startY + 150) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + s.label));
                    tip.add(Component.literal("§7" + s.description));
                    int currentLevel = clientStats.getOrDefault(s.id, 0);
                    tip.add(currentLevel >= s.maxLevel ? Component.literal("§cМаксимальный уровень достигнут") : Component.literal("§bЦена улучшения: 1 очко"));
                    gui.renderComponentTooltip(font, tip, mx, my);
                    return;
                }
                startY += 170;
            }
        } else {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + t.label));
                    tip.add(Component.literal("§7" + t.description));
                    tip.add(Component.literal("§bЦена: " + t.cost));
                    if (clientTalents.contains(t.id)) {
                        tip.add(Component.literal("§aИзучено"));
                    } else {
                        if (t.parent != null && !clientTalents.contains(t.parent.id)) tip.add(Component.literal("§8Требуется: " + t.parent.label));
                        if (isBranchBlocked(t)) tip.add(Component.literal("§8Путь заблокирован другим классом"));
                        if (currentCount >= limit) tip.add(Component.literal("§cДостигнут лимит навыков звезды"));
                    }
                    gui.renderComponentTooltip(font, tip, mx, my);
                    break;
                }
            }
        }
    }

    private boolean isBranchBlocked(Talent t) {
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;
        for (String ownedId : clientTalents) {
            Talent owned = Talent.getById(ownedId);
            if (owned == null || owned.branch.isEmpty()) continue;
            if (t.branch.equals(owned.branch)) {
                if (!isSameHierarchy(t, owned)) {
                    if (t.parent == owned.parent || isDivergent(t, owned)) return true;
                }
            }
        }
        return false;
    }
    private boolean isAncestor(Talent a, Talent target) {
        Talent current = target.parent;
        while (current != null) { if (current == a) return true; current = current.parent; }
        return false;
    }
    private boolean isSameHierarchy(Talent a, Talent b) { return isAncestor(a, b) || isAncestor(b, a); }
    private boolean isDivergent(Talent t, Talent owned) {
        for (Talent pA = t.parent; pA != null; pA = pA.parent) {
            for (Talent pB = owned.parent; pB != null; pB = pB.parent) {
                if (pA == pB) {
                    Talent cT = findChildTowards(pA, t), cO = findChildTowards(pA, owned);
                    if (cT != null && cO != null && cT != cO) return true;
                }
            }
        }
        return false;
    }
    private Talent findChildTowards(Talent ancestor, Talent target) {
        Talent curr = target;
        while (curr != null && curr.parent != ancestor) curr = curr.parent;
        return curr;
    }
    private void drawTab(GuiGraphics gui, String text, int x, int y, boolean active) {
        gui.fill(x, y, x + 90, y + 22, active ? 0xFFFFAA00 : 0xFF222222);
        gui.renderOutline(x, y, 90, 22, 0xFFFFFFFF);
        gui.drawString(font, text, x + 45 - font.width(text) / 2, y + 7, active ? 0xFF000000 : 0xFFFFFFFF);
    }
    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (my >= 10 && my <= 32) {
            if (mx >= 10 && mx <= 100) { isStatsTab = false; return true; }
            if (mx >= 115 && mx <= 215) { isStatsTab = true; return true; }
        }
        if (my > height - 35) return super.mouseClicked(mx, my, btn);
        float rx = (float) (mx - width / 2f - scrollX) / zoom;
        float ry = (float) (my - height / 2f - scrollY) / zoom;
        if (isStatsTab) {
            int startY = -220;
            for (AttributeStat s : AttributeStat.values()) {
                if (rx >= 350 && rx <= 470 && ry >= startY + 15 && ry <= startY + 135) {
                    if (getAvailablePoints() >= 1) ModNetworking.CHANNEL.send(new C2SUpgradeStat(s.id), PacketDistributor.SERVER.noArg());
                    return true;
                }
                startY += 170;
            }
        } else {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    if (getAvailablePoints() >= t.cost) ModNetworking.CHANNEL.send(new C2SPurchaseTalent(t.id), PacketDistributor.SERVER.noArg());
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }
    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { scrollX += dx; scrollY += dy; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { zoom = (sy > 0) ? Math.min(zoom + 0.1f, 2.0f) : Math.max(zoom - 0.1f, 0.1f); return true; }
    @Override public boolean isPauseScreen() { return false; }
}