package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

public enum Talent {
    START("start", "Истоки", "Ваше приключение начинается здесь", 0, 0, 0, null, ""),

    // --- ВОИН (Танк/Ближний бой) ---
    WARRIOR_BASE("warrior_base", "Путь Воина", "Основы закалки", 1, -80, -40, START, "combat_class"),
    SHIELD_BLOCK("shield_block", "Мастер щита", "Блокирование урона", 2, -110, -80, WARRIOR_BASE, ""),
    HEAVY_STRIKE("heavy_strike", "Тяжелый удар", "+15% к урону мечом", 2, -60, -80, WARRIOR_BASE, ""),
    IRON_WILL("iron_will", "Железная воля", "Иммунитет к отбрасыванию", 3, -110, -120, SHIELD_BLOCK, ""),
    JUGGERNAUT("juggernaut", "Джаггернаут", "Ультимативная защита", 5, -85, -160, IRON_WILL, ""),

    // --- ЛУЧНИК (Дальний бой/Скорость) ---
    ARCHER_BASE("archer_base", "Путь Лучника", "Глаз сокола", 1, -30, -40, START, "combat_class"),
    LONG_SHOT("long_shot", "Дальний выстрел", "Урон от дистанции", 2, -45, -80, ARCHER_BASE, ""),
    QUICK_DRAW("quick_draw", "Быстрая тетива", "Скорость перезарядки лука", 2, -15, -80, ARCHER_BASE, ""),
    EXPLOSIVE_ARROW("explosive_arrow", "Разрывные стрелы", "Урон по области", 3, -15, -120, QUICK_DRAW, ""),
    EAGLE_EYE("eagle_eye", "Орлиный взор", "100% точность", 5, -30, -160, EXPLOSIVE_ARROW, ""),

    // --- МАГ (Заклинания/Мана) ---
    MAGE_BASE("mage_base", "Путь Мага", "Тайные знания", 1, 30, -40, START, "combat_class"),
    FIRE_ELEMENT("fire_element", "Магия Огня", "Поджог врагов", 2, 15, -80, MAGE_BASE, "mage_element"),
    FROST_ELEMENT("frost_element", "Магия Льда", "Замедление врагов", 2, 45, -80, MAGE_BASE, "mage_element"),
    ARCANE_POWER("arcane_power", "Тайная мощь", "Усиление всех заклинаний", 3, 45, -120, FROST_ELEMENT, ""),
    ARCHMAGE("archmage", "Архимаг", "Бесконечная мана", 5, 30, -160, ARCANE_POWER, ""),

    // --- АССАСИН (Криты/Скрытность) ---
    ASSASSIN_BASE("assassin_base", "Путь Ассасина", "Искусство тени", 1, 80, -40, START, "combat_class"),
    POISON_BLADE("poison_blade", "Отравленный клинок", "Периодический урон", 2, 60, -80, ASSASSIN_BASE, ""),
    BACKSTAB("backstab", "Удар в спину", "Крит в спину x2", 2, 110, -80, ASSASSIN_BASE, ""),
    SHADOW_STEP("shadow_step", "Шаг тени", "Шанс уклонения +20%", 3, 110, -120, BACKSTAB, ""),
    NIGHT_STALKER("night_stalker", "Ночной охотник", "Инвиз ночью", 5, 85, -160, SHADOW_STEP, "");

    public final String id, label, description, branch;
    public final int cost, x, y;
    public final Talent parent;
    public final ResourceLocation icon;

    Talent(String id, String label, String description, int cost, int x, int y, Talent parent, String branch) {
        this.id = id; this.label = label; this.description = description;
        this.cost = cost; this.x = x; this.y = y; this.parent = parent; this.branch = branch;
        this.icon = ResourceLocation.fromNamespaceAndPath("lvluping", "textures/gui/talents/" + id + ".png");
    }

    public static Talent getById(String id) {
        for (Talent t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}