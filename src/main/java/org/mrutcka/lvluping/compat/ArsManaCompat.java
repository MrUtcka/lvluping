package org.mrutcka.lvluping.compat;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

public final class ArsManaCompat {
    private static volatile boolean initTried = false;
    private static Method getManaMethod;
    private static Method getCurrentManaMethod;
    private static Method removeManaMethod;

    private static void init() {
        if (initTried) return;
        initTried = true;
        try {
            Class<?> capRegistry = Class.forName("com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry");
            Class<?> manaCap = Class.forName("com.hollingsworth.arsnouveau.common.capability.ManaCap");
            Class<?> iManaCap = Class.forName("com.hollingsworth.arsnouveau.api.mana.IManaCap");
            getManaMethod = capRegistry.getMethod("getMana", LivingEntity.class);
            getCurrentManaMethod = iManaCap.getMethod("getCurrentMana");
            removeManaMethod = iManaCap.getMethod("removeMana", double.class);
            if (!manaCap.isAssignableFrom(getManaMethod.getReturnType())) {
                getManaMethod = null;
            }
        } catch (Throwable t) {
            getManaMethod = null;
            getCurrentManaMethod = null;
            removeManaMethod = null;
        }
    }

    public static double getCurrentMana(LivingEntity entity) {
        init();
        if (getManaMethod == null || getCurrentManaMethod == null) return 0.0;
        try {
            Object cap = getManaMethod.invoke(null, entity);
            if (cap == null) return 0.0;
            Object val = getCurrentManaMethod.invoke(cap);
            return val instanceof Double d ? d : 0.0;
        } catch (Throwable t) {
            return 0.0;
        }
    }

    public static boolean tryConsumeMana(LivingEntity entity, double cost) {
        if (cost <= 0) return true;
        init();
        if (getManaMethod == null || getCurrentManaMethod == null || removeManaMethod == null) return false;
        try {
            Object cap = getManaMethod.invoke(null, entity);
            if (cap == null) return false;
            double cur = (double) getCurrentManaMethod.invoke(cap);
            if (cur + 1e-6 < cost) return false;
            removeManaMethod.invoke(cap, cost);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

