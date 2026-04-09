package org.mrutcka.lvluping.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.Talent;
import org.mrutcka.lvluping.network.C2SPurchaseTalent;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectScreen extends Screen {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talent_tree_bg.png");

    private static final ResourceLocation ICON_WARRIOR = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/classes/warrior.png");
    private static final ResourceLocation ICON_ARCHER = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/classes/archer.png");
    private static final ResourceLocation ICON_MAGE = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/classes/mage.png");
    private static final ResourceLocation ICON_ASSASSIN = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/classes/assassin.png");

    private static final ResourceLocation ICON_FALLBACK = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/lock.png");

    private String pending = null;

    public ClassSelectScreen() {
        super(Component.literal("Выбор класса"));
    }

    private static boolean isRaceLocked(String classBaseId) {
        Talent t = Talent.getById(classBaseId);
        return t == null || t.isForbiddenForRace(TalentScreen.clientRace);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderSimpleBackground(gui);

        {
            String title = "Выбор класса";
            int tw = font.width(title);
            gui.drawString(font, title, (width - tw) / 2, 18, 0xFFFFFF, true);
        }

        int card = 80;
        int gap = 16;
        int totalW = card * 4 + gap * 3;
        int startX = (width - totalW) / 2;
        int startY = 62;

        var cards = getCards(startX, startY, card, gap);

        for (Card c : cards) {
            boolean locked = isRaceLocked(c.id);
            boolean picked = c.id.equals(pending);
            boolean grey = locked || (pending != null && !picked);
            int bg = picked ? 0xFF2A6CFF : (locked ? 0xFF151515 : 0xFF1E1E1E);
            int outline = locked ? 0xFFAA2222 : (picked ? 0xFFFFFFFF : 0xFFAAAAAA);

            gui.fill(c.x, c.y, c.x + c.w, c.y + c.h, bg);
            gui.renderOutline(c.x, c.y, c.w, c.h, outline);

            RenderSystem.setShaderColor(grey ? 0.35f : 1f, grey ? 0.35f : 1f, grey ? 0.35f : 1f, 1f);
            blitIcon(gui, c.icon, c.x + 7, c.y + 7, c.w - 14, c.h - 14);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        if (pending != null && !isRaceLocked(pending)) {
            int bw = 180;
            int bh = 30;
            int bx = (width - bw) / 2;
            int by = height - 42;
            gui.fill(bx, by, bx + bw, by + bh, 0xFF2A6CFF);
            gui.renderOutline(bx, by, bw, bh, 0xFFFFFFFF);
            String t = "Выбрать";
            int tw = font.width(t);
            gui.drawString(font, t, bx + (bw - tw) / 2, by + 10, 0xFFFFFF, true);
        }

        Card hovered = null;
        for (Card c : cards) {
            if (mouseX >= c.x && mouseX <= c.x + c.w && mouseY >= c.y && mouseY <= c.y + c.h) {
                hovered = c;
                break;
            }
        }
        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("§6" + hovered.title));
            tip.add(Component.literal("§7" + hovered.shortDesc));
            if (isRaceLocked(hovered.id)) {
                tip.add(Component.literal("§cКласс недоступен для расы «" + TalentScreen.clientRace.label + "»"));
            }
            gui.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int card = 80;
        int gap = 16;
        int totalW = card * 4 + gap * 3;
        int startX = (width - totalW) / 2;
        int startY = 62;
        var cards = getCards(startX, startY, card, gap);

        for (Card c : cards) {
            if (mx >= c.x && mx <= c.x + c.w && my >= c.y && my <= c.y + c.h) {
                if (isRaceLocked(c.id)) {
                    pending = null;
                    return true;
                }
                pending = c.id;
                return true;
            }
        }

        if (pending != null && !isRaceLocked(pending)) {
            int bw = 180;
            int bh = 30;
            int bx = (width - bw) / 2;
            int by = height - 42;
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                PacketDistributor.sendToServer(new C2SPurchaseTalent(pending));
                pending = null;
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void renderSimpleBackground(GuiGraphics gui) {
        RenderSystem.setShaderTexture(0, BG);
        int textureSize = 16;
        gui.blit(BG, 0, 0, 0f, 0f, width, height, textureSize, textureSize);
        gui.fill(0, 0, width, height, 0x66000000);
    }

    private void blitIcon(GuiGraphics gui, ResourceLocation icon, int x, int y, int w, int h) {
        try {
            gui.blit(icon, x, y, 0, 0, w, h, w, h);
        } catch (Exception e) {
            gui.blit(ICON_FALLBACK, x, y, 0, 0, w, h, 64, 64);
        }
    }

    private List<Card> getCards(int startX, int startY, int card, int gap) {
        List<Card> out = new ArrayList<>();
        out.add(new Card("warrior_base", "Воин", "Способный и доблестный воитель, что сражается на передовой.", ICON_WARRIOR, startX, startY, card, card));
        out.add(new Card("archer_base", "Лучник", "Меткий боец, контролирующий дистанцию и наносит урон издалека.", ICON_ARCHER, startX + card + gap, startY, card, card));
        out.add(new Card("mage_base", "Маг", "Владеет тайной магией и маной, разрушая врагов заклинаниями.", ICON_MAGE, startX + (card + gap) * 2, startY, card, card));
        out.add(new Card("assassin_base", "Ассасин", "Тень и скорость: внезапные атаки, контроль и уход из боя.", ICON_ASSASSIN, startX + (card + gap) * 3, startY, card, card));
        return out;
    }

    private record Card(String id, String title, String shortDesc, ResourceLocation icon, int x, int y, int w, int h) {}
}
