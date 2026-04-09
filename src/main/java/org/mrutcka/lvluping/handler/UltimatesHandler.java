package org.mrutcka.lvluping.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.handler.TalentAbilityHandler;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.util.AllyHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public final class UltimatesHandler {

    private static final double BROTHERHOOD_RADIUS = 6.0;
    private static final int BROTHERHOOD_EFFECT_DURATION_TICKS = 40;
    private static final int BROTHERHOOD_PLAYER_DAMAGE_RESISTANCE_AMPLIFIER = 1;
    private static final int BROTHERHOOD_PLAYER_REGENERATION_AMPLIFIER = 0;
    private static final int BROTHERHOOD_ENEMY_SLOWDOWN_AMPLIFIER = 2;
    private static final double BROTHERHOOD_BOX_Y_MIN_OFFSET = 2.0;
    private static final double BROTHERHOOD_BOX_Y_MAX_OFFSET = 4.0;
    private static final int LIGHT_RAY_EFFECT_INTERVAL_TICKS = 20;
    private static final int LIGHT_RAY_UNDEAD_SLOW_DURATION_TICKS = 40;
    private static final int LIGHT_RAY_UNDEAD_SLOW_AMPLIFIER = 1;
    private static final double LIGHT_RAY_BEAM_RADIUS_BLOCKS = 1.0;
    private static final int LIGHT_RAY_BEACON_LAYER_END_ROD_COUNT = 6;
    private static final int LIGHT_RAY_BEACON_LAYER_ENCHANT_COUNT = 4;
    private static final double LIGHT_RAY_BEACON_LAYER_DISK_THICKNESS = 0.08;
    private static final double LIGHT_RAY_BEACON_PARTICLE_SPEED = 0.02;
    private static final int LIGHT_RAY_PARTICLE_INTERVAL_TICKS = 4;
    private static final String NBT_LIGHT_RAY_LIGHT_POS = "lvluping_c_light_ray_light_pos";
    private static final String NBT_W_LIGHT_FORM_LIGHT = "lvluping_w_light_form_light_pos";
    private static final ResourceLocation LIGHT_FORM_MOVE_LOCK_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "light_form_move_lock");

    private static final BlockState LIGHT_RAY_BLOCK_LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);

    private static final int LIGHT_SPHERE_EFFECT_INTERVAL_TICKS = 20;

    private static final int SLOW_SPHERE_EFFECT_INTERVAL_TICKS = 10;
    private static final double SLOW_SPHERE_PLATE_HALF_HEIGHT = 0.5;

    public static void playLightRayBeaconVisual(ServerLevel level, double cx, double cz, double yMin, double yMax) {
        if (yMax <= yMin) return;
        int yStart = Mth.floor(yMin);
        int yEnd = Mth.floor(yMax);
        double spread = LIGHT_RAY_BEAM_RADIUS_BLOCKS;
        for (int y = yStart; y <= yEnd; y++) {
            double py = y + 0.5;
            level.sendParticles(ParticleTypes.END_ROD, cx, py, cz, LIGHT_RAY_BEACON_LAYER_END_ROD_COUNT,
                    spread, LIGHT_RAY_BEACON_LAYER_DISK_THICKNESS, spread, LIGHT_RAY_BEACON_PARTICLE_SPEED);
            level.sendParticles(ParticleTypes.ENCHANT, cx, py, cz, LIGHT_RAY_BEACON_LAYER_ENCHANT_COUNT,
                    spread * 0.85, LIGHT_RAY_BEACON_LAYER_DISK_THICKNESS * 0.85, spread * 0.85, LIGHT_RAY_BEACON_PARTICLE_SPEED * 0.6);
        }
    }

    public static void placeLightRayLightBlocks(ServerLevel level, ServerPlayer player, double cx, double cz, double yMin, double yMax) {
        int yStart = Mth.floor(yMin);
        int yEnd = Mth.floor(yMax);
        int bx = Mth.floor(cx);
        int bz = Mth.floor(cz);
        ArrayList<Long> placed = new ArrayList<>();
        for (int y = yStart; y <= yEnd; y++) {
            BlockPos pos = new BlockPos(bx, y, bz);
            if (!level.hasChunkAt(pos)) continue;
            BlockState was = level.getBlockState(pos);
            if (was.isAir()) {
                if (level.setBlock(pos, LIGHT_RAY_BLOCK_LIGHT, 3)) {
                    placed.add(pos.asLong());
                }
            } else if (was.is(Blocks.WATER)) {
                BlockState inWater = LIGHT_RAY_BLOCK_LIGHT.setValue(LightBlock.WATERLOGGED, true);
                if (level.setBlock(pos, inWater, 3)) {
                    placed.add(pos.asLong());
                }
            }
        }
        long[] arr = new long[placed.size()];
        for (int i = 0; i < placed.size(); i++) {
            arr[i] = placed.get(i);
        }
        player.getPersistentData().putLongArray(NBT_LIGHT_RAY_LIGHT_POS, arr);
    }

    public static void removeLightRayLightBlocks(ServerLevel level, CompoundTag pd) {
        if (!pd.contains(NBT_LIGHT_RAY_LIGHT_POS)) return;
        long[] arr = pd.getLongArray(NBT_LIGHT_RAY_LIGHT_POS);
        for (long packed : arr) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.hasChunkAt(pos)) continue;
            BlockState bs = level.getBlockState(pos);
            if (!bs.is(Blocks.LIGHT)) continue;
            if (bs.getValue(LightBlock.WATERLOGGED)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        pd.remove(NBT_LIGHT_RAY_LIGHT_POS);
    }

    public static void removeLightFormBlocks(ServerLevel level, CompoundTag pd) {
        if (!pd.contains(NBT_W_LIGHT_FORM_LIGHT)) return;
        long[] arr = pd.getLongArray(NBT_W_LIGHT_FORM_LIGHT);
        for (long packed : arr) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.hasChunkAt(pos)) continue;
            BlockState bs = level.getBlockState(pos);
            if (!bs.is(Blocks.LIGHT)) continue;
            if (bs.getValue(LightBlock.WATERLOGGED)) {
                level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        pd.remove(NBT_W_LIGHT_FORM_LIGHT);
    }

    private static boolean tryPlaceLightBlock(ServerLevel level, BlockPos pos, HashSet<Long> seen, ArrayList<Long> placed) {
        if (!level.hasChunkAt(pos)) return false;
        if (!seen.add(pos.asLong())) return false;
        BlockState was = level.getBlockState(pos);
        if (was.isAir()) {
            if (level.setBlock(pos, LIGHT_RAY_BLOCK_LIGHT, 3)) {
                placed.add(pos.asLong());
                return true;
            }
        } else if (was.is(Blocks.WATER)) {
            BlockState inWater = LIGHT_RAY_BLOCK_LIGHT.setValue(LightBlock.WATERLOGGED, true);
            if (level.setBlock(pos, inWater, 3)) {
                placed.add(pos.asLong());
                return true;
            }
        }
        return false;
    }

    private static void refreshLightFormBlocks(ServerLevel level, ServerPlayer player, double radius) {
        CompoundTag pd = player.getPersistentData();
        removeLightFormBlocks(level, pd);
        if (radius < 0.5) return;
        double px = player.getX();
        double pz = player.getZ();
        int baseY = Mth.floor(player.getY());
        int segments = Mth.clamp(Mth.ceil(2 * Math.PI * radius * 1.4), 24, 128);
        HashSet<Long> seen = new HashSet<>();
        ArrayList<Long> placed = new ArrayList<>();
        for (int i = 0; i < segments; i++) {
            double ang = (Math.PI * 2) * i / segments;
            double wx = px + Math.cos(ang) * radius;
            double wz = pz + Math.sin(ang) * radius;
            BlockPos pos = BlockPos.containing(wx, baseY, wz);
            tryPlaceLightBlock(level, pos, seen, placed);
        }
        long[] out = new long[placed.size()];
        for (int i = 0; i < placed.size(); i++) {
            out[i] = placed.get(i);
        }
        if (out.length > 0) {
            pd.putLongArray(NBT_W_LIGHT_FORM_LIGHT, out);
        }
    }

    public static void applyLightFormMoveLock(ServerPlayer player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(LIGHT_FORM_MOVE_LOCK_ID);
            attr.addTransientModifier(new AttributeModifier(LIGHT_FORM_MOVE_LOCK_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void clearLightFormMoveLock(ServerPlayer player) {
        var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(LIGHT_FORM_MOVE_LOCK_ID);
        }
    }

    public static void clearLightFormNoGravity(ServerPlayer player) {
        player.setNoGravity(false);
    }

    private static boolean isUndead(LivingEntity e) {
        if (!(e instanceof net.minecraft.world.entity.Mob mob)) return false;
        return mob.getType() == EntityType.SKELETON
                || mob.getType() == EntityType.ZOMBIE
                || mob.getType() == EntityType.STRAY
                || mob.getType() == EntityType.WITHER_SKELETON;
    }

    public static boolean isBerserkActive(Player player) {
        if (player.level().isClientSide()) return false;
        long until = player.getPersistentData().getLong("lvluping_berserk_until");
        return until > 0 && player.level().getGameTime() < until;
    }

    public static boolean isInvulnerabilityActive(Player player) {
        if (player.level().isClientSide()) return false;
        long until = player.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
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

            AABB box = new AABB(cx - BROTHERHOOD_RADIUS, cy - BROTHERHOOD_BOX_Y_MIN_OFFSET, cz - BROTHERHOOD_RADIUS,
                    cx + BROTHERHOOD_RADIUS, cy + BROTHERHOOD_BOX_Y_MAX_OFFSET, cz + BROTHERHOOD_RADIUS);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e.distanceToSqr(center) > BROTHERHOOD_RADIUS * BROTHERHOOD_RADIUS) continue;
                if (e == warrior) continue;
                if (e instanceof Player p) {
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BROTHERHOOD_EFFECT_DURATION_TICKS, BROTHERHOOD_PLAYER_DAMAGE_RESISTANCE_AMPLIFIER, false, false));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BROTHERHOOD_EFFECT_DURATION_TICKS, BROTHERHOOD_PLAYER_REGENERATION_AMPLIFIER, false, false));
                } else {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BROTHERHOOD_EFFECT_DURATION_TICKS, BROTHERHOOD_ENEMY_SLOWDOWN_AMPLIFIER, false, false));
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

            int lvl = PlayerLevels.getAbilityLevel(warrior.getUUID(), "w_ult_final_countdown", PlayerLevels.getPlayerTalents(warrior.getUUID()));
            double hpRatio = target.getHealth() / Math.max(0.01, target.getMaxHealth());
            float base = (float) AbilityUpgradeConfig.getDouble("w_ult_final_countdown", "base_damage", lvl, 15.0);
            float scale = (float) AbilityUpgradeConfig.getDouble("w_ult_final_countdown", "missing_hp_scale", lvl, 20.0);
            float baseDamage = base + (float) (2.0 - hpRatio) * scale;
            double radius = AbilityUpgradeConfig.getDouble("w_ult_final_countdown", "radius", lvl, 4.0);
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

            long divProtUntil = player.getPersistentData().getLong("lvluping_cleric_divine_protection_until");
            if (divProtUntil > time) {
                if (time % 20 == 0) {
                    float hps = player.getPersistentData().getFloat("lvluping_cleric_divine_hps");
                    if (hps > 0) {
                        player.heal(hps);
                    }
                }
            } else if (divProtUntil > 0) {
                var dpd = player.getPersistentData();
                dpd.remove("lvluping_cleric_divine_protection_until");
                dpd.remove("lvluping_cleric_divine_shield_pct");
                dpd.remove("lvluping_cleric_divine_hps");
            }

            // --- M_ULT_CHAOS ---
            long chaosUntil = player.getPersistentData().getLong("lvluping_m_ult_chaos_wave_until");
            if (chaosUntil > time) {
                var pd = player.getPersistentData();
                double cx = pd.getDouble("lvluping_m_ult_chaos_center_x");
                double cy = pd.getDouble("lvluping_m_ult_chaos_center_y");
                double cz = pd.getDouble("lvluping_m_ult_chaos_center_z");
                double maxR = pd.getDouble("lvluping_m_ult_chaos_max_r");
                double prevR = pd.getDouble("lvluping_m_ult_chaos_prev_r");
                long dur = pd.getLong("lvluping_m_ult_chaos_wave_dur");

                float dmg = pd.getFloat("lvluping_m_ult_chaos_dmg");
                int slowTicks = pd.getInt("lvluping_m_ult_chaos_slow_ticks");
                int slowAmp = pd.getInt("lvluping_m_ult_chaos_slow_amp");
                int burnTicks = pd.getInt("lvluping_m_ult_chaos_burn_ticks");

                if (dur <= 0) dur = 1;
                long elapsed = dur - (chaosUntil - time);
                double progress = Math.min(1.0, Math.max(0.0, elapsed / (double) dur));
                double currR = maxR * progress;

                Vec3 center = new Vec3(cx, cy, cz);
                double prevR2 = prevR * prevR;
                double currR2 = currR * currR;

                AABB search = new AABB(cx - maxR, cy - 2, cz - maxR, cx + maxR, cy + 4, cz + maxR);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search)) {
                    if (e == player) continue;
                    if (e.distanceToSqr(center) <= currR2 && e.distanceToSqr(center) > prevR2) {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), burnTicks));
                        if (slowTicks > 0) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmp, false, false));
                        }
                    }
                }

                if (time % 2 == 0) {
                    int points = 14;
                    for (int i = 0; i < points; i++) {
                        double ang = (time * 0.15 + i * (Math.PI * 2 / points));
                        double px = cx + Math.cos(ang) * currR;
                        double pz = cz + Math.sin(ang) * currR;
                        double py = cy + 0.8;
                        level.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, 0.15, 0.25, 0.15, 0.01);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, px, py + 0.2, pz, 2, 0.15, 0.25, 0.15, 0.02);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py + 0.4, pz, 2, 0.15, 0.25, 0.15, 0.08);
                    }
                }

                pd.putDouble("lvluping_m_ult_chaos_prev_r", currR);
            } else if (player.getPersistentData().contains("lvluping_m_ult_chaos_wave_until")) {
                var pd = player.getPersistentData();
                pd.remove("lvluping_m_ult_chaos_wave_until");
                pd.remove("lvluping_m_ult_chaos_wave_dur");
                pd.remove("lvluping_m_ult_chaos_center_x");
                pd.remove("lvluping_m_ult_chaos_center_y");
                pd.remove("lvluping_m_ult_chaos_center_z");
                pd.remove("lvluping_m_ult_chaos_prev_r");
                pd.remove("lvluping_m_ult_chaos_max_r");
                pd.remove("lvluping_m_ult_chaos_dmg");
                pd.remove("lvluping_m_ult_chaos_slow_ticks");
                pd.remove("lvluping_m_ult_chaos_slow_amp");
                pd.remove("lvluping_m_ult_chaos_burn_ticks");
            }

            // --- M_ULT_LIGHT_RAY ---
            long rayUntil = player.getPersistentData().getLong("lvluping_c_light_ray_until");
            if (rayUntil > time) {
                var pd = player.getPersistentData();
                double cx = pd.getDouble("lvluping_c_light_ray_cx");
                double cy = pd.getDouble("lvluping_c_light_ray_cy");
                double cz = pd.getDouble("lvluping_c_light_ray_cz");
                double slowRadius = pd.getDouble("lvluping_c_light_ray_slow_r");
                double beamRadius = pd.getDouble("lvluping_c_light_ray_beam_r");
                double yMin = pd.getDouble("lvluping_c_light_ray_ymin");
                double yMax = pd.getDouble("lvluping_c_light_ray_ymax");
                float healBase = pd.getFloat("lvluping_c_light_ray_heal");
                float dmg = pd.getFloat("lvluping_c_light_ray_damage");

                var talents = PlayerLevels.getPlayerTalents(player.getUUID());
                float healMul = TalentAbilityHandler.getClericHealingAmpMult(player, talents);

                AABB area = new AABB(cx - slowRadius, yMin, cz - slowRadius, cx + slowRadius, yMax, cz + slowRadius);

                if (time % LIGHT_RAY_PARTICLE_INTERVAL_TICKS == 0) {
                    playLightRayBeaconVisual(level, cx, cz, yMin, yMax);
                    if (time % 20 == 0) {
                        level.playSound(null, cx, cy, cz, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.2f, 1.0f);
                    }
                }

                if (time % LIGHT_RAY_EFFECT_INTERVAL_TICKS == 0) {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e == null) continue;

                        double dx = e.getX() - cx;
                        double dz = e.getZ() - cz;
                        double dist2 = dx * dx + dz * dz;

                        boolean beneficial = e == player || AllyHelper.isSupportAlly(player, e);
                        if (beneficial) {
                            if (dist2 <= beamRadius * beamRadius) {
                                e.heal(healBase * healMul);
                            }
                            continue;
                        }

                        if (!isUndead(e)) continue;

                        if (dist2 <= slowRadius * slowRadius) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LIGHT_RAY_UNDEAD_SLOW_DURATION_TICKS, LIGHT_RAY_UNDEAD_SLOW_AMPLIFIER, false, false));
                        }
                        if (dist2 <= beamRadius * beamRadius) {
                            e.hurt(player.damageSources().magic(), dmg);
                        }
                    }
                }
            } else if (rayUntil > 0) {
                var pd = player.getPersistentData();
                removeLightRayLightBlocks(level, pd);
                pd.remove("lvluping_c_light_ray_until");
                pd.remove("lvluping_c_light_ray_cx");
                pd.remove("lvluping_c_light_ray_cy");
                pd.remove("lvluping_c_light_ray_cz");
                pd.remove("lvluping_c_light_ray_slow_r");
                pd.remove("lvluping_c_light_ray_beam_r");
                pd.remove("lvluping_c_light_ray_ymin");
                pd.remove("lvluping_c_light_ray_ymax");
                pd.remove("lvluping_c_light_ray_heal");
                pd.remove("lvluping_c_light_ray_damage");
            }

            // --- M_CLERIC_LIGHT ---
            long sphereUntil = player.getPersistentData().getLong("lvluping_c_cleric_light_until");
            if (sphereUntil > time) {
                var pd = player.getPersistentData();
                double cx = pd.getDouble("lvluping_c_cleric_light_cx");
                double cy = pd.getDouble("lvluping_c_cleric_light_cy");
                double cz = pd.getDouble("lvluping_c_cleric_light_cz");
                double radius = pd.getDouble("lvluping_c_cleric_light_radius");
                float healBase = pd.getFloat("lvluping_c_cleric_light_heal");
                float dmg = pd.getFloat("lvluping_c_cleric_light_damage");

                var talents = PlayerLevels.getPlayerTalents(player.getUUID());
                float healMul = TalentAbilityHandler.getClericHealingAmpMult(player, talents);

                if (time % LIGHT_SPHERE_EFFECT_INTERVAL_TICKS == 0) {
                    AABB area = new AABB(cx - radius, cy - radius, cz - radius, cx + radius, cy + radius, cz + radius);
                    double r2 = radius * radius;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e == null) continue;

                        double dx = e.getX() - cx;
                        double dy = e.getY() - cy;
                        double dz = e.getZ() - cz;
                        if (dx * dx + dy * dy + dz * dz > r2) continue;

                        if (e == player || AllyHelper.isSupportAlly(player, e)) {
                            e.heal(healBase * healMul);
                        } else if (isUndead(e)) {
                            e.hurt(player.damageSources().magic(), dmg);
                        }
                    }
                }

                if (time % 2 == 0) {
                    int points = 10;
                    for (int i = 0; i < points; i++) {
                        double ang = (time * 0.12 + i * (Math.PI * 2 / points));
                        double offX = Math.cos(ang) * radius;
                        double offZ = Math.sin(ang) * radius;
                        level.sendParticles(ParticleTypes.END_ROD, cx + offX, cy, cz + offZ, 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }
            } else if (sphereUntil > 0) {
                var pd = player.getPersistentData();
                pd.remove("lvluping_c_cleric_light_until");
                pd.remove("lvluping_c_cleric_light_cx");
                pd.remove("lvluping_c_cleric_light_cy");
                pd.remove("lvluping_c_cleric_light_cz");
                pd.remove("lvluping_c_cleric_light_radius");
                pd.remove("lvluping_c_cleric_light_heal");
                pd.remove("lvluping_c_cleric_light_damage");
            }

            // --- M_ULT_SLOW_SPHERE ---
            long slowUntil = player.getPersistentData().getLong("lvluping_c_slow_sphere_until");
            if (slowUntil > time) {
                var pd = player.getPersistentData();
                double cx = pd.getDouble("lvluping_c_slow_sphere_cx");
                double cy = pd.getDouble("lvluping_c_slow_sphere_cy");
                double cz = pd.getDouble("lvluping_c_slow_sphere_cz");
                double radius = pd.getDouble("lvluping_c_slow_sphere_radius");
                int slowTicks = pd.getInt("lvluping_c_slow_sphere_slow_ticks");
                int slowAmp = pd.getInt("lvluping_c_slow_sphere_slow_amp");
                int speedTicks = pd.getInt("lvluping_c_slow_sphere_speed_ticks");
                int speedAmp = pd.getInt("lvluping_c_slow_sphere_speed_amp");

                if (time % SLOW_SPHERE_EFFECT_INTERVAL_TICKS == 0) {
                    double r2 = radius * radius;

                    double yMin = cy - SLOW_SPHERE_PLATE_HALF_HEIGHT;
                    double yMax = cy + SLOW_SPHERE_PLATE_HALF_HEIGHT;
                    AABB area = new AABB(cx - radius, yMin, cz - radius, cx + radius, yMax, cz + radius);

                    {
                        double pdx = player.getX() - cx;
                        double pdz = player.getZ() - cz;
                        if (pdx * pdx + pdz * pdz <= r2) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, speedTicks, speedAmp, false, false));
                        }
                    }

                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e == null || e == player) continue;

                        double dx = e.getX() - cx;
                        double dz = e.getZ() - cz;
                        if (dx * dx + dz * dz > r2) continue;

                        boolean allied = AllyHelper.isSupportAlly(player, e);

                        if (allied) {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, speedTicks, speedAmp, false, false));
                        } else {
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmp, false, false));
                        }
                    }
                }

                if (time % 5 == 0) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, cx, cy + 0.1, cz, 10, radius * 0.15, 0.05, radius * 0.15, 0.02);
                }
            } else if (slowUntil > 0) {
                var pd = player.getPersistentData();
                pd.remove("lvluping_c_slow_sphere_until");
                pd.remove("lvluping_c_slow_sphere_cx");
                pd.remove("lvluping_c_slow_sphere_cy");
                pd.remove("lvluping_c_slow_sphere_cz");
                pd.remove("lvluping_c_slow_sphere_radius");
                pd.remove("lvluping_c_slow_sphere_slow_ticks");
                pd.remove("lvluping_c_slow_sphere_slow_amp");
                pd.remove("lvluping_c_slow_sphere_speed_ticks");
                pd.remove("lvluping_c_slow_sphere_speed_amp");
            }

            long meteorAt = player.getPersistentData().getLong("lvluping_m_meteor_at");
            if (meteorAt > 0 && time >= meteorAt) {
                player.getPersistentData().remove("lvluping_m_meteor_at");
                double mx = player.getPersistentData().getDouble("lvluping_m_meteor_x");
                double my = player.getPersistentData().getDouble("lvluping_m_meteor_y");
                double mz = player.getPersistentData().getDouble("lvluping_m_meteor_z");
                player.getPersistentData().remove("lvluping_m_meteor_x");
                player.getPersistentData().remove("lvluping_m_meteor_y");
                player.getPersistentData().remove("lvluping_m_meteor_z");

                int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_meteor", PlayerLevels.getPlayerTalents(player.getUUID()));
                double radius = AbilityUpgradeConfig.getDouble("m_ult_meteor", "radius", lvl, 6.0);
                float dmg = (float) AbilityUpgradeConfig.getDouble("m_ult_meteor", "damage", lvl, 18.0);
                Vec3 pos = new Vec3(mx, my, mz);

                level.playSound(null, mx, my, mz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.6f);
                level.playSound(null, mx, my, mz, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 0.8f);
                for (int i = 0; i < 40; i++) {
                    level.sendParticles(ParticleTypes.FLAME, mx, my + 1.0, mz, 2, radius * 0.3, 0.8, radius * 0.3, 0.02);
                    level.sendParticles(ParticleTypes.LAVA, mx, my + 0.5, mz, 1, radius * 0.25, 0.4, radius * 0.25, 0.02);
                    level.sendParticles(ParticleTypes.CLOUD, mx, my + 0.4, mz, 2, radius * 0.5, 0.3, radius * 0.5, 0.03);
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(mx - radius, my - 2, mz - radius, mx + radius, my + 4, mz + radius))) {
                    if (e == player) continue;
                    if (e.distanceToSqr(pos) <= radius * radius) {
                        e.hurt(player.damageSources().playerAttack(player), dmg);
                        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 80));
                    }
                }
                CommonEventsHandler.igniteBlocksInHorizontalRadius(level, pos, radius);
            }

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

            var wpd = player.getPersistentData();
            long lightFormUntil = wpd.getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
            if (lightFormUntil > 0 && lightFormUntil <= time) {
                removeLightFormBlocks(level, wpd);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_RADIUS_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_HEAL_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_SHIELD_RATIO_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_AX_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_AY_KEY);
                wpd.remove(TalentAbilityHandler.W_LIGHT_FORM_AZ_KEY);
                clearLightFormMoveLock(player);
                clearLightFormNoGravity(player);
                player.removeEffect(MobEffects.GLOWING);
            }
            lightFormUntil = wpd.getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
            if (lightFormUntil > time) {
                applyLightFormMoveLock(player);
                if (!player.hasEffect(MobEffects.GLOWING)) {
                    player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                }
                double pr = wpd.getDouble(TalentAbilityHandler.W_LIGHT_FORM_RADIUS_KEY);
                float healPs = wpd.getFloat(TalentAbilityHandler.W_LIGHT_FORM_HEAL_KEY);
                float shieldRatio = wpd.getFloat(TalentAbilityHandler.W_LIGHT_FORM_SHIELD_RATIO_KEY);
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                refreshLightFormBlocks(level, player, pr);
                var talents = PlayerLevels.getPlayerTalents(player.getUUID());
                if (time % 20 == 0) {
                    double pr2 = pr * pr;
                    AABB pbox = player.getBoundingBox().inflate(pr, 2.0, pr);
                    float healMul = TalentAbilityHandler.getClericHealingAmpMult(player, talents);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, pbox)) {
                        if (e == player) continue;
                        double dx = e.getX() - px;
                        double dz = e.getZ() - pz;
                        if (dx * dx + dz * dz > pr2) continue;
                        boolean allied = AllyHelper.isSupportAlly(player, e);
                        if (allied) {
                            e.heal(healPs * healMul);
                            float cap = e.getMaxHealth() * shieldRatio;
                            if (cap > 0.01f) {
                                e.setAbsorptionAmount(Math.max(e.getAbsorptionAmount(), cap));
                            }
                        }
                    }
                }
                if (time % 2 == 0) {
                    for (int i = 0; i < 36; i++) {
                        double angle = (time * 0.1 + i * (Math.PI * 2 / 36)) % (Math.PI * 2);
                        double rx = Math.cos(angle) * pr;
                        double rz = Math.sin(angle) * pr;
                        level.sendParticles(ParticleTypes.END_ROD, px + rx, py + 0.12, pz + rz, 2, 0.05, 0.02, 0.05, 0.008);
                        level.sendParticles(ParticleTypes.ENCHANT, px + rx * 0.94, py + 0.2, pz + rz * 0.94, 1, 0.03, 0.02, 0.03, 0.004);
                    }
                    for (int i = 0; i < 12; i++) {
                        double angle = (time * 0.15 + i * (Math.PI * 2 / 12)) % (Math.PI * 2);
                        double r2 = pr * 0.72;
                        double rx = Math.cos(angle) * r2;
                        double rz = Math.sin(angle) * r2;
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, px + rx, py + 0.08, pz + rz, 1, 0.02, 0.01, 0.02, 0.001);
                    }
                }
                if (time % 40 == 0) {
                    level.playSound(null, px, py, pz, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.32f, 1.5f);
                }
            }

            long wWingsUntil = wpd.getLong("lvluping_paladin_wings_until");
            if (wWingsUntil > 0 && wWingsUntil <= time) {
                if (!player.isCreative()) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                wpd.remove("lvluping_paladin_wings_until");
            }

            long antiUntil = player.getPersistentData().getLong("lvluping_m_anti_magic_until");
            if (antiUntil > time) {
                if (time % 3 == 0) {
                    double r = player.getPersistentData().getDouble("lvluping_m_anti_magic_radius");
                    for (int i = 0; i < 16; i++) {
                        double ang = (time * 0.12 + i * (Math.PI * 2 / 16.0)) % (Math.PI * 2);
                        level.sendParticles(ParticleTypes.END_ROD, x + Math.cos(ang) * r, y + 1.0, z + Math.sin(ang) * r, 1, 0.02, 0.02, 0.02, 0.01);
                    }
                }
            }
        }
    }
}
