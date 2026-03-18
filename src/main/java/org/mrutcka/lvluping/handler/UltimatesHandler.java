package org.mrutcka.lvluping.handler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class UltimatesHandler {

    private static final double BROTHERHOOD_RADIUS = 6.0;

    public static boolean isBerserkActive(Player player) {
        if (player.level().isClientSide()) return false;
        long until = player.getPersistentData().getLong("lvluping_berserk_until");
        return until > 0 && player.level().getGameTime() < until;
    }

    public static boolean isInvulnerabilityActive(Player player) {
        if (player.level().isClientSide()) return false;
        long until = player.getPersistentData().getLong("lvluping_invulnerability_until");
        return until > 0 && player.level().getGameTime() < until;
    }

    public static boolean isBrotherhoodActive(Player player) {
        if (player.level().isClientSide()) return false;
        return player.getPersistentData().getLong("lvluping_brotherhood_until") > player.level().getGameTime();
    }

    public static void tickBrotherhood(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer warrior : level.players()) {
            long until = warrior.getPersistentData().getLong("lvluping_brotherhood_until");
            if (until <= time) continue;

            double cx = warrior.getPersistentData().getDouble("lvluping_brotherhood_x");
            double cy = warrior.getPersistentData().getDouble("lvluping_brotherhood_y");
            double cz = warrior.getPersistentData().getDouble("lvluping_brotherhood_z");
            Vec3 center = new Vec3(cx, cy, cz);

            if (warrior.distanceToSqr(center) > BROTHERHOOD_RADIUS * BROTHERHOOD_RADIUS) {
                warrior.getPersistentData().remove("lvluping_brotherhood_until");
                warrior.getPersistentData().remove("lvluping_brotherhood_x");
                warrior.getPersistentData().remove("lvluping_brotherhood_y");
                warrior.getPersistentData().remove("lvluping_brotherhood_z");
                continue;
            }

            AABB box = new AABB(cx - BROTHERHOOD_RADIUS, cy - 2, cz - BROTHERHOOD_RADIUS,
                    cx + BROTHERHOOD_RADIUS, cy + 4, cz + BROTHERHOOD_RADIUS);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e.distanceToSqr(center) > BROTHERHOOD_RADIUS * BROTHERHOOD_RADIUS) continue;
                if (e == warrior) continue;
                if (e instanceof Player p) {
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
                } else {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
                }
            }
        }
    }

    public static void tickFinalCountdown(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer warrior : level.players()) {
            long at = warrior.getPersistentData().getLong("lvluping_final_countdown_at");
            if (at <= 0 || time < at) continue;

            warrior.getPersistentData().remove("lvluping_final_countdown_at");
            if (!warrior.getPersistentData().hasUUID("lvluping_final_countdown_target")) {
                warrior.getPersistentData().remove("lvluping_final_countdown_target");
                continue;
            }
            UUID targetUuid = warrior.getPersistentData().getUUID("lvluping_final_countdown_target");
            warrior.getPersistentData().remove("lvluping_final_countdown_target");

            LivingEntity target = null;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, warrior.getBoundingBox().inflate(64))) {
                if (e.getUUID().equals(targetUuid)) {
                    target = e;
                    break;
                }
            }
            if (target == null || !target.isAlive()) continue;

            double hpRatio = target.getHealth() / Math.max(0.01, target.getMaxHealth());
            float baseDamage = 15f + (float) (2.0 - hpRatio) * 20f;
            double radius = 4.0;
            Vec3 pos = target.position();

            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius))) {
                if (e.distanceTo(target) <= radius) {
                    e.getPersistentData().putLong("lvluping_fc_lightning_immune", time + 60);
                }
            }

            for (int i = 0; i <= 20; i++) {
                double py = pos.y + 14 - (i * 0.7);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, py, pos.z, 3, 0.1, 0.1, 0.1, 0.05);
                level.sendParticles(ParticleTypes.CRIT, pos.x, py, pos.z, 2, 0.08, 0.08, 0.08, 0.08);
                level.sendParticles(ParticleTypes.ENCHANT, pos.x, py, pos.z, 1, 0.05, 0.05, 0.05, 0.03);
            }

            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            lightning.moveTo(pos.x, pos.y, pos.z);
            level.addFreshEntity(lightning);

            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.5f);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 0.35f);

            for (int i = 0; i < 50; i++) {
                level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 1, pos.z, 1, 0.5, 0.5, 0.5, 0.12);
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 3, radius, 0.5, radius, 0.05);
                level.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + 1.5, pos.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.5, pos.z, 5, 0.4, 0.5, 0.4, 0.1);
            }

            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius))) {
                if (e.distanceTo(target) <= radius) {
                    e.hurt(warrior.damageSources().playerAttack(warrior), baseDamage);
                }
            }
        }
    }

    public static void tickUltimateEffects(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            if (player.getPersistentData().getLong("lvluping_berserk_until") > time) {
                if (time % 5 == 0) {
                    level.sendParticles(ParticleTypes.LAVA, x, y + 0.5, z, 2, 0.3, 0.2, 0.3, 0.01);
                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y + 1.0, z, 3, 0.25, 0.3, 0.25, 0.02);
                }
            }

            long brotherhoodUntil = player.getPersistentData().getLong("lvluping_brotherhood_until");
            if (brotherhoodUntil > time) {
                double cx = player.getPersistentData().getDouble("lvluping_brotherhood_x");
                double cy = player.getPersistentData().getDouble("lvluping_brotherhood_y");
                double cz = player.getPersistentData().getDouble("lvluping_brotherhood_z");
                if (time % 8 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (time * 0.1 + i * 0.5) % (Math.PI * 2);
                        double rx = Math.cos(angle) * BROTHERHOOD_RADIUS;
                        double rz = Math.sin(angle) * BROTHERHOOD_RADIUS;
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx + rx, cy + 0.1, cz + rz, 1, 0.05, 0.05, 0.05, 0.01);
                        level.sendParticles(ParticleTypes.ENCHANT, cx + rx * 0.7, cy + 0.3, cz + rz * 0.7, 1, 0, 0.02, 0, 0.01);
                    }
                }
            }

            if (player.getPersistentData().getLong("lvluping_invulnerability_until") > time) {
                if (time % 2 == 0) {
                    final double auraRadius = 1.2;
                    for (int i = 0; i < 24; i++) {
                        double angle = (time * 0.08 + i * (Math.PI * 2 / 24)) % (Math.PI * 2);
                        double rx = Math.cos(angle) * auraRadius;
                        double rz = Math.sin(angle) * auraRadius;
                        level.sendParticles(ParticleTypes.END_ROD, x + rx, y + 0.5, z + rz, 2, 0.05, 0.1, 0.05, 0.01);
                        level.sendParticles(ParticleTypes.ENCHANT, x + rx * 0.7, y + 0.6, z + rz * 0.7, 1, 0.03, 0.05, 0.03, 0.008);
                    }
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.8, z, 2, 0.2, 0.2, 0.2, 0.008);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, x, y + 0.5, z, 3, 0.35, 0.35, 0.35, 0.015);
                }
                if (time % 40 == 0) {
                    level.playSound(null, x, y, z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.2f, 1.5f);
                }
            }
        }
    }
}
