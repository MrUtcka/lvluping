package org.mrutcka.lvluping.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.mrutcka.lvluping.data.AttributeStat;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.lang.reflect.Method;

public final class ArsManaCompat {
    private static volatile boolean initTried = false;
    private static Method getManaMethod;
    private static Method getCurrentManaMethod;
    private static Method removeManaMethod;
    private static Method addManaMethod;
    private static Method setManaMethod;
    private static Method getMaxManaMethod;
    private static Method setMaxManaMethod;

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
            try { addManaMethod = iManaCap.getMethod("addMana", double.class); } catch (Throwable ignored) { addManaMethod = null; }
            try { setManaMethod = iManaCap.getMethod("setMana", double.class); } catch (Throwable ignored) { setManaMethod = null; }
            try { getMaxManaMethod = iManaCap.getMethod("getMaxMana"); } catch (Throwable ignored) { getMaxManaMethod = null; }
            try { setMaxManaMethod = iManaCap.getMethod("setMaxMana", int.class); } catch (Throwable ignored) { setMaxManaMethod = null; }
            if (!manaCap.isAssignableFrom(getManaMethod.getReturnType())) {
                getManaMethod = null;
            }
        } catch (Throwable t) {
            getManaMethod = null;
            getCurrentManaMethod = null;
            removeManaMethod = null;
            addManaMethod = null;
            setManaMethod = null;
            getMaxManaMethod = null;
            setMaxManaMethod = null;
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
            if (entity instanceof ServerPlayer sp) {
                int manaLvl = PlayerLevels.getStatLevel(sp.getUUID(), AttributeStat.MANA.id);
                double ratio = Math.min(1.0, Math.max(0, manaLvl) / 30.0);
                double multiplier = 1.0 - (0.75 * ratio); // 1.0 -> 0.25
                cost *= multiplier;
            }
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

    public static boolean tryAddMana(LivingEntity entity, double amount) {
        if (amount <= 0) return true;
        init();
        if (getManaMethod == null || getCurrentManaMethod == null) return false;
        try {
            Object cap = getManaMethod.invoke(null, entity);
            if (cap == null) return false;
            if (addManaMethod != null) {
                addManaMethod.invoke(cap, amount);
                return true;
            }
            if (setManaMethod != null) {
                double cur = (double) getCurrentManaMethod.invoke(cap);
                setManaMethod.invoke(cap, cur + amount);
                return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Integer getMaxMana(LivingEntity entity) {
        init();
        if (getManaMethod == null || getMaxManaMethod == null) return null;
        try {
            Object cap = getManaMethod.invoke(null, entity);
            if (cap == null) return null;
            Object val = getMaxManaMethod.invoke(cap);
            return val instanceof Number n ? n.intValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean trySetMaxMana(LivingEntity entity, int maxMana) {
        init();
        if (getManaMethod == null || setMaxManaMethod == null) return false;
        try {
            Object cap = getManaMethod.invoke(null, entity);
            if (cap == null) return false;
            setMaxManaMethod.invoke(cap, Math.max(1, maxMana));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

