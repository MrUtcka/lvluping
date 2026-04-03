package org.mrutcka.lvluping;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class LvlupingItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, LvlupingMod.MODID);

    public static final Supplier<Item> JUDGEMENT_HAMMER = ITEMS.register("judgement_hammer",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> UNBREAKABLE_SHIELD_ORBIT = ITEMS.register("unbreakable_shield_orbit",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> HUNTER_TRAP_MODEL = ITEMS.register("hunter_trap_model",
            () -> new Item(new Item.Properties()));

    /** Модель дерева для слияния (рейнджер). */
    public static final Supplier<Item> RANGER_TREE_MODEL = ITEMS.register("ranger_tree_model",
            () -> new Item(new Item.Properties()));

    /** Модель кустов/корней (колючий куст и т.п.). */
    public static final Supplier<Item> RANGER_KORNI_MODEL = ITEMS.register("ranger_korni_model",
            () -> new Item(new Item.Properties()));

    /** Модель тотема жизни (ульт рейнджера). */
    public static final Supplier<Item> RANGER_TOTEM_MODEL = ITEMS.register("ranger_totem_model",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> ASSASSIN_WALL_MODEL = ITEMS.register("assassin_wall_model",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> ASSASSIN_TRIPWIRE_MODEL = ITEMS.register("assassin_tripwire_model",
            () -> new Item(new Item.Properties()));

    public static final Supplier<Item> ASSASSIN_CAMP_MODEL = ITEMS.register("assassin_camp_model",
            () -> new Item(new Item.Properties()));
}
