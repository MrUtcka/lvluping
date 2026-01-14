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

        // 1. Поле талантов (Слой 1 - Самый нижний)
        gui.pose().pushPose();
        gui.pose().translate(width / 2f + scrollX, height / 2f + scrollY, 0);
        gui.pose().scale(zoom, zoom, 1.0f);
        if (isStatsTab) renderStatsArea(gui); else renderTalentsArea(gui);
        gui.pose().popPose();

        // 2. HUD (Слой 2 - Поверх поля)
        renderHUD(gui);

        // 3. Вкладки (Слой 3 - Поверх нод и HUD)
        drawTab(gui, "НАВЫКИ", 10, 10, !isStatsTab);
        drawTab(gui, "СТАТЫ", 105, 10, isStatsTab);

        // 4. Подсказки (Слой 4 - Самый верхний)
        renderTooltips(gui, mouseX, mouseY);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void renderTalentsArea(GuiGraphics gui) {
        // 1. Сначала рисуем линии
        for (Talent t : Talent.values()) {
            if (t.parent != null) {
                // Линия светится, если родитель куплен
                int lineColor = clientTalents.contains(t.parent.id) ? 0xFFFFAA00 : 0xFF444444;
                drawLine(gui, t.x, t.y, t.parent.x, t.parent.y, lineColor);
            }
        }

        // 2. Рисуем сами ноды
        for (Talent t : Talent.values()) {
            boolean unlocked = clientTalents.contains(t.id);
            boolean parentUnlocked = (t.parent == null || clientTalents.contains(t.parent.id));
            boolean branchBlocked = isBranchBlocked(t);
            boolean limitReached = !unlocked && getTalentCount() >= PlayerLevels.getTalentLimit(clientStars);

            // Определяем состояние ноды для визуальных эффектов
            boolean canPurchase = parentUnlocked && !branchBlocked && !limitReached;

            // Цвета фона
            int bgColor;
            if (unlocked) {
                bgColor = 0xFF00AA00; // Куплено (Зеленый)
            } else if (branchBlocked) {
                bgColor = 0xFF330000; // Заблокировано веткой (Темно-красный)
            } else if (!parentUnlocked) {
                bgColor = 0xFF222222; // Недоступно (Темно-серый)
            } else {
                bgColor = 0xFF444444; // Можно купить (Светло-серый)
            }

            // Отрисовка квадрата ноды
            gui.fill(t.x - 12, t.y - 12, t.x + 12, t.y + 12, bgColor);

            // Рамка ноды
            int outlineColor = unlocked ? 0xFFAAFF00 : (canPurchase ? 0xFFFFFFFF : 0xFF555555);
            gui.renderOutline(t.x - 12, t.y - 12, 24, 24, outlineColor);

            // Иконка с затемнением
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (!unlocked) {
                if (branchBlocked) {
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f);  // Красный оттенок для чужих веток
                } else if (!parentUnlocked) {
                    RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1.0f); // Сильное затемнение, если родитель не куплен
                }
            }

            gui.blit(t.icon, t.x - 8, t.y - 8, 0, 0, 16, 16, 16, 16);
            RenderSystem.setShaderColor(1, 1, 1, 1); // Сброс цвета
        }
    }

    private void renderStatsArea(GuiGraphics gui) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        for (Stat s : Stat.values()) {
            int curLvl = clientStats.getOrDefault(s.id, 0);
            boolean isMax = curLvl >= s.maxLevel;
            int avail = getAvailablePoints();

            // Отрисовка карточки
            gui.fill(s.x, s.y, s.x + 120, s.y + 40, 0xAA000000);
            gui.renderOutline(s.x, s.y, 120, 40, isMax ? 0xFFFFAA00 : 0xFF555555);

            // Текст названия и уровня (Важно: Z-уровень текста)
            gui.pose().pushPose();
            gui.pose().translate(0, 0, 100); // Поднимаем текст над фоном
            gui.drawString(font, "§e" + s.label, s.x + 5, s.y + 5, 0xFFFFFF);

            String progress = isMax ? "§6MAKC" : "§7" + curLvl + "/" + s.maxLevel;
            gui.drawString(font, progress, s.x + 5, s.y + 22, 0xFFFFFF);

            // Кнопка улучшения
            boolean canBuy = !isMax && avail > 0;
            int btnCol = canBuy ? 0xFF00AA00 : 0xFF333333;
            gui.fill(s.x + 90, s.y + 10, s.x + 115, s.y + 35, btnCol);
            gui.renderOutline(s.x + 90, s.y + 10, 25, 25, 0xFFFFFFFF);
            gui.drawString(font, "+", s.x + 99, s.y + 18, 0xFFFFFF);
            gui.pose().popPose();
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
        int spentT = clientTalents.stream().map(Talent::getById).filter(Objects::nonNull).mapToInt(t -> t.cost).sum();
        int spentS = clientStats.values().stream().mapToInt(v -> v).sum();
        return clientLevel - (spentT + spentS);
    }

    private long getTalentCount() { return clientTalents.stream().filter(id -> !id.equals("start")).count(); }

    private boolean isBranchBlocked(Talent t) {
        // Если ветка не указана или талант уже куплен — блокировки нет
        if (t.branch.isEmpty() || clientTalents.contains(t.id)) return false;

        for (String ownedId : clientTalents) {
            Talent owned = Talent.getById(ownedId);
            if (owned == null || owned.branch.isEmpty()) continue;

            // Если у купленного таланта та же ветка (например, "combat_class")
            if (t.branch.equals(owned.branch)) {
                // Проверяем, находятся ли они на одной "линии" наследования.
                // Если мы купили что-то из той же ветки, что НЕ является предком
                // и НЕ является потомком текущего таланта — значит это параллельная ветка.
                if (!isSameHierarchy(t, owned)) {
                    // Дополнительная проверка: блокируем, только если у них общий родитель
                    // или если они оба являются "базовыми" талантами веток.
                    if (t.parent == owned.parent) {
                        return true;
                    }

                    // Проверка для глубоких веток: если их общие предки расходятся
                    // на каком-то этапе, и один из путей уже выбран.
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

    // Проверяет, лежат ли два таланта на одной прямой линии (родитель -> ребенок)
    private boolean isSameHierarchy(Talent a, Talent b) {
        return isAncestor(a, b) || isAncestor(b, a);
    }

    // Проверяет, являются ли ветки расходящимися (имеют общего родителя, но разные пути)
    private boolean isDivergent(Talent t, Talent owned) {
        Talent parentA = t.parent;
        while (parentA != null) {
            Talent parentB = owned.parent;
            while (parentB != null) {
                // Если у них нашелся общий предок, но их следующие шаги к цели разные
                if (parentA == parentB) {
                    // Находим детей этого общего предка, ведущих к T и OWNED
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

    // Помогает найти, какой именно "ребенок" предка ведет к целевому таланту
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
        // Клик по вкладкам теперь проверяется по экранным координатам (без зума)
        if (my >= 10 && my <= 32) {
            if (mx >= 10 && mx <= 100) { isStatsTab = false; return true; }
            if (mx >= 105 && mx <= 195) { isStatsTab = true; return true; }
        }
        float rx = (float) (mx - width / 2f - scrollX) / zoom;
        float ry = (float) (my - height / 2f - scrollY) / zoom;
        if (isStatsTab) {
            for (Stat s : Stat.values()) if (rx >= s.x + 90 && rx <= s.x + 115 && ry >= s.y + 10 && ry <= s.y + 35) ModNetworking.CHANNEL.send(new C2SUpgradeStat(s.id), PacketDistributor.SERVER.noArg());
        } else {
            for (Talent t : Talent.values()) if (rx >= t.x - 12 && rx <= t.x + 12 && ry >= t.y - 12 && ry <= t.y + 12) ModNetworking.CHANNEL.send(new C2SPurchaseTalent(t.id), PacketDistributor.SERVER.noArg());
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
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        zoom = (sy > 0) ? Math.min(zoom + 0.1f, 2.0f) : Math.max(zoom - 0.1f, 0.5f); return true;
    }
    @Override public boolean isPauseScreen() { return false; }
}