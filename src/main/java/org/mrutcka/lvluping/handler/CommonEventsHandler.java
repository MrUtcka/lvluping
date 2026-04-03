package org.mrutcka.lvluping.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.compat.ArsManaCompat;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.S2CSyncCooldown;
import org.mrutcka.lvluping.network.S2CUnbreakableShieldOrbit;


import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class CommonEventsHandler {
    public static void igniteBlocksInHorizontalRadius(ServerLevel level, Vec3 pos, double radius) {
        if (radius <= 1.0e-4) return;
        BlockPos center = BlockPos.containing(pos);
        int r = Mth.ceil(radius);
        double r2 = radius * radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - 8);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + 4);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((double) dx * dx + (double) dz * dz > r2) continue;
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                for (int y = maxY; y >= minY; y--) {
                    BlockPos here = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(here)) break;
                    BlockState ground = level.getBlockState(here);
                    BlockState above = level.getBlockState(here.above());
                    if (!above.isAir()) continue;
                    if (ground.isAir()) continue;
                    BlockPos firePos = here.above();
                    if (BaseFireBlock.canBePlacedAt(level, firePos, Direction.UP)) {
                        level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                    }
                    break;
                }
            }
        }
    }

    private static final int W_UNBREAKABLE_REGENERATION_DURATION_TICKS = 100;
    private static final int W_UNBREAKABLE_REGENERATION_AMPLIFIER = 0;
    private static final String W_UNBREAKABLE_ABSORB_BEFORE_KEY = "lvluping_unbreakable_absorb_before";
    private static final String W_UNBREAKABLE_ABSORB_OUR_KEY = "lvluping_unbreakable_absorb_our";
    private static final String W_UNBREAKABLE_ABSORB_UNTIL_KEY = "lvluping_unbreakable_absorb_until";

    private static final float UNBREAKABLE_RESCUE_HEALTH = 2f;
    private static final float DAMAGE_REDUCTION_MULTIPLIER_HALF = 0.5f;

    private static final int W_PARRY_WEAKNESS_DURATION_TICKS = 60;
    private static final int W_PARRY_WEAKNESS_AMPLIFIER = 0;

    private static final int W_STUN_EFFECT_DURATION_TICKS = 40;
    private static final int W_STUN_EFFECT_AMPLIFIER = 128;

    private static final int TICKS_PER_SECOND = 20;

    private static final int M_MANA_FLOW_TICK_INTERVAL = 20;

    private static final double CLERIC_PRAYER_AABB_INFLATE_Y = 2.0;
    private static final double CLERIC_PRAYER_AABB_INFLATE_XZ = 4.0;

    private static final double CLERIC_STILL_POSITION_EPSILON_SQR = 0.0004;
    private static final double CLERIC_STILL_MAX_SPEED_SQR = 0.01;
    private static final int CLERIC_STILL_TICKS_CAP = 6000;
    private static final int CLERIC_READ_PRAYER_STILL_TICKS_REQUIRED = 60;
    private static final int CLERIC_READ_PRAYER_HEAL_INTERVAL_TICKS = 20;
    private static final int CLERIC_MANA_PRAYER_STILL_TICKS_REQUIRED = 100;

    private static final double ANTI_MAGIC_DEFAULT_RADIUS = 6.0;
    private static final long ANTI_MAGIC_REFLECT_SUPPRESS_TICKS = 2L;
    private static final double REFLECT_PROJECTILE_MIN_AWAY_LEN_SQR = 1.0e-6;
    private static final double REFLECT_PROJECTILE_OFFSET = 0.2;

    private static final float ICE_PROJECTILE_HIT_SOUND_VOLUME = 0.7f;
    private static final float ICE_PROJECTILE_HIT_SOUND_PITCH = 1.2f;
    private static final int ICE_PROJECTILE_HIT_ENTITY_PARTICLE_COUNT = 16;
    private static final double ICE_PROJECTILE_HIT_SPREAD_XZ = 0.35;
    private static final double ICE_PROJECTILE_HIT_SPREAD_Y = 0.6;
    private static final int ICE_PROJECTILE_HIT_GROUND_PARTICLE_COUNT = 10;
    private static final double ICE_PROJECTILE_HIT_GROUND_SPREAD = 0.25;

    private static final int UNBREAKABLE_TOTEM_PARTICLE_COUNT = 30;
    private static final float UNBREAKABLE_TOTEM_PARTICLE_SPREAD_XZ = 0.4f;
    private static final float UNBREAKABLE_TOTEM_PARTICLE_SPREAD_Y = 0.5f;

    private static final float W_PARRY_BLOCK_SOUND_PITCH = 1.2f;
    private static final int W_PARRY_CRIT_PARTICLE_COUNT = 15;
    private static final double W_PARRY_CRIT_SPREAD = 0.3;
    private static final double W_PARRY_CRIT_SPEED = 0.2;

    private static final float W_BARRIER_GLASS_PITCH = 1.2f;
    private static final float W_BARRIER_SHIELD_VOLUME = 0.8f;
    private static final int W_BARRIER_POOF_COUNT = 10;
    private static final double W_BARRIER_POOF_SPREAD = 0.2;

    private static final float W_SHIELD_STRIKE_BLOCK_PITCH = 0.9f;
    private static final int W_SHIELD_STRIKE_CRIT_PARTICLE_COUNT = 8;
    private static final double W_SHIELD_STRIKE_CRIT_SPREAD = 0.3;
    private static final double W_SHIELD_STRIKE_CRIT_SPEED = 0.2;

    private static final float M_MAGIC_BARRIER_SOUND_PITCH = 1.6f;
    private static final int M_MAGIC_BARRIER_PARTICLE_COUNT = 12;
    private static final double M_MAGIC_BARRIER_PARTICLE_SPREAD_XZ = 0.5;
    private static final double M_MAGIC_BARRIER_PARTICLE_SPREAD_Y = 0.8;

    private static final int M_STONE_SKIN_PARTICLE_COUNT = 12;
    private static final double M_STONE_SKIN_PARTICLE_SPREAD_XZ = 0.4;
    private static final double M_STONE_SKIN_PARTICLE_SPREAD_Y = 0.6;

    private static final float W_ULT_BERSERK_MISSING_HP_DAMAGE_SCALE = 1.2f;
    private static final float W_ULT_BERSERK_HEAL_MAX_HP_RATIO = 0.02f;
    private static final int W_ULT_BERSERK_HIT_PARTICLE_COUNT = 3;

    private static final double AS_CRIT_BACKSTAB_DOT_THRESHOLD = 0.7;
    private static final float AS_CRIT_DAMAGE_MULTIPLIER = 2.0f;
    private static final float AS_CRIT_SOUND_PITCH = 1.2f;

    private static final float A_POWER_ARROW_CRIT_MULTIPLIER = 2.0f;

    private static final int W_COMBO_CHAIN_WINDOW_TICKS = 20;
    private static final int W_COMBO_MAX_STACK = 10;
    private static final float W_COMBO_SOUND_PITCH_BASE = 0.5f;
    private static final float W_COMBO_SOUND_PITCH_PER_STACK = 0.1f;
    private static final float W_COMBO_DAMAGE_PER_STACK = 0.1f;
    private static final int W_COMBO_CRIT_PARTICLE_COUNT = 8;
    private static final int W_COMBO_SWEEP_PARTICLE_COUNT = 2;

    private static final float W_STUN_PROC_CHANCE = 0.2f;
    private static final float W_STUN_ANVIL_PITCH = 1.5f;

    private static final int W_BLOODLUST_HITS_TO_HEAL = 5;
    private static final float W_BLOODLUST_HEAL_FROM_DAMAGE_RATIO = 0.3f;

    private static final float W_ARMOR_BREAKER_DAMAGE_MULTIPLIER = 1.5f;
    private static final int W_ARMOR_BREAKER_ARMOR_DEBUFF_DURATION_TICKS = 100;
    private static final int W_ARMOR_BREAKER_COOLDOWN_TICKS = 200;
    private static final float W_ARMOR_BREAKER_ANVIL_PITCH = 0.7f;
    private static final int W_ARMOR_BREAKER_CRIT_PARTICLE_COUNT = 10;

    private static final double BARRIER_RENDER_ANGLE_SCALE = 0.15;
    private static final int BARRIER_RENDER_RING_POINTS = 4;
    private static final double BARRIER_RENDER_RING_RADIUS = 1.5;
    private static final double BARRIER_RENDER_Y_OFFSET = 1.0;
    private static final double BARRIER_RENDER_PARTICLE_SPEED = 0.01;

    private static void enforceLightFormAnchor(ServerPlayer sp) {
        var pd = sp.getPersistentData();
        long gt = sp.level().getGameTime();
        if (pd.getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) <= gt) return;
        double ax = pd.getDouble(TalentAbilityHandler.W_LIGHT_FORM_AX_KEY);
        double ay = pd.getDouble(TalentAbilityHandler.W_LIGHT_FORM_AY_KEY);
        double az = pd.getDouble(TalentAbilityHandler.W_LIGHT_FORM_AZ_KEY);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.fallDistance = 0f;
        if (sp.isFallFlying()) {
            sp.stopFallFlying();
        }
        if (sp.getAbilities().flying) {
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
        }
        sp.setPos(ax, ay, az);
        sp.setDeltaMovement(Vec3.ZERO);
    }

    @SubscribeEvent
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        enforceLightFormAnchor(sp);
        long gameTime = sp.level().getGameTime();
        long climbUntil = sp.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_WALL_CLIMB_UNTIL_KEY);
        if (climbUntil > gameTime && hasWallForClimb(sp)) {
            Vec3 m = sp.getDeltaMovement();
            double up = sp.isShiftKeyDown() ? 0.12 : 0.26;
            sp.setDeltaMovement(m.x, Math.min(0.48, m.y + up), m.z);
            sp.fallDistance = 0f;
            sp.hurtMarked = true;
        }
    }

    private static boolean hasWallForClimb(ServerPlayer p) {
        var level = p.serverLevel();
        double px = p.getX();
        double pz = p.getZ();
        double[] heights = {0.25, 0.55 * p.getBbHeight(), 0.85 * p.getBbHeight()};
        for (double dy : heights) {
            Vec3 base = new Vec3(px, p.getY() + dy, pz);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Vec3 end = base.add(dir.getStepX() * 0.55, 0, dir.getStepZ() * 0.55);
                var hit = level.clip(new ClipContext(base, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK) {
                    var bhr = (BlockHitResult) hit;
                    var state = level.getBlockState(bhr.getBlockPos());
                    if (!state.isAir() && !state.getCollisionShape(level, bhr.getBlockPos()).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickPostLightFormLast(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        enforceLightFormAnchor(sp);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        BarrierRender(player);

        if (!player.level().isClientSide) {
            if (player instanceof ServerPlayer sp) {
                long lfPost = sp.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
                if (lfPost > sp.level().getGameTime() && sp.isUsingItem()) {
                    sp.stopUsingItem();
                }
            }
            if (player instanceof ServerPlayer sp) {
                long until = sp.getPersistentData().getLong("lvluping_possession_until");
                if (until > 0) {
                    if (until <= sp.level().getGameTime()) {
                        sp.getPersistentData().remove("lvluping_possession_until");
                        sp.getPersistentData().remove("lvluping_possession_mob");
                        sp.stopRiding();
                        sp.setInvisible(false);
                        sp.setInvulnerable(false);
                        sp.setNoGravity(false);
                        sp.setDeltaMovement(0, 0, 0);
                    } else if (sp.getVehicle() != null) {
                        sp.setInvisible(true);
                        sp.setInvulnerable(true);
                        sp.setNoGravity(true);
                        sp.setDeltaMovement(0, 0, 0);
                        sp.setPos(sp.getVehicle().getX(), sp.getVehicle().getY(), sp.getVehicle().getZ());
                    }
                }
            }

            if (player instanceof ServerPlayer sp) {
                Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());
                long gameTime = sp.level().getGameTime();
                long climbUntil = sp.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_WALL_CLIMB_UNTIL_KEY);
                if (climbUntil > 0 && climbUntil <= gameTime) {
                    sp.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_WALL_CLIMB_UNTIL_KEY);
                }
                long unbreakableAbsorbUntil = sp.getPersistentData().getLong(W_UNBREAKABLE_ABSORB_UNTIL_KEY);
                if (unbreakableAbsorbUntil > 0 && unbreakableAbsorbUntil <= gameTime) {
                    var pd = sp.getPersistentData();
                    float beforeAbs = pd.getFloat(W_UNBREAKABLE_ABSORB_BEFORE_KEY);
                    float ourAbs = pd.getFloat(W_UNBREAKABLE_ABSORB_OUR_KEY);
                    float cur = sp.getAbsorptionAmount();
                    float floor = Math.max(0f, beforeAbs);
                    sp.setAbsorptionAmount(Math.max(floor, cur - Math.max(0f, ourAbs)));
                    pd.remove(W_UNBREAKABLE_ABSORB_BEFORE_KEY);
                    pd.remove(W_UNBREAKABLE_ABSORB_OUR_KEY);
                    pd.remove(W_UNBREAKABLE_ABSORB_UNTIL_KEY);
                }
                if (talents.contains("w_paladin_providence")) {
                    long provAt = sp.getPersistentData().getLong("lvluping_paladin_prov_at");
                    if (provAt > 0 && gameTime >= provAt) {
                        sp.getPersistentData().remove("lvluping_paladin_prov_at");
                        int plvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "w_paladin_providence", talents);
                        double ch = AbilityUpgradeConfig.getDouble("w_paladin_providence", "chance", plvl, 0.1);
                        if (sp.getRandom().nextDouble() < ch) {
                            TalentAbilityHandler.removeHarmfulEffects(sp, 1);
                        }
                    }
                }
                if (talents.contains("m_mana_flow") && talents.contains("m_spellcaster_base") && sp.level().getGameTime() % M_MANA_FLOW_TICK_INTERVAL == 0) {
                    int mfLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_mana_flow", talents);
                    double bonusPct = AbilityUpgradeConfig.getDouble("m_mana_flow", "regen_bonus_percent", mfLvl, 15.0);
                    double basePerSec = 3.0;
                    double perSec = basePerSec * (1.0 + bonusPct / 100.0);
                    ArsManaCompat.tryAddMana(sp, perSec);
                }

                if (talents.contains("m_soft_landing") && talents.contains("m_spellcaster_base") && sp.level().getGameTime() % 15 == 0) {
                    int slLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_soft_landing", talents);
                    double jm = AbilityUpgradeConfig.getDouble("m_soft_landing", "jump_height_mult", slLvl, 1.0);
                    int jumpAmp = 0;
                    if (jm >= 1.35) jumpAmp = 2;
                    else if (jm >= 1.15) jumpAmp = 1;
                    sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, jumpAmp, false, false));
                }

                // --- M_CLERIC_PRAYER ---
                if (talents.contains("m_cleric_prayer") && sp.level().getGameTime() % TICKS_PER_SECOND == 0) {
                    int pLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_cleric_prayer", talents);
                    float heal = (float) AbilityUpgradeConfig.getDouble("m_cleric_prayer", "heal_per_sec", pLvl, 1.0);
                    double prayRadius = AbilityUpgradeConfig.getDouble("m_cleric_prayer", "radius", pLvl, 4.0);
                    heal *= TalentAbilityHandler.getClericHealingAmpMult(sp, talents);

                    AABB box = sp.getBoundingBox().inflate(prayRadius, CLERIC_PRAYER_AABB_INFLATE_Y, prayRadius);
                    for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class, box)) {
                        if (e == sp) continue;

                        boolean allied = e.isAlliedTo(sp);
                        if (!allied && e instanceof net.minecraft.world.entity.Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner")
                                && mob.getPersistentData().getUUID("lvluping_summon_owner").equals(sp.getUUID())) {
                            allied = true;
                        }
                        if (!allied) continue;
                        e.heal(heal);
                    }
                }

                // --- M_CLERIC_READ_PRAYER / M_CLERIC_MANA_PRAYER ---
                if ((talents.contains("m_cleric_read_prayer") || talents.contains("m_cleric_mana_prayer")) && sp.level().getGameTime() % 1 == 0) {
                    long time = sp.level().getGameTime();
                    var pd = sp.getPersistentData();

                    double speedSqr = sp.getDeltaMovement().lengthSqr();
                    double lastX = pd.getDouble("lvluping_cleric_last_x");
                    double lastZ = pd.getDouble("lvluping_cleric_last_z");
                    double dx = sp.getX() - lastX;
                    double dz = sp.getZ() - lastZ;
                    boolean standing = (dx * dx + dz * dz) <= CLERIC_STILL_POSITION_EPSILON_SQR && speedSqr <= CLERIC_STILL_MAX_SPEED_SQR;

                    int stillTicks = pd.getInt("lvluping_cleric_still_ticks");
                    if (standing) stillTicks++;
                    else stillTicks = 0;
                    stillTicks = Math.min(stillTicks, CLERIC_STILL_TICKS_CAP);
                    pd.putInt("lvluping_cleric_still_ticks", stillTicks);
                    pd.putDouble("lvluping_cleric_last_x", sp.getX());
                    pd.putDouble("lvluping_cleric_last_z", sp.getZ());

                    long nextHealAt = pd.getLong("lvluping_cleric_next_read_heal_at");
                    boolean martyrActive = sp.getPersistentData().getLong("lvluping_cleric_martyr_until") > time;

                    if (talents.contains("m_cleric_read_prayer") && stillTicks >= CLERIC_READ_PRAYER_STILL_TICKS_REQUIRED && !martyrActive) {
                        if (nextHealAt <= 0) {
                            nextHealAt = time + CLERIC_READ_PRAYER_HEAL_INTERVAL_TICKS;
                        } else if (time >= nextHealAt) {
                            int rpLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_cleric_read_prayer", talents);
                            float heal = (float) AbilityUpgradeConfig.getDouble("m_cleric_read_prayer", "heal_per_sec", rpLvl, 1.0);
                            heal *= TalentAbilityHandler.getClericHealingAmpMult(sp, talents);
                            sp.heal(heal);
                            nextHealAt = time + CLERIC_READ_PRAYER_HEAL_INTERVAL_TICKS;
                        }
                        pd.putLong("lvluping_cleric_next_read_heal_at", nextHealAt);
                    } else {
                        pd.remove("lvluping_cleric_next_read_heal_at");
                    }

                    if (talents.contains("m_cleric_mana_prayer") && stillTicks >= CLERIC_MANA_PRAYER_STILL_TICKS_REQUIRED && time % TICKS_PER_SECOND == 0) {
                        int mpLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_cleric_mana_prayer", talents);
                        double pct = AbilityUpgradeConfig.getDouble("m_cleric_mana_prayer", "mana_percent_per_sec", mpLvl, 5.0);
                        Integer maxM = ArsManaCompat.getMaxMana(sp);
                        if (maxM != null && maxM > 0) {
                            ArsManaCompat.tryAddMana(sp, maxM * (pct / 100.0));
                        }
                    }

                    // --- M_ULT_MARTYR ---
                    long martyrUntilUlt = sp.getPersistentData().getLong("lvluping_cleric_martyr_until");
                    if (martyrUntilUlt > time && time % TICKS_PER_SECOND == 0) {
                        int mLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_ult_martyr", talents);
                        float selfDps = (float) AbilityUpgradeConfig.getDouble("m_ult_martyr", "self_damage_per_sec", mLvl, 1.0);
                        float teamHps = (float) AbilityUpgradeConfig.getDouble("m_ult_martyr", "team_heal_per_sec", mLvl, 2.0);
                        double teamR = AbilityUpgradeConfig.getDouble("m_ult_martyr", "team_radius", mLvl, 3.0);
                        float maxSelf = Math.max(0f, sp.getHealth() - 0.5f);
                        float dmg = Math.min(selfDps, maxSelf);
                        if (dmg > 0) {
                            sp.hurt(sp.damageSources().magic(), dmg);
                        }
                        AABB mBox = sp.getBoundingBox().inflate(teamR, 4, teamR);
                        for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class, mBox)) {
                            if (e == sp) continue;
                            boolean allied = e.isAlliedTo(sp);
                            if (!allied && e instanceof net.minecraft.world.entity.Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner")
                                    && mob.getPersistentData().getUUID("lvluping_summon_owner").equals(sp.getUUID())) {
                                allied = true;
                            }
                            if (allied) {
                                e.heal(teamHps);
                            }
                        }
                    }
                }
            }

            decrementCooldown(player, "cd_slide");
            decrementCooldown(player, "cd_smoke");
            decrementCooldown(player, "cd_dash");
            decrementCooldown(player, "cd_parry");
            decrementCooldown(player, "cd_buff");
            decrementCooldown(player, "cd_w_seismic");
            decrementCooldown(player, "cd_w_spin");
            decrementCooldown(player, "cd_w_unbreakable");
            decrementCooldown(player, "cd_w_armor_breaker");
            decrementCooldown(player, "cd_w_swordmaster_concentration");
            decrementCooldown(player, "cd_w_swordmaster_steel_body");
            decrementCooldown(player, "cd_w_barbarian_battle_cry");
            decrementCooldown(player, "cd_w_barbarian_bloodletting");
            decrementCooldown(player, "cd_w_barbarian_frenzy");
            decrementWindow(player, "lvluping_parry_window");
            decrementWindow(player, "lvluping_barrier_window");
            decrementWindow(player, "lvluping_shield_window");
            decrementCooldown(player, "cd_w_provocation");
            decrementCooldown(player, "cd_w_paladin_blessing");
            decrementCooldown(player, "cd_w_paladin_immolation");
            decrementCooldown(player, "cd_w_ult_paladin_wings");
            decrementCooldown(player, "cd_w_ult_paladin_sacrifice");
            decrementCooldown(player, "cd_w_ult_berserk");
            decrementCooldown(player, "cd_w_ult_final_countdown");
            decrementCooldown(player, "cd_w_ult_invulnerability");
            decrementCooldown(player, "cd_w_ult_swordmaster_hurricane");
            decrementCooldown(player, "cd_w_ult_barbarian_taste_blood");
            decrementCooldown(player, "cd_w_ult_swordmaster_omnislash");
            decrementCooldown(player, "cd_w_ult_swordmaster_blade_wall");
            decrementCooldown(player, "cd_w_ult_swordmaster_perfect_cut");
            decrementCooldown(player, "cd_w_ult_barbarian_feast");
            decrementCooldown(player, "cd_m_fireball");
            decrementCooldown(player, "cd_m_lightning");
            decrementCooldown(player, "cd_m_ice");
            decrementCooldown(player, "cd_m_teleport");
            decrementCooldown(player, "cd_m_summon");
            decrementCooldown(player, "cd_m_sacrifice");
            decrementCooldown(player, "cd_m_command");
            decrementCooldown(player, "cd_m_stone_skin");
            decrementCooldown(player, "cd_m_magic_barrier");
            decrementCooldown(player, "cd_m_ult_gate");
            decrementCooldown(player, "cd_m_ult_absorption");
            decrementCooldown(player, "cd_m_ult_totem_form");
            decrementCooldown(player, "cd_m_ult_possession");
            decrementCooldown(player, "cd_m_ult_elemental");
            decrementCooldown(player, "cd_m_ult_meteor");
            decrementCooldown(player, "cd_m_ult_ice_block");
            decrementCooldown(player, "cd_m_ult_anti_magic");
            decrementCooldown(player, "cd_m_ult_illusions");
            decrementCooldown(player, "cd_m_ult_chaos");
            decrementCooldown(player, "cd_m_cleric_heal");
            decrementCooldown(player, "cd_m_cleric_blessing");
            decrementCooldown(player, "cd_m_cleric_light");
            decrementCooldown(player, "cd_m_ult_light_ray");
            decrementCooldown(player, "cd_m_ult_resurrection");
            decrementCooldown(player, "cd_m_ult_martyr");
            decrementCooldown(player, "cd_m_ult_slow_sphere");
            decrementCooldown(player, "cd_m_ult_divine_protection");

            decrementCooldown(player, "cd_a_hunter_trap");
            decrementCooldown(player, "cd_a_hunter_call_nature");
            decrementCooldown(player, "cd_a_hunter_poison_arrow");
            decrementCooldown(player, "cd_a_hunter_net");
            decrementCooldown(player, "cd_a_hunter_escape");

            decrementCooldown(player, "cd_a_ranger_entangle_arrow");
            decrementCooldown(player, "cd_a_ranger_evasion");
            decrementCooldown(player, "cd_a_ranger_thunder_arrow");
            decrementCooldown(player, "cd_a_ranger_thorn_bush");

            decrementCooldown(player, "cd_a_musketeer_quick_reload");
            decrementCooldown(player, "cd_a_musketeer_incendiary");
            decrementCooldown(player, "cd_a_musketeer_aimed_shot");
            decrementCooldown(player, "cd_a_musketeer_holster");

            decrementCooldown(player, "cd_a_ult_hunter_ult_shot");
            decrementCooldown(player, "cd_a_ult_hunter_pack");
            decrementCooldown(player, "cd_a_ult_hunter_sniper");
            decrementCooldown(player, "cd_a_ult_hunter_track");

            decrementCooldown(player, "cd_a_ult_ranger_wrath");
            decrementCooldown(player, "cd_a_ult_ranger_life_totem");
            decrementCooldown(player, "cd_a_ult_ranger_merge");
            decrementCooldown(player, "cd_a_ult_ranger_roots");

            decrementCooldown(player, "cd_a_ult_musketeer_barrage");
            decrementCooldown(player, "cd_a_ult_musketeer_grenade");
            decrementCooldown(player, "cd_a_ult_musketeer_concussion");
            decrementCooldown(player, "cd_as_rogue_strong_poison");
            decrementCooldown(player, "cd_as_rogue_trip");
            decrementCooldown(player, "cd_as_rogue_blind");
            decrementCooldown(player, "cd_as_wanderer_barricade");
            decrementCooldown(player, "cd_as_wanderer_climb");
            decrementCooldown(player, "cd_as_wanderer_tripwire");
            decrementCooldown(player, "cd_as_assassin_mark");
            decrementCooldown(player, "cd_as_assassin_shuriken");
            decrementCooldown(player, "cd_as_assassin_rupture");
            decrementCooldown(player, "cd_as_assassin_adrenaline");
            decrementCooldown(player, "cd_as_ult_rogue_perfect_kill");
            decrementCooldown(player, "cd_as_ult_rogue_poison_veil");
            decrementCooldown(player, "cd_as_ult_rogue_confusion");
            decrementCooldown(player, "cd_as_ult_rogue_vanish");
            decrementCooldown(player, "cd_as_ult_wanderer_camp");
            decrementCooldown(player, "cd_as_ult_wanderer_dagger_rain");
            decrementCooldown(player, "cd_as_ult_wanderer_thorn_trail");
            decrementCooldown(player, "cd_as_ult_wanderer_ghosts");
            decrementCooldown(player, "cd_as_ult_assassin_blade_dance");
            decrementCooldown(player, "cd_as_ult_assassin_immobilize");
            decrementCooldown(player, "cd_as_ult_assassin_black_mist");
            decrementCooldown(player, "cd_as_ult_assassin_double");

            if (UltimatesHandler.isBerserkActive(player) && player.isBlocking()) {
                player.stopUsingItem();
            }
            if (UltimatesHandler.isInvulnerabilityActive(player) && player.isUsingItem()) {
                player.stopUsingItem();
            }

        }
    }

    @SubscribeEvent
    public static void onLivingDeathClearLightForm(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;
        UltimatesHandler.removeLightFormBlocks(sl, sp.getPersistentData());
        var pd = sp.getPersistentData();
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_RADIUS_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_HEAL_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_SHIELD_RATIO_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AX_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AY_KEY);
        pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AZ_KEY);
        UltimatesHandler.clearLightFormMoveLock(sp);
        UltimatesHandler.clearLightFormNoGravity(sp);
        sp.removeEffect(MobEffects.GLOWING);
    }

    @SubscribeEvent
    public static void onPossessedSummonDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
        if (!mob.getPersistentData().hasUUID("lvluping_summon_owner")) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        UUID ownerUuid = mob.getPersistentData().getUUID("lvluping_summon_owner");
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) return;
        var pd = owner.getPersistentData();
        if (!pd.hasUUID("lvluping_possession_mob")) return;
        if (!mob.getUUID().equals(pd.getUUID("lvluping_possession_mob"))) return;

        pd.remove("lvluping_possession_until");
        pd.remove("lvluping_possession_mob");
        owner.stopRiding();
        owner.teleportTo(sl, mob.getX(), mob.getY(), mob.getZ(), owner.getYRot(), owner.getXRot());
        owner.setInvisible(false);
        owner.setInvulnerable(false);
        owner.setNoGravity(false);
        owner.setDeltaMovement(0, 0, 0);
    }

    @SubscribeEvent
    public static void onBarbarianKillPassives(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        Set<String> talents = PlayerLevels.getPlayerTalents(killer.getUUID());
        if (talents.contains("w_barbarian_bloodthirst")) {
            int lvl = PlayerLevels.getAbilityLevel(killer.getUUID(), "w_barbarian_bloodthirst", talents);
            float heal = (float) AbilityUpgradeConfig.getDouble("w_barbarian_bloodthirst", "heal_on_kill", lvl, 2.0);
            killer.heal(heal);
        }
        if (talents.contains("w_barbarian_kill_frenzy") && killer.level() instanceof ServerLevel sl) {
            int lvl = PlayerLevels.getAbilityLevel(killer.getUUID(), "w_barbarian_kill_frenzy", talents);
            int dur = AbilityUpgradeConfig.getInt("w_barbarian_kill_frenzy", "duration_ticks", lvl, 100);
            float asMult = (float) AbilityUpgradeConfig.getDouble("w_barbarian_kill_frenzy", "attack_speed_mult", lvl, 1.1);
            killer.getPersistentData().putLong(TalentAbilityHandler.W_BARBARIAN_KILL_FRENZY_UNTIL_KEY, sl.getGameTime() + dur);
            killer.getPersistentData().putFloat(TalentAbilityHandler.W_BARBARIAN_KILL_FRENZY_AS_MULT_KEY, asMult);
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, killer.getX(), killer.getY() + 1.0, killer.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ProvocationHandler.tickProvocation(serverLevel);
            SummonerHandler.tick(serverLevel);
            UltimatesHandler.tickBrotherhood(serverLevel);
            UltimatesHandler.tickFinalCountdown(serverLevel);
            UltimatesHandler.tickUltimateEffects(serverLevel);

            long time = serverLevel.getGameTime();
            java.util.HashSet<java.util.UUID> tickedIllusions = new java.util.HashSet<>();
            for (ServerPlayer p : serverLevel.players()) {
                Set<String> talents = PlayerLevels.getPlayerTalents(p.getUUID());
                if (talents.contains("as_rogue_poison_immune")) {
                    p.removeEffect(MobEffects.POISON);
                }
                if (talents.contains("as_rogue_night_eye")) {
                    p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false));
                }
                if (talents.contains("as_wanderer_no_slow")) {
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
                if (talents.contains("a_musketeer_stability")) {
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    p.removeEffect(MobEffects.WEAKNESS);
                }
                if (talents.contains("as_wanderer_fastest") && p.hasEffect(MobEffects.INVISIBILITY)) {
                    int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "as_wanderer_fastest", talents);
                    int amp = AbilityUpgradeConfig.getInt("as_wanderer_fastest", "invis_speed_amp", lvl, 0);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, Math.max(0, amp), false, false));
                }
                if (talents.contains("as_assassin_silent_step") && p.isCrouching()) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 0, false, false));
                }

                AABB illArea = p.getBoundingBox().inflate(96, 64, 96);
                for (net.minecraft.world.entity.decoration.ArmorStand ill : serverLevel.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, illArea)) {
                    var pd = ill.getPersistentData();
                    if (!pd.getBoolean("lvluping_spell_illusion")) continue;
                    if (!pd.hasUUID("lvluping_illusion_owner")) continue;
                    if (!p.getUUID().equals(pd.getUUID("lvluping_illusion_owner"))) continue;
                    if (!tickedIllusions.add(ill.getUUID())) continue;

                    long until = pd.getLong("lvluping_illusion_until");
                    if (until > 0 && until <= time) {
                        serverLevel.sendParticles(ParticleTypes.POOF, ill.getX(), ill.getY() + 1.0, ill.getZ(), 18, 0.35, 0.6, 0.35, 0.02);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, ill.getX(), ill.getY() + 0.8, ill.getZ(), 24, 0.4, 0.5, 0.4, 0.01);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, ill.getX(), ill.getY() + 1.0, ill.getZ(), 12, 0.4, 0.6, 0.4, 0.1);
                        ill.discard();
                        continue;
                    }

                    double offX = pd.getDouble("lvluping_illusion_off_x");
                    double offZ = pd.getDouble("lvluping_illusion_off_z");
                    double yawRad = Math.toRadians(p.getYRot());
                    double cos = Math.cos(yawRad);
                    double sin = Math.sin(yawRad);
                    double worldOffX = offX * cos - offZ * sin;
                    double worldOffZ = offX * sin + offZ * cos;
                    ill.moveTo(p.getX() + worldOffX, p.getY() + 0.1, p.getZ() + worldOffZ, p.getYRot(), p.getXRot());

                    LivingEntity desired = p.getLastHurtMob();
                    if (desired == null || !desired.isAlive() || desired.level() != serverLevel || desired == p) desired = p.getLastHurtByMob();
                    if (desired == null || !desired.isAlive() || desired.level() != serverLevel || desired == p) continue;

                    int cd = pd.getInt("lvluping_illusion_cast_cd");
                    if (cd > 0) {
                        pd.putInt("lvluping_illusion_cast_cd", cd - 1);
                        continue;
                    }
                    pd.putInt("lvluping_illusion_cast_cd", 18);

                    boolean canFire = talents.contains("m_fireball");
                    boolean canLightning = talents.contains("m_lightning");
                    boolean canIce = talents.contains("m_ice_arrow");
                    int variants = (canFire ? 1 : 0) + (canLightning ? 1 : 0) + (canIce ? 1 : 0);
                    if (variants == 0) continue;
                    int pick = serverLevel.random.nextInt(variants);
                    String spell = canFire && pick-- == 0 ? "fire"
                            : canLightning && pick-- == 0 ? "lightning"
                            : "ice";

                    Vec3 eye = new Vec3(ill.getX(), ill.getY() + 1.6, ill.getZ());
                    if ("fire".equals(spell)) {
                        int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "m_fireball", talents);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("m_fireball", "damage", lvl, 8.0);
                        var fb = net.minecraft.world.entity.EntityType.SMALL_FIREBALL.create(serverLevel);
                        if (fb != null) {
                            fb.setPos(eye.x, eye.y, eye.z);
                            fb.setOwner(ill);
                            Vec3 to = desired.getEyePosition().subtract(eye).normalize().scale(0.7);
                            fb.setDeltaMovement(to);
                            fb.getPersistentData().putFloat("lvluping_magic_damage", dmg);
                            serverLevel.addFreshEntity(fb);
                        }
                        serverLevel.sendParticles(ParticleTypes.FLAME, eye.x, eye.y, eye.z, 8, 0.15, 0.15, 0.15, 0.01);
                        continue;
                    }
                    if ("lightning".equals(spell)) {
                        int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "m_lightning", talents);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("m_lightning", "damage", lvl, 10.0);
                        desired.hurt(serverLevel.damageSources().magic(), dmg);
                        var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(serverLevel);
                        if (bolt != null) {
                            bolt.moveTo(desired.getX(), desired.getY(), desired.getZ());
                            bolt.setVisualOnly(true);
                            serverLevel.addFreshEntity(bolt);
                        }
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, desired.getX(), desired.getY() + 1.0, desired.getZ(), 12, 0.35, 0.6, 0.35, 0.1);
                        continue;
                    }
                    int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "m_ice_arrow", talents);
                    float dmg = (float) AbilityUpgradeConfig.getDouble("m_ice_arrow", "damage", lvl, 7.0);
                    int slowTicks = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_ticks", lvl, 60);
                    int slowAmp = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_amp", lvl, 1);
                    var snowball = net.minecraft.world.entity.EntityType.SNOWBALL.create(serverLevel);
                    if (snowball != null) {
                        snowball.setOwner(ill);
                        snowball.setPos(eye.x, eye.y, eye.z);
                        Vec3 to = desired.getEyePosition().subtract(eye).normalize();
                        snowball.shoot(to.x, to.y, to.z, 1.5f, 0.5f);
                        snowball.getPersistentData().putBoolean("lvluping_ice_projectile", true);
                        snowball.getPersistentData().putFloat("lvluping_ice_damage", dmg);
                        snowball.getPersistentData().putInt("lvluping_ice_slow_ticks", slowTicks);
                        snowball.getPersistentData().putInt("lvluping_ice_slow_amp", slowAmp);
                        serverLevel.addFreshEntity(snowball);
                    }
                }
                long hunterTrapUntil = p.getPersistentData().getLong(TalentAbilityHandler.A_HUNTER_TRAP_UNTIL_KEY);
                if (hunterTrapUntil > 0 && hunterTrapUntil <= time && p.getPersistentData().hasUUID(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY)) {
                    UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY);
                    TalentAbilityHandler.broadcastHunterTrapHide(serverLevel, vid);
                    p.getPersistentData().remove(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY);
                }
                long thornVisUntil = p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_THORN_UNTIL_KEY);
                if (thornVisUntil > 0 && thornVisUntil <= time && p.getPersistentData().hasUUID(TalentAbilityHandler.A_RANGER_THORN_VISUAL_KEY)) {
                    UUID tvid = p.getPersistentData().getUUID(TalentAbilityHandler.A_RANGER_THORN_VISUAL_KEY);
                    TalentAbilityHandler.broadcastRangerThornHide(serverLevel, tvid);
                    p.getPersistentData().remove(TalentAbilityHandler.A_RANGER_THORN_VISUAL_KEY);
                }
                long totemVisUntil = p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_TOTEM_UNTIL_KEY);
                if (totemVisUntil > 0 && totemVisUntil <= time && p.getPersistentData().hasUUID(TalentAbilityHandler.A_RANGER_TOTEM_VISUAL_KEY)) {
                    UUID ovid = p.getPersistentData().getUUID(TalentAbilityHandler.A_RANGER_TOTEM_VISUAL_KEY);
                    TalentAbilityHandler.broadcastRangerLifeTotemHide(serverLevel, ovid);
                    p.getPersistentData().remove(TalentAbilityHandler.A_RANGER_TOTEM_VISUAL_KEY);
                }
                long barrUntil = p.getPersistentData().getLong("lvluping_as_barricade_remove_at");
                if (barrUntil > 0 && barrUntil <= time) {
                    int barrX = p.getPersistentData().getInt("lvluping_as_barricade_x");
                    int barrY = p.getPersistentData().getInt("lvluping_as_barricade_y");
                    int barrZ = p.getPersistentData().getInt("lvluping_as_barricade_z");
                    float barrRot = p.getPersistentData().getFloat(TalentAbilityHandler.AS_WANDERER_BARRICADE_Y_ROT_KEY);
                    TalentAbilityHandler.removeAssassinBarricadeBarriers(serverLevel, barrX, barrY, barrZ, barrRot);
                    p.getPersistentData().remove("lvluping_as_barricade_remove_at");
                    p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_BARRICADE_Y_ROT_KEY);
                    if (p.getPersistentData().hasUUID(TalentAbilityHandler.AS_WANDERER_BARRICADE_VISUAL_KEY)) {
                        UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.AS_WANDERER_BARRICADE_VISUAL_KEY);
                        TalentAbilityHandler.broadcastAssassinBarricadeHide(serverLevel, vid);
                        p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_BARRICADE_VISUAL_KEY);
                    }
                }
                long twUntil = p.getPersistentData().getLong("lvluping_as_tripwire_until");
                if (twUntil > time) {
                    double x = p.getPersistentData().getDouble("lvluping_as_tripwire_x");
                    double y = p.getPersistentData().getDouble("lvluping_as_tripwire_y");
                    double z = p.getPersistentData().getDouble("lvluping_as_tripwire_z");
                    double r = p.getPersistentData().getDouble("lvluping_as_tripwire_r");
                    float dmg = p.getPersistentData().getFloat("lvluping_as_tripwire_dmg");
                    AABB area = new AABB(x - r, y - 2, z - r, x + r, y + 2, z + r);
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e == p || e.isAlliedTo(p)) continue;
                        e.hurt(p.damageSources().playerAttack(p), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false));
                        p.getPersistentData().putLong("lvluping_as_tripwire_until", 0);
                        if (p.getPersistentData().hasUUID(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY)) {
                            UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY);
                            TalentAbilityHandler.broadcastAssassinTripwireHide(serverLevel, vid);
                            p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY);
                        }
                        break;
                    }
                } else if (twUntil > 0 && twUntil <= time) {
                    p.getPersistentData().putLong("lvluping_as_tripwire_until", 0);
                    if (p.getPersistentData().hasUUID(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY)) {
                        UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY);
                        TalentAbilityHandler.broadcastAssassinTripwireHide(serverLevel, vid);
                        p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_TRIPWIRE_VISUAL_KEY);
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_THORN_TRAIL_UNTIL_KEY) > time) {
                    float dps = p.getPersistentData().getFloat("lvluping_as_wanderer_thorn_trail_dps");
                    if (time % 10 == 0) {
                        AABB area = p.getBoundingBox().inflate(1.8, 1.0, 1.8);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e == p || e.isAlliedTo(p)) continue;
                            e.hurt(p.damageSources().playerAttack(p), dps);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                        }
                    }
                    serverLevel.sendParticles(ParticleTypes.CRIT, p.getX(), p.getY() + 0.1, p.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_R_KEY);
                    float dps = p.getPersistentData().getFloat(TalentAbilityHandler.AS_ROGUE_POISON_VEIL_DPS_KEY);
                    if (time % 20 == 0) {
                        AABB area = new AABB(x - r, y - 2, z - r, x + r, y + 2, z + r);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e.isAlliedTo(p)) continue;
                            e.hurt(p.damageSources().magic(), dps);
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false));
                        }
                    }
                    serverLevel.sendParticles(ParticleTypes.WITCH, x, y + 0.7, z, 8, r * 0.25, 0.3, r * 0.25, 0.02);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_CAMP_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.AS_WANDERER_CAMP_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.AS_WANDERER_CAMP_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.AS_WANDERER_CAMP_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.AS_WANDERER_CAMP_R_KEY);
                    float hps = p.getPersistentData().getFloat(TalentAbilityHandler.AS_WANDERER_CAMP_HPS_KEY);
                    if (time % 20 == 0) {
                        AABB area = new AABB(x - r, y - 2, z - r, x + r, y + 2, z + r);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e == p || e.isAlliedTo(p)) e.heal(hps);
                        }
                    }
                    serverLevel.sendParticles(ParticleTypes.HEART, x, y + 1.0, z, 3, r * 0.2, 0.2, r * 0.2, 0.01);
                } else {
                    long cu = p.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_CAMP_UNTIL_KEY);
                    if (cu > 0 && cu <= time && p.getPersistentData().hasUUID(TalentAbilityHandler.AS_WANDERER_CAMP_VISUAL_KEY)) {
                        UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.AS_WANDERER_CAMP_VISUAL_KEY);
                        TalentAbilityHandler.broadcastAssassinCampHide(serverLevel, vid);
                        p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_CAMP_VISUAL_KEY);
                        p.getPersistentData().remove(TalentAbilityHandler.AS_WANDERER_CAMP_UNTIL_KEY);
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.AS_WANDERER_DAGGER_RAIN_UNTIL_KEY) > time) {
                    int shots = p.getPersistentData().getInt(TalentAbilityHandler.AS_WANDERER_DAGGER_RAIN_SHOTS_KEY);
                    if (shots > 0 && time % 6 == 0) {
                        p.getPersistentData().putInt(TalentAbilityHandler.AS_WANDERER_DAGGER_RAIN_SHOTS_KEY, shots - 1);
                        Snowball sb = net.minecraft.world.entity.EntityType.SNOWBALL.create(serverLevel);
                        if (sb != null) {
                            sb.setOwner(p);
                            sb.setPos(p.getX(), p.getEyeY(), p.getZ());
                            Vec3 dir = p.getLookAngle().normalize();
                            sb.shoot(dir.x, dir.y, dir.z, 1.8f, 6.0f);
                            sb.getPersistentData().putBoolean("lvluping_as_shuriken", true);
                            sb.getPersistentData().putFloat("lvluping_as_shuriken_dmg", 3.0f);
                            sb.getPersistentData().putInt("lvluping_as_shuriken_bleed_ticks", 40);
                            sb.getPersistentData().putFloat("lvluping_as_shuriken_bleed_dps", 1.0f);
                            serverLevel.addFreshEntity(sb);
                        }
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.AS_ASSASSIN_BLACK_MIST_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ASSASSIN_BLACK_MIST_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ASSASSIN_BLACK_MIST_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ASSASSIN_BLACK_MIST_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.AS_ASSASSIN_BLACK_MIST_R_KEY);
                    AABB area = new AABB(x - r, y - 2, z - r, x + r, y + 2, z + r);
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e.isAlliedTo(p)) continue;
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                    }
                    if (p.distanceToSqr(x, y, z) <= r * r) {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
                    }
                    serverLevel.sendParticles(ParticleTypes.SMOKE, x, y + 1.0, z, 10, r * 0.25, 0.3, r * 0.25, 0.02);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_HUNTER_TRAP_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.A_HUNTER_TRAP_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.A_HUNTER_TRAP_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.A_HUNTER_TRAP_Z_KEY);
                    if (p.getPersistentData().hasUUID("lvluping_a_hunter_trap_entity")) {
                        UUID tu = p.getPersistentData().getUUID("lvluping_a_hunter_trap_entity");
                        var ent = serverLevel.getEntity(tu);
                        if (ent instanceof net.minecraft.world.entity.Display.ItemDisplay id) {
                            id.setPos(x, y + 0.02, z);
                            id.setDeltaMovement(Vec3.ZERO);
                        }
                    }
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.A_HUNTER_TRAP_R_KEY);
                    float dmg = p.getPersistentData().getFloat(TalentAbilityHandler.A_HUNTER_TRAP_DMG_KEY);
                    int root = p.getPersistentData().getInt(TalentAbilityHandler.A_HUNTER_TRAP_ROOT_KEY);
                    AABB area = new AABB(x - r, y - 2.0, z - r, x + r, y + 2.0, z + r);
                    boolean triggered = false;
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e == p || e.isAlliedTo(p)) continue;
                        if (e.distanceToSqr(x, y, z) > r * r) continue;
                        e.hurt(p.damageSources().playerAttack(p), dmg);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 8, false, false));
                        triggered = true;
                        break;
                    }
                    if (triggered) {
                        p.getPersistentData().putLong(TalentAbilityHandler.A_HUNTER_TRAP_UNTIL_KEY, 0);
                        if (p.getPersistentData().hasUUID(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY)) {
                            UUID vid = p.getPersistentData().getUUID(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY);
                            TalentAbilityHandler.broadcastHunterTrapHide(serverLevel, vid);
                            p.getPersistentData().remove(TalentAbilityHandler.A_HUNTER_TRAP_VISUAL_KEY);
                        }
                        serverLevel.playSound(null, x, y, z, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.9f, 0.9f);
                    }
                }
                if (talents.contains("a_ult_hunter_sniper")) {
                    int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "a_ult_hunter_sniper", talents);
                    int need = AbilityUpgradeConfig.getInt("a_ult_hunter_sniper", "aim_ticks", lvl, 40);
                    long cdUntil = p.getPersistentData().getLong("lvluping_hunter_sniper_cd_until");
                    if (cdUntil <= time && p.isUsingItem() && p.getDeltaMovement().horizontalDistanceSqr() < 0.0025) {
                        int acc = p.getPersistentData().getInt("lvluping_hunter_sniper_aim_acc") + 1;
                        p.getPersistentData().putInt("lvluping_hunter_sniper_aim_acc", acc);
                        if (acc >= need) {
                            p.getPersistentData().putBoolean("lvluping_hunter_sniper_ready", true);
                            if (time % 10 == 0) serverLevel.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getEyeY(), p.getZ(), 2, 0.05, 0.05, 0.05, 0.01);
                        }
                    } else {
                        p.getPersistentData().putInt("lvluping_hunter_sniper_aim_acc", 0);
                    }
                }
                long mergeUntil = p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_MERGE_UNTIL_KEY);
                if (mergeUntil > 0 && time >= mergeUntil && p.getPersistentData().hasUUID("lvluping_ranger_merge_tree_vis")) {
                    java.util.UUID treeVid = p.getPersistentData().getUUID("lvluping_ranger_merge_tree_vis");
                    TalentAbilityHandler.broadcastRangerMergeTreeHide(serverLevel, treeVid);
                    p.getPersistentData().remove("lvluping_ranger_merge_tree_vis");
                    p.getPersistentData().putLong(TalentAbilityHandler.A_RANGER_MERGE_UNTIL_KEY, 0L);
                    p.removeEffect(MobEffects.INVISIBILITY);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_MERGE_UNTIL_KEY) > time) {
                    double ax = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_MERGE_AX_KEY);
                    double ay = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_MERGE_AY_KEY);
                    double az = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_MERGE_AZ_KEY);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.teleportTo(serverLevel, ax, ay, az, p.getYRot(), p.getXRot());
                    p.resetFallDistance();
                    float hps = p.getPersistentData().getFloat(TalentAbilityHandler.A_RANGER_MERGE_HPS_KEY);
                    if (time % 20 == 0 && hps > 0f) {
                        p.heal(hps);
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, ax, ay + 1.0, az, 8, 0.4, 0.6, 0.4, 0.03);
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_THORN_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_THORN_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_THORN_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_THORN_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_THORN_R_KEY);
                    float dps = p.getPersistentData().getFloat(TalentAbilityHandler.A_RANGER_THORN_DPS_KEY);
                    int slowAmp = p.getPersistentData().getInt(TalentAbilityHandler.A_RANGER_THORN_SLOW_AMP_KEY);
                    if (time % 20 == 0) {
                        AABB area = new AABB(x - r, y - 2.0, z - r, x + r, y + 2.0, z + r);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e.isAlliedTo(p)) continue;
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, Math.max(0, slowAmp), false, false));
                            var epd = e.getPersistentData();
                            if (!epd.getBoolean("lvluping_thorn_init")) {
                                epd.putBoolean("lvluping_thorn_init", true);
                                epd.putDouble("lvluping_thorn_px", e.getX());
                                epd.putDouble("lvluping_thorn_py", e.getY());
                                epd.putDouble("lvluping_thorn_pz", e.getZ());
                                continue;
                            }
                            double px = epd.getDouble("lvluping_thorn_px");
                            double py = epd.getDouble("lvluping_thorn_py");
                            double pz = epd.getDouble("lvluping_thorn_pz");
                            double moved = (e.getX() - px) * (e.getX() - px) + (e.getY() - py) * (e.getY() - py) + (e.getZ() - pz) * (e.getZ() - pz);
                            boolean moving = moved > 0.00015 || e.getDeltaMovement().horizontalDistanceSqr() > 1.0e-6;
                            epd.putDouble("lvluping_thorn_px", e.getX());
                            epd.putDouble("lvluping_thorn_py", e.getY());
                            epd.putDouble("lvluping_thorn_pz", e.getZ());
                            if (moving) {
                                e.hurt(p.damageSources().playerAttack(p), Math.max(0f, dps));
                            }
                        }
                    }
                    if (time % 35 == 0) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.5, z, 2, r * 0.15, 0.12, r * 0.15, 0.01);
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_TOTEM_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_TOTEM_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_TOTEM_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_TOTEM_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_TOTEM_R_KEY);
                    float total = p.getPersistentData().getFloat(TalentAbilityHandler.A_RANGER_TOTEM_HEAL_TOTAL_KEY);
                    if (time % 20 == 0 && total > 0f) {
                        float heal = total * (float) p.getMaxHealth() / 10.0f;
                        AABB area = new AABB(x - r, y - 3.0, z - r, x + r, y + 3.0, z + r);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e == p || e.isAlliedTo(p)) {
                                e.heal(heal);
                                serverLevel.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + 1.0, e.getZ(), 1, 0.2, 0.3, 0.2, 0.0);
                            }
                        }
                    }
                    if (time % 25 == 0) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 1, r * 0.15, 0.4, r * 0.15, 0.02);
                    }
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_WRATH_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_WRATH_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_WRATH_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_WRATH_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_WRATH_R_KEY);
                    if (time % 20 == 0) {
                        AABB area = new AABB(x - r, y - 3.0, z - r, x + r, y + 3.0, z + r);
                        LivingEntity best = null;
                        double bestD2 = Double.MAX_VALUE;
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e.isAlliedTo(p)) continue;
                            double d2 = (e.getX() - x) * (e.getX() - x) + (e.getZ() - z) * (e.getZ() - z);
                            if (d2 < bestD2) {
                                bestD2 = d2;
                                best = e;
                            }
                        }
                        if (best != null) {
                            var bolt = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, serverLevel);
                            bolt.moveTo(best.getX(), best.getY(), best.getZ());
                            bolt.setVisualOnly(true);
                            serverLevel.addFreshEntity(bolt);
                            best.hurt(p.damageSources().playerAttack(p), 4.0f);
                        }
                    }
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.0, z, 6, r * 0.25, 0.7, r * 0.25, 0.03);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_RANGER_ROOTS_UNTIL_KEY) > time) {
                    double x = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_ROOTS_X_KEY);
                    double y = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_ROOTS_Y_KEY);
                    double z = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_ROOTS_Z_KEY);
                    double r = p.getPersistentData().getDouble(TalentAbilityHandler.A_RANGER_ROOTS_R_KEY);
                    AABB area = new AABB(x - r, y - 2.0, z - r, x + r, y + 2.0, z + r);
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e.isAlliedTo(p)) continue;
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 9, false, false));
                    }
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.6, z, 6, r * 0.3, 0.2, r * 0.3, 0.02);
                }
                if (p.getPersistentData().getLong(TalentAbilityHandler.A_MUSK_BARRAGE_UNTIL_KEY) > time) {
                    int shotsLeft = p.getPersistentData().getInt(TalentAbilityHandler.A_MUSK_BARRAGE_SHOTS_KEY);
                    if (shotsLeft > 0 && time % 5 == 0) {
                        shotsLeft--;
                        p.getPersistentData().putInt(TalentAbilityHandler.A_MUSK_BARRAGE_SHOTS_KEY, shotsLeft);
                        net.minecraft.world.entity.projectile.Arrow a = net.minecraft.world.entity.EntityType.ARROW.create(serverLevel);
                        if (a != null) {
                            a.setOwner(p);
                            a.setBaseDamage(a.getBaseDamage() + 1.0);
                            a.setPos(p.getX(), p.getEyeY() - 0.1, p.getZ());
                            Vec3 dir = p.getLookAngle().normalize();
                            a.shoot(dir.x, dir.y, dir.z, 2.2f, 8.0f);
                            serverLevel.addFreshEntity(a);
                            serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.4f, 1.8f);
                        }
                    }
                }
                if (talents.contains("a_ranger_quick_step")) {
                    int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "a_ranger_quick_step", talents);
                    int need = AbilityUpgradeConfig.getInt("a_ranger_quick_step", "no_damage_ticks_required", lvl, 200);
                    int speedTicks = AbilityUpgradeConfig.getInt("a_ranger_quick_step", "speed_ticks", lvl, 60);
                    int speedAmp = AbilityUpgradeConfig.getInt("a_ranger_quick_step", "speed_amp", lvl, 1);
                    float mult = (float) AbilityUpgradeConfig.getDouble("a_ranger_quick_step", "first_hit_mult", lvl, 2.0);
                    int icd = AbilityUpgradeConfig.getInt("a_ranger_quick_step", "internal_cd", lvl, 200);
                    long cdUntil = p.getPersistentData().getLong("lvluping_ranger_quick_step_cd_until");
                    if (cdUntil <= time) {
                        long lastDmg = p.getPersistentData().getLong("lvluping_last_taken_damage_at");
                        if (time - lastDmg >= need) {
                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600000, speedAmp, false, false));
                            p.getPersistentData().putBoolean("lvluping_ranger_quick_step_speed", true);
                            p.getPersistentData().putLong("lvluping_ranger_quick_step_until", time + speedTicks);
                            p.getPersistentData().putFloat("lvluping_ranger_quick_step_mult", mult);
                            p.getPersistentData().putLong("lvluping_ranger_quick_step_cd_until", time + icd);
                            serverLevel.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
                            serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.6f);
                        }
                    }
                }
                if (talents.contains("a_musketeer_smoke")) {
                    int lvl = PlayerLevels.getAbilityLevel(p.getUUID(), "a_musketeer_smoke", talents);
                    double hpTh = AbilityUpgradeConfig.getDouble("a_musketeer_smoke", "hp_threshold", lvl, 0.35);
                    int dur = AbilityUpgradeConfig.getInt("a_musketeer_smoke", "duration_ticks", lvl, 80);
                    double radius = AbilityUpgradeConfig.getDouble("a_musketeer_smoke", "radius", lvl, 4.0);
                    int slowAmp = AbilityUpgradeConfig.getInt("a_musketeer_smoke", "slow_amp", lvl, 1);
                    int icd = AbilityUpgradeConfig.getInt("a_musketeer_smoke", "internal_cd", lvl, 300);
                    long cdUntil = p.getPersistentData().getLong("lvluping_musk_smoke_cd_until");
                    if (cdUntil <= time && p.getMaxHealth() > 0f && (p.getHealth() / p.getMaxHealth()) <= hpTh) {
                        p.getPersistentData().putLong("lvluping_musk_smoke_cd_until", time + icd);
                        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0, false, false));
                        serverLevel.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 0.9f, 1.0f);
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 40, 0.6, 0.3, 0.6, 0.02);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius, 2.0, radius))) {
                            if (e == p) continue;
                            if (e.isAlliedTo(p)) continue;
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, slowAmp, false, false));
                        }
                    }
                }
            }

            if (time % 20 == 0) {
                for (ServerPlayer p : serverLevel.players()) {
                    AABB area = p.getBoundingBox().inflate(64.0, 16.0, 64.0);
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        long bu = e.getPersistentData().getLong("lvluping_hunter_bleed_until");
                        if (bu <= time) continue;
                        float dps = e.getPersistentData().getFloat("lvluping_hunter_bleed_dps");
                        UUID src = e.getPersistentData().hasUUID("lvluping_hunter_bleed_src") ? e.getPersistentData().getUUID("lvluping_hunter_bleed_src") : null;
                        if (src == null) continue;
                        Player pl = serverLevel.getPlayerByUUID(src);
                        if (!(pl instanceof ServerPlayer sp)) continue;
                        e.hurt(sp.damageSources().playerAttack(sp), Math.max(0f, dps));
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1.0, e.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                for (ServerPlayer p : serverLevel.players()) {
                    AABB area = p.getBoundingBox().inflate(64.0, 16.0, 64.0);
                    for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                        long bu = e.getPersistentData().getLong("lvluping_as_bleed_until");
                        if (bu <= time) continue;
                        float dps = e.getPersistentData().getFloat("lvluping_as_bleed_dps");
                        UUID src = e.getPersistentData().hasUUID("lvluping_as_bleed_src") ? e.getPersistentData().getUUID("lvluping_as_bleed_src") : null;
                        if (src == null) continue;
                        Player pl = serverLevel.getPlayerByUUID(src);
                        if (!(pl instanceof ServerPlayer sp)) continue;
                        e.hurt(sp.damageSources().playerAttack(sp), Math.max(0f, dps));
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1.0, e.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
                    }
                }
                for (ServerPlayer p : serverLevel.players()) {
                    long bdUntil = p.getPersistentData().getLong("lvluping_as_blade_dance_until");
                    if (bdUntil > time && p.getPersistentData().hasUUID("lvluping_as_blade_dance_target")) {
                        long next = p.getPersistentData().getLong("lvluping_as_blade_dance_next_hit_at");
                        int left = p.getPersistentData().getInt("lvluping_as_blade_dance_hits_left");
                        if (left > 0 && next <= time) {
                            UUID tid = p.getPersistentData().getUUID("lvluping_as_blade_dance_target");
                            var ent = serverLevel.getEntity(tid);
                            if (ent instanceof LivingEntity t && t.isAlive() && t != p) {
                                float mult = p.getPersistentData().getFloat("lvluping_as_blade_dance_mult");
                                float base = (float) p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                                t.invulnerableTime = 0;
                                t.hurt(p.damageSources().playerAttack(p), Math.max(0.1f, base * mult));
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 0.9, t.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                                int bleedTicks = p.getPersistentData().getInt("lvluping_as_blade_dance_bleed_ticks");
                                t.getPersistentData().putLong("lvluping_as_bleed_until", Math.max(t.getPersistentData().getLong("lvluping_as_bleed_until"), time + bleedTicks));
                                t.getPersistentData().putFloat("lvluping_as_bleed_dps", Math.max(t.getPersistentData().getFloat("lvluping_as_bleed_dps"), 1.4f));
                                t.getPersistentData().putUUID("lvluping_as_bleed_src", p.getUUID());
                            }
                            p.getPersistentData().putInt("lvluping_as_blade_dance_hits_left", left - 1);
                            p.getPersistentData().putLong("lvluping_as_blade_dance_next_hit_at", time + 2);
                        }
                    } else if (bdUntil > 0 && bdUntil <= time) {
                        p.getPersistentData().remove("lvluping_as_blade_dance_until");
                        p.getPersistentData().remove("lvluping_as_blade_dance_target");
                        p.getPersistentData().remove("lvluping_as_blade_dance_hits_left");
                        p.getPersistentData().remove("lvluping_as_blade_dance_mult");
                        p.getPersistentData().remove("lvluping_as_blade_dance_bleed_ticks");
                        p.getPersistentData().remove("lvluping_as_blade_dance_next_hit_at");
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onSummonChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.Mob mob)) return;
        if (!mob.getPersistentData().hasUUID("lvluping_summon_owner")) return;
        var newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget == null) return;
        UUID ownerUuid = mob.getPersistentData().getUUID("lvluping_summon_owner");
        if (newTarget.getUUID().equals(ownerUuid)) {
            event.setNewAboutToBeSetTarget(null);
            event.setCanceled(true);
            mob.setTarget(null);
            return;
        }
        if (mob.level() instanceof ServerLevel sl && !SummonerHandler.isAllowedTargetForOwner(sl, ownerUuid, newTarget)) {
            event.setNewAboutToBeSetTarget(null);
            event.setCanceled(true);
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
            if (PlayerLevels.getCooldown(player.getUUID(), "cd_slide") <= 0) {
                TalentAbilityHandler.refillSlideCharges(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
            if (PlayerLevels.getCooldown(player.getUUID(), "cd_slide") <= 0) {
                TalentAbilityHandler.refillSlideCharges(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
            if (PlayerLevels.getCooldown(player.getUUID(), "cd_slide") <= 0) {
                TalentAbilityHandler.refillSlideCharges(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            UltimatesHandler.removeLightRayLightBlocks(sl, sp.getPersistentData());
            UltimatesHandler.removeLightFormBlocks(sl, sp.getPersistentData());
            var pd = sp.getPersistentData();
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_RADIUS_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_HEAL_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_SHIELD_RATIO_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AX_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AY_KEY);
            pd.remove(TalentAbilityHandler.W_LIGHT_FORM_AZ_KEY);
            UltimatesHandler.clearLightFormMoveLock(sp);
            UltimatesHandler.clearLightFormNoGravity(sp);
            sp.removeEffect(MobEffects.GLOWING);
        }
    }

    private static void decrementCooldown(Player player, String key) {
        int val = PlayerLevels.getCooldown(player.getUUID(), key);
        if (val > 0) {
            val -= 1;
            PlayerLevels.setCooldown(player.getUUID(), key, val);
            player.getPersistentData().putInt(key, val);
            if (val == 0 && "cd_slide".equals(key) && player instanceof ServerPlayer sp) {
                TalentAbilityHandler.refillSlideCharges(sp);
            }
        } else {
            player.getPersistentData().putInt(key, 0);
        }
    }

    private static void decrementWindow(Player player, String key) {
        int val = player.getPersistentData().getInt(key);
        if (val > 0) {
            player.getPersistentData().putInt(key, val - 1);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (attacker.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > attacker.level().getGameTime()) {
            event.setCanceled(true);
            return;
        }
        if (attacker.getPersistentData().getLong("lvluping_m_ice_block_until") > attacker.level().getGameTime()) {
            event.setCanceled(true);
            return;
        }
        Set<String> talents = PlayerLevels.getPlayerTalents(attacker.getUUID());

        // --- A_DAGGER ---
    }

    @SubscribeEvent
    public static void onRightClickItemLightForm(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (sp.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > sp.level().getGameTime()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlockLightForm(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (sp.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > sp.level().getGameTime()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (sp.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > sp.level().getGameTime()) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Player player) {
            Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
            if (talents.contains("a_power")) {
                int charge = event.getCharge();
                if (charge >= TICKS_PER_SECOND) {
                }
            }
            if (player instanceof ServerPlayer sp && talents.contains("a_hunter_light_hand")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "a_hunter_light_hand", talents);
                double chance = AbilityUpgradeConfig.getDouble("a_hunter_light_hand", "save_arrow_chance", lvl, 0.2);
                if (sp.getRandom().nextDouble() < chance) {
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW, 1));
                    if (sp.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, sp.getX(), sp.getY() + 1.0, sp.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer sp)) return;
        TalentAbilityHandler.onArcherArrowSpawned(sp, arrow);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide() || !(living.level() instanceof ServerLevel sl)) return;
        long time = sl.getGameTime();
        var epd = living.getPersistentData();
        long until = epd.getLong(TalentAbilityHandler.RANGER_ROOTS_UNTIL_KEY);
        if (until > 0) {
            if (time >= until) {
                epd.remove(TalentAbilityHandler.RANGER_ROOTS_UNTIL_KEY);
                epd.remove(TalentAbilityHandler.RANGER_ROOTS_DPS_KEY);
                epd.remove(TalentAbilityHandler.RANGER_ROOTS_OWNER_KEY);
                TalentAbilityHandler.broadcastRangerRootsTargetHide(sl, living.getId());
            } else {
                living.setDeltaMovement(Vec3.ZERO);
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 9, false, false));
                if (time % 20 == 0) {
                    float dps = epd.getFloat(TalentAbilityHandler.RANGER_ROOTS_DPS_KEY);
                    if (dps > 0f && epd.hasUUID(TalentAbilityHandler.RANGER_ROOTS_OWNER_KEY)) {
                        var ownerId = epd.getUUID(TalentAbilityHandler.RANGER_ROOTS_OWNER_KEY);
                        ServerPlayer atk = sl.getServer().getPlayerList().getPlayer(ownerId);
                        if (atk != null) {
                            living.hurt(atk.damageSources().playerAttack(atk), dps);
                        } else {
                            living.hurt(sl.damageSources().magic(), dps);
                        }
                    }
                }
            }
        }

        if (epd.getBoolean("lvluping_as_double_clone")) {
            long explodeAt = epd.getLong("lvluping_as_double_explode_at");
            if (explodeAt > 0 && time >= explodeAt) {
                double r = epd.getDouble("lvluping_as_double_r");
                float dmg = epd.getFloat("lvluping_as_double_dmg");
                int bleedTicks = epd.getInt("lvluping_as_double_bleed_ticks");
                float bleedDps = epd.getFloat("lvluping_as_double_bleed_dps");
                UUID ownerId = epd.hasUUID("lvluping_as_double_owner") ? epd.getUUID("lvluping_as_double_owner") : null;
                ServerPlayer owner = ownerId != null ? sl.getServer().getPlayerList().getPlayer(ownerId) : null;
                AABB area = living.getBoundingBox().inflate(Math.max(0.5, r), 2.5, Math.max(0.5, r));
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (e == living) continue;
                    if (owner != null && (e == owner || e.isAlliedTo(owner))) continue;
                    if (owner != null) e.hurt(owner.damageSources().playerAttack(owner), dmg);
                    else e.hurt(sl.damageSources().magic(), dmg);
                    if (bleedTicks > 0) {
                        e.getPersistentData().putLong("lvluping_as_bleed_until", Math.max(e.getPersistentData().getLong("lvluping_as_bleed_until"), time + bleedTicks));
                        e.getPersistentData().putFloat("lvluping_as_bleed_dps", Math.max(e.getPersistentData().getFloat("lvluping_as_bleed_dps"), bleedDps));
                        if (owner != null) e.getPersistentData().putUUID("lvluping_as_bleed_src", owner.getUUID());
                    }
                }
                sl.sendParticles(ParticleTypes.EXPLOSION, living.getX(), living.getY() + 0.6, living.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                sl.sendParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + 0.7, living.getZ(), 22, 0.6, 0.5, 0.6, 0.05);
                sl.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.15f);
                living.discard();
            }
        }

    }

    private static void clearArrowNextArrowNbt(CompoundTag ad) {
        ad.remove(TalentAbilityHandler.A_NEXT_ARROW_EFFECT_KEY);
        ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
        ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
        ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P3_KEY);
        ad.remove(TalentAbilityHandler.A_NEXT_ARROW_UNTIL_KEY);
    }

    private static boolean isRangerArrowHeadshot(AbstractArrow arrow, LivingEntity victim) {
        return arrow.getY() >= victim.getEyeY() - 0.4;
    }

    private static boolean livingHasHarmfulDebuff(LivingEntity e) {
        for (MobEffectInstance inst : e.getActiveEffects()) {
            if (inst == null || inst.getEffect() == null) continue;
            if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                return true;
            }
        }
        return false;
    }

    private static void spawnThunderAt(ServerLevel sl, Vec3 pos) {
        var bolt = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, sl);
        bolt.moveTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(true);
        sl.addFreshEntity(bolt);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    private static void handleRangerArrowImpact(ServerLevel sl, AbstractArrow arrow, ServerPlayer owner, HitResult hit) {
        var opd = owner.getPersistentData();
        long now = sl.getGameTime();
        Set<String> ot = PlayerLevels.getPlayerTalents(owner.getUUID());
        Vec3 pos = hit.getLocation();

        if (opd.getBoolean("lvluping_ranger_wrath_pending") && opd.getLong("lvluping_ranger_wrath_next_until") >= now && ot.contains("a_ult_ranger_wrath")) {
            int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_ult_ranger_wrath", ot);
            int dur = AbilityUpgradeConfig.getInt("a_ult_ranger_wrath", "duration_ticks", lvl, 100);
            double r = AbilityUpgradeConfig.getDouble("a_ult_ranger_wrath", "radius", lvl, 8.0);
            opd.remove("lvluping_ranger_wrath_pending");
            opd.remove("lvluping_ranger_wrath_next_until");
            opd.putLong(TalentAbilityHandler.A_RANGER_WRATH_UNTIL_KEY, sl.getGameTime() + dur);
            opd.putDouble(TalentAbilityHandler.A_RANGER_WRATH_X_KEY, pos.x);
            opd.putDouble(TalentAbilityHandler.A_RANGER_WRATH_Y_KEY, pos.y);
            opd.putDouble(TalentAbilityHandler.A_RANGER_WRATH_Z_KEY, pos.z);
            opd.putDouble(TalentAbilityHandler.A_RANGER_WRATH_R_KEY, r);
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.7f, 0.9f);
        }

        CompoundTag ad = arrow.getPersistentData();
        int eff = ad.getInt(TalentAbilityHandler.A_NEXT_ARROW_EFFECT_KEY);
        if (eff == 4) {
            spawnThunderAt(sl, pos);
            if (hit instanceof BlockHitResult) {
                clearArrowNextArrowNbt(ad);
            }
        } else if (eff == 2 && hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity victim && victim.isAlive()) {
            if (!victim.isAlliedTo(owner)) {
                int slowTicks = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                int slowAmp = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                if (slowTicks > 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, Math.max(0, slowAmp), false, false));
                }
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, victim.getX(), victim.getY() + 0.7, victim.getZ(), 14, 0.35, 0.25, 0.35, 0.03);
            }
            clearArrowNextArrowNbt(ad);
        } else if (eff == 14 && hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity victim && victim.isAlive() && victim != owner) {
            if (!victim.isAlliedTo(owner)) {
                int rootTicks = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                float dps = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                long until = sl.getGameTime() + rootTicks;
                var vpd = victim.getPersistentData();
                vpd.putLong(TalentAbilityHandler.RANGER_ROOTS_UNTIL_KEY, until);
                vpd.putFloat(TalentAbilityHandler.RANGER_ROOTS_DPS_KEY, dps);
                vpd.putUUID(TalentAbilityHandler.RANGER_ROOTS_OWNER_KEY, owner.getUUID());
                TalentAbilityHandler.broadcastRangerRootsTargetShow(sl, victim.getId(), until);
                sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.85f, 0.95f);
            }
            clearArrowNextArrowNbt(ad);
        } else if (eff == 14 && hit instanceof BlockHitResult) {
            clearArrowNextArrowNbt(ad);
        } else if (eff == 42) {
            double r = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
            float dmg = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
            AABB area = new AABB(pos.x - r, pos.y - 2.0, pos.z - r, pos.x + r, pos.y + 2.0, pos.z + r);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                if (e == owner || e.isAlliedTo(owner)) continue;
                if (e.distanceToSqr(pos) > r * r) continue;
                e.hurt(owner.damageSources().playerAttack(owner), dmg);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            }
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
            sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 30, r * 0.25, 0.25, r * 0.25, 0.02);
            clearArrowNextArrowNbt(ad);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel sl)) return;

        if (projectile instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer owner) {
            handleRangerArrowImpact(sl, arrow, owner, event.getRayTraceResult());
        }

        long time = sl.getGameTime();
        if (event.getRayTraceResult() instanceof EntityHitResult ehr && ehr.getEntity() instanceof ServerPlayer sp) {
            long bladeWallUntil = sp.getPersistentData().getLong(TalentAbilityHandler.W_SWORDMASTER_BLADE_WALL_UNTIL_KEY);
            if (bladeWallUntil > time) {
                projectile.discard();
                event.setCanceled(true);
                return;
            }
        }

        if (projectile.getPersistentData().getLong("lvluping_anti_magic_reflect_until") > time) return;
        ServerPlayer bestProtector = null;
        double bestDist2 = Double.MAX_VALUE;
        for (ServerPlayer p : sl.players()) {
            long antiUntil = p.getPersistentData().getLong("lvluping_m_anti_magic_until");
            if (antiUntil <= time) continue;

            if (projectile.getOwner() != null && projectile.getOwner().getUUID().equals(p.getUUID())) continue;

            double radius = p.getPersistentData().getDouble("lvluping_m_anti_magic_radius");
            if (radius <= 0) radius = ANTI_MAGIC_DEFAULT_RADIUS;

            double dist2 = p.distanceToSqr(projectile);
            if (dist2 <= radius * radius && dist2 < bestDist2) {
                bestDist2 = dist2;
                bestProtector = p;
            }
        }

        if (bestProtector != null) {
            projectile.getPersistentData().putLong("lvluping_anti_magic_reflect_until", time + ANTI_MAGIC_REFLECT_SUPPRESS_TICKS);
            reflectProjectile(projectile, bestProtector);
            event.setCanceled(true);
            return;
        }

        HitResult hit = event.getRayTraceResult();

        if (projectile instanceof SmallFireball fireball && fireball.getPersistentData().getBoolean("lvluping_spell_fireball")) {
            double igniteR = fireball.getPersistentData().getDouble("lvluping_fireball_ignite_r");
            if (igniteR > 1.0e-4) {
                Vec3 pos = hit.getLocation();
                AABB zone = new AABB(pos.x - igniteR, pos.y - 1.0, pos.z - igniteR, pos.x + igniteR, pos.y + 2.5, pos.z + igniteR);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, zone)) {
                    if (e.distanceToSqr(pos) <= igniteR * igniteR) {
                        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 80));
                    }
                }
                igniteBlocksInHorizontalRadius(sl, pos, igniteR);
            }
        }

        if (!(projectile instanceof Snowball snowball)) return;
        var pd = snowball.getPersistentData();
        if (pd.getBoolean("lvluping_as_shuriken")) {
            float dmg = pd.getFloat("lvluping_as_shuriken_dmg");
            int bleedTicks = pd.getInt("lvluping_as_shuriken_bleed_ticks");
            float bleedDps = pd.getFloat("lvluping_as_shuriken_bleed_dps");
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
                if (snowball.getOwner() instanceof Player owner) {
                    living.hurt(owner.damageSources().playerAttack(owner), dmg);
                    living.getPersistentData().putLong("lvluping_as_bleed_until", sl.getGameTime() + bleedTicks);
                    living.getPersistentData().putFloat("lvluping_as_bleed_dps", bleedDps);
                    living.getPersistentData().putUUID("lvluping_as_bleed_src", owner.getUUID());
                } else {
                    living.hurt(sl.damageSources().magic(), dmg);
                }
                sl.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + 1.0, living.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
            }
            snowball.discard();
            event.setCanceled(true);
            return;
        }
        if (pd.getBoolean("lvluping_musk_grenade")) {
            double r = pd.getDouble("lvluping_musk_grenade_r");
            float dmg = pd.getFloat("lvluping_musk_grenade_dmg");
            Vec3 pos = hit.getLocation();
            AABB area = new AABB(pos.x - r, pos.y - 2.0, pos.z - r, pos.x + r, pos.y + 2.0, pos.z + r);
            Player owner = snowball.getOwner() instanceof Player p ? p : null;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                if (owner != null && e.isAlliedTo(owner)) continue;
                if (e.distanceToSqr(pos) > r * r) continue;
                if (owner != null) e.hurt(owner.damageSources().playerAttack(owner), dmg);
                else e.hurt(sl.damageSources().magic(), dmg);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
            }
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
            sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.2, pos.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 35, r * 0.25, 0.3, r * 0.25, 0.02);
            snowball.discard();
            event.setCanceled(true);
            return;
        }
        if (pd.getBoolean("lvluping_hunter_net_projectile")) {
            double r = pd.getFloat("lvluping_hunter_net_radius");
            int root = pd.getInt("lvluping_hunter_net_root");
            Vec3 pos = hit.getLocation();
            AABB area = new AABB(pos.x - r, pos.y - 2.0, pos.z - r, pos.x + r, pos.y + 2.0, pos.z + r);
            Player owner = snowball.getOwner() instanceof Player p ? p : null;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                if (owner != null && e.isAlliedTo(owner)) continue;
                if (e.distanceToSqr(pos) > r * r) continue;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 8, false, false));
            }
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 0.8f, 0.9f);
            sl.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.5, pos.z, 24, r * 0.3, 0.2, r * 0.3, 0.04);
            snowball.discard();
            event.setCanceled(true);
            return;
        }
        if (!pd.getBoolean("lvluping_ice_projectile")) return;

        float dmg = pd.getFloat("lvluping_ice_damage");
        int slowTicks = pd.getInt("lvluping_ice_slow_ticks");
        int slowAmp = pd.getInt("lvluping_ice_slow_amp");

        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            if (snowball.getOwner() instanceof Player owner) {
                living.hurt(owner.damageSources().playerAttack(owner), dmg);
            } else {
                living.hurt(sl.damageSources().magic(), dmg);
            }
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmp, false, false));
            sl.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, ICE_PROJECTILE_HIT_SOUND_VOLUME, ICE_PROJECTILE_HIT_SOUND_PITCH);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, living.getX(), living.getY() + 1.0, living.getZ(), ICE_PROJECTILE_HIT_ENTITY_PARTICLE_COUNT, ICE_PROJECTILE_HIT_SPREAD_XZ, ICE_PROJECTILE_HIT_SPREAD_Y, ICE_PROJECTILE_HIT_SPREAD_XZ, 0.04);
        } else {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, snowball.getX(), snowball.getY(), snowball.getZ(), ICE_PROJECTILE_HIT_GROUND_PARTICLE_COUNT, ICE_PROJECTILE_HIT_GROUND_SPREAD, ICE_PROJECTILE_HIT_GROUND_SPREAD, ICE_PROJECTILE_HIT_GROUND_SPREAD, 0.03);
        }

        snowball.discard();
        event.setCanceled(true);
    }

    private static void reflectProjectile(Projectile projectile, ServerPlayer reflector) {
        projectile.setOwner(reflector);
        Vec3 v = projectile.getDeltaMovement();
        projectile.setDeltaMovement(-v.x, -v.y, -v.z);

        Vec3 away = projectile.position().subtract(reflector.position());
        if (away.lengthSqr() < REFLECT_PROJECTILE_MIN_AWAY_LEN_SQR) {
            away = v;
        }
        away = away.normalize();
        projectile.setPos(reflector.getX() + away.x * REFLECT_PROJECTILE_OFFSET, reflector.getY() + away.y * REFLECT_PROJECTILE_OFFSET, reflector.getZ() + away.z * REFLECT_PROJECTILE_OFFSET);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (isSummonerOrOwnedSummonKiller(event.getSource().getEntity(), sl)) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (isSummonerOrOwnedSummonKiller(event.getAttackingPlayer(), sl)) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity victim
                && event.getEntity().level() instanceof ServerLevel sl
                && event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow
                && arrow.getOwner() instanceof ServerPlayer owner) {
            long now = sl.getGameTime();
            Set<String> ot = PlayerLevels.getPlayerTalents(owner.getUUID());
            var ad = arrow.getPersistentData();
            int eff = ad.getInt(TalentAbilityHandler.A_NEXT_ARROW_EFFECT_KEY);
            if (eff != 0) {
                float amount = event.getAmount();
                if (eff == 1) {
                    float dmg = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    int root = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                    if (root > 0) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 6, false, false));
                    amount += Math.max(0f, dmg);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.PLAYERS, 0.9f, 1.2f);
                    sl.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.7, victim.getZ(), 18, 0.4, 0.3, 0.4, 0.02);
                } else if (eff == 3) {
                    int ticks = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    int amp = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                    if (ticks > 0) victim.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, Math.max(0, amp), false, false));
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 0.6f, 1.6f);
                    sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + 0.8, victim.getZ(), 14, 0.35, 0.35, 0.35, 0.02);
                } else if (eff == 4) {
                    float bonus = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    amount += Math.max(0f, bonus);
                } else if (eff == 5) {
                    double r = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    int root = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                    AABB area = victim.getBoundingBox().inflate(r, 2.0, r);
                    for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                        if (e.isAlliedTo(owner)) continue;
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 8, false, false));
                    }
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 0.8f, 0.9f);
                    sl.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.7, victim.getZ(), 30, r * 0.3, 0.2, r * 0.3, 0.05);
                } else if (eff == 11) {
                    int fire = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    victim.setRemainingFireTicks(Math.max(victim.getRemainingFireTicks(), fire));
                    sl.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 0.7, victim.getZ(), 18, 0.35, 0.25, 0.35, 0.02);
                } else if (eff == 12) {
                    float mult = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    if (mult > 0.01f) amount *= mult;
                    sl.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 0.8, victim.getZ(), 22, 0.35, 0.35, 0.35, 0.12);
                } else if (eff == 20) {
                    float mult = ad.getFloat(TalentAbilityHandler.A_HUNTER_ULT_SHOT_MULT_KEY);
                    float boss = ad.getFloat(TalentAbilityHandler.A_HUNTER_ULT_SHOT_BOSS_MULT_KEY);
                    float m = mult <= 0f ? 1f : mult;
                    if (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                        if (boss > 0f) m *= boss;
                    }
                    amount *= m;
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.4f);
                    sl.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.8, victim.getZ(), 1, 0, 0, 0, 0);
                } else if (eff == 30) {
                    int root = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    int weak = (int) ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                    if (root > 0) victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 7, false, false));
                    if (weak > 0) victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weak, 1, false, false));
                    sl.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.7, victim.getZ(), 25, 0.45, 0.25, 0.45, 0.05);
                } else if (eff == 31) {
                    float th = ad.getFloat(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                    if (th > 0f && victim.getHealth() / Math.max(0.01f, victim.getMaxHealth()) <= th) {
                        amount = 999999f;
                        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.7f, 1.6f);
                        sl.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0, victim.getZ(), 35, 0.4, 0.6, 0.4, 0.05);
                    }
                }
                event.setAmount(amount);
            }
            if (ad.getLong(TalentAbilityHandler.A_NEXT_ARROW_UNTIL_KEY) > 0) {
                ad.remove(TalentAbilityHandler.A_NEXT_ARROW_EFFECT_KEY);
                ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P1_KEY);
                ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P2_KEY);
                ad.remove(TalentAbilityHandler.A_NEXT_ARROW_P3_KEY);
                ad.remove(TalentAbilityHandler.A_NEXT_ARROW_UNTIL_KEY);
            }

            if (ot.contains("a_hunter_anatomy")) {
                int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_hunter_anatomy", ot);
                float mult = (float) AbilityUpgradeConfig.getDouble("a_hunter_anatomy", "ranged_damage_mult", lvl, 1.1);
                if (mult > 0.01f) event.setAmount(event.getAmount() * mult);
            }
            if (ot.contains("a_hunter_base")) {
                int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_hunter_base", ot);
                float mult = (float) AbilityUpgradeConfig.getDouble("a_hunter_base", "ranged_damage_mult", lvl, 1.1);
                if (mult > 0.01f) event.setAmount(event.getAmount() * mult);
            }
            if (ot.contains("a_ranger_bow_mastery")) {
                int bl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_ranger_bow_mastery", ot);
                double hm = AbilityUpgradeConfig.getDouble("a_ranger_bow_mastery", "headshot_damage_mult", bl, 1.25);
                if (hm > 1.0 && isRangerArrowHeadshot(arrow, victim)) {
                    event.setAmount(event.getAmount() * (float) hm);
                }
            }

            if (ot.contains("a_musketeer_fast_hand")) {
                int fl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_musketeer_fast_hand", ot);
                float mult = (float) AbilityUpgradeConfig.getDouble("a_musketeer_fast_hand", "ranged_damage_mult", fl, 1.05);
                if (mult > 0.01f) event.setAmount(event.getAmount() * mult);
            }
            if (ot.contains("a_musketeer_trained_eye") && livingHasHarmfulDebuff(victim)) {
                int tel = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_musketeer_trained_eye", ot);
                float mult = (float) AbilityUpgradeConfig.getDouble("a_musketeer_trained_eye", "damage_mult_vs_cc", tel, 1.15);
                if (mult > 1.0f) event.setAmount(event.getAmount() * mult);
            }

            if (ot.contains("a_ult_musketeer_execution")) {
                int xl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_ult_musketeer_execution", ot);
                float th = (float) AbilityUpgradeConfig.getDouble("a_ult_musketeer_execution", "hp_threshold", xl, 0.25);
                if (th > 0f && victim.getHealth() / Math.max(0.01f, victim.getMaxHealth()) <= th) {
                    event.setAmount(999999f);
                    sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.7f, 1.6f);
                    sl.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1.0, victim.getZ(), 18, 0.35, 0.55, 0.35, 0.04);
                }
            }

            if (ot.contains("a_musketeer_piercing_buckshot")) {
                int bl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_musketeer_piercing_buckshot", ot);
                int maxExtra = AbilityUpgradeConfig.getInt("a_musketeer_piercing_buckshot", "pierce_count", bl, 3);
                float mult = (float) AbilityUpgradeConfig.getDouble("a_musketeer_piercing_buckshot", "pierce_damage_mult", bl, 0.85);
                double range = AbilityUpgradeConfig.getDouble("a_musketeer_piercing_buckshot", "range", bl, 18.0);
                Vec3 rawDir = arrow.getDeltaMovement();
                if (rawDir.lengthSqr() > 1.0e-6) {
                    final Vec3 dir = rawDir.normalize();
                    Vec3 start = victim.position().add(0, victim.getBbHeight() * 0.55, 0);
                    Vec3 end = start.add(dir.scale(range));
                    AABB box = new AABB(
                            Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
                            Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)
                    ).inflate(1.25, 1.0, 1.25);

                    var candidates = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != victim && e != owner && !e.isAlliedTo(owner));
                    candidates.sort(java.util.Comparator.comparingDouble(e -> e.position().subtract(start).dot(dir)));

                    int done = 0;
                    float base = event.getAmount();
                    for (LivingEntity e : candidates) {
                        if (done >= maxExtra) break;
                        Vec3 rel = e.position().add(0, e.getBbHeight() * 0.55, 0).subtract(start);
                        double t = rel.dot(dir);
                        if (t <= 0.75 || t > range) continue;
                        Vec3 closest = start.add(dir.scale(t));
                        if (e.distanceToSqr(closest.x, closest.y, closest.z) > 1.6) continue;
                        e.hurt(owner.damageSources().playerAttack(owner), base * mult);
                        done++;
                    }
                }
            }
            if (ot.contains("a_hunter_poison_arrow")) {
                int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_hunter_poison_arrow", ot);
                long cdUntil = owner.getPersistentData().getLong("lvluping_hunter_poison_cd_until");
                if (cdUntil <= now) {
                    int ticks = AbilityUpgradeConfig.getInt("a_hunter_poison_arrow", "poison_ticks", lvl, 100);
                    int amp = AbilityUpgradeConfig.getInt("a_hunter_poison_arrow", "poison_amp", lvl, 0);
                    int cd = AbilityUpgradeConfig.getInt("a_hunter_poison_arrow", "cooldown", lvl, 160);
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, amp, false, false));
                    owner.getPersistentData().putLong("lvluping_hunter_poison_cd_until", now + cd);
                    PlayerLevels.setCooldown(owner.getUUID(), "cd_a_hunter_poison_arrow", cd);
                    owner.getPersistentData().putInt("cd_a_hunter_poison_arrow", cd);
                    PacketDistributor.sendToPlayer(owner, new S2CSyncCooldown("cd_a_hunter_poison_arrow", cd));
                }
            }
            if (ot.contains("a_ult_hunter_sniper")) {
                int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_ult_hunter_sniper", ot);
                long cdUntil = owner.getPersistentData().getLong("lvluping_hunter_sniper_cd_until");
                boolean ready = owner.getPersistentData().getBoolean("lvluping_hunter_sniper_ready");
                if (ready && cdUntil <= now) {
                    float mult = (float) AbilityUpgradeConfig.getDouble("a_ult_hunter_sniper", "damage_mult", lvl, 4.0);
                    int cd = AbilityUpgradeConfig.getInt("a_ult_hunter_sniper", "cooldown", lvl, 900);
                    event.setAmount(event.getAmount() * mult);
                    owner.getPersistentData().putBoolean("lvluping_hunter_sniper_ready", false);
                    owner.getPersistentData().putLong("lvluping_hunter_sniper_cd_until", now + cd);
                    PlayerLevels.setCooldown(owner.getUUID(), "cd_a_ult_hunter_sniper", cd);
                    owner.getPersistentData().putInt("cd_a_ult_hunter_sniper", cd);
                    PacketDistributor.sendToPlayer(owner, new S2CSyncCooldown("cd_a_ult_hunter_sniper", cd));
                }
            }
            if (ot.contains("a_ult_hunter_ult_shot")) {
                int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "a_ult_hunter_ult_shot", ot);
                long cdUntil = owner.getPersistentData().getLong("lvluping_hunter_ult_shot_cd_until");
                if (cdUntil <= now) {
                    float mult = (float) AbilityUpgradeConfig.getDouble("a_ult_hunter_ult_shot", "damage_mult", lvl, 3.0);
                    float boss = (float) AbilityUpgradeConfig.getDouble("a_ult_hunter_ult_shot", "boss_mult", lvl, 2.0);
                    int cd = AbilityUpgradeConfig.getInt("a_ult_hunter_ult_shot", "cooldown", lvl, 900);
                    float m = mult;
                    if (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss || victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                        m *= boss;
                    }
                    event.setAmount(event.getAmount() * m);
                    owner.getPersistentData().putLong("lvluping_hunter_ult_shot_cd_until", now + cd);
                    PlayerLevels.setCooldown(owner.getUUID(), "cd_a_ult_hunter_ult_shot", cd);
                    owner.getPersistentData().putInt("cd_a_ult_hunter_ult_shot", cd);
                    PacketDistributor.sendToPlayer(owner, new S2CSyncCooldown("cd_a_ult_hunter_ult_shot", cd));
                }
            }
            long trackUntil = owner.getPersistentData().getLong(TalentAbilityHandler.A_HUNTER_TRACK_UNTIL_KEY);
            if (trackUntil > now && owner.getPersistentData().hasUUID(TalentAbilityHandler.A_HUNTER_TRACK_TARGET_KEY)
                    && owner.getPersistentData().getUUID(TalentAbilityHandler.A_HUNTER_TRACK_TARGET_KEY).equals(victim.getUUID())) {
                float mult = owner.getPersistentData().getFloat("lvluping_a_hunter_track_mult");
                if (mult > 0.01f) event.setAmount(event.getAmount() * mult);
                sl.sendParticles(ParticleTypes.GLOW, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.2, 0.4, 0.2, 0.02);
            }
        }

        if (event.getSource().getDirectEntity() instanceof ServerPlayer atk) {
            Set<String> at = PlayerLevels.getPlayerTalents(atk.getUUID());
            if (at.contains("w_paladin_smite")) {
                int lvl = PlayerLevels.getAbilityLevel(atk.getUUID(), "w_paladin_smite", at);
                if (atk.getMainHandItem().is(ItemTags.SWORDS)) {
                    float bonus = (float) AbilityUpgradeConfig.getDouble("w_paladin_smite", "bonus_damage", lvl, 1.0);
                    event.setAmount(event.getAmount() + bonus);
                    if (event.getEntity() instanceof LivingEntity le && le.getType().is(EntityTypeTags.UNDEAD)) {
                        int ticks = AbilityUpgradeConfig.getInt("w_paladin_smite", "undead_fire_ticks", lvl, 40);
                        le.setRemainingFireTicks(Math.max(le.getRemainingFireTicks(), ticks));
                    }
                }
            }
        }
        if (event.getEntity() instanceof ServerPlayer spLf && event.getEntity().level() instanceof ServerLevel slLf) {
            long lfUntil = spLf.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY);
            if (lfUntil > slLf.getGameTime()) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            sp.getPersistentData().putLong("lvluping_last_taken_damage_at", sl.getGameTime());
            Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());
            if (talents.contains("a_hunter_escape") && event.getAmount() > 0f && !event.isCanceled()) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "a_hunter_escape", talents);
                if (PlayerLevels.getCooldown(sp.getUUID(), "cd_a_hunter_escape") <= 0) {
                    int speedTicks = AbilityUpgradeConfig.getInt("a_hunter_escape", "speed_ticks", lvl, 40);
                    int speedAmp = AbilityUpgradeConfig.getInt("a_hunter_escape", "speed_amp", lvl, 1);
                    int icd = AbilityUpgradeConfig.getInt("a_hunter_escape", "internal_cd", lvl, 200);
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, speedTicks, speedAmp, false, false));
                    TalentAbilityHandler.setPassiveInternalCooldown(sp, "cd_a_hunter_escape", icd);
                }
            }
            if (talents.contains("as_assassin_adrenaline")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "as_assassin_adrenaline", talents);
                if (PlayerLevels.getCooldown(sp.getUUID(), "cd_as_assassin_adrenaline") <= 0) {
                    float hpAfter = sp.getHealth() - event.getAmount();
                    if (sp.getMaxHealth() > 0f && hpAfter / sp.getMaxHealth() <= 0.5f) {
                        int ticks = AbilityUpgradeConfig.getInt("as_assassin_adrenaline", "speed_ticks", lvl, 60);
                        int amp = AbilityUpgradeConfig.getInt("as_assassin_adrenaline", "speed_amp", lvl, 1);
                        int icd = AbilityUpgradeConfig.getInt("as_assassin_adrenaline", "internal_cd", lvl, 1200);
                        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, amp, false, false));
                        TalentAbilityHandler.setPassiveInternalCooldown(sp, "cd_as_assassin_adrenaline", icd);
                    }
                }
            }
        }
        if (event.getEntity().level() instanceof ServerLevel slInv) {
            long invUntil = event.getEntity().getPersistentData().getLong(TalentAbilityHandler.CLERIC_INVULN_UNTIL_KEY);
            if (invUntil > slInv.getGameTime()) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof ServerPlayer martyrPlayer && event.getEntity().level() instanceof ServerLevel slM) {
            long martyrUntil = martyrPlayer.getPersistentData().getLong("lvluping_cleric_martyr_until");
            if (martyrUntil > slM.getGameTime()) {
                float hp = martyrPlayer.getHealth();
                if (event.getAmount() >= hp && hp > 0.5f) {
                    event.setAmount(Math.max(0f, hp - 0.5f));
                }
            }
        }
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.LightningBolt) {
                long immuneUntil = event.getEntity().getPersistentData().getLong("lvluping_fc_lightning_immune");
                if (immuneUntil > serverLevel.getGameTime()) {
                    event.setCanceled(true);
                    event.getEntity().getPersistentData().remove("lvluping_fc_lightning_immune");
                    return;
                }
            }
        }

        // --- M_ULT_DIVINE_PROTECTION ---
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            long shieldUntil = event.getEntity().getPersistentData().getLong("lvluping_cleric_divine_protection_until");
            float shieldPct = event.getEntity().getPersistentData().getFloat("lvluping_cleric_divine_shield_pct");
            if (shieldUntil > serverLevel.getGameTime() && shieldPct > 0f) {
                event.setAmount(event.getAmount() * (1.0f - shieldPct));
            }
        }
        if (event.getEntity() instanceof ServerPlayer sp) {
            Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());
            float amount = event.getAmount();
            if (talents.contains("w_barbarian_thick_skin")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "w_barbarian_thick_skin", talents);
                double red = AbilityUpgradeConfig.getDouble("w_barbarian_thick_skin", "damage_reduction", lvl, 0.05);
                amount *= (float) (1.0 - red);
            }
            if (talents.contains("w_barbarian_indestructible_body")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "w_barbarian_indestructible_body", talents);
                double red = AbilityUpgradeConfig.getDouble("w_barbarian_indestructible_body", "damage_reduction", lvl, 0.08);
                amount *= (float) (1.0 - red);
            }
            long frenzyUntil = sp.getPersistentData().getLong(TalentAbilityHandler.W_BARBARIAN_FRENZY_UNTIL_KEY);
            if (sp.level() instanceof ServerLevel sl && frenzyUntil > sl.getGameTime()) {
                float inMult = sp.getPersistentData().getFloat(TalentAbilityHandler.W_BARBARIAN_FRENZY_INCOMING_MULT_KEY);
                if (inMult <= 0f) inMult = 1f;
                amount *= inMult;
            }
            if (sp.level() instanceof ServerLevel sl) {
                long steelUntil = sp.getPersistentData().getLong(TalentAbilityHandler.W_SWORDMASTER_STEEL_BODY_UNTIL_KEY);
                if (steelUntil > sl.getGameTime()) {
                    float mult = sp.getPersistentData().getFloat(TalentAbilityHandler.W_SWORDMASTER_STEEL_BODY_INCOMING_MULT_KEY);
                    if (mult > 0f) amount *= mult;
                }
            }
            event.setAmount(amount);
        }

        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            ServerPlayer provoker = ProvocationHandler.getActiveProvoker(serverLevel);
            var attacker = event.getSource().getEntity();
            if (provoker != null && attacker != null && attacker != provoker && event.getEntity() != provoker) {
                event.setAmount(event.getAmount() * DAMAGE_REDUCTION_MULTIPLIER_HALF);
            }
        }

        if (event.getEntity() instanceof LivingEntity victim && event.getEntity().level() instanceof ServerLevel slAura) {
            for (ServerPlayer pal : slAura.players()) {
                Set<String> pt = PlayerLevels.getPlayerTalents(pal.getUUID());
                if (!pt.contains("w_paladin_aura")) continue;
                int alvl = PlayerLevels.getAbilityLevel(pal.getUUID(), "w_paladin_aura", pt);
                double pr = AbilityUpgradeConfig.getDouble("w_paladin_aura", "radius", alvl, 5.0);
                double red = AbilityUpgradeConfig.getDouble("w_paladin_aura", "damage_reduction", alvl, 0.05);
                if (pal.distanceToSqr(victim) > pr * pr) continue;
                boolean allied = victim.isAlliedTo(pal);
                if (!allied && victim instanceof net.minecraft.world.entity.Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner")
                        && mob.getPersistentData().getUUID("lvluping_summon_owner").equals(pal.getUUID())) {
                    allied = true;
                }
                if (!allied) continue;
                event.setAmount((float) (event.getAmount() * (1.0 - red)));
                break;
            }
        }

        if (event.getEntity() instanceof ServerPlayer spv && spv.isBlocking()) {
            Set<String> vt = PlayerLevels.getPlayerTalents(spv.getUUID());
            if (vt.contains("w_paladin_shield_faith") && event.getSource().getDirectEntity() instanceof LivingEntity le) {
                int slvl = PlayerLevels.getAbilityLevel(spv.getUUID(), "w_paladin_shield_faith", vt);
                double rr = AbilityUpgradeConfig.getDouble("w_paladin_shield_faith", "reflect_ratio", slvl, 0.25);
                float ref = event.getAmount() * (float) rr;
                if (ref > 0.01f) {
                    le.hurt(spv.damageSources().thorns(spv), ref);
                }
            }
        }

        if (event.getEntity() instanceof Player victim) {
            Set<String> talents = PlayerLevels.getPlayerTalents(victim.getUUID());

            if (talents.contains("w_unbreakable") && PlayerLevels.getCooldown(victim.getUUID(), "cd_w_unbreakable") <= 0) {
                if (victim.getHealth() - event.getAmount() <= 0f) {
                    event.setCanceled(true);
                    int ulvl = PlayerLevels.getAbilityLevel(victim.getUUID(), "w_unbreakable", talents);
                    int regenTicks = AbilityUpgradeConfig.getInt("w_unbreakable", "regen_ticks", ulvl, W_UNBREAKABLE_REGENERATION_DURATION_TICKS);
                    int regenAmp = AbilityUpgradeConfig.getInt("w_unbreakable", "regen_amp", ulvl, W_UNBREAKABLE_REGENERATION_AMPLIFIER);
                    int absorbTicks = AbilityUpgradeConfig.getInt("w_unbreakable", "duration_ticks", ulvl, W_UNBREAKABLE_REGENERATION_DURATION_TICKS);
                    double absorbRatio = AbilityUpgradeConfig.getDouble("w_unbreakable", "absorption_max_hp_ratio", ulvl, 0.2);
                    float beforeAbs = victim.getAbsorptionAmount();
                    float wantAbs = (float) (victim.getMaxHealth() * absorbRatio);
                    float afterAbs = Math.max(beforeAbs, wantAbs);
                    float ourAbs = Math.max(0f, afterAbs - beforeAbs);
                    victim.setHealth(UNBREAKABLE_RESCUE_HEALTH);
                    victim.setAbsorptionAmount(afterAbs);
                    victim.getPersistentData().putFloat(W_UNBREAKABLE_ABSORB_BEFORE_KEY, beforeAbs);
                    victim.getPersistentData().putFloat(W_UNBREAKABLE_ABSORB_OUR_KEY, ourAbs);
                    victim.getPersistentData().putLong(W_UNBREAKABLE_ABSORB_UNTIL_KEY, victim.level().getGameTime() + absorbTicks);
                    victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, regenAmp, false, false));
                    PlayerLevels.setCooldown(victim.getUUID(), "cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN);
                    victim.getPersistentData().putInt("cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN);
                    if (victim instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN));
                    }
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f);
                    if (victim.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1, victim.getZ(), UNBREAKABLE_TOTEM_PARTICLE_COUNT, UNBREAKABLE_TOTEM_PARTICLE_SPREAD_XZ, UNBREAKABLE_TOTEM_PARTICLE_SPREAD_Y, UNBREAKABLE_TOTEM_PARTICLE_SPREAD_XZ, 0.2);
                        long orbitUntil = sl.getGameTime() + regenTicks;
                        for (ServerPlayer p : sl.players()) {
                            PacketDistributor.sendToPlayer(p, new S2CUnbreakableShieldOrbit(victim.getUUID(), orbitUntil));
                        }
                    }
                    return;
                }
            }

            if (UltimatesHandler.isBerserkActive(victim)) {
            } else {
            // --- W_PARRY ---
            if (victim.getPersistentData().getInt("lvluping_parry_window") > 0) {
                event.setCanceled(true);
                victim.getPersistentData().putInt("lvluping_parry_window", 0);

                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, W_PARRY_BLOCK_SOUND_PITCH);

                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), W_PARRY_CRIT_PARTICLE_COUNT, W_PARRY_CRIT_SPREAD, W_PARRY_CRIT_SPREAD, W_PARRY_CRIT_SPREAD, W_PARRY_CRIT_SPEED);
                }

                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, W_PARRY_WEAKNESS_DURATION_TICKS, W_PARRY_WEAKNESS_AMPLIFIER, false, false));
                }
            }

            // --- W_BARRIER ---
            if (victim.getPersistentData().getInt("lvluping_barrier_window") > 0) {
                event.setCanceled(true);
                victim.getPersistentData().putInt("lvluping_barrier_window", 0);

                victim.level().playSound(null, victim.getX(), victim.getY() + 1, victim.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, W_BARRIER_GLASS_PITCH);
                victim.level().playSound(null, victim.getX(), victim.getY() + 1, victim.getZ(),
                        SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, W_BARRIER_SHIELD_VOLUME, W_BARRIER_SHIELD_VOLUME);

                if (victim.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 1, victim.getZ(), W_BARRIER_POOF_COUNT, W_BARRIER_POOF_SPREAD, W_BARRIER_POOF_SPREAD, W_BARRIER_POOF_SPREAD, 0.1);
                }
            }

            }
        }

        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel sl) {
            Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());

            if (talents.contains("m_soft_landing") && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
                event.setCanceled(true);
                return;
            }

            if (talents.contains("m_magic_barrier") && talents.contains("m_spellcaster_base")) {
                int cd = sp.getPersistentData().getInt("cd_m_magic_barrier");
                if (cd <= 0 && event.getAmount() > 0.0f) {
                    event.setCanceled(true);
                    int bLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_magic_barrier", talents);
                    int cdt = AbilityUpgradeConfig.getInt("m_magic_barrier", "cooldown", bLvl, 600);
                    float extraHp = (float) AbilityUpgradeConfig.getDouble("m_magic_barrier", "extra_hp", bLvl, 2.0);
                    sp.setAbsorptionAmount(Math.max(sp.getAbsorptionAmount(), extraHp));
                    PlayerLevels.setCooldown(sp.getUUID(), "cd_m_magic_barrier", cdt);
                    sp.getPersistentData().putInt("cd_m_magic_barrier", cdt);
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_m_magic_barrier", cdt));
                    sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, M_MAGIC_BARRIER_SOUND_PITCH);
                    sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0, sp.getZ(), M_MAGIC_BARRIER_PARTICLE_COUNT, M_MAGIC_BARRIER_PARTICLE_SPREAD_XZ, M_MAGIC_BARRIER_PARTICLE_SPREAD_Y, M_MAGIC_BARRIER_PARTICLE_SPREAD_XZ, 0.02);
                    return;
                }
            }

            if (talents.contains("m_stone_skin") && talents.contains("m_spellcaster_base")) {
                int cd = sp.getPersistentData().getInt("cd_m_stone_skin");
                if (cd <= 0 && event.getAmount() > 0.0f) {
                    int skLvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "m_stone_skin", talents);
                    int dur = AbilityUpgradeConfig.getInt("m_stone_skin", "duration_ticks", skLvl, 60);
                    int cdt = AbilityUpgradeConfig.getInt("m_stone_skin", "cooldown", skLvl, 200);
                    int resAmp = AbilityUpgradeConfig.getInt("m_stone_skin", "resistance_amp", skLvl, 1);
                    int slowAmp = AbilityUpgradeConfig.getInt("m_stone_skin", "slow_amp", skLvl, 1);
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, resAmp, false, false));
                    if (slowAmp >= 0) {
                        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, slowAmp, false, false));
                    }
                    PlayerLevels.setCooldown(sp.getUUID(), "cd_m_stone_skin", cdt);
                    sp.getPersistentData().putInt("cd_m_stone_skin", cdt);
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_m_stone_skin", cdt));
                    sl.sendParticles(ParticleTypes.ASH, sp.getX(), sp.getY() + 0.8, sp.getZ(), M_STONE_SKIN_PARTICLE_COUNT, M_STONE_SKIN_PARTICLE_SPREAD_XZ, M_STONE_SKIN_PARTICLE_SPREAD_Y, M_STONE_SKIN_PARTICLE_SPREAD_XZ, 0.02);
                }
            }

            long iceUntil = sp.getPersistentData().getLong("lvluping_m_ice_block_until");
            if (iceUntil > sl.getGameTime()) {
                event.setCanceled(true);
                return;
            }

            long antiUntil = sp.getPersistentData().getLong("lvluping_m_anti_magic_until");
            if (antiUntil > sl.getGameTime()) {
                if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer rsp && !rsp.level().isClientSide()) {
            Set<String> rt = PlayerLevels.getPlayerTalents(rsp.getUUID());
            if (event.getSource().is(DamageTypes.FALL) && rt.contains("a_ranger_agility")) {
                int alvl = PlayerLevels.getAbilityLevel(rsp.getUUID(), "a_ranger_agility", rt);
                double fmult = AbilityUpgradeConfig.getDouble("a_ranger_agility", "fall_damage_mult", alvl, 0.0);
                event.setNewDamage((float) (event.getNewDamage() * fmult));
            }
            if (rt.contains("a_ranger_evasion")) {
                int evl = PlayerLevels.getAbilityLevel(rsp.getUUID(), "a_ranger_evasion", rt);
                double ch = AbilityUpgradeConfig.getDouble("a_ranger_evasion", "proc_chance", evl, 0.25);
                int max = AbilityUpgradeConfig.getInt("a_ranger_evasion", "max_stacks", evl, 2);
                if (rsp.getRandom().nextDouble() < ch) {
                    int s = rsp.getPersistentData().getInt("lvluping_ranger_evasion_stacks");
                    if (s < max) {
                        rsp.getPersistentData().putInt("lvluping_ranger_evasion_stacks", s + 1);
                        PacketDistributor.sendToPlayer(rsp, new S2CSyncCooldown("lvluping_ranger_evasion_stacks", s + 1));
                    }
                }
            }
            if (rt.contains("a_ranger_quick_step") && rsp.getPersistentData().getBoolean("lvluping_ranger_quick_step_speed")) {
                rsp.removeEffect(MobEffects.MOVEMENT_SPEED);
                rsp.getPersistentData().putBoolean("lvluping_ranger_quick_step_speed", false);
                rsp.getPersistentData().putFloat("lvluping_ranger_quick_step_mult", 0f);
            }
        }
        if (event.getEntity().level() instanceof ServerLevel slOut) {
            long gt = slOut.getGameTime();
            var srcEnt = event.getSource().getEntity();
            if (srcEnt instanceof ServerPlayer atk) {
                if (atk.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > gt) {
                    event.setNewDamage(0f);
                    return;
                }
            }
            var direct = event.getSource().getDirectEntity();
            if (direct instanceof net.minecraft.world.entity.projectile.Projectile proj && proj.getOwner() instanceof ServerPlayer atk2) {
                if (atk2.getPersistentData().getLong(TalentAbilityHandler.W_LIGHT_FORM_UNTIL_KEY) > gt) {
                    event.setNewDamage(0f);
                    return;
                }
            }
        }
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob summonAttacker
                && summonAttacker.getPersistentData().hasUUID("lvluping_summon_owner")) {
            double mult = summonAttacker.getPersistentData().getDouble("lvluping_summon_damage_mult");
            if (mult > 0.0 && Math.abs(mult - 1.0) > 1.0e-6) {
                event.setNewDamage((float) (event.getNewDamage() * mult));
            }
        }
        if (event.getSource().getEntity() instanceof Player attacker) {
            Set<String> talents = PlayerLevels.getPlayerTalents(attacker.getUUID());
            LivingEntity target = event.getEntity();
            long currentTime = attacker.level().getGameTime();

            if (talents.contains("a_dagger") && TalentAbilityHandler.isDagger(attacker.getMainHandItem().getItem())) {
                event.setNewDamage(event.getNewDamage() * 1.2f);
            }

            long blessUntil = attacker.getPersistentData().getLong("lvluping_blessing_damage_until");
            if (blessUntil > currentTime) {
                float bMult = attacker.getPersistentData().getFloat("lvluping_blessing_damage_mult");
                if (bMult > 1e-3f) {
                    event.setNewDamage(event.getNewDamage() * bMult);
                }
            }

            // --- W_ULT_BERSERK ---
            if (UltimatesHandler.isBerserkActive(attacker)) {
                float maxH = attacker.getMaxHealth();
                float curH = attacker.getHealth();
                float missingRatio = maxH > 0 ? 1f - (curH / maxH) : 0f;
                float multiplier = 1f + missingRatio * W_ULT_BERSERK_MISSING_HP_DAMAGE_SCALE;
                event.setNewDamage(event.getNewDamage() * multiplier);
                attacker.heal(maxH * W_ULT_BERSERK_HEAL_MAX_HP_RATIO);
                if (attacker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), W_ULT_BERSERK_HIT_PARTICLE_COUNT, 0.2, 0.3, 0.2, 0.02);
                }
            }

            // --- AS_CRIT ---
            if (talents.contains("as_crit")) {
                Vec3 lookA = attacker.getLookAngle().normalize();
                Vec3 lookT = target.getLookAngle().normalize();

                if (lookA.dot(lookT) > AS_CRIT_BACKSTAB_DOT_THRESHOLD) {
                    event.setNewDamage(event.getOriginalDamage() * AS_CRIT_DAMAGE_MULTIPLIER);
                    attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, AS_CRIT_SOUND_PITCH);
                }
            }
            if (attacker.getPersistentData().getLong(TalentAbilityHandler.AS_MARK_UNTIL_KEY) > currentTime
                    && attacker.getPersistentData().hasUUID(TalentAbilityHandler.AS_MARK_TARGET_KEY)
                    && attacker.getPersistentData().getUUID(TalentAbilityHandler.AS_MARK_TARGET_KEY).equals(target.getUUID())) {
                Set<String> at = PlayerLevels.getPlayerTalents(attacker.getUUID());
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_mark", at);
                float mult = (float) AbilityUpgradeConfig.getDouble("as_assassin_mark", "damage_mult", lvl, 1.25);
                event.setNewDamage(event.getNewDamage() * mult);
            }
            if (talents.contains("as_assassin_base") && target.getPersistentData().getLong("lvluping_as_bleed_until") > currentTime) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_base", talents);
                float m = (float) AbilityUpgradeConfig.getDouble("as_assassin_base", "bleed_target_damage_mult", lvl, 1.15);
                event.setNewDamage(event.getNewDamage() * m);
            }
            if (talents.contains("as_assassin_bloodletter") && target.getPersistentData().getLong("lvluping_as_bleed_until") > currentTime) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_bloodletter", talents);
                float m = (float) AbilityUpgradeConfig.getDouble("as_assassin_bloodletter", "damage_mult_vs_bleed", lvl, 1.2);
                event.setNewDamage(event.getNewDamage() * m);
            }
            if (talents.contains("as_assassin_dark_style")) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_dark_style", talents);
                float normal = (float) AbilityUpgradeConfig.getDouble("as_assassin_dark_style", "normal_mult", lvl, 0.7);
                float backstab = (float) AbilityUpgradeConfig.getDouble("as_assassin_dark_style", "backstab_mult", lvl, 1.25);
                float stealth = (float) AbilityUpgradeConfig.getDouble("as_assassin_dark_style", "stealth_crit_mult", lvl, 1.2);
                event.setNewDamage(event.getNewDamage() * normal);
                Vec3 toAttacker = attacker.position().subtract(target.position());
                if (toAttacker.lengthSqr() > 0.0001) {
                    Vec3 lookT = target.getLookAngle().normalize();
                    Vec3 dir = toAttacker.normalize();
                    if (lookT.dot(dir) < -0.3) {
                        event.setNewDamage(event.getNewDamage() * backstab);
                    }
                }
                if (attacker.hasEffect(MobEffects.INVISIBILITY)) {
                    event.setNewDamage(event.getNewDamage() * stealth);
                }
            }
            if (talents.contains("as_wanderer_knife_edge") && attacker.hasEffect(MobEffects.INVISIBILITY)) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_wanderer_knife_edge", talents);
                float m = (float) AbilityUpgradeConfig.getDouble("as_wanderer_knife_edge", "stealth_crit_mult", lvl, 1.2);
                event.setNewDamage(event.getNewDamage() * m);
            }
            if (talents.contains("as_assassin_throat")) {
                Vec3 lookA = attacker.getLookAngle().normalize();
                Vec3 lookT = target.getLookAngle().normalize();
                if (lookA.dot(lookT) > 0.3) {
                    int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_throat", talents);
                    int bleedTicks = AbilityUpgradeConfig.getInt("as_assassin_throat", "bleed_ticks", lvl, 80);
                    int bonus = 0;
                    if (talents.contains("as_assassin_sharp_blades")) {
                        int bl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "as_assassin_sharp_blades", talents);
                        bonus = AbilityUpgradeConfig.getInt("as_assassin_sharp_blades", "bleed_bonus_ticks", bl, 20);
                    }
                    float dps = (float) AbilityUpgradeConfig.getDouble("as_assassin_throat", "bleed_dps", lvl, 1.0);
                    target.getPersistentData().putLong("lvluping_as_bleed_until", currentTime + bleedTicks + bonus);
                    target.getPersistentData().putFloat("lvluping_as_bleed_dps", dps);
                    target.getPersistentData().putUUID("lvluping_as_bleed_src", attacker.getUUID());
                }
            }
            if (attacker.getPersistentData().getLong(TalentAbilityHandler.AS_NEXT_HIT_UNTIL_KEY) > currentTime) {
                int eff = attacker.getPersistentData().getInt(TalentAbilityHandler.AS_NEXT_HIT_EFFECT_KEY);
                if (eff == 1) {
                    int ticks = (int) attacker.getPersistentData().getFloat(TalentAbilityHandler.AS_NEXT_HIT_P1_KEY);
                    int amp = (int) attacker.getPersistentData().getFloat(TalentAbilityHandler.AS_NEXT_HIT_P2_KEY);
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, amp, false, false));
                } else if (eff == 2) {
                    float m = attacker.getPersistentData().getFloat(TalentAbilityHandler.AS_NEXT_HIT_P1_KEY);
                    if (m > 0f) event.setNewDamage(event.getNewDamage() * m);
                }
                attacker.getPersistentData().remove(TalentAbilityHandler.AS_NEXT_HIT_EFFECT_KEY);
                attacker.getPersistentData().remove(TalentAbilityHandler.AS_NEXT_HIT_P1_KEY);
                attacker.getPersistentData().remove(TalentAbilityHandler.AS_NEXT_HIT_P2_KEY);
                attacker.getPersistentData().remove(TalentAbilityHandler.AS_NEXT_HIT_UNTIL_KEY);
            }

            // --- A_POWER ---
            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                if (talents.contains("a_power")) {
                    if (arrow.isCritArrow()) {
                        event.setNewDamage(event.getOriginalDamage() * A_POWER_ARROW_CRIT_MULTIPLIER);
                    }
                }
            }
            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                if (talents.contains("a_hunter_wounding_shot")) {
                    int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "a_hunter_wounding_shot", talents);
                    double chance = AbilityUpgradeConfig.getDouble("a_hunter_wounding_shot", "chance", lvl, 0.6);
                    if (attacker.getRandom().nextDouble() < chance && attacker.level() instanceof ServerLevel sl) {
                        int bleedTicks = AbilityUpgradeConfig.getInt("a_hunter_wounding_shot", "bleed_ticks", lvl, 80);
                        float dps = (float) AbilityUpgradeConfig.getDouble("a_hunter_wounding_shot", "bleed_dps", lvl, 1.0);
                        long until = sl.getGameTime() + bleedTicks;
                        target.getPersistentData().putLong("lvluping_hunter_bleed_until", until);
                        target.getPersistentData().putFloat("lvluping_hunter_bleed_dps", dps);
                        target.getPersistentData().putUUID("lvluping_hunter_bleed_src", attacker.getUUID());
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.25, 0.25, 0.25, 0.06);
                    }
                }
                if (attacker.getPersistentData().getLong("lvluping_ranger_quick_step_until") > currentTime) {
                    float mult = attacker.getPersistentData().getFloat("lvluping_ranger_quick_step_mult");
                    if (mult > 1f) event.setNewDamage(event.getNewDamage() * mult);
                    attacker.getPersistentData().remove("lvluping_ranger_quick_step_until");
                }
            }

            // --- W_COMBO ---
            float comboMultiplier = 1.0f;
            if (talents.contains("w_combo")) {
                long lastHit = attacker.getPersistentData().getLong("lvluping_last_hit");
                int combo = attacker.getPersistentData().getInt("lvluping_combo");

                if (currentTime - lastHit < W_COMBO_CHAIN_WINDOW_TICKS) {
                    attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2f, W_COMBO_SOUND_PITCH_BASE + (combo * W_COMBO_SOUND_PITCH_PER_STACK));
                    combo = Math.min(combo + 1, W_COMBO_MAX_STACK);
                } else {
                    combo = 1;
                }

                attacker.getPersistentData().putInt("lvluping_combo", combo);
                attacker.getPersistentData().putLong("lvluping_last_hit", currentTime);

                if (attacker instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("lvluping_combo", combo));
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("lvluping_last_hit", (int)currentTime));
                    if (combo > 1 && attacker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), W_COMBO_CRIT_PARTICLE_COUNT, 0.3, 0.3, 0.3, 0.2);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), W_COMBO_SWEEP_PARTICLE_COUNT, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                comboMultiplier = 1.0f + (combo * W_COMBO_DAMAGE_PER_STACK);
                event.setNewDamage(event.getNewDamage() * comboMultiplier);
            }


            // --- W_STUN ---
            if (talents.contains("w_stun") && attacker.getRandom().nextFloat() < W_STUN_PROC_CHANCE) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, W_STUN_EFFECT_DURATION_TICKS, W_STUN_EFFECT_AMPLIFIER, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, W_STUN_EFFECT_DURATION_TICKS, W_STUN_EFFECT_AMPLIFIER, false, false));

                attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, W_STUN_ANVIL_PITCH);

                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.5, target.getZ(), 1, 0, 0, 0, 0);
                }
            }

            // --- W_BLOODLUST ---
            if (talents.contains("w_bloodlust")) {
                int blvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "w_bloodlust", talents);
                int hitsNeed = AbilityUpgradeConfig.getInt("w_bloodlust", "hits_to_heal", blvl, W_BLOODLUST_HITS_TO_HEAL);
                double healRatio = AbilityUpgradeConfig.getDouble("w_bloodlust", "heal_ratio", blvl, W_BLOODLUST_HEAL_FROM_DAMAGE_RATIO);
                int hits = attacker.getPersistentData().getInt("lvluping_w_hits");
                hits++;
                if (hits >= hitsNeed) {
                    hits = 0;
                    float healAmount = (float) (event.getNewDamage() * healRatio);
                    attacker.heal(healAmount);

                    attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 0.8f);

                    if (attacker.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                                8, 0.3, 0.4, 0.3, 0.1);
                        sl.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 0.5, attacker.getZ(),
                                4, 0.2, 0.2, 0.2, 0.1);
                    }
                }
                attacker.getPersistentData().putInt("lvluping_w_hits", hits);
            }
            if (talents.contains("w_swordmaster_sharp_blade")) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "w_swordmaster_sharp_blade", talents);
                double chance = AbilityUpgradeConfig.getDouble("w_swordmaster_sharp_blade", "double_damage_chance", lvl, 0.1);
                if (attacker.getRandom().nextDouble() < chance) {
                    event.setNewDamage(event.getNewDamage() * 2f);
                    if (attacker.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.25, 0.25, 0.25, 0.1);
                    }
                }
            }
            if (talents.contains("w_barbarian_rage")) {
                int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "w_barbarian_rage", talents);
                double perPct = AbilityUpgradeConfig.getDouble("w_barbarian_rage", "bonus_damage_per_missing_hp_pct", lvl, 0.003);
                double missPct = Math.max(0.0, (attacker.getMaxHealth() - attacker.getHealth()) / Math.max(1.0, attacker.getMaxHealth()));
                event.setNewDamage((float) (event.getNewDamage() * (1.0 + missPct * 100.0 * perPct)));
            }
            if (talents.contains("w_swordmaster_balance")) {
                boolean hasShield = attacker.getMainHandItem().is(net.minecraft.world.item.Items.SHIELD) || attacker.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD);
                if (!hasShield) {
                    int lvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "w_swordmaster_balance", talents);
                    double m = AbilityUpgradeConfig.getDouble("w_swordmaster_balance", "attack_speed_mult_no_shield", lvl, 1.1);
                    event.setNewDamage((float) (event.getNewDamage() * (1.0 + (m - 1.0) * 0.7)));
                }
            }
            if (attacker.level() instanceof ServerLevel sl) {
                long concUntil = attacker.getPersistentData().getLong(TalentAbilityHandler.W_SWORDMASTER_CONCENTRATION_UNTIL_KEY);
                if (concUntil > sl.getGameTime()) {
                    float mult = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_SWORDMASTER_CONCENTRATION_MULT_KEY);
                    if (mult > 0f) event.setNewDamage(event.getNewDamage() * mult);
                }
                long frenzyUntil = attacker.getPersistentData().getLong(TalentAbilityHandler.W_BARBARIAN_FRENZY_UNTIL_KEY);
                if (frenzyUntil > sl.getGameTime()) {
                    float mult = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_BARBARIAN_FRENZY_DAMAGE_MULT_KEY);
                    if (mult > 0f) event.setNewDamage(event.getNewDamage() * mult);
                }
                long bloodUntil = attacker.getPersistentData().getLong(TalentAbilityHandler.W_BARBARIAN_TASTE_BLOOD_UNTIL_KEY);
                if (bloodUntil > sl.getGameTime()) {
                    float ratio = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_BARBARIAN_TASTE_BLOOD_RATIO_KEY);
                    if (ratio > 0f) attacker.heal(event.getNewDamage() * ratio);
                }
                long killFrenzyUntil = attacker.getPersistentData().getLong(TalentAbilityHandler.W_BARBARIAN_KILL_FRENZY_UNTIL_KEY);
                if (killFrenzyUntil > sl.getGameTime()) {
                    float asMult = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_BARBARIAN_KILL_FRENZY_AS_MULT_KEY);
                    if (asMult > 1f) event.setNewDamage(event.getNewDamage() * (1f + (asMult - 1f) * 0.6f));
                }
                long hurricaneUntil = attacker.getPersistentData().getLong(TalentAbilityHandler.W_SWORDMASTER_HURRICANE_UNTIL_KEY);
                if (hurricaneUntil > sl.getGameTime()) {
                    float asMult = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_SWORDMASTER_HURRICANE_AS_MULT_KEY);
                    if (asMult > 1f) event.setNewDamage(event.getNewDamage() * (1f + (asMult - 1f) * 0.7f));
                }
            }
            if (attacker.getPersistentData().getBoolean(TalentAbilityHandler.W_SWORDMASTER_PERFECT_CUT_READY_KEY)) {
                float ratio = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_SWORDMASTER_PERFECT_CUT_RATIO_KEY);
                float bossMult = attacker.getPersistentData().getFloat(TalentAbilityHandler.W_SWORDMASTER_PERFECT_CUT_BOSS_MULT_KEY);
                float extra = target.getMaxHealth() * Math.max(0f, ratio);
                if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                    extra *= Math.max(0f, bossMult);
                }
                event.setNewDamage(event.getNewDamage() + extra);
                attacker.getPersistentData().putBoolean(TalentAbilityHandler.W_SWORDMASTER_PERFECT_CUT_READY_KEY, false);
            }

            // --- W_ARMOR_BREAKER ---
            if (talents.contains("w_armor_breaker") && PlayerLevels.getCooldown(attacker.getUUID(), "cd_w_armor_breaker") <= 0) {
                if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                    int alvl = PlayerLevels.getAbilityLevel(attacker.getUUID(), "w_armor_breaker", talents);
                    float dmult = (float) AbilityUpgradeConfig.getDouble("w_armor_breaker", "damage_mult", alvl, W_ARMOR_BREAKER_DAMAGE_MULTIPLIER);
                    int debuffTicks = AbilityUpgradeConfig.getInt("w_armor_breaker", "heal_reduction_duration_ticks", alvl, W_ARMOR_BREAKER_ARMOR_DEBUFF_DURATION_TICKS);
                    int cd = AbilityUpgradeConfig.getInt("w_armor_breaker", "cooldown", alvl, W_ARMOR_BREAKER_COOLDOWN_TICKS);
                    float newDamage = event.getNewDamage() * dmult;
                    event.setNewDamage(newDamage);

                    if (attacker.level() instanceof ServerLevel sl) {
                        long expire = sl.getGameTime() + debuffTicks;
                        target.getPersistentData().putLong("lvluping_w_armor_break_until", expire);

                        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8f, W_ARMOR_BREAKER_ANVIL_PITCH);
                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(),
                                W_ARMOR_BREAKER_CRIT_PARTICLE_COUNT, 0.3, 0.3, 0.3, 0.15);
                    }

                    PlayerLevels.setCooldown(attacker.getUUID(), "cd_w_armor_breaker", cd);
                    attacker.getPersistentData().putInt("cd_w_armor_breaker", cd);
                    if (attacker instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_w_armor_breaker", cd));
                    }
                }
            }
        }
    }

    private static final String PURITY_REAPPLY_SKIP = "lvluping_purity_reapply_skip";
    private static final String SHADOW_WRAP_REAPPLY_SKIP = "lvluping_shadow_wrap_reapply_skip";

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;
        if (living instanceof ServerPlayer pPal && event.getEffectInstance() != null) {
            MobEffectInstance ei = event.getEffectInstance();
            if (ei.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                Set<String> tt = PlayerLevels.getPlayerTalents(pPal.getUUID());
                if (tt.contains("w_paladin_providence")) {
                    pPal.getPersistentData().putLong("lvluping_paladin_prov_at", pPal.level().getGameTime() + 20);
                }
            }
        }
        if (living.getPersistentData().getBoolean(PURITY_REAPPLY_SKIP)) {
            living.getPersistentData().remove(PURITY_REAPPLY_SKIP);
            return;
        }
        if (living.getPersistentData().getBoolean(SHADOW_WRAP_REAPPLY_SKIP)) {
            living.getPersistentData().remove(SHADOW_WRAP_REAPPLY_SKIP);
            return;
        }
        if (living instanceof ServerPlayer spInv && event.getEffectInstance() != null) {
            MobEffectInstance inst = event.getEffectInstance();
            if (inst.getEffect().is(MobEffects.INVISIBILITY)) {
                Set<String> talents = PlayerLevels.getPlayerTalents(spInv.getUUID());
                if (talents.contains("as_wanderer_shadow_wrap")) {
                    int lvl = PlayerLevels.getAbilityLevel(spInv.getUUID(), "as_wanderer_shadow_wrap", talents);
                    int pct = AbilityUpgradeConfig.getInt("as_wanderer_shadow_wrap", "invis_duration_bonus_pct", lvl, 15);
                    int newDur = (int) Math.round(inst.getDuration() * (1.0 + pct / 100.0));
                    if (newDur < 1) newDur = 1;
                    if (newDur > inst.getDuration()) {
                        var holder = inst.getEffect();
                        int amp = inst.getAmplifier();
                        boolean amb = inst.isAmbient();
                        boolean vis = inst.isVisible();
                        boolean show = inst.showIcon();
                        spInv.getPersistentData().putBoolean(SHADOW_WRAP_REAPPLY_SKIP, true);
                        spInv.removeEffect(holder);
                        spInv.addEffect(new MobEffectInstance(holder, newDur, amp, amb, vis, show));
                    }
                    return;
                }
            }
        }
        if (!(living instanceof ServerPlayer player)) return;
        Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
        if (!talents.contains("m_cleric_purity")) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (inst == null) return;
        if (inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_purity", talents);
        double reduction = AbilityUpgradeConfig.getDouble("m_cleric_purity", "duration_reduction", lvl, 0.15);
        int newDur = (int) (inst.getDuration() * (1.0 - reduction));
        if (newDur < 1) newDur = 1;
        if (newDur >= inst.getDuration()) return;
        var holder = inst.getEffect();
        int amp = inst.getAmplifier();
        boolean amb = inst.isAmbient();
        boolean vis = inst.isVisible();
        boolean show = inst.showIcon();
        player.getPersistentData().putBoolean(PURITY_REAPPLY_SKIP, true);
        player.removeEffect(holder);
        player.addEffect(new MobEffectInstance(holder, newDur, amp, amb, vis, show));
    }

    private static void BarrierRender(Player player) {
        if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
            int barrierTicks = player.getPersistentData().getInt("lvluping_barrier_window");
            if (barrierTicks > 0) {
                double time = serverLevel.getGameTime() * BARRIER_RENDER_ANGLE_SCALE;
                for (int i = 0; i < BARRIER_RENDER_RING_POINTS; i++) {
                    double angle = time + (i * (Math.PI / BARRIER_RENDER_RING_POINTS) * 2.0);
                    double x = player.getX() + Math.cos(angle) * BARRIER_RENDER_RING_RADIUS;
                    double z = player.getZ() + Math.sin(angle) * BARRIER_RENDER_RING_RADIUS;
                    double y = player.getY() + BARRIER_RENDER_Y_OFFSET;

                    serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, BARRIER_RENDER_PARTICLE_SPEED);
                }
            }
        }
    }

    private static boolean isSummonerOrOwnedSummonKiller(net.minecraft.world.entity.Entity killer, ServerLevel level) {
        if (killer == null) return false;
        if (killer instanceof ServerPlayer sp) {
            return PlayerLevels.getPlayerTalents(sp.getUUID()).contains("m_summoner_base");
        }
        if (killer instanceof net.minecraft.world.entity.Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner")) {
            UUID ownerUuid = mob.getPersistentData().getUUID("lvluping_summon_owner");
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
            return owner != null && PlayerLevels.getPlayerTalents(ownerUuid).contains("m_summoner_base");
        }
        return false;
    }
}