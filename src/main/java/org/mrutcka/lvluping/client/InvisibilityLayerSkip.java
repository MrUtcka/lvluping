package org.mrutcka.lvluping.client;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class InvisibilityLayerSkip {
    private InvisibilityLayerSkip() {}

    public static boolean hideEquipmentLayers(LivingEntity entity) {
        return entity instanceof Player player && player.hasEffect(MobEffects.INVISIBILITY);
    }
}
