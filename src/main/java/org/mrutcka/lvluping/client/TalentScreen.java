package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.Talent;
import org.mrutcka.lvluping.network.C2SPurchaseTalent;
import org.mrutcka.lvluping.network.ModNetworking;
import java.util.*;

public class TalentScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");
    public static int clientLevel = 0;
    public static Set<String> clientTalents = new HashSet<>();

    private float scrollX, scrollY, zoom = 1.0f;

    public TalentScreen() { super(Component.literal("Дерево Навыков")); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderRepeatingBackground(gui);

        gui.pose().pushPose();
        gui.pose().translate(this.width / 2f + scrollX, this.height / 2f + scrollY, 0);
        gui.pose().scale(zoom, zoom, 1.0f);

        for (Talent t : Talent.values()) {
            if (t.parent != null) drawLine(gui, t.x, t.y, t.parent.x, t.parent.y, 0xFF555555);
        }

        Talent hovered = null;
        float relX = (mouseX - this.width / 2f - scrollX) / zoom;
        float relY = (mouseY - this.height / 2f - scrollY) / zoom;

        for (Talent t : Talent.values()) {
            boolean bought = clientTalents.contains(t.id);
            boolean blocked = Talent.isGroupBlocked(t, clientTalents) || (t.parent != null && !clientTalents.contains(t.parent.id));

            int color = bought ? 0xFF00AA00 : (blocked ? 0xFF330000 : 0xFF444444);
            gui.fill(t.x-13, t.y-13, t.x+13, t.y+13, (blocked && !bought) ? 0xFFFF0000 : 0xFF000000);
            gui.fill(t.x-12, t.y-12, t.x+12, t.y+12, color);

            RenderSystem.setShaderColor(1, 1, 1, 1);
            gui.blit(t.icon, t.x-8, t.y-8, 0, 0, 16, 16, 16, 16);

            if (relX >= t.x-12 && relX <= t.x+12 && relY >= t.y-12 && relY <= t.y+12) hovered = t;
        }
        gui.pose().popPose();

        int spent = clientTalents.stream().map(Talent::getById).filter(Objects::nonNull).mapToInt(t -> t.cost).sum();
        int displayPoints = clientLevel - spent;

        if (hovered != null) renderTooltip(gui, hovered, mouseX, mouseY, displayPoints);
        gui.drawString(this.font, "Доступно очков: " + displayPoints, 10, 10, 0xFFFF00);
    }

    private void renderTooltip(GuiGraphics gui, Talent t, int mx, int my, int points) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("§6" + t.label));
        lines.add(Component.literal("§7" + t.description));
        if (clientTalents.contains(t.id)) lines.add(Component.literal("§aИзучено"));
        else if (Talent.isGroupBlocked(t, clientTalents)) lines.add(Component.literal("§cПуть закрыт другой веткой"));
        else if (t.parent != null && !clientTalents.contains(t.parent.id)) lines.add(Component.literal("§cТребуется: " + t.parent.label));
        else if (points < t.cost) lines.add(Component.literal("§eНедостаточно очков (Нужно: " + t.cost + ")"));
        else lines.add(Component.literal("§bНажмите для покупки (Стоимость: " + t.cost + ")"));
        gui.renderComponentTooltip(this.font, lines, mx, my);
    }

    private void renderRepeatingBackground(GuiGraphics gui) {
        int size = 16;
        int offX = Math.round(scrollX % size);
        int offY = Math.round(scrollY % size);
        for (int x = -size; x < this.width + size; x += size) {
            for (int y = -size; y < this.height + size; y += size) {
                gui.blit(BG, x + offX, y + offY, 0, 0, size, size, size, size);
            }
        }
    }

    private void drawLine(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        for (int i = 0; i <= (int)dist; i++) {
            double f = (double) i / dist;
            gui.fill((int)(x1 + f * dx)-1, (int)(y1 + f * dy)-1, (int)(x1 + f * dx)+1, (int)(y1 + f * dy)+1, color);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            float rx = (float)(mx - this.width/2f - scrollX)/zoom;
            float ry = (float)(my - this.height/2f - scrollY)/zoom;
            for (Talent t : Talent.values()) {
                if (rx >= t.x-12 && rx <= t.x+12 && ry >= t.y-12 && ry <= t.y+12) {
                    ModNetworking.CHANNEL.send(new C2SPurchaseTalent(t.id), PacketDistributor.SERVER.noArg());
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, b);
    }

    @Override public boolean mouseDragged(double mx, double my, int b, double dx, double dy) { scrollX += dx; scrollY += dy; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { zoom = (sy > 0) ? Math.min(zoom+0.1f, 1.5f) : Math.max(zoom-0.1f, 0.5f); return true; }
    @Override public boolean isPauseScreen() { return false; }
}