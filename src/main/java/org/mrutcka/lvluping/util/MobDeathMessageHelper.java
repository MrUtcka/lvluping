package org.mrutcka.lvluping.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;

public final class MobDeathMessageHelper {
    private MobDeathMessageHelper() {}

    public static boolean isDeathFromMob(DamageSource source) {
        return resolveMobKiller(source) != null;
    }

    public static Mob resolveMobKiller(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof Mob mobOwner) {
            return mobOwner;
        }
        if (direct instanceof Mob mobDirect) {
            return mobDirect;
        }
        Entity causing = source.getEntity();
        if (causing instanceof Mob mobEntity) {
            return mobEntity;
        }
        if (causing instanceof Projectile projectile2 && projectile2.getOwner() instanceof Mob mobFromProjectile) {
            return mobFromProjectile;
        }
        return null;
    }

    public static Component expeditionDeath(ServerPlayer player) {
        return Component.translatable("death.lvluping.expedition", player.getDisplayName());
    }
}
