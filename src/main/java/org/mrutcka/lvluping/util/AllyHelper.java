package org.mrutcka.lvluping.util;



import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.Mob;



public final class AllyHelper {
    private AllyHelper() {}


    public static boolean isSupportAlly(ServerPlayer caster, LivingEntity target) {

        if (target == null || !target.isAlive()) return false;

        if (target == caster) return true;

        if (target instanceof ServerPlayer) return true;

        return target instanceof Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner");

    }

}


