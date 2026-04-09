package org.mrutcka.lvluping;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mrutcka.lvluping.entity.AssassinBarricadeEntity;

import java.util.function.Supplier;

public final class LvlupingEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, LvlupingMod.MODID);

    public static final Supplier<EntityType<AssassinBarricadeEntity>> ASSASSIN_BARRICADE = ENTITY_TYPES.register("assassin_barricade",
            () -> EntityType.Builder.of(AssassinBarricadeEntity::new, MobCategory.MISC)
                    .sized(3.0f, 2.0f)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build(LvlupingMod.MODID + ":assassin_barricade"));
}
