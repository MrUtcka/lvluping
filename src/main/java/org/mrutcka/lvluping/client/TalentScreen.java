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

import java.util.*;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");
    public static int clientLevel = 0, clientStars = 2;
    public static Set<String> clientTalents = new HashSet<>();
    public static Map<String, Integer> clientStats = new HashMap<>();

    private float scrollX = 0, scrollY = 0, zoom = 1.0f;
    private boolean isStatsTab = false;

    public TalentScreen() { super(Component.literal("Меню Развития")); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderRepeatingBackground(gui);

        gui.pose().pushPose();
        gui.pose().translate(width / 2f + scrollX, height / 2f + scrollY, 0);
        gui.pose().scale(zoom, zoom, 1.0f);
        if (isStatsTab) {
            renderStatsArea(gui);
        } else {
            renderTalentsArea(gui);
        }
        gui.pose().popPose();

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 100);

        renderHUD(gui);

        int tabY = 10;
        drawTab(gui, "ТАЛАНТЫ", 10, tabY, !isStatsTab);
        drawTab(gui, "ХАРАКТЕРИСТИКИ", 115, tabY, isStatsTab);

        gui.pose().popPose();

        renderTooltips(gui, mouseX, mouseY);
    }

    private void renderStatsArea(GuiGraphics gui) {
        int startY = -60;
        for (AttributeStat s : AttributeStat.values()) {
            int level = clientStats.getOrDefault(s.id, 0);
            int x = -100;
            int y = startY;

            gui.fill(x, y, x + 200, y + 30, 0xAA000000);
            gui.renderOutline(x, y, 200, 30, 0xFFFFFFFF);

            if (s.icon != null) {
                gui.blit(s.icon, x + 7, y + 7, 0, 0, 16, 16, 16, 16);
            }

            gui.drawString(font, s.label + " [" + level + "/" + s.maxLevel + "]", x + 30, y + 11, 0xFFFFFF);

            int btnX = x + 170;
            int btnY = y + 5;
            boolean canUpgrade = getAvailablePoints() > 0 && level < s.maxLevel;

            gui.fill(btnX, btnY, btnX + 20, btnY + 20, canUpgrade ? 0xFF00AA00 : 0xFF555555);
            gui.renderOutline(btnX, btnY, 20, 20, 0xFFFFFFFF);
            gui.drawString(font, "+", btnX + 7, btnY + 6, 0xFFFFFF);

            startY += 35;
        }
    }

    private void renderTalentsArea(GuiGraphics gui) {
        for (Talent t : Talent.values()) {
            if (t.parent != null) {
                int lineColor = clientTalents.contains(t.parent.id) ? 0xFFFFAA00 : 0xFF444444;
                drawLine(gui, t.x, t.y, t.parent.x, t.parent.y, lineColor);
            }
        }

        for (Talent t : Talent.values()) {
            boolean unlocked = clientTalents.contains(t.id);
            boolean parentUnlocked = (t.parent == null || clientTalents.contains(t.parent.id));
            boolean branchBlocked = isBranchBlocked(t);
            boolean limitReached = !unlocked && getTalentCount() >= PlayerLevels.getTalentLimit(clientStars);

            boolean canPurchase = parentUnlocked && !branchBlocked && !limitReached;

            int bgColor;
            if (unlocked) {
                bgColor = 0xFF00AA00;
            } else if (branchBlocked) {
                bgColor = 0xFF330000;
            } else if (!parentUnlocked) {
                bgColor = 0xFF330000;
            } else {
                bgColor = 0xFF444444;
            }

            gui.fill(t.x - 12, t.y - 12, t.x + 12, t.y + 12, bgColor);

            int outlineColor = unlocked ? 0xFFAAFF00 : (canPurchase ? 0xFFFFFFFF : 0xFF555555);
            gui.renderOutline(t.x - 12, t.y - 12, 24, 24, outlineColor);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (!unlocked) {
                if (branchBlocked) {
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
                } else if (!parentUnlocked) {
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
                }
            }

            gui.blit(t.icon, t.x - 8, t.y - 8, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private void renderHUD(GuiGraphics gui) {
        gui.fill(0, height - 35, width, height, 0xDD000000);
        gui.renderOutline(0, height - 35, width, 1, 0xFFAAAAAA);
        gui.drawString(font, "Уровень: §f" + clientLevel + " §e" + "★".repeat(clientStars), 15, height - 25, 0xFFFFFF);
        String pts = "Очки: §b" + getAvailablePoints();
        gui.drawString(font, pts, width / 2 - font.width(pts) / 2, height - 25, 0xFFFFFF);
        String lim = "Лимит: " + getTalentCount() + "/" + PlayerLevels.getTalentLimit(clientStars);
        gui.drawString(font, lim, width - font.width(lim) - 15, height - 25, 0xFFFFFF);
    }

    private void renderTooltips(GuiGraphics gui, int mx, int my) {
        float rx = (mx - width / 2f - scrollX) / zoom;
        float ry = (my - height / 2f - scrollY) / zoom;

        if (!isStatsTab) {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 12 && rx <= t.x + 12 && ry >= t.y - 12 && ry <= t.y + 12) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + t.label));
                    tip.add(Component.literal("§7" + t.description));
                    tip.add(Component.literal("§bЦена: " + t.cost));

                    if (clientTalents.contains(t.id)) {
                        tip.add(Component.literal("§aИзучено"));
                    } else {
                        if (t.parent != null && !clientTalents.contains(t.parent.id)) {
                            tip.add(Component.literal("§8Требуется: " + t.parent.label));
                        }
                        if (isBranchBlocked(t)) {
                            tip.add(Component.literal("§8Путь заблокирован другим классом"));
                        }
                        if (getTalentCount() >= PlayerLevels.getTalentLimit(clientStars)) {
                            tip.add(Component.literal("§cДостигнут лимит навыков звезды"));
                        }
                    }
                    gui.renderComponentTooltip(font, tip, mx, my);
                }
            }
        }
    }

    private int getAvailablePoints() {
        int spentOnTalents = clientTalents.stream()
                .map(Talent::getById)
                .filter(Objects::nonNull)
                .mapToInt(t -> t.cost)
                .sum();

        int spentOnStats = clientStats.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        return clientLevel - (spentOnTalents + spentOnStats);
    }

    private long getTalentCount() { return clientTalents.stream().filter(id -> !id.equals("start")).count(); }

    private boolean isBranchBlocked(Talent t) {
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;

        for (String ownedId : clientTalents) {
            Talent owned = Talent.getById(ownedId);
            if (owned == null || owned.branch.isEmpty()) continue;

            if (t.branch.equals(owned.branch)) {
                if (!isSameHierarchy(t, owned)) {
                    if (t.parent == owned.parent) {
                        return true;
                    }

                    if (isDivergent(t, owned)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAncestor(Talent a, Talent target) {
        if (target == null || a == null) return false;

        Talent current = target.parent;
        while (current != null) {
            if (current == a) return true;
            current = current.parent;
        }
        return false;
    }

    private boolean isSameHierarchy(Talent a, Talent b) {
        return isAncestor(a, b) || isAncestor(b, a);
    }

    private boolean isDivergent(Talent t, Talent owned) {
        Talent parentA = t.parent;
        while (parentA != null) {
            Talent parentB = owned.parent;
            while (parentB != null) {
                if (parentA == parentB) {
                    Talent childToT = findChildTowards(parentA, t);
                    Talent childToOwned = findChildTowards(parentA, owned);
                    if (childToT != null && childToOwned != null && childToT != childToOwned) {
                        return true;
                    }
                }
                parentB = parentB.parent;
            }
            parentA = parentA.parent;
        }
        return false;
    }

    private Talent findChildTowards(Talent ancestor, Talent target) {
        Talent curr = target;
        while (curr != null && curr.parent != ancestor) {
            curr = curr.parent;
        }
        return curr;
    }

    private void drawTab(GuiGraphics gui, String text, int x, int y, boolean active) {
        gui.fill(x, y, x + 90, y + 22, active ? 0xFFFFAA00 : 0xFF222222);
        gui.renderOutline(x, y, 90, 22, 0xFFFFFFFF);
        gui.drawString(font, text, x + 45 - font.width(text) / 2, y + 7, active ? 0xFF000000 : 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (my >= 10 && my <= 30) {
            if (mx >= 10 && mx <= 110) {
                isStatsTab = false;
                return true;
            }
            if (mx >= 115 && mx <= 215) {
                isStatsTab = true;
                return true;
            }
        }

        if (my > height - 60) {
            return super.mouseClicked(mx, my, btn);
        }

        float rx = (float) (mx - width / 2f - scrollX) / zoom;
        float ry = (float) (my - height / 2f - scrollY) / zoom;

        if (isStatsTab) {
            int startY = -60;
            for (AttributeStat s : AttributeStat.values()) {
                int btnX = 70;
                int btnY = startY + 5;
                if (rx >= btnX && rx <= btnX + 20 && ry >= btnY && ry <= btnY + 20) {
                    if (getAvailablePoints() >= 1) {
                        ModNetworking.CHANNEL.send(new C2SUpgradeStat(s.id), PacketDistributor.SERVER.noArg());
                    }
                    return true;
                }
                startY += 35;
            }
        } else {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 12 && rx <= t.x + 12 && ry >= t.y - 12 && ry <= t.y + 12) {
                    if (getAvailablePoints() >= t.cost) {
                        ModNetworking.CHANNEL.send(new C2SPurchaseTalent(t.id), PacketDistributor.SERVER.noArg());
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void renderRepeatingBackground(GuiGraphics gui) {
        int s = 32; int ox = Math.round(scrollX % s), oy = Math.round(scrollY % s);
        for (int x = -s; x < width + s; x += s) for (int y = -s; y < height + s; y += s) gui.blit(BG, x + ox, y + oy, 0, 0, s, s, s, s);
    }

    private void drawLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1; float d = (float) Math.sqrt(dx * dx + dy * dy);
        for (int i = 0; i <= (int) d; i++) {
            float f = i / d; gui.fill((int) (x1 + f * dx) - 1, (int) (y1 + f * dy) - 1, (int) (x1 + f * dx) + 1, (int) (y1 + f * dy) + 1, color);
        }
    }

    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { scrollX += dx; scrollY += dy; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { zoom = (sy > 0) ? Math.min(zoom + 0.1f, 2.0f) : Math.max(zoom - 0.1f, 0.5f); return true; }
    @Override public boolean isPauseScreen() { return false; }
}