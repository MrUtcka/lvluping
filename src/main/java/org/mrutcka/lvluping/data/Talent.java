package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

public enum Talent {
    // 0. НАЧАЛО
    START("start", "Истоки", "Ваше приключение начинается здесь", 0, 0, 0, null, ""),

    // --- 1. ВОИН ---
    WARRIOR_BASE("warrior_base", "Воин", "Путь грубой силы", 1, 200, 0, START, "combat_class"),
    WARRIOR_BLOCK("warrior_block", "Блок", "Мастерское владение щитом", 2, 400, -200, WARRIOR_BASE, "warrior_spec"),
    WARRIOR_COMBO("warrior_combo", "Комбо", "Серия быстрых ударов", 2, 400, 0, WARRIOR_BASE, "warrior_spec"),
    WARRIOR_STUN("warrior_stun", "Оглушение", "Шанс дезориентировать цель", 2, 400, 200, WARRIOR_BASE, "warrior_spec"),

    // --- 2. ЛУЧНИК ---
    ARCHER_BASE("archer_base", "Лучник", "Мастер дальнего боя", 1, -200, 0, START, "combat_class"),
    ARCHER_DASH("archer_dash", "Деш спиной", "Быстрый разрыв дистанции", 2, -400, 0, ARCHER_BASE, "archer_spec"),
    ARCHER_DAGGER("archer_dagger", "Кинжал", "Защита в ближнем бою", 2, -400, -200, ARCHER_BASE, "archer_spec"),
    ARCHER_STRONG_SHOT("archer_strong_shot", "Сильный выстрел", "Пробивающая мощь", 2, -400, 200, ARCHER_BASE, "archer_spec"),

    // --- 3. МАГ ---
    MAGE_BASE("mage_base", "Маг", "Повелитель стихий", 1, 0, -200, START, "combat_class"),
    MAGE_BARRIER("mage_barrier", "Барьер", "Магический щит", 2, 0, -400, MAGE_BASE, "mage_spec"),
    MAGE_BUFF_DEF("mage_buff_def", "Баф Защита", "Укрепление брони заклинанием", 2, -200, -400, MAGE_BASE, "mage_spec"),
    MAGE_BUFF_ATK("mage_buff_atk", "Баф Атака", "Увеличение магической мощи", 2, 200, -400, MAGE_BASE, "mage_spec"),

    // --- 4. АССАСИН ---
    ASSASSIN_BASE("assassin_base", "Ассасин", "Мастер скрытности", 1, 0, 200, START, "combat_class"),
    ASSASSIN_SLIDE("assassin_slide", "Подкат", "Маневренность и скорость", 2, 0, 400, ASSASSIN_BASE, "assassin_spec"),
    ASSASSIN_SMOKE("assassin_smoke", "Смок", "Дымовая завеса для побега", 2, 200, 400, ASSASSIN_BASE, "assassin_spec"),
    ASSASSIN_STEALTH_CRIT("assassin_stealth_crit", "Крит с инвиза", "Смертельный удар из тени", 2, -200, 400, ASSASSIN_BASE, "assassin_spec");

    public final String id, label, description, branch;
    public final int cost, x, y;
    public final Talent parent;
    public final ResourceLocation icon;

    Talent(String id, String label, String description, int cost, int x, int y, Talent parent, String branch) {
        this.id = id; this.label = label; this.description = description;
        this.cost = cost; this.x = x; this.y = y; this.parent = parent; this.branch = branch;
        this.icon = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talents/" + id + ".png");
    }

    public static Talent getById(String id) {
        for (Talent t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}