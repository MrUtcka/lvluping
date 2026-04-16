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
import net.minecraft.util.Mth;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_wallpaper_4096.png");
    private static final int WALLPAPER_TEX_W = 4096;
    private static final int WALLPAPER_TEX_H = 4096;
    private static final int WALLPAPER_CENTER_Y = -1000;
    private static final ResourceLocation LOCK_ICON = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/lock.png");

    private static final ResourceLocation FRAME_WARRIOR_GOLD = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/kvadrat.png");
    private static final ResourceLocation FRAME_WARRIOR_SILVER = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/kvadrat_notfull.png");
    private static final ResourceLocation FRAME_MAGE_GOLD = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/circle.png");
    private static final ResourceLocation FRAME_MAGE_SILVER = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/circle_notfull.png");
    private static final ResourceLocation FRAME_ARCHER_GOLD = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/6ygl.png");
    private static final ResourceLocation FRAME_ARCHER_SILVER = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/6ygl_notfull.png");
    private static final ResourceLocation FRAME_ASSASSIN_GOLD = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/romb.png");
    private static final ResourceLocation FRAME_ASSASSIN_SILVER = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/frames/romb_notfull.png");

    public static int clientLevel = 0;
    public static int clientBonusPoints = 0;
    public static int clientStatPointsSpent = 0;
    public static int clientStars = 2;
    public static Set<String> clientTalents = new HashSet<>();
    public static Set<String> clientAdminGrantedTalents = new HashSet<>();
    public static int clientTalentBudgetDebt = 0;
    public static Map<String, Integer> clientStats = new HashMap<>();
    public static Map<String, Integer> clientAbilityLevels = new HashMap<>();
    public static Race clientRace = Race.HUMAN;

    private static final int TALENT_FRAME_SIZE = 128;
    private static final int CLASS_ICON_DRAW_SIZE = 64;
    private static final int CLASS_ICON_HALF = CLASS_ICON_DRAW_SIZE / 2;
    private static final int CLASS_BASE_ICON_TEX_SIZE = 64;

    private static final int STAT_POLY_R = 120;
    private static final int STAT_ICON_DRAW = 108;
    private static final int STAT_ICON_HALF = STAT_ICON_DRAW / 2;
    private static final int STAT_LABEL_PANEL_HALF_W = 104;
    private static final int STAT_LABEL_PANEL_TOP = 86;
    private static final int STAT_LABEL_PANEL_H = 38;

    private float scrollX = 0, scrollY = 0, zoom = 0.4f;
    private boolean isStatsTab = false;

    public TalentScreen() {
        super(Component.literal("Меню Развития"));
    }

    @Override
    protected void init() {
        super.init();
        String chosen = getChosenTopClassBaseIdClient();
        if (chosen != null) {
            Talent base = Talent.getById(chosen);
            if (base != null) {
                float desiredY = height * 0.25f;
                scrollX = -base.x * zoom;
                scrollY = (desiredY - height / 2f) - base.y * zoom;
            }
        }
        clampScroll();
    }

    private void clampScroll() {
        float tw = WALLPAPER_TEX_W * zoom;
        float minSX = width / 2f - tw / 2f;
        float maxSX = tw / 2f - width / 2f;
        float loX = Math.min(minSX, maxSX);
        float hiX = Math.max(minSX, maxSX);
        scrollX = Mth.clamp(scrollX, loX, hiX);

        float cy = WALLPAPER_CENTER_Y;
        float topTy = cy - WALLPAPER_TEX_H / 2f;
        float botTy = cy + WALLPAPER_TEX_H / 2f;
        float minSY = height / 2f - botTy * zoom;
        float maxSY = -height / 2f - topTy * zoom;
        float loY = Math.min(minSY, maxSY);
        float hiY = Math.max(minSY, maxSY);
        scrollY = Mth.clamp(scrollY, loY, hiY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int availablePoints = getAvailablePoints();
        long talentCount = getTalentCount();
        int talentLimit = PlayerLevels.getTalentLimit(clientStars);

        renderScreenBackdrop(gui);

        gui.pose().pushPose();
        gui.pose().translate(width / 2f + scrollX, height / 2f + scrollY, 0);
        gui.pose().scale(zoom, zoom, 1.0f);

        renderWallpaperInTreeSpace(gui);

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

                drawTalentConnectionLine(gui, t, parent, color);
            }
        }

        for (Talent t : Talent.values()) {
            if (!isTalentVisible(t)) continue;
            boolean isUnlocked = clientTalents.contains(t.id);

            boolean hasUnlockedParent = hasUnlockedParentForPurchase(t);

            boolean branchBlocked = isBranchBlocked(t);
            boolean canAfford = availablePoints >= t.cost;
            boolean underLimit = currentCount < limit;

            boolean raceForbidden = t.isForbiddenForRace(clientRace);

            boolean canPurchase = !isUnlocked && hasUnlockedParent && !branchBlocked && !raceForbidden && underLimit && canAfford;

            if (!isUnlocked && !canPurchase) {
                RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            drawTalentIconBlit(gui, t);

            if (!isUnlocked) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                gui.blit(LOCK_ICON, t.x - 32, t.y - 32, 0, 0, 64, 64, 64, 64);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (isUnlocked) {
                RenderSystem.enableBlend();
                drawTalentFrame(gui, t);
            } else {
                int outlineColor = canPurchase ? 0xFFFFFFFF : 0xFF444444;
                drawTalentOutline(gui, t, outlineColor);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void drawTalentFrame(GuiGraphics gui, Talent t) {
        int max = 1;
        int lvl = 1;
        if (AbilityUpgradeConfig.has(t.id) && AbilityUpgradeConfig.isUpgradeable(t.id)) {
            lvl = clientAbilityLevels.getOrDefault(t.id, 1);
            max = AbilityUpgradeConfig.getMaxLevel(t.id);
        }
        boolean maxed = lvl >= max;

        ResourceLocation frame = getFrameTexture(t, maxed ? FrameKind.GOLD : FrameKind.SILVER);
        int w = TALENT_FRAME_SIZE;
        int h = TALENT_FRAME_SIZE;
        gui.blit(frame, t.x - w / 2, t.y - h / 2, 0, 0, w, h, w, h);
    }

    private static boolean isTopClassBaseNode(Talent t) {
        String id = t.id;
        return "warrior_base".equals(id) || "mage_base".equals(id) || "archer_base".equals(id) || "assassin_base".equals(id);
    }

    private static void drawTalentIconBlit(GuiGraphics gui, Talent t) {
        var icon = TalentIconUtil.icon(t);
        if (isTopClassBaseNode(t)) {
            gui.blit(icon, t.x - CLASS_ICON_HALF, t.y - CLASS_ICON_HALF, 0, 0, CLASS_ICON_DRAW_SIZE, CLASS_ICON_DRAW_SIZE, CLASS_BASE_ICON_TEX_SIZE, CLASS_BASE_ICON_TEX_SIZE);
        } else {
            int h = TALENT_FRAME_SIZE / 2;
            gui.blit(icon, t.x - h, t.y - h, 0, 0, TALENT_FRAME_SIZE, TALENT_FRAME_SIZE, 128, 128);
        }
    }

    private enum FrameKind { GOLD, SILVER }

    private static boolean isClassEvolutionNode(Talent t) {
        return t == Talent.W_EVO || t == Talent.A_EVO || t == Talent.M_EVO || t == Talent.AS_EVO;
    }

    private static boolean isSubclassBaseNode(Talent t) {
        String br = t.branch;
        return br != null && br.endsWith("_subclass");
    }

    private static boolean usesSquareWarriorFrameAndOutline(Talent t) {
        return isClassEvolutionNode(t) || isSubclassBaseNode(t);
    }

    private ResourceLocation getFrameTexture(Talent t, FrameKind kind) {
        if (usesSquareWarriorFrameAndOutline(t)) {
            return kind == FrameKind.GOLD ? FRAME_WARRIOR_GOLD : FRAME_WARRIOR_SILVER;
        }
        String id = t.id.toLowerCase();
        String branch = t.branch.toLowerCase();
        boolean isArcher = id.contains("archer") || branch.contains("archer") || id.startsWith("a_");
        boolean isMage = id.contains("mage") || branch.contains("mage") || id.startsWith("m_");
        boolean isAssassin = id.contains("assassin") || branch.contains("assassin") || id.startsWith("as_");

        if (isArcher) return kind == FrameKind.GOLD ? FRAME_ARCHER_GOLD : FRAME_ARCHER_SILVER;
        if (isMage) return kind == FrameKind.GOLD ? FRAME_MAGE_GOLD : FRAME_MAGE_SILVER;
        if (isAssassin) return kind == FrameKind.GOLD ? FRAME_ASSASSIN_GOLD : FRAME_ASSASSIN_SILVER;
        return kind == FrameKind.GOLD ? FRAME_WARRIOR_GOLD : FRAME_WARRIOR_SILVER;
    }

    private record TalentShapeParams(int radius, int sides, float rotationDeg) {}

    private static TalentShapeParams getTalentShapeParams(Talent t) {
        int halfSize = 74;
        if (usesSquareWarriorFrameAndOutline(t)) {
            return new TalentShapeParams((int) (halfSize * 1.414), 4, 45f);
        }
        String id = t.id.toLowerCase();
        String branch = t.branch.toLowerCase();
        if (id.contains("archer") || branch.contains("archer") || id.startsWith("a_")) {
            return new TalentShapeParams(halfSize, 6, 90f);
        } else if (id.contains("mage") || branch.contains("mage") || id.startsWith("m_")) {
            return new TalentShapeParams(halfSize, 32, 0f);
        } else if (id.contains("assassin") || branch.contains("assassin") || id.startsWith("as_")) {
            return new TalentShapeParams((int) (halfSize * 1.1), 4, 0f);
        } else {
            return new TalentShapeParams((int) (halfSize * 1.414), 4, 45f);
        }
    }

    private static float outlineRayExitDistance(int cx, int cy, float nx, float ny, TalentShapeParams sp) {
        float r = sp.radius;
        float offset = (float) Math.toRadians(sp.rotationDeg);
        float angleStep = (float) (2 * Math.PI / sp.sides);
        float bestT = Float.MAX_VALUE;
        for (int i = 0; i < sp.sides; i++) {
            float a1 = i * angleStep + offset;
            float a2 = (i + 1) * angleStep + offset;
            float x1 = cx + Mth.cos(a1) * r;
            float y1 = cy + Mth.sin(a1) * r;
            float x2 = cx + Mth.cos(a2) * r;
            float y2 = cy + Mth.sin(a2) * r;
            float t = raySegmentIntersectT(cx, cy, nx, ny, x1, y1, x2, y2);
            if (!Float.isNaN(t) && t > 0.5f && t < bestT) bestT = t;
        }
        return bestT == Float.MAX_VALUE ? r : bestT;
    }

    private static float raySegmentIntersectT(float ox, float oy, float dx, float dy, float ax, float ay, float bx, float by) {
        float wx = bx - ax, wy = by - ay;
        float denom = dx * wy - dy * wx;
        if (Mth.abs(denom) < 1e-5f) return Float.NaN;
        float vx = ax - ox, vy = ay - oy;
        float t = (vx * wy - vy * wx) / denom;
        float u = (vx * dy - vy * dx) / denom;
        if (t >= 0.5f && u >= 0f && u <= 1f) return t;
        return Float.NaN;
    }

    private void drawTalentConnectionLine(GuiGraphics gui, Talent from, Talent to, int color) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-4f) return;
        float len = Mth.sqrt(lenSq);
        float nx = dx / len;
        float ny = dy / len;
        TalentShapeParams sFrom = getTalentShapeParams(from);
        TalentShapeParams sTo = getTalentShapeParams(to);
        float trimFrom = outlineRayExitDistance(from.x, from.y, nx, ny, sFrom);
        float trimTo = outlineRayExitDistance(to.x, to.y, -nx, -ny, sTo);
        if (len <= trimFrom + trimTo + 2f) return;
        int x1 = Math.round(from.x + nx * trimFrom);
        int y1 = Math.round(from.y + ny * trimFrom);
        int x2 = Math.round(to.x - nx * trimTo);
        int y2 = Math.round(to.y - ny * trimTo);
        drawOptimizedLine(gui, x1, y1, x2, y2, color);
    }

    private void drawTalentOutline(GuiGraphics gui, Talent t, int outlineColor) {
        TalentShapeParams p = getTalentShapeParams(t);
        drawPolygonOutline(gui, t.x, t.y, p.radius, p.sides, p.rotationDeg, outlineColor);
    }

    private void drawPolygonOutline(GuiGraphics gui, int x, int y, int radius, int sides, float rotationDeg, int outlineColor) {
        float angleStep = (float) (2 * Math.PI / sides);
        float offset = (float) Math.toRadians(rotationDeg);
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

    private void drawTalentShape(GuiGraphics gui, Talent t, int bgColor, int outlineColor) {
        TalentShapeParams p = getTalentShapeParams(t);
        drawPolygon(gui, t.x, t.y, p.radius, p.sides, p.rotationDeg, bgColor, outlineColor);
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

                        boolean raceForbidden = t.isForbiddenForRace(clientRace);
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
                if (isPointerOnStat(rx, ry, s)) {
                    int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
                    List<Component> tip = new ArrayList<>();
                    tip.add(Component.literal("§6" + s.label));
                    tip.add(Component.literal("§7" + s.description));
                    tip.add(Component.literal("§eУровень: " + level + "/" + s.maxLevel));
                    if (level >= s.maxLevel) tip.add(Component.literal("§aМаксимум"));
                    gui.renderComponentTooltip(font, tip, mx, my);
                    break;
                }
            }
        }
    }

    private void renderScreenBackdrop(GuiGraphics gui) {
        gui.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderWallpaperInTreeSpace(GuiGraphics gui) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, BG);
        int ox = -WALLPAPER_TEX_W / 2;
        int oy = WALLPAPER_CENTER_Y - WALLPAPER_TEX_H / 2;
        gui.blit(BG, ox, oy, 0, 0, WALLPAPER_TEX_W, WALLPAPER_TEX_H, WALLPAPER_TEX_W, WALLPAPER_TEX_H);
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

    private boolean isPointerOnStat(float rx, float ry, AttributeStat s) {
        if (Mth.abs(rx - s.x) > STAT_POLY_R) return false;
        int bottom = s.y + STAT_LABEL_PANEL_TOP + STAT_LABEL_PANEL_H;
        return ry >= s.y - STAT_POLY_R && ry <= bottom;
    }

    private boolean statCanPurchaseNext(AttributeStat s) {
        return switch (s) {
            case MANA -> getAvailablePoints() >= 1;
            case DAMAGE -> (ClientStatTrainingHud.hasData && !ClientStatTrainingHud.dmgMaxed
                    && ClientStatTrainingHud.dmgProg >= ClientStatTrainingHud.dmgNeed)
                    || getAvailablePoints() >= 1;
            case SPEED -> (ClientStatTrainingHud.hasData && !ClientStatTrainingHud.spdMaxed
                    && ClientStatTrainingHud.spdProg >= ClientStatTrainingHud.spdNeed)
                    || getAvailablePoints() >= 1;
            case HEALTH -> (ClientStatTrainingHud.hasData && !ClientStatTrainingHud.hpMaxed
                    && ClientStatTrainingHud.hpProg >= ClientStatTrainingHud.hpNeed)
                    || getAvailablePoints() >= 1;
        };
    }

    private void renderStatsArea(GuiGraphics gui, int points) {
        for (AttributeStat s : AttributeStat.values()) {
            int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
            boolean atCap = level >= s.maxLevel;
            boolean canPurchase = !atCap && statCanPurchaseNext(s);

            int bgColor;
            int outlineColor;
            int panelOutline;
            int panelBarFill;
            if (atCap) {
                bgColor = 0xFF151f18;
                outlineColor = 0xFF5a8a62;
                panelOutline = 0xFF6a9a72;
                panelBarFill = 0x446a9a72;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else if (canPurchase) {
                bgColor = 0xFF1c1c26;
                outlineColor = 0xFFd4a84b;
                panelOutline = 0xFFb8923a;
                panelBarFill = 0x44d4a84b;
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                bgColor = 0xFF18181f;
                outlineColor = 0xFF4a4a58;
                panelOutline = 0xFF3a3a48;
                panelBarFill = 0x22333340;
                RenderSystem.setShaderColor(0.52f, 0.52f, 0.55f, 1.0f);
            }

            drawPolygon(gui, s.x, s.y, STAT_POLY_R, 32, 0f, bgColor, outlineColor);
            gui.blit(s.icon, s.x - STAT_ICON_HALF, s.y - STAT_ICON_HALF, 0, 0,
                    STAT_ICON_DRAW, STAT_ICON_DRAW, STAT_ICON_DRAW, STAT_ICON_DRAW);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            int px1 = s.x - STAT_LABEL_PANEL_HALF_W;
            int py1 = s.y + STAT_LABEL_PANEL_TOP;
            int pw = STAT_LABEL_PANEL_HALF_W * 2;
            int ph = STAT_LABEL_PANEL_H;
            gui.fill(px1 + 1, py1 + 1, px1 + pw - 1, py1 + ph - 1, 0xE012121a);
            gui.renderOutline(px1, py1, pw, ph, panelOutline);
            gui.fill(px1 + 2, py1 + 2, px1 + pw - 2, py1 + 4, panelBarFill);

            String label = s.label;
            int labelW = font.width(label);
            int labelY = py1 + 6;
            int labelColor = atCap ? 0xFFc8e8cc : (canPurchase ? 0xFFF2ECDC : 0xFF9a9aaa);
            gui.drawString(font, label, s.x - labelW / 2, labelY, labelColor, false);

            String num = String.valueOf(level);
            String slashMax = " / " + s.maxLevel;
            int lvlTotalW = font.width(num + slashMax);
            int levelY = py1 + 20;
            int lx = s.x - lvlTotalW / 2;
            int numColor = atCap ? 0xFF8ecf8e : (canPurchase ? 0xFFf0c040 : 0xFF8a8a96);
            gui.drawString(font, num, lx, levelY, numColor, false);
            gui.drawString(font, slashMax, lx + font.width(num), levelY, 0xFF6b6b78, false);
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
                .filter(id -> !clientAdminGrantedTalents.contains(id))
                .map(Talent::getById)
                .filter(Objects::nonNull)
                .filter(t -> !isFreeClassTalent(t.id))
                .mapToInt(t -> t.cost)
                .sum();
        int spentOnUpgrades = 0;
        for (var e : clientAbilityLevels.entrySet()) {
            String id = e.getKey();
            if (clientAdminGrantedTalents.contains(id)) continue;
            int lvl = e.getValue() == null ? 0 : e.getValue();
            if (lvl <= 1) continue;
            for (int next = 2; next <= lvl; next++) {
                spentOnUpgrades += AbilityUpgradeConfig.getUpgradePointCost(id, next);
            }
        }
        return clientLevel - spentOnTalents - spentOnUpgrades - clientStatPointsSpent + clientBonusPoints - clientTalentBudgetDebt;
    }

    private static boolean isFreeClassTalent(String id) {
        return "start".equals(id) || "warrior_base".equals(id) || "archer_base".equals(id) || "mage_base".equals(id) || "assassin_base".equals(id);
    }

    private long getTalentCount() {
        return clientTalents.stream().filter(id -> !isFreeClassTalent(id)).count();
    }

    private boolean isBranchBlocked(Talent t) {
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;
        Talent root = Talent.subclassRootFor(t);
        if (root != null) {
            for (Talent b : Talent.subclassBasesFor(t)) {
                if (b != root && clientTalents.contains(b.id)) return true;
            }
        }
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
        gui.drawString(font, text, x + 5, y + 7, active ? 0xFF000000 : 0xFFFFFFFF, false);
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
                if (isPointerOnStat(rx, ry, s)) {
                    int level = (clientStats.getOrDefault(s.id, 0)) + (clientRace.bonuses.getOrDefault(s.id, 0));
                    if (level >= s.maxLevel) return true;
                    if (statCanPurchaseNext(s)) {
                        PacketDistributor.sendToServer(new C2SUpgradeStat(s.id));
                    }
                    return true;
                }
            }
        } else {
            for (Talent t : Talent.values()) {
                if (!isTalentVisible(t)) continue;
                if (rx >= t.x - 74 && rx <= t.x + 74 && ry >= t.y - 74 && ry <= t.y + 74) {
                    if (!clientTalents.contains(t.id)) {
                        if (getAvailablePoints() >= t.cost) {
                            PacketDistributor.sendToServer(new C2SPurchaseTalent(t.id));
                        }
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
        String chosen = getChosenTopClassBaseIdClient();
        if (chosen == null) return false;
        if (Arrays.stream(t.parents).anyMatch(p -> p.id.equals("start"))) return t.id.equals(chosen);
        if (t.id.equals(chosen)) return true;
        if (!isDescendantOfChosen(t, chosen)) return false;

        if (!isVisibleWithinEvolutionCap(t, chosen)) return false;

        Talent root = Talent.subclassRootFor(t);
        if (root != null) {
            List<Talent> subBases = Talent.subclassBasesFor(t);
            boolean isSubclassChoiceNode = subBases.contains(t);
            Talent chosenSub = null;
            for (Talent b : subBases) {
                if (clientTalents.contains(b.id)) {
                    chosenSub = b;
                    break;
                }
            }
            if (chosenSub != null) {
                return root == chosenSub;
            }
            if (!isSubclassChoiceNode) {
                return false;
            }
        }

        return true;
    }

    public static String getChosenTopClassBaseIdClient() {
        if (clientTalents.contains("warrior_base")) return "warrior_base";
        if (clientTalents.contains("archer_base")) return "archer_base";
        if (clientTalents.contains("mage_base")) return "mage_base";
        if (clientTalents.contains("assassin_base")) return "assassin_base";
        return null;
    }

    private static Talent evolutionTalentForClassBaseId(String chosenClassId) {
        if (chosenClassId == null) return null;
        return switch (chosenClassId) {
            case "warrior_base" -> Talent.W_EVO;
            case "archer_base" -> Talent.A_EVO;
            case "mage_base" -> Talent.M_EVO;
            case "assassin_base" -> Talent.AS_EVO;
            default -> null;
        };
    }

    private boolean isVisibleWithinEvolutionCap(Talent t, String chosenClassId) {
        Talent evo = evolutionTalentForClassBaseId(chosenClassId);
        if (evo == null || Talent.getById(chosenClassId) == null) return true;

        if (clientTalents.contains(evo.id)) {
            for (Talent subBase : Talent.subclassBasesFor(evo)) {
                if (clientTalents.contains(subBase.id) && (t == subBase || Talent.isAncestorOf(subBase, t))) {
                    return true;
                }
            }
        }

        if (Talent.isAncestorOf(evo, t)) {
            if (!clientTalents.contains(evo.id)) return false;
            if (Arrays.asList(t.parents).contains(evo)) return true;
            return Arrays.stream(t.parents).anyMatch(p -> clientTalents.contains(p.id));
        }

        if (t == evo) return true;
        return Talent.isAncestorOf(t, evo);
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

    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        scrollX += (float) dx;
        scrollY += (float) dy;
        clampScroll();
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        zoom = (sy > 0) ? Math.min(zoom + 0.1f, 2.0f) : Math.max(zoom - 0.1f, 0.1f);
        clampScroll();
        return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}