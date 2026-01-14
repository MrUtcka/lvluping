package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;
import java.util.Set;

public enum Talent {
    ROOT("root", "Начало", 0, 0, 0, null, "none", "Стартовая точка", "root.png"),

    // КЛАССЫ (Уровень 1, Группа "class")
    WARRIOR("warrior", "Воин", -160, -80, 1, ROOT, "class", "Мастер меча", "warrior.png"),
    MAGE("mage", "Маг", -80, -80, 1, ROOT, "class", "Мастер магии", "mage.png"),
    ARCHER("archer", "Лучник", 0, -80, 1, ROOT, "class", "Мастер лука", "archer.png"),
    ASSASSIN("assassin", "Убийца", 80, -80, 1, ROOT, "class", "Мастер кинжала", "assassin.png"),
    PALADIN("paladin", "Паладин", 160, -80, 1, ROOT, "class", "Мастер света", "paladin.png"),

    // СПЕЦИАЛИЗАЦИИ (Уровень 2)
    BERSERK("berserk", "Берсерк", -200, -160, 2, WARRIOR, "war_spec", "Ярость битвы", "berserk.png"),
    FIRE_MAGE("fire_mage", "Маг Огня", -100, -160, 3, MAGE, "mage_spec", "Сила пламени", "fire.png");

    public final String id, label, description, group;
    public final int x, y, cost;
    public final Talent parent;
    public final ResourceLocation icon;

    Talent(String id, String label, int x, int y, int cost, Talent parent, String group, String description, String iconName) {
        this.id = id; this.label = label; this.x = x; this.y = y;
        this.cost = cost; this.parent = parent; this.group = group;
        this.description = description;
        this.icon = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/talents/" + iconName);
    }

    public static Talent getById(String id) {
        for (Talent t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    public static boolean isGroupBlocked(Talent target, Set<String> ownedIds) {
        if (target.group.equals("none")) return false;
        for (String id : ownedIds) {
            Talent ownedT = getById(id);
            if (ownedT != null && !ownedT.id.equals(target.id) && ownedT.group.equals(target.group)) return true;
        }
        return false;
    }
}