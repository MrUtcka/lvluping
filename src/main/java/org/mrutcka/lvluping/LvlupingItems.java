package org.mrutcka.lvluping;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class LvlupingItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, LvlupingMod.MODID);

    public static final Supplier<Item> JUDGEMENT_HAMMER = ITEMS.register("judgement_hammer",
            () -> new Item(new Item.Properties()));
}
