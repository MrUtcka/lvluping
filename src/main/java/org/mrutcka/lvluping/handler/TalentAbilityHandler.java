package org.mrutcka.lvluping.handler;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.compat.ArsManaCompat;
import org.mrutcka.lvluping.network.S2CProvocationHint;
import org.mrutcka.lvluping.network.S2CSyncCooldown;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TalentAbilityHandler {

    /** Цель перед игроком для Финального отсчёта: рейкаст или ближайшая сущность в конусе. */
    private static LivingEntity getTargetInFront(ServerPlayer player, double range, double coneDeg) {
        HitResult hit = player.pick(range, 0f, false);
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le && le != player)
            return le;
        Vec3 look = player.getLookAngle().normalize();
        double coneRad = Math.toRadians(coneDeg);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))
                .stream()
                .filter(e -> e != player && e.isAlive())
                .filter(e -> {
                    Vec3 to = e.position().subtract(player.position()).normalize();
                    return look.dot(to) >= Math.cos(coneRad) && player.distanceTo(e) <= range;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static final int SLIDE_COOLDOWN = 200;
    private static final int SMOKE_COOLDOWN = 400;
    private static final int DASH_COOLDOWN = 160;
    private static final int BUFF_COOLDOWN = 600;
    private static final int BARRIER_WINDOW = 200;
    private static final int M_FIRE_COOLDOWN = 80;
    private static final int M_ICE_COOLDOWN = 80;
    private static final int M_TELEPORT_COOLDOWN = 120;
    private static final int M_SUMMON_COOLDOWN = 200;
    private static final int M_SACRIFICE_COOLDOWN = 60;
    private static final int M_COMMAND_COOLDOWN = 40;

    public static final int SHIELD_WINDOW = 40;
    public static final int SHIELD_COOLDOWN = 160;
    private static final int SHIELD_STUN_DURATION = 40;
    private static final int SEISMIC_COOLDOWN = 200;
    private static final int IRON_SKIN_COOLDOWN = 300;
    private static final int SPIN_COOLDOWN = 220;
    private static final int HEAVY_STEP_COOLDOWN = 160;
    public static final int UNBREAKABLE_COOLDOWN = 1800;
    private static final int PARRY_WINDOW = 20;
    private static final int PARRY_COOLDOWN = 100;
    public static final int PROVOCATION_COOLDOWN = 240;
    public static final int PROVOCATION_DURATION_TICKS = 60;
    private static final int ULT_BERSERK_DURATION = 160;
    private static final int ULT_BERSERK_COOLDOWN = 600;
    private static final int ULT_BROTHERHOOD_DURATION = 120;
    private static final int ULT_BROTHERHOOD_COOLDOWN = 500;
    private static final int ULT_FINAL_COUNTDOWN_DELAY = 60;
    private static final int ULT_FINAL_COUNTDOWN_COOLDOWN = 400;
    private static final int ULT_INVULNERABILITY_DURATION = 120;
    private static final int ULT_INVULNERABILITY_COOLDOWN = 500;

    public static boolean isDagger(Item item) {
        return item == Items.IRON_SWORD;
    }

    private static void setCooldown(ServerPlayer player, String key, int ticks) {
        PlayerLevels.setCooldown(player.getUUID(), key, ticks);
        player.getPersistentData().putInt(key, ticks);
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, ticks));
    }

    public static void syncAllCooldowns(ServerPlayer player) {
        String[] keys = {
                "cd_slide", "cd_smoke", "cd_dash", "cd_parry", "cd_buff",
                "cd_w_shield", "cd_w_seismic", "cd_w_iron_skin",
                "cd_w_spin", "cd_w_heavy_step", "cd_w_unbreakable", "cd_w_armor_breaker",
                "cd_w_provocation", "cd_w_ult_berserk", "cd_w_ult_brotherhood", "cd_w_ult_final_countdown", "cd_w_ult_invulnerability"
                , "cd_m_fire", "cd_m_ice", "cd_m_teleport", "cd_m_summon", "cd_m_sacrifice", "cd_m_command"
        };
        for (String key : keys) {
            int val = PlayerLevels.getCooldown(player.getUUID(), key);
            player.getPersistentData().putInt(key, val);
            if (val > 0) {
                PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, val));
            }
        }
    }

    public static void handleAbilityUse(ServerPlayer player, int slot) {
        Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
        var data = player.getPersistentData();

        switch (slot) {
            case 0 -> {
                // W_SHIELD_STRIKE
                if (talents.contains("w_shield_strike")) {
                    boolean hasShield =
                            player.getMainHandItem().getItem() instanceof ShieldItem ||
                            player.getOffhandItem().getItem() instanceof ShieldItem;
                    if (!hasShield) break;

                    int window = data.getInt("lvluping_shield_window");
                    int cd = data.getInt("cd_w_shield");

                    if (window > 0 && cd <= 0) {
                        if (player.level() instanceof ServerLevel serverLevel) {
                            Vec3 look = player.getLookAngle().normalize();
                            double range = 3.0;
                            LivingEntity hitTarget = null;

                            for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(range, 1.0, range))) {
                                if (e == player) continue;
                                Vec3 to = e.position().subtract(player.position()).normalize();
                                if (look.dot(to) > 0.5 && player.distanceTo(e) <= range) {
                                    hitTarget = e;
                                    break;
                                }
                            }

                            if (hitTarget != null) {
                                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 2.0f;
                                hitTarget.hurt(player.damageSources().playerAttack(player), baseDamage);

                                hitTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHIELD_STUN_DURATION, 4, false, false));
                                hitTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SHIELD_STUN_DURATION, 1, false, false));

                                serverLevel.playSound(null, hitTarget.getX(), hitTarget.getY(), hitTarget.getZ(),
                                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.8f);
                                serverLevel.sendParticles(ParticleTypes.CRIT, hitTarget.getX(), hitTarget.getY() + 1.0, hitTarget.getZ(),
                                        10, 0.4, 0.4, 0.4, 0.2);
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, hitTarget.getX(), hitTarget.getY() + 0.3, hitTarget.getZ(),
                                        4, 0.0, 0.0, 0.0, 0.0);
                            }
                        }

                        data.putInt("lvluping_shield_window", 0);
                        setCooldown(player, "cd_w_shield", SHIELD_COOLDOWN);
                        return;
                    }

                    if (cd <= 0 && window <= 0) {
                        data.putInt("lvluping_shield_window", SHIELD_WINDOW);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.2f);
                        if (player.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(),
                                    10, 0.3, 0.3, 0.3, 0.1);
                        }
                        return;
                    }
                }

                // --- W_PARRY ---
                if (talents.contains("w_parry") && data.getInt("cd_parry") <= 0) {
                    setCooldown(player, "lvluping_parry_window", PARRY_WINDOW);
                    setCooldown(player, "cd_parry", PARRY_COOLDOWN);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0f, 1.0f);
                    return;
                }

                if (talents.contains("m_summon_servant") && data.getInt("cd_m_summon") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double cost = 60.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_summon", 20);
                            return;
                        }
                        Mob summon;
                        if (player.isCrouching()) {
                            summon = net.minecraft.world.entity.EntityType.ZOMBIE.create(serverLevel);
                        } else {
                            summon = net.minecraft.world.entity.EntityType.SKELETON.create(serverLevel);
                        }
                        if (summon == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_summon", 20);
                            return;
                        }
                        Vec3 forward = player.getLookAngle().normalize();
                        double sx = player.getX() + forward.x * 2.0;
                        double sy = player.getY();
                        double sz = player.getZ() + forward.z * 2.0;
                        summon.moveTo(sx, sy, sz, player.getYRot(), 0);
                        serverLevel.addFreshEntity(summon);
                        long until = serverLevel.getGameTime() + 20L * 30L;
                        SummonerHandler.addSummon(player, summon, until);
                        serverLevel.playSound(null, sx, sy, sz, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.9f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, sx, sy + 0.8, sz, 15, 0.3, 0.4, 0.3, 0.02);
                        setCooldown(player, "cd_m_summon", M_SUMMON_COOLDOWN);
                    }
                    return;
                }

                if (talents.contains("m_fire_lightning") && data.getInt("cd_m_fire") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        boolean lightning = player.isCrouching();
                        double cost = lightning ? 45.0 : 30.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_fire", 20);
                            return;
                        }
                        if (lightning) {
                            LivingEntity target = getTargetInFront(player, 20.0, 20.0);
                            if (target == null) {
                                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                                setCooldown(player, "cd_m_fire", 20);
                                return;
                            }
                            var bolt = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, serverLevel);
                            bolt.moveTo(target.getX(), target.getY(), target.getZ());
                            bolt.setVisualOnly(true);
                            serverLevel.addFreshEntity(bolt);
                            target.hurt(player.damageSources().playerAttack(player), 8.0f);
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.6, 0.4, 0.15);
                        } else {
                            Vec3 look = player.getLookAngle().normalize();
                            SmallFireball fb = net.minecraft.world.entity.EntityType.SMALL_FIREBALL.create(serverLevel);
                            if (fb != null) {
                                fb.setOwner(player);
                                fb.moveTo(player.getX(), player.getEyeY() - 0.1, player.getZ(), player.getYRot(), player.getXRot());
                                fb.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
                                serverLevel.addFreshEntity(fb);
                            }
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.2f);
                        }
                        setCooldown(player, "cd_m_fire", M_FIRE_COOLDOWN);
                    }
                    return;
                }

                // --- M_BARRIER ---
                if (talents.contains("m_barrier") && data.getInt("cd_buff") <= 0) {
                    setCooldown(player, "cd_buff", BUFF_COOLDOWN);
                    setCooldown(player, "lvluping_barrier_window", BARRIER_WINDOW);

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);

                    if (talents.contains("m_buff_def")) {
player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2, false, false));
                    if (talents.contains("m_buff_atk")) {
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false));
                        }
                    }
                }
            }
            case 1 -> {
                // --- W_PROVOCATION ---
                if (talents.contains("w_provocation") && data.getInt("cd_w_provocation") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        long until = serverLevel.getGameTime() + PROVOCATION_DURATION_TICKS;
                        player.getPersistentData().putLong("lvluping_provocation_until", until);
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, PROVOCATION_DURATION_TICKS, 0, false, false));
                        org.mrutcka.lvluping.handler.ProvocationHandler.setProvokerTeam(player, true);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.7f);
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                        setCooldown(player, "cd_w_provocation", PROVOCATION_COOLDOWN);
                        for (ServerPlayer p : serverLevel.players()) {
                            if (p != player) PacketDistributor.sendToPlayer(p, new S2CProvocationHint(true));
                        }
                    }
                    return;
                }

                // --- AS_SLIDE ---
                if (talents.contains("as_slide") && data.getInt("cd_slide") <= 0) {
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(look.x * 1.4, 0, look.z * 1.4);
                    player.hurtMarked = true;
                    setCooldown(player, "cd_slide", SLIDE_COOLDOWN);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
                }

                // --- W_HEAVY_STEP ---
                if (talents.contains("w_heavy_step") && data.getInt("cd_w_heavy_step") <= 0) {
                    Vec3 look = player.getLookAngle().normalize();
                    player.setDeltaMovement(look.x * 1.4, 0.1, look.z * 1.4);
                    player.hurtMarked = true;

                    if (player.level() instanceof ServerLevel serverLevel) {
                        double range = 3.5;
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range, 1.0, range))) {
                            if (e == player) continue;
                            if (player.distanceTo(e) <= range) {
                                Vec3 kb = e.position().subtract(player.position()).normalize().scale(1.2);
                                e.push(kb.x, 0.4, kb.z);
                                e.hurt(player.damageSources().playerAttack(player), 2);
                            }
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.0f, 0.6f);
                        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                                20, 0.4, 0.1, 0.4, 0.05);
                    }

                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 1, false, false));
                    setCooldown(player, "cd_w_heavy_step", HEAVY_STEP_COOLDOWN);
                    return;
                }

                if (talents.contains("m_summon_sacrifice") && data.getInt("cd_m_sacrifice") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double cost = 10.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_sacrifice", 20);
                            return;
                        }
                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        if (summons.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_sacrifice", 20);
                            return;
                        }
                        Mob mob = summons.get(0);
                        mob.discard();
                        player.heal(5.0f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.6f);
                        serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.5, 0.4, 0.1);
                        setCooldown(player, "cd_m_sacrifice", M_SACRIFICE_COOLDOWN);
                    }
                    return;
                }

                if (talents.contains("m_ice_arrow") && data.getInt("cd_m_ice") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double cost = 25.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ice", 20);
                            return;
                        }
                        LivingEntity target = getTargetInFront(player, 18.0, 25.0);
                        if (target == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ice", 20);
                            return;
                        }
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, false));
                        target.hurt(player.damageSources().playerAttack(player), 6.0f);
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
                        setCooldown(player, "cd_m_ice", M_ICE_COOLDOWN);
                    }
                    return;
                }
            }
            case 2 -> {
                // --- W_SPIN ---
                if (talents.contains("w_spin") && data.getInt("cd_w_spin") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double range = 3.0;
                        int hitCount = 0;

                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range, 1.0, range))) {
                            if (e == player) continue;
                            if (player.distanceTo(e) <= range) {
                                float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                                e.hurt(player.damageSources().playerAttack(player), dmg);
                                hitCount++;
                                serverLevel.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 1.0, e.getZ(),
                                        6, 0.3, 0.4, 0.3, 0.15);
                            }
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.5, player.getZ(),
                                16, 0.0, 0.0, 0.0, 0.0);

                        int cd = SPIN_COOLDOWN;
                        if (hitCount >= 3) cd = SPIN_COOLDOWN / 2;
                        setCooldown(player, "cd_w_spin", cd);
                    }
                    return;
                }

                // --- W_SEISMIC ---
                if (talents.contains("w_seismic") && data.getInt("cd_w_seismic") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        Vec3 look = player.getLookAngle().normalize();
                        double range = 6.0;
                        double coneHalfAngleRad = Math.toRadians(40);
                        double angleCos = Math.cos(coneHalfAngleRad);

                        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8f + 3.0f;
                        Vec3 playerPos = player.position();

                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range, 2.0, range))) {
                            if (e == player) continue;
                            Vec3 to = e.position().subtract(playerPos).normalize();
                            if (look.dot(to) > angleCos && player.distanceTo(e) <= range) {
                                e.hurt(player.damageSources().playerAttack(player), baseDamage);
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                                Vec3 knockback = to.scale(0.5).add(0, 0.25, 0);
                                e.push(knockback.x, knockback.y, knockback.z);
                            }
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.5f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.3f, 0.7f);

                        double yaw = Math.atan2(-look.x, look.z);
                        int rays = 7;
                        int stepsPerRay = 14;
                        for (int r = 0; r < rays; r++) {
                            double angle = -coneHalfAngleRad + (2.0 * coneHalfAngleRad * r / Math.max(1, rays - 1));
                            double rayYaw = yaw + angle;
                            double dx = -Math.sin(rayYaw);
                            double dz = Math.cos(rayYaw);
                            for (int s = 1; s <= stepsPerRay; s++) {
                                double dist = (range * s) / stepsPerRay;
                                double px = player.getX() + dx * dist + (serverLevel.random.nextDouble() - 0.5) * 0.3;
                                double pz = player.getZ() + dz * dist + (serverLevel.random.nextDouble() - 0.5) * 0.3;
                                double py = player.getY() + 0.1 + serverLevel.random.nextDouble() * 0.2;
                                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0.02, 0.02, 0.02, 0.01);
                                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), px, py, pz, 1, 0.05, 0.02, 0.05, 0.02);
                                if (s % 2 == 0) {
                                    serverLevel.sendParticles(ParticleTypes.POOF, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                                }
                            }
                        }
                    }
                    setCooldown(player, "cd_w_seismic", SEISMIC_COOLDOWN);
                    return;
                }

                // --- A_DASH ---
                if (talents.contains("a_dash") && data.getInt("cd_dash") <= 0) {
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(-look.x * 1.2, 0, -look.z * 1.2);
                    player.hurtMarked = true;
                    setCooldown(player, "cd_dash", DASH_COOLDOWN);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 2.0f);
                    return;
                }

                if (talents.contains("m_summon_command") && data.getInt("cd_m_command") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double cost = 5.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", 20);
                            return;
                        }
                        LivingEntity target = getTargetInFront(player, 25.0, 20.0);
                        if (target == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", 20);
                            return;
                        }
                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        if (summons.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", 20);
                            return;
                        }
                        for (Mob m : summons) {
                            m.setTarget(target);
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.7f, 1.4f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.4, 0.6, 0.4, 0.12);
                        setCooldown(player, "cd_m_command", M_COMMAND_COOLDOWN);
                    }
                    return;
                }

                if (talents.contains("m_teleport") && data.getInt("cd_m_teleport") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        double cost = 40.0;
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_teleport", 20);
                            return;
                        }
                        Vec3 look = player.getLookAngle().normalize();
                        double dist = 7.0;
                        Vec3 start = player.position();
                        Vec3 dest = start.add(look.scale(dist));
                        for (int i = 0; i < 6; i++) {
                            if (serverLevel.noCollision(player, player.getBoundingBox().move(dest.x - start.x, dest.y - start.y, dest.z - start.z))) break;
                            dist -= 1.0;
                            dest = start.add(look.scale(dist));
                        }
                        player.teleportTo(serverLevel, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
                        player.resetFallDistance();
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                        serverLevel.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 30, 0.5, 0.8, 0.5, 0.1);
                        setCooldown(player, "cd_m_teleport", M_TELEPORT_COOLDOWN);
                    }
                    return;
                }
            }
            case 3 -> {
                // --- W_IRON_SKIN ---
                if (talents.contains("w_iron_skin") && data.getInt("cd_w_iron_skin") <= 0) {
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.WEAKNESS);

                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, false));

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0f, 0.5f);
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                                20, 0.4, 0.6, 0.4, 0.02);
                    }

                    setCooldown(player, "cd_w_iron_skin", IRON_SKIN_COOLDOWN);
                    return;
                }

                // --- AS_SMOKE ---
                if (talents.contains("as_smoke") && data.getInt("cd_smoke") <= 0) {
                    ServerLevel level = player.serverLevel();

                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    for (int i = 0; i < 60; i++) {
                        double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                        double offsetY = level.random.nextDouble() * 1.5;
                        double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;

                        level.sendParticles(
                                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                player.getX() + offsetX,
                                player.getY() + offsetY,
                                player.getZ() + offsetZ,
                                2,
                                0.0, 0.1, 0.0,
                                0.05
                        );
                    }

                    for (int i = 0; i < 20; i++) {
                        level.sendParticles(
                                ParticleTypes.LARGE_SMOKE,
                                player.getX() + (level.random.nextDouble() - 0.5) * 2,
                                player.getY() + 0.5,
                                player.getZ() + (level.random.nextDouble() - 0.5) * 2,
                                2, 0.1, 0.2, 0.1, 0.02
                        );
                    }

                    setCooldown(player, "cd_smoke", SMOKE_COOLDOWN);
                }
            }
            case 4 -> {
                // --- W_ULT_BERSERK ---
                if (talents.contains("w_ult_berserk") && data.getInt("cd_w_ult_berserk") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        long until = serverLevel.getGameTime() + ULT_BERSERK_DURATION;
                        player.getPersistentData().putLong("lvluping_berserk_until", until);
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ULT_BERSERK_DURATION, 0, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ULT_BERSERK_DURATION, 0, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ULT_BERSERK_DURATION, 1, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP, ULT_BERSERK_DURATION, 0, false, false));
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.5f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.8f);
                        for (int i = 0; i < 40; i++) {
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.5, 0.5, 0.5, 0.08);
                            serverLevel.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.3, 0.2, 0.3, 0.02);
                            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 1, 0.25, 0.25, 0.25, 0.05);
                        }
                        setCooldown(player, "cd_w_ult_berserk", ULT_BERSERK_COOLDOWN);
                    }
                    return;
                }
                // --- W_ULT_BROTHERHOOD ---
                if (talents.contains("w_ult_brotherhood") && data.getInt("cd_w_ult_brotherhood") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        long until = serverLevel.getGameTime() + ULT_BROTHERHOOD_DURATION;
                        player.getPersistentData().putLong("lvluping_brotherhood_until", until);
                        player.getPersistentData().putDouble("lvluping_brotherhood_x", player.getX());
                        player.getPersistentData().putDouble("lvluping_brotherhood_y", player.getY());
                        player.getPersistentData().putDouble("lvluping_brotherhood_z", player.getZ());
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.9f, 0.5f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.2f);
                        for (int i = 0; i < 60; i++) {
                            double angle = (i / 60.0) * Math.PI * 2;
                            double rx = Math.cos(angle) * 6;
                            double rz = Math.sin(angle) * 6;
                            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX() + rx, player.getY() + 0.2, player.getZ() + rz, 2, 0.1, 0.1, 0.1, 0.03);
                            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX() + rx * 0.5, player.getY() + 0.5, player.getZ() + rz * 0.5, 1, 0, 0.05, 0, 0.02);
                        }
                        setCooldown(player, "cd_w_ult_brotherhood", ULT_BROTHERHOOD_COOLDOWN);
                    }
                    return;
                }
                // --- W_ULT_FINAL_COUNTDOWN ---
                if (talents.contains("w_ult_final_countdown") && data.getInt("cd_w_ult_final_countdown") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        LivingEntity target = getTargetInFront(player, 10.0, 25.0);
                        if (target != null) {
                            long at = serverLevel.getGameTime() + ULT_FINAL_COUNTDOWN_DELAY;
                            player.getPersistentData().putLong("lvluping_final_countdown_at", at);
                            player.getPersistentData().putUUID("lvluping_final_countdown_target", target.getUUID());
                            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
                            for (ServerPlayer p : serverLevel.players()) {
                                PacketDistributor.sendToPlayer(p, new org.mrutcka.lvluping.network.S2CJudgementHammerEffect(tx, ty, tz, ULT_FINAL_COUNTDOWN_DELAY, target.getUUID()));
                            }
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 0.7f);
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.5f);
                            for (int i = 0; i < 25; i++) {
                                serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.4, 0.6, 0.4, 0.15);
                                serverLevel.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0.2, 0.3, 0.2, 0.05);
                            }
                            setCooldown(player, "cd_w_ult_final_countdown", ULT_FINAL_COUNTDOWN_COOLDOWN);
                        } else {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.5f);
                            setCooldown(player, "cd_w_ult_final_countdown", 20);
                        }
                    }
                    return;
                }
                // --- W_ULT_INVULNERABILITY ---
                if (talents.contains("w_ult_invulnerability") && data.getInt("cd_w_ult_invulnerability") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        long until = serverLevel.getGameTime() + ULT_INVULNERABILITY_DURATION;
                        player.getPersistentData().putLong("lvluping_invulnerability_until", until);
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, ULT_INVULNERABILITY_DURATION, 0, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ULT_INVULNERABILITY_DURATION, 2, false, false));
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.0f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);
                        for (int i = 0; i < 40; i++) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.5, 0.5, 0.5, 0.02);
                            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.4, 0.4, 0.4, 0.015);
                        }
                        setCooldown(player, "cd_w_ult_invulnerability", ULT_INVULNERABILITY_COOLDOWN);
                    }
                    return;
                }
            }
        }
    }
}

