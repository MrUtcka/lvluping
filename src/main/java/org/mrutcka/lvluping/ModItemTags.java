package org.mrutcka.lvluping;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> ARCHER_DAGGERS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "archer_daggers"));

    private ModItemTags() {}
}
