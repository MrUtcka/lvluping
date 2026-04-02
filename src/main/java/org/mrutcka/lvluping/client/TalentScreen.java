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
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;

import java.util.*;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");
    private static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/lock.png");
    private static final ResourceLocation UPGRADE_ICON = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/upgrade.png");
    private static final ResourceLocation UPGRADE_ICON_FALLBACK = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/lock.png");

    public static int clientLevel = 0;
    public static int clientStars = 2;
    public static Set<String> clientTalents = new HashSet<>();
    public static Map<String, Integer> clientStats = new HashMap<>();
    public static Map<String, Integer> clientAbilityLevels = new HashMap<>();
    public static Race clientRace = Race.HUMAN;

    private float scrollX = 0, scrollY = 0, zoom = 0.3f;
    private boolean isStatsTab = false;

    public TalentScreen() {
        super(Component.literal("Меню Развития"));
    }

    @Override
    protected void init() {
        super.init();
        String chosen = getChosenClassBaseIdClient();
        if (chosen != null) {
            Talent base = Talent.getById(chosen);
            if (base != null) {
                float desiredY = height * 0.25f;
                scrollX = -base.x * zoom;
                scrollY = (desiredY - height / 2f) - base.y * zoom;
            }
        }
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
            if (!isTalentVisible(t)) continue;
            for (Talent parent : t.parents) {
                boolean parentUnlocked = clientTalents.contains(parent.id);
                boolean branchBlocked = isBranchBlocked(t);

                int color;
                if (parentUnlocked && !branchBlocked) {
                    color = 0xFFFFFFAA;
                } else {
                    color = 0xFF444444;
                }

                drawOptimizedLine(gui, t.x, t.y, parent.x, parent.y, color);
            }
        }

        for (Talent t : Talent.values()) {
            if (!isTalentVisible(t)) continue;
            boolean isUnlocked = clientTalents.contains(t.id);

            boolean hasUnlockedParent = hasUnlockedParentForPurchase(t);

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

            drawTalentShape(gui, t, bgColor, outlineColor);

            if (!isUnlocked && !canPurchase) {
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            gui.blit(t.icon, t.x - 64, t.y - 64, 0, 0, 128, 128, 128, 128);

            if (!isUnlocked) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                gui.blit(LOCK_ICON, t.x - 32, t.y - 32, 0, 0, 64, 64, 64, 64);
            } else if (AbilityUpgradeConfig.has(t.id) && AbilityUpgradeConfig.isUpgradeable(t.id)) {
                int lvl = clientAbilityLevels.getOrDefault(t.id, 1);
                int max = AbilityUpgradeConfig.getMaxLevel(t.id);
                if (lvl < max) {
                    int cost = AbilityUpgradeConfig.getUpgradePointCost(t.id, lvl + 1);
                    if (availablePoints >= cost) {
                        int w = 128;
                        int h = 128;
                        int px = t.x - 64;
                        int py = t.y - 64;
                        try {
                            gui.blit(UPGRADE_ICON, px, py, 0, 0, w, h, w, h);
                        } catch (Exception e) {
                            gui.blit(UPGRADE_ICON_FALLBACK, px + 4, py + 4, 0, 0, w - 8, h - 8, 64, 64);
                        }
                    }
                }
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void drawTalentShape(GuiGraphics gui, Talent t, int bgColor, int outlineColor) {
        int x = t.x;
        int y = t.y;
        int halfSize = 74;
        String id = t.id.toLowerCase();
        String branch = t.branch.toLowerCase();

        if (id.contains("archer") || branch.contains("archer") || id.startsWith("a_")) {
            drawPolygon(gui, x, y, halfSize, 6, 90f, bgColor, outlineColor);
        } else if (id.contains("mage") || branch.contains("mage") || id.startsWith("m_")) {
            drawPolygon(gui, x, y, halfSize, 32, 0f, bgColor, outlineColor);
        } else if (id.contains("assassin") || branch.contains("assassin") || id.startsWith("as_")) {
            drawPolygon(gui, x, y, (int)(halfSize * 1.1), 4, 0f, bgColor, outlineColor);
        } else {
            drawPolygon(gui, x, y, (int)(halfSize * 1.414), 4, 45f, bgColor, outlineColor);
        }
    }

    private void drawPolygon(GuiGraphics gui, int x, int y, int radius, int sides, float rotationDeg, int color, int outlineColor) {
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = tesselator.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_FAN, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);

        float angleStep = (float) (2 * Math.PI / sides);
        float offset = (float) Math.toRadians(rotationDeg);

        buffer.addVertex(gui.pose().last().pose(), (float)x, (float)y, 0).setColor(color);
        for (int i = 0; i <= sides; i++) {
            float angle = i * angleStep + offset;
            float vx = x + (float) Math.cos(angle) * radius;
            float vy = y + (float) Math.sin(angle) * radius;
            buffer.addVertex(gui.pose().last().pose(), vx, vy, 0).setColor(color);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());

        for (int i = 0; i < sides; i++) {
            float a1 = i * angleStep + offset;
            float a2 = (i + 1) * angleStep + offset;
            int x1 = x + (int) (Math.cos(a1) * radius);
            int y1 = y + (int) (Math.sin(a1) * radius);
            int x2 = x + (int) (Math.cos(a2) * radius);
            int y2 = y + (int) (Math.sin(a2) * radius);

            drawOptimizedLine(gui, x1, y1, x2, y2, outlineColor);
        }
    }

    private void renderTooltips(GuiGraphics gui, int mx, int my, long currentCount, int limit) {
        float rx = (mx - width / 2f - scrollX) / zoom;
        float ry = (my - height / 2f - scrollY) / zoom;

        if (!isStatsTab) {
            for (Talent t : Talent.values()) {
                if (!isTalentVisible(t)) continue;
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + t.label));
                    if (clientTalents.contains(t.id)) {
                        tip.add(Component.literal("§7" + t.description));
                        if (AbilityUpgradeConfig.has(t.id) && AbilityUpgradeConfig.isUpgradeable(t.id)) {
                            int lvl = clientAbilityLevels.getOrDefault(t.id, 1);
                            int max = AbilityUpgradeConfig.getMaxLevel(t.id);
                            tip.add(Component.literal("§eУровень: " + lvl + "/" + max));
                        }
                        tip.add(Component.literal("§aИзучено"));
                    } else {
                        tip.add(Component.literal("§7" + t.description));
                        addMutualExclusionTooltip(tip, t);
                        tip.add(Component.literal("§bЦена: " + t.cost));
                        boolean hasUnlockedParent = hasUnlockedParentForPurchase(t);

                        if (!hasUnlockedParent && t.parents.length > 0) {
                            if (t.requiresAllParents()) {
                                ArrayList<String> missing = new ArrayList<>();
                                for (Talent p : t.parents) {
                                    if (!clientTalents.contains(p.id)) missing.add(p.label);
                                }
                                if (!missing.isEmpty()) {
                                    tip.add(Component.literal("§cНе изучено: " + String.join(", ", missing)));
                                }
                            } else {
                                tip.add(Component.literal("§cНужен родительский навык"));
                            }
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
        } else {
            for (AttributeStat s : AttributeStat.values()) {
                if (rx >= s.x - 74 && rx <= s.x + 74 && ry >= s.y - 74 && ry <= s.y + 74) {
                    int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + s.label));
                    tip.add(Component.literal("§7" + s.description));
                    tip.add(Component.literal("§eУровень: " + level + "/" + s.maxLevel));
                    if (getAvailablePoints() < 1) tip.add(Component.literal("§cНет очков"));
                    if (level >= s.maxLevel) tip.add(Component.literal("§aМаксимум"));
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
        for (AttributeStat s : AttributeStat.values()) {
            int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
            boolean canUpgrade = points > 0 && level < s.maxLevel;
            int bgColor = 0xFF222222;
            int outlineColor = canUpgrade ? 0xFFFFFFFF : 0xFF555555;

            drawPolygon(gui, s.x, s.y, 120, 32, 0f, bgColor, outlineColor);
            if (!canUpgrade) {
                RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
            gui.blit(s.icon, s.x - 64, s.y - 64, 0, 0, 128, 128, 128, 128);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            gui.pose().pushPose();
            gui.pose().translate(s.x - 62, s.y + 76, 0);
            gui.pose().scale(1.2f, 1.2f, 1.2f);
            gui.drawString(font, s.label + " " + level + "/" + s.maxLevel, 0, 0, 0xFFFFFF);
            gui.pose().popPose();

            if (canUpgrade) {
                gui.blit(UPGRADE_ICON, s.x - 64, s.y - 64, 0, 0, 128, 128, 128, 128);
            }
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
        int spentOnTalents = clientTalents.stream()
                .map(Talent::getById)
                .filter(Objects::nonNull)
                .filter(t -> !isFreeClassTalent(t.id))
                .mapToInt(t -> t.cost)
                .sum();
        int spentOnStats = clientStats.values().stream().mapToInt(Integer::intValue).sum();
        int spentOnUpgrades = 0;
        for (var e : clientAbilityLevels.entrySet()) {
            String id = e.getKey();
            int lvl = e.getValue() == null ? 0 : e.getValue();
            if (lvl <= 1) continue;
            for (int next = 2; next <= lvl; next++) {
                spentOnUpgrades += AbilityUpgradeConfig.getUpgradePointCost(id, next);
            }
        }
        return clientLevel - (spentOnTalents + spentOnStats + spentOnUpgrades);
    }

    private static boolean isFreeClassTalent(String id) {
        return "start".equals(id) || "warrior_base".equals(id) || "archer_base".equals(id) || "mage_base".equals(id) || "assassin_base".equals(id);
    }

    private long getTalentCount() {
        return clientTalents.stream().filter(id -> !isFreeClassTalent(id)).count();
    }

    private boolean isBranchBlocked(Talent t) {
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;
        for (String ownedId : clientTalents) {
            Talent owned = Talent.getById(ownedId);
            if (owned == null || owned.branch.isEmpty() || owned == t) continue;
            if (t.branch.equals(owned.branch)) {
                if (!Talent.isSameHierarchy(t, owned)) return true;
            }
        }
        return false;
    }

    private static void addMutualExclusionTooltip(List<Component> tip, Talent t) {
        List<Talent> peers = Talent.mutuallyExclusivePeers(t);
        if (peers.isEmpty()) return;
        ArrayList<String> labels = new ArrayList<>(peers.size());
        for (Talent p : peers) labels.add(p.label);
        tip.add(Component.literal("§7Взаимоисключается с: §f" + String.join(", ", labels)));
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
            for (AttributeStat s : AttributeStat.values()) {
                if (rx >= s.x - 74 && rx <= s.x + 74 && ry >= s.y - 74 && ry <= s.y + 74) {
                    int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
                    if (getAvailablePoints() >= 1 && level < s.maxLevel) PacketDistributor.sendToServer(new C2SUpgradeStat(s.id));
                    return true;
                }
            }
        } else {
            for (Talent t : Talent.values()) {
                if (!isTalentVisible(t)) continue;
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    if (!clientTalents.contains(t.id)) {
                        PacketDistributor.sendToServer(new C2SPurchaseTalent(t.id));
                    } else if (AbilityUpgradeConfig.has(t.id)) {
                        PacketDistributor.sendToServer(new C2SUpgradeAbility(t.id));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean isTalentVisible(Talent t) {
        if (clientTalents.contains(t.id)) return true;
        if (t.parents.length == 0) return true;
        String chosen = getChosenClassBaseIdClient();
        if (chosen == null) return false;
        if (Arrays.stream(t.parents).anyMatch(p -> p.id.equals("start"))) return t.id.equals(chosen);
        if (t.id.equals(chosen)) return true;
        return isDescendantOfChosen(t, chosen);
    }

    public static String getChosenClassBaseIdClient() {
        if (clientTalents.contains("m_cleric_base")) return "m_cleric_base";
        if (clientTalents.contains("m_summoner_base")) return "m_summoner_base";
        if (clientTalents.contains("m_spellcaster_base")) return "m_spellcaster_base";
        if (clientTalents.contains("a_hunter_base")) return "a_hunter_base";
        if (clientTalents.contains("a_ranger_base")) return "a_ranger_base";
        if (clientTalents.contains("a_musketeer_base")) return "a_musketeer_base";

        if (clientTalents.contains("warrior_base")) return "warrior_base";
        if (clientTalents.contains("archer_base")) return "archer_base";
        if (clientTalents.contains("mage_base")) return "mage_base";
        if (clientTalents.contains("assassin_base")) return "assassin_base";
        return null;
    }

    private boolean isDescendantOfChosen(Talent t, String chosenId) {
        for (Talent p : t.parents) {
            if (p.id.equals(chosenId)) return true;
            if (isDescendantOfChosen(p, chosenId)) return true;
        }
        return false;
    }

    private boolean hasUnlockedParentForPurchase(Talent t) {
        return t.parentsSatisfiedForPurchase(clientTalents);
    }

    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { scrollX += dx; scrollY += dy; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { zoom = (sy > 0) ? Math.min(zoom + 0.1f, 2.0f) : Math.max(zoom - 0.1f, 0.1f); return true; }
    @Override public boolean isPauseScreen() { return false; }
}