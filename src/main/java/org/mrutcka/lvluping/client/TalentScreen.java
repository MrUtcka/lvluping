package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.*;
import org.mrutcka.lvluping.network.*;
import com.mojang.math.Axis;

import java.util.*;

import static org.mrutcka.lvluping.data.PlayerLevels.getRace;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");

    public static int clientLevel = 0;
    public static int clientStars = 2;
    public static Set<String> clientTalents = new HashSet<>();
    public static Map<String, Integer> clientStats = new HashMap<>();
    public static Race clientRace = Race.HUMAN;

    private float scrollX = 0, scrollY = 0, zoom = 0.3f;
    private boolean isStatsTab = false;

    public TalentScreen() {
        super(Component.literal("Меню Развития"));
    }

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
            renderTalentsArea(gui, availablePoints, talentCount, talentLimit);
        }
        gui.pose().popPose();

        renderHUD(gui, availablePoints, talentCount, talentLimit);
        drawTab(gui, "ТАЛАНТЫ", 10, 10, !isStatsTab);
        drawTab(gui, "ХАРАКТЕРИСТИКИ", 115, 10, isStatsTab);

        renderTooltips(gui, mouseX, mouseY, talentCount, talentLimit);
    }

    private void renderTalentsArea(GuiGraphics gui, int availablePoints, long currentCount, int limit) {
        for (Talent t : Talent.values()) {
            for (Talent parent : t.parents) {
                boolean parentUnlocked = clientTalents.contains(parent.id);
                boolean branchBlocked = isBranchBlocked(t);

                int color;
                if (parentUnlocked && !branchBlocked) {
                    color = 0xFFFFAA00;
                } else {
                    color = 0xFF444444;
                }

                drawOptimizedLine(gui, t.x, t.y, parent.x, parent.y, color);
            }
        }

        for (Talent t : Talent.values()) {
            boolean isUnlocked = clientTalents.contains(t.id);

            boolean hasUnlockedParent = (t.parents.length == 0) ||
                    Arrays.stream(t.parents).anyMatch(p -> clientTalents.contains(p.id));

            boolean branchBlocked = isBranchBlocked(t);
            boolean canAfford = availablePoints >= t.cost;
            boolean underLimit = currentCount < limit;

            boolean raceForbidden = false;
            for (Race r : t.forbiddenRaces) {
                if (r == clientRace) {
                    raceForbidden = true;
                    break;
                }
            }

            boolean canPurchase = !isUnlocked && hasUnlockedParent && !branchBlocked && !raceForbidden && underLimit && canAfford;

            int bgColor = isUnlocked ? 0xFF00AA00 : (branchBlocked || raceForbidden || !hasUnlockedParent ? 0xFF222222 : 0xFF444444);
            int outlineColor = isUnlocked ? 0xFFAAFF00 : (canPurchase ? 0xFFFFFFFF : 0xFF555555);

            int halfSize = 74;
            gui.fill(t.x - halfSize, t.y - halfSize, t.x + halfSize, t.y + halfSize, bgColor);
            gui.renderOutline(t.x - halfSize, t.y - halfSize, halfSize * 2, halfSize * 2, outlineColor);

            if (!isUnlocked && !canPurchase) {
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            gui.blit(t.icon, t.x - 64, t.y - 64, 0, 0, 128, 128, 128, 128);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderTooltips(GuiGraphics gui, int mx, int my, long currentCount, int limit) {
        float rx = (mx - width / 2f - scrollX) / zoom;
        float ry = (my - height / 2f - scrollY) / zoom;

        if (!isStatsTab) {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + t.label));
                    tip.add(Component.literal("§7" + t.description));
                    tip.add(Component.literal("§bЦена: " + t.cost));

                    if (clientTalents.contains(t.id)) {
                        tip.add(Component.literal("§aИзучено"));
                    } else {
                        boolean hasUnlockedParent = (t.parents.length == 0) ||
                                Arrays.stream(t.parents).anyMatch(p -> clientTalents.contains(p.id));

                        if (!hasUnlockedParent && t.parents.length > 0) {
                            tip.add(Component.literal("§cНужен родительский навык"));
                        }
                        if (isBranchBlocked(t)) tip.add(Component.literal("§8Путь заблокирован выбором другой ветки"));
                        if (currentCount >= limit) tip.add(Component.literal("§cЛимит навыков исчерпан"));

                        boolean raceForbidden = Arrays.asList(t.forbiddenRaces).contains(clientRace);
                        if (raceForbidden) {
                            tip.add(Component.literal("§cВаша раса (" + clientRace.label + ") не может обуздать эту силу"));
                        }
                    }
                    gui.renderComponentTooltip(font, tip, mx, my);
                    break;
                }
            }
        }
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

    private void renderStatsArea(GuiGraphics gui, int points) {
        int startY = -220;
        for (AttributeStat s : AttributeStat.values()) {
            int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
            int x = -500;

            gui.fill(x, startY, x + 1000, startY + 150, 0xAA000000);
            gui.renderOutline(x, startY, 1000, 150, 0xFFFFFFFF);

            if (s.icon != null) {
                gui.blit(s.icon, x + 15, startY + 15, 0, 0, 120, 120, 120, 120);
            }

            gui.pose().pushPose();
            gui.pose().translate(x + 150, startY + 55, 0);
            gui.pose().scale(2.5f, 2.5f, 2.5f);
            gui.drawString(font, s.label + " [" + level + "/" + s.maxLevel + "]", 0, 0, 0xFFFFFF);
            gui.pose().popPose();

            boolean canUpgrade = points > 0 && level < s.maxLevel;
            int buttonX = x + 850;
            int buttonY = startY + 15;

            gui.fill(buttonX, buttonY, buttonX + 120, buttonY + 120, canUpgrade ? 0xFF00AA00 : 0xFF555555);
            gui.renderOutline(buttonX, buttonY, 120, 120, 0xFFFFFFFF);

            gui.pose().pushPose();
            gui.pose().translate(buttonX + 35, buttonY + 20, 0);
            gui.pose().scale(5.0f, 5.0f, 5.0f);
            gui.drawString(font, "+", 0, 0, canUpgrade ? 0xFFFFFFFF : 0xFFAAAAAA);
            gui.pose().popPose();

            startY += 170;
        }
    }

    private void renderHUD(GuiGraphics gui, int points, long count, int limit) {
        gui.fill(0, height - 35, width, height, 0xDD000000);
        gui.renderOutline(0, height - 35, width, 1, 0xFFAAAAAA);
        gui.drawString(font, "Уровень: §f" + clientLevel + " §e" + "★".repeat(clientStars), 15, height - 25, 0xFFFFFF);
        gui.drawString(font, "Очки: §b" + points, width / 2 - 20, height - 25, 0xFFFFFF);
        gui.drawString(font, "Лимит: " + count + "/" + limit, width - 100, height - 25, 0xFFFFFF);
    }

    private int getAvailablePoints() {
        int spentOnTalents = clientTalents.stream().map(Talent::getById).filter(Objects::nonNull).mapToInt(t -> t.cost).sum();
        int spentOnStats = clientStats.values().stream().mapToInt(Integer::intValue).sum();
        return clientLevel - (spentOnTalents + spentOnStats);
    }

    private long getTalentCount() {
        return clientTalents.stream().filter(id -> !id.equals("start")).count();
    }

    private boolean isBranchBlocked(Talent t) {
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;
        for (String ownedId : clientTalents) {
            Talent owned = Talent.getById(ownedId);
            if (owned == null || owned.branch.isEmpty() || owned == t) continue;
            if (t.branch.equals(owned.branch)) {
                if (!isSameHierarchy(t, owned)) return true;
            }
        }
        return false;
    }

    private boolean isAncestor(Talent potentialAncestor, Talent target) {
        for (Talent parent : target.parents) {
            if (parent == potentialAncestor || isAncestor(potentialAncestor, parent)) return true;
        }
        return false;
    }

    private boolean isSameHierarchy(Talent a, Talent b) {
        return isAncestor(a, b) || isAncestor(b, a);
    }

    private void drawTab(GuiGraphics gui, String text, int x, int y, boolean active) {
        gui.fill(x, y, x + 100, y + 22, active ? 0xFFFFAA00 : 0xFF222222);
        gui.renderOutline(x, y, 100, 22, 0xFFFFFFFF);
        gui.drawString(font, text, x + 5, y + 7, active ? 0xFF000000 : 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (my >= 10 && my <= 32) {
            if (mx >= 10 && mx <= 110) { isStatsTab = false; return true; }
            if (mx >= 115 && mx <= 215) { isStatsTab = true; return true; }
        }

        float rx = (float) (mx - width / 2f - scrollX) / zoom;
        float ry = (float) (my - height / 2f - scrollY) / zoom;

        if (isStatsTab) {
            int startY = -220;
            for (AttributeStat s : AttributeStat.values()) {
                if (rx >= 350 && rx <= 470 && ry >= startY + 15 && ry <= startY + 135) {
                    if (getAvailablePoints() >= 1) PacketDistributor.sendToServer(new C2SUpgradeStat(s.id));
                    return true;
                }
                startY += 170;
            }
        } else {
            for (Talent t : Talent.values()) {
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    if (!clientTalents.contains(t.id)) {
                        PacketDistributor.sendToServer(new C2SPurchaseTalent(t.id));
                    }
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