package org.mrutcka.lvluping.data;

import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

public enum AttributeStat {
    HEALTH("health", "Здоровье", "Увеличивает макс. ХП", 30),
    DAMAGE("damage", "Сила", "Увеличивает урон ближнего боя", 30),
    SPEED("speed", "Скорость", "Увеличивает скорость бега", 30);

    public final String id, label, description;
    public final int maxLevel;
    public final ResourceLocation icon;

    AttributeStat(String id, String label, String description, int maxLevel) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.maxLevel = maxLevel;
        this.icon = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "textures/gui/attributes/" + id + ".png");
    }

    public static AttributeStat getById(String id) {
        for (AttributeStat s : values()) if (s.id.equals(id)) return s;
        return null;
    }
}