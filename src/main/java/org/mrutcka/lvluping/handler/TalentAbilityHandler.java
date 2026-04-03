package org.mrutcka.lvluping.handler;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.compat.ArsManaCompat;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.network.S2CHunterTrapHide;
import org.mrutcka.lvluping.network.S2CHunterTrapShow;
import org.mrutcka.lvluping.network.S2CRangerLifeTotemHide;
import org.mrutcka.lvluping.network.S2CRangerLifeTotemShow;
import org.mrutcka.lvluping.network.S2CRangerRootsTargetHide;
import org.mrutcka.lvluping.network.S2CRangerRootsTargetShow;
import org.mrutcka.lvluping.network.S2CRangerThornHide;
import org.mrutcka.lvluping.network.S2CRangerThornShow;
import org.mrutcka.lvluping.network.S2CProvocationHint;
import org.mrutcka.lvluping.network.S2CSyncCooldown;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TalentAbilityHandler {
    public static void removeHarmfulEffects(ServerPlayer p, int maxCount) {
        int n = 0;
        for (MobEffectInstance inst : p.getActiveEffects()) {
            if (n >= maxCount) break;
            if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                p.removeEffect(inst.getEffect());
                n++;
            }
        }
    }

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

    private static final double LOOK_POINT_FALLBACK_RANGE = 10.0;
    private static final double LOOK_POINT_NORMAL_PUSH = 0.35;

    public static Vec3 snapStandingPosition(ServerLevel level, ServerPlayer player, Vec3 v) {
        int x = Mth.floor(v.x);
        int z = Mth.floor(v.z);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int startY = Mth.clamp((int) Math.ceil(v.y) + 4, minY, maxY);
        Vec3 start = new Vec3(x + 0.5, startY, z + 0.5);
        Vec3 end = new Vec3(x + 0.5, minY, z + 0.5);
        HitResult down = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (down instanceof BlockHitResult bhr) {
            return new Vec3(v.x, bhr.getBlockPos().getY() + 1.0 + 1.0e-3, v.z);
        }
        return v;
    }

    private static Vec3 getLookPointOnBlock(ServerLevel level, ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        HitResult hit = level.clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit instanceof BlockHitResult bhr) {
            Vec3 loc = bhr.getLocation();
            Direction d = bhr.getDirection();
            Vec3 normal = new Vec3(d.getStepX(), d.getStepY(), d.getStepZ());
            return snapStandingPosition(level, player, loc.add(normal.scale(LOOK_POINT_NORMAL_PUSH)));
        }
        return snapStandingPosition(level, player, end);
    }

    private static double applyClericBaseMana(ServerPlayer player, Set<String> talents, double baseCost) {
        if (!talents.contains("m_cleric_base")) return baseCost;
        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_base", talents);
        return baseCost * AbilityUpgradeConfig.getDouble("m_cleric_base", "mana_mult", lvl, 0.8);
    }

    public static float getClericHealingAmpMult(ServerPlayer player, Set<String> talents) {
        if (!talents.contains("m_cleric_healing_amp")) return 1.0f;
        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_healing_amp", talents);
        return (float) AbilityUpgradeConfig.getDouble("m_cleric_healing_amp", "heal_mult", lvl, 1.15);
    }

    public static double applySpellcasterManaCost(ServerPlayer player, Set<String> talents, double baseCost) {
        double out = baseCost;
        if (talents.contains("m_spellcaster_base")) {
            int bl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_spellcaster_base", talents);
            out *= AbilityUpgradeConfig.getDouble("m_spellcaster_base", "mana_mult", bl, 0.9);
        }
        if (talents.contains("m_recharge") && talents.contains("m_spellcaster_base")) {
            int rl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_recharge", talents);
            double red = AbilityUpgradeConfig.getDouble("m_recharge", "mana_cost_reduction", rl, 0.0);
            out *= (1.0 - red);
        }
        if (talents.contains("m_soft_landing") && talents.contains("m_spellcaster_base")) {
            int sl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_soft_landing", talents);
            double red = AbilityUpgradeConfig.getDouble("m_soft_landing", "mana_cost_reduction", sl, 0.0);
            out *= (1.0 - red);
        }
        return out;
    }

    private static java.util.List<LivingEntity> getLightningTargets(ServerPlayer player, ServerLevel level, int maxTargets, double range, double coneDeg) {
        if (maxTargets <= 0) return java.util.List.of();
        Vec3 look = player.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0, look.z);
        final Vec3 lookH = lookFlat.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : lookFlat.normalize();
        double coneRad = Math.toRadians(coneDeg);
        double cosHalf = Math.cos(coneRad * 0.5);
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range))
                .stream()
                .filter(e -> e != player && e.isAlive())
                .filter(e -> player.distanceToSqr(e) <= range * range)
                .filter(e -> {
                    Vec3 to = e.position().subtract(player.position());
                    Vec3 toH = new Vec3(to.x, 0, to.z);
                    double len2 = toH.lengthSqr();
                    if (len2 < 1.0e-6) return false;
                    toH = toH.normalize();
                    return lookH.dot(toH) >= cosHalf;
                })
                .sorted(java.util.Comparator.comparingDouble(player::distanceToSqr))
                .limit(maxTargets)
                .toList();
    }
    private static final int SLIDE_COOLDOWN = 200;
    private static final int SMOKE_COOLDOWN = 400;
    private static final double DASH_DELTA_BACK_MULT = 1.2;
    private static final int DASH_COOLDOWN = 160;
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
    private static final int W_ULT_BERSERK_FIRE_RESISTANCE_AMPLIFIER = 0;
    private static final int W_ULT_BERSERK_REGENERATION_AMPLIFIER = 0;
    private static final int W_ULT_BERSERK_MOVEMENT_SPEED_AMPLIFIER = 1;
    private static final int W_ULT_BERSERK_JUMP_AMPLIFIER = 0;
    private static final int W_PROVOCATION_GLOWING_AMPLIFIER = 0;
    private static final double SHIELD_STRIKE_RANGE = 3.0;
    private static final double SHIELD_STRIKE_HITBOX_Y_THICKNESS = 1.0;
    private static final double SHIELD_STRIKE_LOOK_DOT_THRESHOLD = 0.5;
    private static final float SHIELD_STRIKE_DAMAGE_BONUS = 2.0f;
    private static final int SHIELD_STRIKE_SLOW_AMPLIFIER = 4;
    private static final int SHIELD_STRIKE_WEAKNESS_AMPLIFIER = 1;
    private static final double SLIDE_DELTA_MULT_XZ = 1.4;
    private static final double HEAVY_STEP_RANGE = 3.5;
    private static final double HEAVY_STEP_HITBOX_Y_THICKNESS = 1.0;
    private static final double HEAVY_STEP_DELTA_Y_BOOST = 0.1;
    private static final double HEAVY_STEP_KNOCKBACK_SCALE = 1.2;
    private static final double HEAVY_STEP_PUSH_Y = 0.4;
    private static final float HEAVY_STEP_DAMAGE = 2.0f;
    private static final int HEAVY_STEP_SELF_DAMAGE_RESISTANCE_DURATION_TICKS = 20;
    private static final int HEAVY_STEP_SELF_DAMAGE_RESISTANCE_AMPLIFIER = 1;
    private static final double SPIN_RANGE = 3.0;
    private static final double SPIN_HITBOX_Y_THICKNESS = 1.0;
    private static final int SPIN_HALF_CD_MIN_HITCOUNT = 3;
    private static final double SEISMIC_RANGE = 6.0;
    private static final double SEISMIC_CONE_HALF_ANGLE_DEG = 40.0;
    private static final float SEISMIC_DAMAGE_MULT = 0.8f;
    private static final float SEISMIC_DAMAGE_BONUS = 3.0f;
    private static final int SEISMIC_SLOW_DURATION_TICKS = 60;
    private static final int SEISMIC_SLOW_AMPLIFIER = 1;
    private static final double SEISMIC_KNOCKBACK_SCALE = 0.5;
    private static final double SEISMIC_KNOCKBACK_Y = 0.25;
    private static final double SEISMIC_HITBOX_Y_THICKNESS = 2.0;
    private static final int IRON_SKIN_EFFECT_DURATION_TICKS = 100;
    private static final int IRON_SKIN_RESISTANCE_AMPLIFIER = 2;
    private static final int IRON_SKIN_SLOWDOWN_AMPLIFIER = 0;
    private static final int SMOKE_EFFECT_DURATION_TICKS = 200;
    private static final int TELEPORT_NO_COLLISION_ATTEMPTS = 6;
    private static final double TELEPORT_BACKTRACK_STEP = 1.0;
    private static final int BUFF_COOLDOWN = 600;
    private static final int BARRIER_WINDOW = 200;
    private static final int M_FIRE_COOLDOWN = 80;
    private static final int M_ICE_COOLDOWN = 80;
    private static final int M_TELEPORT_COOLDOWN = 120;
    private static final int M_SUMMON_COOLDOWN = 200;
    private static final int M_SACRIFICE_COOLDOWN = 60;
    private static final int M_COMMAND_COOLDOWN = 40;
    private static final int ABILITY_FAIL_COOLDOWN = 20;
    private static final int M_CLERIC_SMALL_HEAL_COOLDOWN = 120;
    private static final int M_CLERIC_BLESSING_COOLDOWN = 140;
    private static final int M_CLERIC_LIGHT_COOLDOWN = 140;
    private static final int M_ULT_LIGHT_RAY_COOLDOWN = 900;
    private static final int M_ULT_RESURRECTION_COOLDOWN = 3600;
    public static final String CLERIC_INVULN_UNTIL_KEY = "lvluping_cleric_invuln_until";
    public static final String W_LIGHT_FORM_UNTIL_KEY = "lvluping_w_light_form_until";
    public static final String W_LIGHT_FORM_RADIUS_KEY = "lvluping_w_light_form_radius";
    public static final String W_LIGHT_FORM_HEAL_KEY = "lvluping_w_light_form_heal";
    public static final String W_LIGHT_FORM_SHIELD_RATIO_KEY = "lvluping_w_light_form_shield_ratio";
    public static final String W_LIGHT_FORM_AX_KEY = "lvluping_w_light_form_ax";
    public static final String W_LIGHT_FORM_AY_KEY = "lvluping_w_light_form_ay";
    public static final String W_LIGHT_FORM_AZ_KEY = "lvluping_w_light_form_az";
    public static final String W_SWORDMASTER_CONCENTRATION_UNTIL_KEY = "lvluping_w_sm_concentration_until";
    public static final String W_SWORDMASTER_CONCENTRATION_MULT_KEY = "lvluping_w_sm_concentration_mult";
    public static final String W_BARBARIAN_FRENZY_UNTIL_KEY = "lvluping_w_barbarian_frenzy_until";
    public static final String W_BARBARIAN_FRENZY_DAMAGE_MULT_KEY = "lvluping_w_barbarian_frenzy_damage_mult";
    public static final String W_BARBARIAN_FRENZY_INCOMING_MULT_KEY = "lvluping_w_barbarian_frenzy_incoming_mult";
    public static final String W_BARBARIAN_KILL_FRENZY_UNTIL_KEY = "lvluping_w_barbarian_kill_frenzy_until";
    public static final String W_BARBARIAN_KILL_FRENZY_AS_MULT_KEY = "lvluping_w_barbarian_kill_frenzy_as_mult";
    public static final String W_BARBARIAN_TASTE_BLOOD_UNTIL_KEY = "lvluping_w_barbarian_taste_blood_until";
    public static final String W_BARBARIAN_TASTE_BLOOD_RATIO_KEY = "lvluping_w_barbarian_taste_blood_ratio";
    public static final String W_SWORDMASTER_HURRICANE_UNTIL_KEY = "lvluping_w_sm_hurricane_until";
    public static final String W_SWORDMASTER_HURRICANE_AS_MULT_KEY = "lvluping_w_sm_hurricane_as_mult";
    public static final String W_SWORDMASTER_STEEL_BODY_UNTIL_KEY = "lvluping_w_sm_steel_body_until";
    public static final String W_SWORDMASTER_STEEL_BODY_INCOMING_MULT_KEY = "lvluping_w_sm_steel_body_incoming_mult";
    public static final String W_SWORDMASTER_BLADE_WALL_UNTIL_KEY = "lvluping_w_sm_blade_wall_until";
    public static final String W_SWORDMASTER_PERFECT_CUT_READY_KEY = "lvluping_w_sm_perfect_cut_ready";
    public static final String W_SWORDMASTER_PERFECT_CUT_RATIO_KEY = "lvluping_w_sm_perfect_cut_ratio";
    public static final String W_SWORDMASTER_PERFECT_CUT_BOSS_MULT_KEY = "lvluping_w_sm_perfect_cut_boss_mult";
    public static final String W_BARBARIAN_FEAST_UNTIL_KEY = "lvluping_w_barbarian_feast_until";
    public static final String W_BARBARIAN_FEAST_DPS_KEY = "lvluping_w_barbarian_feast_dps";
    public static final String W_BARBARIAN_FEAST_LIFESTEAL_KEY = "lvluping_w_barbarian_feast_ls";
    public static final String A_NEXT_ARROW_EFFECT_KEY = "lvluping_a_next_arrow_effect";
    public static final String A_NEXT_ARROW_P1_KEY = "lvluping_a_next_arrow_p1";
    public static final String A_NEXT_ARROW_P2_KEY = "lvluping_a_next_arrow_p2";
    public static final String A_NEXT_ARROW_P3_KEY = "lvluping_a_next_arrow_p3";
    public static final String A_NEXT_ARROW_UNTIL_KEY = "lvluping_a_next_arrow_until";
    public static final String RANGER_ROOTS_UNTIL_KEY = "lvluping_ranger_roots_until";
    public static final String RANGER_ROOTS_DPS_KEY = "lvluping_ranger_roots_dps";
    public static final String RANGER_ROOTS_OWNER_KEY = "lvluping_ranger_roots_owner";
    public static final String A_HUNTER_TRACK_UNTIL_KEY = "lvluping_a_hunter_track_until";
    public static final String A_HUNTER_TRACK_TARGET_KEY = "lvluping_a_hunter_track_target";
    public static final String A_HUNTER_SNIPER_AIM_UNTIL_KEY = "lvluping_a_hunter_sniper_aim_until";
    public static final String A_HUNTER_SNIPER_MULT_KEY = "lvluping_a_hunter_sniper_mult";
    public static final String A_HUNTER_ULT_SHOT_MULT_KEY = "lvluping_a_hunter_ult_shot_mult";
    public static final String A_HUNTER_ULT_SHOT_BOSS_MULT_KEY = "lvluping_a_hunter_ult_shot_boss_mult";
    public static final String A_RANGER_THORN_UNTIL_KEY = "lvluping_a_ranger_thorn_until";
    public static final String A_RANGER_THORN_X_KEY = "lvluping_a_ranger_thorn_x";
    public static final String A_RANGER_THORN_Y_KEY = "lvluping_a_ranger_thorn_y";
    public static final String A_RANGER_THORN_Z_KEY = "lvluping_a_ranger_thorn_z";
    public static final String A_RANGER_THORN_R_KEY = "lvluping_a_ranger_thorn_r";
    public static final String A_RANGER_THORN_DPS_KEY = "lvluping_a_ranger_thorn_dps";
    public static final String A_RANGER_THORN_SLOW_AMP_KEY = "lvluping_a_ranger_thorn_slow_amp";
    public static final String A_RANGER_THORN_VISUAL_KEY = "lvluping_a_ranger_thorn_visual_id";
    public static final String A_RANGER_TOTEM_UNTIL_KEY = "lvluping_a_ranger_totem_until";
    public static final String A_RANGER_TOTEM_X_KEY = "lvluping_a_ranger_totem_x";
    public static final String A_RANGER_TOTEM_Y_KEY = "lvluping_a_ranger_totem_y";
    public static final String A_RANGER_TOTEM_Z_KEY = "lvluping_a_ranger_totem_z";
    public static final String A_RANGER_TOTEM_R_KEY = "lvluping_a_ranger_totem_r";
    public static final String A_RANGER_TOTEM_HEAL_TOTAL_KEY = "lvluping_a_ranger_totem_heal_total";
    public static final String A_RANGER_TOTEM_VISUAL_KEY = "lvluping_a_ranger_totem_visual_id";
    public static final String A_RANGER_WRATH_UNTIL_KEY = "lvluping_a_ranger_wrath_until";
    public static final String A_RANGER_WRATH_X_KEY = "lvluping_a_ranger_wrath_x";
    public static final String A_RANGER_WRATH_Y_KEY = "lvluping_a_ranger_wrath_y";
    public static final String A_RANGER_WRATH_Z_KEY = "lvluping_a_ranger_wrath_z";
    public static final String A_RANGER_WRATH_R_KEY = "lvluping_a_ranger_wrath_r";
    public static final String A_RANGER_ROOTS_UNTIL_KEY = "lvluping_a_ranger_roots_until";
    public static final String A_RANGER_ROOTS_X_KEY = "lvluping_a_ranger_roots_x";
    public static final String A_RANGER_ROOTS_Y_KEY = "lvluping_a_ranger_roots_y";
    public static final String A_RANGER_ROOTS_Z_KEY = "lvluping_a_ranger_roots_z";
    public static final String A_RANGER_ROOTS_R_KEY = "lvluping_a_ranger_roots_r";
    public static final String A_RANGER_MERGE_UNTIL_KEY = "lvluping_a_ranger_merge_until";
    public static final String A_RANGER_MERGE_AX_KEY = "lvluping_a_ranger_merge_ax";
    public static final String A_RANGER_MERGE_AY_KEY = "lvluping_a_ranger_merge_ay";
    public static final String A_RANGER_MERGE_AZ_KEY = "lvluping_a_ranger_merge_az";
    public static final String A_RANGER_MERGE_HPS_KEY = "lvluping_a_ranger_merge_hps";
    public static final String A_MUSK_BARRAGE_UNTIL_KEY = "lvluping_a_musk_barrage_until";
    public static final String A_MUSK_BARRAGE_SHOTS_KEY = "lvluping_a_musk_barrage_shots";
    public static final String A_HUNTER_TRAP_UNTIL_KEY = "lvluping_a_hunter_trap_until";
    public static final String A_HUNTER_TRAP_X_KEY = "lvluping_a_hunter_trap_x";
    public static final String A_HUNTER_TRAP_Y_KEY = "lvluping_a_hunter_trap_y";
    public static final String A_HUNTER_TRAP_Z_KEY = "lvluping_a_hunter_trap_z";
    public static final String A_HUNTER_TRAP_R_KEY = "lvluping_a_hunter_trap_r";
    public static final String A_HUNTER_TRAP_DMG_KEY = "lvluping_a_hunter_trap_dmg";
    public static final String A_HUNTER_TRAP_ROOT_KEY = "lvluping_a_hunter_trap_root";
    public static final String A_HUNTER_TRAP_VISUAL_KEY = "lvluping_a_hunter_trap_visual_id";
    public static final String AS_WANDERER_BARRICADE_VISUAL_KEY = "lvluping_as_barricade_visual_id";
    public static final String AS_WANDERER_BARRICADE_Y_ROT_KEY = "lvluping_as_barricade_y_rot";
    /** Server: wall-climb from as_wanderer_climb until this game time. */
    public static final String AS_WANDERER_WALL_CLIMB_UNTIL_KEY = "lvluping_wall_climb_until";
    public static final String LVLUPING_SLIDE_CHARGES_KEY = "lvluping_slide_charges";
    public static final String AS_WANDERER_TRIPWIRE_VISUAL_KEY = "lvluping_as_tripwire_visual_id";
    public static final String AS_WANDERER_CAMP_VISUAL_KEY = "lvluping_as_camp_visual_id";

    public static void broadcastHunterTrapShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, long untilGameTime) {
        var pkt = new S2CHunterTrapShow(vid, x, y, z, groundYRot, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastHunterTrapHide(ServerLevel sl, UUID vid) {
        var pkt = new S2CHunterTrapHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastAssassinBarricadeShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, long untilGameTime) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinBarricadeShow(vid, x, y, z, groundYRot, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void broadcastAssassinBarricadeHide(ServerLevel sl, UUID vid) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinBarricadeHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void placeAssassinBarricadeBarriers(ServerLevel sl, int bx, int by, int bz, float yRotDeg) {
        double tRad = Math.toRadians(yRotDeg + 90.0);
        double pdx = Math.cos(tRad);
        double pdz = Math.sin(tRad);
        for (int w = -1; w <= 1; w++) {
            BlockPos column = BlockPos.containing(bx + 0.5 + pdx * w, by, bz + 0.5 + pdz * w);
            for (int h = 0; h < 2; h++) {
                BlockPos pos = column.above(h);
                if (sl.getBlockState(pos).canBeReplaced()) {
                    sl.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                }
            }
        }
    }

    public static void removeAssassinBarricadeBarriers(ServerLevel sl, int bx, int by, int bz, float yRotDeg) {
        double tRad = Math.toRadians(yRotDeg + 90.0);
        double pdx = Math.cos(tRad);
        double pdz = Math.sin(tRad);
        for (int w = -1; w <= 1; w++) {
            BlockPos column = BlockPos.containing(bx + 0.5 + pdx * w, by, bz + 0.5 + pdz * w);
            for (int h = 0; h < 2; h++) {
                BlockPos pos = column.above(h);
                if (sl.getBlockState(pos).is(Blocks.BARRIER)) {
                    sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public static void broadcastAssassinTripwireShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, long untilGameTime) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinTripwireShow(vid, x, y, z, groundYRot, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void broadcastAssassinTripwireHide(ServerLevel sl, UUID vid) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinTripwireHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void broadcastAssassinCampShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, long untilGameTime) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinCampShow(vid, x, y, z, groundYRot, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void broadcastAssassinCampHide(ServerLevel sl, UUID vid) {
        var pkt = new org.mrutcka.lvluping.network.S2CAssassinCampHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) PacketDistributor.sendToPlayer(sp, pkt);
        }
    }

    public static void broadcastRangerMergeTreeShow(ServerLevel sl, ServerPlayer owner, UUID vid, double x, double y, double z, long untilGameTime) {
        var pkt = new org.mrutcka.lvluping.network.S2CMergeTreeShow(vid, x, y, z, owner.getYRot(), untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerMergeTreeHide(ServerLevel sl, UUID vid) {
        var pkt = new org.mrutcka.lvluping.network.S2CMergeTreeHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerThornShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, double radius, long untilGameTime) {
        var pkt = new S2CRangerThornShow(vid, x, y, z, groundYRot, radius, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerThornHide(ServerLevel sl, UUID vid) {
        var pkt = new S2CRangerThornHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerLifeTotemShow(ServerLevel sl, UUID vid, double x, double y, double z, float groundYRot, long untilGameTime) {
        var pkt = new S2CRangerLifeTotemShow(vid, x, y, z, groundYRot, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerLifeTotemHide(ServerLevel sl, UUID vid) {
        var pkt = new S2CRangerLifeTotemHide(vid);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerRootsTargetShow(ServerLevel sl, int entityId, long untilGameTime) {
        var pkt = new S2CRangerRootsTargetShow(entityId, untilGameTime);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void broadcastRangerRootsTargetHide(ServerLevel sl, int entityId) {
        var pkt = new S2CRangerRootsTargetHide(entityId);
        for (ServerPlayer sp : sl.players()) {
            if (sp.level() == sl) {
                PacketDistributor.sendToPlayer(sp, pkt);
            }
        }
    }

    public static void onArcherArrowSpawned(ServerPlayer sp, AbstractArrow arrow) {
        if (arrow.getPersistentData().getBoolean("lvluping_ranger_dup_arrow")) return;
        var pd = sp.getPersistentData();
        var ad = arrow.getPersistentData();
        int eff = pd.getInt(A_NEXT_ARROW_EFFECT_KEY);
        if (eff != 0) {
            ad.putInt(A_NEXT_ARROW_EFFECT_KEY, eff);
            if (pd.contains(A_NEXT_ARROW_P1_KEY)) ad.putFloat(A_NEXT_ARROW_P1_KEY, pd.getFloat(A_NEXT_ARROW_P1_KEY));
            if (pd.contains(A_NEXT_ARROW_P2_KEY)) ad.putFloat(A_NEXT_ARROW_P2_KEY, pd.getFloat(A_NEXT_ARROW_P2_KEY));
            if (pd.contains(A_NEXT_ARROW_P3_KEY)) ad.putFloat(A_NEXT_ARROW_P3_KEY, pd.getFloat(A_NEXT_ARROW_P3_KEY));
            if (pd.contains(A_NEXT_ARROW_UNTIL_KEY)) ad.putLong(A_NEXT_ARROW_UNTIL_KEY, pd.getLong(A_NEXT_ARROW_UNTIL_KEY));
            pd.remove(A_NEXT_ARROW_EFFECT_KEY);
            pd.remove(A_NEXT_ARROW_P1_KEY);
            pd.remove(A_NEXT_ARROW_P2_KEY);
            pd.remove(A_NEXT_ARROW_P3_KEY);
            pd.remove(A_NEXT_ARROW_UNTIL_KEY);
        }
        long time = sp.level().getGameTime();
        Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());
        eff = ad.getInt(A_NEXT_ARROW_EFFECT_KEY);
        if (eff == 0 && talents.contains("a_ranger_thunder_arrow") && pd.getInt("cd_a_ranger_thunder_arrow") <= 0) {
            int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "a_ranger_thunder_arrow", talents);
            float bonus = (float) AbilityUpgradeConfig.getDouble("a_ranger_thunder_arrow", "bonus_damage", lvl, 2.0);
            int cd = AbilityUpgradeConfig.getInt("a_ranger_thunder_arrow", "cooldown", lvl, 180);
            ad.putInt(A_NEXT_ARROW_EFFECT_KEY, 4);
            ad.putFloat(A_NEXT_ARROW_P1_KEY, bonus);
            ad.putLong(A_NEXT_ARROW_UNTIL_KEY, time + 200);
            setCooldown(sp, "cd_a_ranger_thunder_arrow", cd);
        }
        if (talents.contains("a_ranger_nimble_fingers") && sp.level() instanceof ServerLevel sl) {
            int nl = PlayerLevels.getAbilityLevel(sp.getUUID(), "a_ranger_nimble_fingers", talents);
            double ch = AbilityUpgradeConfig.getDouble("a_ranger_nimble_fingers", "double_shot_chance", nl, 0.3);
            if (sp.getRandom().nextDouble() < ch) {
                AbstractArrow dup = EntityType.ARROW.create(sl);
                if (dup != null) {
                    dup.setOwner(sp);
                    dup.setBaseDamage(arrow.getBaseDamage());
                    dup.getPersistentData().merge(arrow.getPersistentData());
                    dup.getPersistentData().putBoolean("lvluping_ranger_dup_arrow", true);
                    if (dup.getPersistentData().getInt(A_NEXT_ARROW_EFFECT_KEY) == 14) {
                        dup.getPersistentData().remove(A_NEXT_ARROW_EFFECT_KEY);
                        dup.getPersistentData().remove(A_NEXT_ARROW_P1_KEY);
                        dup.getPersistentData().remove(A_NEXT_ARROW_P2_KEY);
                        dup.getPersistentData().remove(A_NEXT_ARROW_UNTIL_KEY);
                    }
                    dup.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
                    Vec3 v = arrow.getDeltaMovement();
                    float speed = (float) Math.sqrt(v.lengthSqr());
                    if (speed < 0.05f) speed = 3.0f;
                    dup.shoot(v.x, v.y, v.z, speed, 1.0f);
                    sl.addFreshEntity(dup);
                }
            }
        }

    }
    public static final String AS_NEXT_HIT_EFFECT_KEY = "lvluping_as_next_hit_effect";
    public static final String AS_NEXT_HIT_P1_KEY = "lvluping_as_next_hit_p1";
    public static final String AS_NEXT_HIT_P2_KEY = "lvluping_as_next_hit_p2";
    public static final String AS_NEXT_HIT_UNTIL_KEY = "lvluping_as_next_hit_until";
    public static final String AS_MARK_UNTIL_KEY = "lvluping_as_mark_until";
    public static final String AS_MARK_TARGET_KEY = "lvluping_as_mark_target";
    public static final String AS_ROGUE_POISON_VEIL_UNTIL_KEY = "lvluping_as_rogue_poison_veil_until";
    public static final String AS_ROGUE_POISON_VEIL_X_KEY = "lvluping_as_rogue_poison_veil_x";
    public static final String AS_ROGUE_POISON_VEIL_Y_KEY = "lvluping_as_rogue_poison_veil_y";
    public static final String AS_ROGUE_POISON_VEIL_Z_KEY = "lvluping_as_rogue_poison_veil_z";
    public static final String AS_ROGUE_POISON_VEIL_R_KEY = "lvluping_as_rogue_poison_veil_r";
    public static final String AS_ROGUE_POISON_VEIL_DPS_KEY = "lvluping_as_rogue_poison_veil_dps";
    public static final String AS_WANDERER_CAMP_UNTIL_KEY = "lvluping_as_wanderer_camp_until";
    public static final String AS_WANDERER_CAMP_X_KEY = "lvluping_as_wanderer_camp_x";
    public static final String AS_WANDERER_CAMP_Y_KEY = "lvluping_as_wanderer_camp_y";
    public static final String AS_WANDERER_CAMP_Z_KEY = "lvluping_as_wanderer_camp_z";
    public static final String AS_WANDERER_CAMP_R_KEY = "lvluping_as_wanderer_camp_r";
    public static final String AS_WANDERER_CAMP_HPS_KEY = "lvluping_as_wanderer_camp_hps";
    public static final String AS_WANDERER_DAGGER_RAIN_UNTIL_KEY = "lvluping_as_wanderer_dagger_rain_until";
    public static final String AS_WANDERER_DAGGER_RAIN_SHOTS_KEY = "lvluping_as_wanderer_dagger_rain_shots";
    public static final String AS_WANDERER_THORN_TRAIL_UNTIL_KEY = "lvluping_as_wanderer_thorn_trail_until";
    public static final String AS_ASSASSIN_BLACK_MIST_UNTIL_KEY = "lvluping_as_assassin_black_mist_until";
    public static final String AS_ASSASSIN_BLACK_MIST_X_KEY = "lvluping_as_assassin_black_mist_x";
    public static final String AS_ASSASSIN_BLACK_MIST_Y_KEY = "lvluping_as_assassin_black_mist_y";
    public static final String AS_ASSASSIN_BLACK_MIST_Z_KEY = "lvluping_as_assassin_black_mist_z";
    public static final String AS_ASSASSIN_BLACK_MIST_R_KEY = "lvluping_as_assassin_black_mist_r";
    private static final int M_ULT_MARTYR_COOLDOWN = 900;
    private static final int M_ULT_SLOW_SPHERE_COOLDOWN = 900;
    private static final int M_ULT_DIVINE_PROTECTION_COOLDOWN = 1200;
    private static final double CLERIC_LIGHT_RAY_TARGET_AIM_RANGE_XZ = 25.0;
    private static final double CLERIC_LIGHT_RAY_AIM_CONE_DEG = 25.0;
    private static final double CLERIC_LIGHT_RAY_BEAM_RADIUS = 1.0;
    private static final int CLERIC_LIGHT_RAY_VERTICAL_REACH_BLOCKS = 100;
    private static final double CLERIC_SLOW_SPHERE_TARGET_AIM_RANGE_XZ = 25.0;
    private static final double CLERIC_SLOW_SPHERE_AIM_CONE_DEG = 25.0;
    private static final int BARRIER_EFFECT_DURATION_TICKS = 200;
    private static final int BARRIER_DEF_RESISTANCE_AMPLIFIER = 2;
    private static final int BARRIER_ATK_DAMAGE_BOOST_AMPLIFIER = 1;
    private static final float CLERIC_SMALL_HEAL_HEAL_AMP_MARTYR_MULT = 1.5f;
    private static final int M_ULT_ICE_BLOCK_WEAKNESS_AMPLIFIER = 1;
    private static final int M_ULT_POSSESSION_DAMAGE_RESISTANCE_AMPLIFIER = 1;
    private static final int M_ULT_POSSESSION_MOVEMENT_SPEED_AMPLIFIER = 2;
    private static final int M_ULT_POSSESSION_DAMAGE_BOOST_AMPLIFIER = 2;
    private static final int M_ULT_POSSESSION_GLOWING_AMPLIFIER = 0;
    private static final int M_ULT_ELEMENTAL_DAMAGE_RESISTANCE_AMPLIFIER = 1;
    private static final int M_ULT_ELEMENTAL_MOVEMENT_SPEED_AMPLIFIER = 1;
    private static final int M_ULT_ELEMENTAL_GLOWING_AMPLIFIER = 0;
    private static final double M_ULT_ELEMENTAL_SPAWN_FORWARD_OFFSET = 3.0;
    private static final int M_ULT_TOTEM_FORM_GLOWING_AMPLIFIER = 0;
    private static final int M_ULT_GATE_DAMAGE_RESISTANCE_AMPLIFIER = 1;
    private static final int M_ULT_GATE_MOVEMENT_SPEED_AMPLIFIER = 1;
    private static final int M_ULT_GATE_GLOWING_AMPLIFIER = 0;
    private static final double M_ULT_GATE_SPAWN_RING_RADIUS = 3.0;
    private static final double M_ULT_ILLUSIONS_OFFSET_XZ = 2.2;
    private static final double M_ULT_ILLUSIONS_OFFSET_Y = 0.1;
    private static final int M_ULT_CHAOS_WAVE_DURATION_TICKS = 30;
    private static final int M_ULT_CHAOS_BURN_TICKS = 80;
    private static final double PROJECTILE_SPAWN_OFFSET_EYE_Y = -0.1;
    private static final float FIREBALL_SHOOT_SPEED = 1.5f;
    private static final float FIREBALL_SHOOT_INACCURACY = 0.0f;
    private static final float ICE_ARROW_SHOOT_SPEED = 1.7f;
    private static final float ICE_ARROW_SHOOT_INACCURACY = 0.4f;
    private static final double M_SUMMON_SACRIFICE_AIM_RANGE_XZ = 16.0;
    private static final double M_SUMMON_SACRIFICE_AIM_CONE_DEG = 12.0;
    private static final double M_LIGHTNING_AIM_RANGE_XZ = 20.0;
    private static final double M_LIGHTNING_AIM_CONE_DEG = 100.0;
    private static final double M_CLERIC_SMALL_HEAL_AIM_RANGE_XZ = 20.0;
    private static final double M_CLERIC_SMALL_HEAL_AIM_CONE_DEG = 20.0;
    private static final double M_CLERIC_BLESSING_AIM_RANGE_XZ = 20.0;
    private static final double M_CLERIC_BLESSING_AIM_CONE_DEG = 20.0;
    private static final double M_SUMMON_COMMAND_AIM_RANGE_XZ = 25.0;
    private static final double M_SUMMON_COMMAND_AIM_CONE_DEG = 20.0;
    private static final double M_CLERIC_LIGHT_AIM_RANGE_XZ = 25.0;
    private static final double M_CLERIC_LIGHT_AIM_CONE_DEG = 20.0;
    private static final double M_ICE_ARROW_AIM_RANGE_XZ = 18.0;
    private static final double M_ICE_ARROW_AIM_CONE_DEG = 25.0;
    private static final double M_ULT_METEOR_AIM_RANGE_XZ = 25.0;
    private static final double M_ULT_METEOR_AIM_CONE_DEG = 25.0;
    private static final double M_ULT_ICE_BLOCK_AIM_RANGE_XZ = 18.0;
    private static final double M_ULT_ICE_BLOCK_AIM_CONE_DEG = 25.0;
    private static final double W_ULT_FINAL_COUNTDOWN_AIM_RANGE_XZ = 10.0;
    private static final double W_ULT_FINAL_COUNTDOWN_AIM_CONE_DEG = 25.0;
    private static final double DIVINE_PROTECTION_HITBOX_Y_THICKNESS = 5.0;

    private static void applySummonLoadout(String abilityId, int lvl, Mob summon, double hpMultiplier, int armorBonus) {
        int armorTier = AbilityUpgradeConfig.getInt(abilityId, "armor_tier", lvl, 1) + armorBonus;
        if (armorTier < 0) armorTier = 0;
        if (armorTier > 3) armorTier = 3;
        double hp = AbilityUpgradeConfig.getDouble(abilityId, "health", lvl, summon.getMaxHealth()) * hpMultiplier;
        if (summon.getAttribute(Attributes.MAX_HEALTH) != null) {
            summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
            summon.setHealth((float) hp);
        }
        if ("m_summon_guard".equals(abilityId)) {
            summon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        } else {
            summon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        if (armorTier >= 1) {
            summon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            summon.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        }
        if (armorTier >= 2) {
            summon.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            summon.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        }
        if (armorTier >= 3) {
            summon.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            summon.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            summon.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            summon.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        }
    }

    public static boolean isDagger(Item item) {
        return item == Items.IRON_SWORD;
    }

    public static void setCooldown(ServerPlayer player, String key, int ticks) {
        if (ticks > 0) {
            Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
            if (talents.contains("m_spellcaster_base")) {
                int bl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_spellcaster_base", talents);
                ticks = (int) Math.max(1, Math.round(ticks * AbilityUpgradeConfig.getDouble("m_spellcaster_base", "cooldown_mult", bl, 0.9)));
            }
            if (talents.contains("m_recharge") && talents.contains("m_spellcaster_base")) {
                int rl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_recharge", talents);
                double red = AbilityUpgradeConfig.getDouble("m_recharge", "cooldown_reduction", rl, 0.1);
                ticks = (int) Math.max(1, Math.round(ticks * (1.0 - red)));
            }
        }
        PlayerLevels.setCooldown(player.getUUID(), key, ticks);
        player.getPersistentData().putInt(key, ticks);
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, ticks));
    }

    public static void setPassiveInternalCooldown(ServerPlayer player, String key, int ticks) {
        PlayerLevels.setCooldown(player.getUUID(), key, Math.max(0, ticks));
        player.getPersistentData().putInt(key, Math.max(0, ticks));
        if (ticks > 0) {
            PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, ticks));
        }
    }

    /** Base 1 + bonus from as_wanderer_double_dodge (typically +2 → 3 total). */
    public static int getSlideMaxCharges(ServerPlayer player, Set<String> talents) {
        if (!talents.contains("as_wanderer_double_dodge")) return 1;
        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_wanderer_double_dodge", talents);
        int bonus = AbilityUpgradeConfig.getInt("as_wanderer_double_dodge", "bonus_charges", lvl, 2);
        return 1 + Math.max(0, bonus);
    }

    public static void refillSlideCharges(ServerPlayer player) {
        Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
        if (!talents.contains("as_slide")) return;
        int v = getSlideMaxCharges(player, talents);
        player.getPersistentData().putInt(LVLUPING_SLIDE_CHARGES_KEY, v);
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(LVLUPING_SLIDE_CHARGES_KEY, v));
    }

    public static void syncSlideChargesToClient(ServerPlayer player) {
        if (!PlayerLevels.getPlayerTalents(player.getUUID()).contains("as_slide")) return;
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(LVLUPING_SLIDE_CHARGES_KEY, player.getPersistentData().getInt(LVLUPING_SLIDE_CHARGES_KEY)));
    }

    private static double getSummonerManaCost(ServerPlayer player, Set<String> talents, double baseCost) {
        double out = baseCost;
        if (talents.contains("m_summoner_base")) {
            int evoLvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summoner_base", talents);
            out *= AbilityUpgradeConfig.getDouble("m_summoner_base", "mana_mult", evoLvl, 0.9);
        }
        if (talents.contains("m_summon_efficiency")) {
            int effLvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_efficiency", talents);
            out *= AbilityUpgradeConfig.getDouble("m_summon_efficiency", "mana_mult", effLvl, 0.85);
        }
        return out;
    }

    private static double getSummonerBaseHpMult(ServerPlayer player, Set<String> talents) {
        if (!talents.contains("m_summoner_base")) return 1.0;
        int evoLvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summoner_base", talents);
        return AbilityUpgradeConfig.getDouble("m_summoner_base", "hp_mult", evoLvl, 1.1);
    }

    public static void syncAllCooldowns(ServerPlayer player) {
        String[] keys = {
                "cd_slide", "cd_smoke", "cd_dash", "cd_parry", "cd_buff",
                "cd_w_seismic", "cd_w_spin", "cd_w_unbreakable", "cd_w_armor_breaker",
                "cd_w_swordmaster_concentration", "cd_w_swordmaster_steel_body", "cd_w_barbarian_battle_cry", "cd_w_barbarian_bloodletting", "cd_w_barbarian_frenzy",
                "cd_w_provocation", "cd_w_paladin_blessing", "cd_w_paladin_immolation", "cd_w_ult_berserk", "cd_w_ult_final_countdown", "cd_w_ult_invulnerability", "cd_w_ult_paladin_wings", "cd_w_ult_paladin_sacrifice", "cd_w_ult_swordmaster_hurricane", "cd_w_ult_barbarian_taste_blood"
                , "cd_w_ult_swordmaster_omnislash", "cd_w_ult_swordmaster_blade_wall", "cd_w_ult_swordmaster_perfect_cut", "cd_w_ult_barbarian_feast"
                , "cd_m_fireball", "cd_m_lightning", "cd_m_ice", "cd_m_teleport", "cd_m_summon", "cd_m_sacrifice", "cd_m_command",
                "cd_m_ult_gate", "cd_m_ult_absorption", "cd_m_ult_totem_form", "cd_m_ult_possession", "cd_m_ult_elemental",
                "cd_m_stone_skin", "cd_m_magic_barrier",
                "cd_m_ult_meteor", "cd_m_ult_ice_block", "cd_m_ult_anti_magic", "cd_m_ult_illusions", "cd_m_ult_chaos"
                , "cd_m_cleric_heal", "cd_m_cleric_blessing", "cd_m_cleric_light",
                "cd_m_ult_light_ray", "cd_m_ult_resurrection", "cd_m_ult_martyr", "cd_m_ult_slow_sphere", "cd_m_ult_divine_protection",

                "cd_a_hunter_trap", "cd_a_hunter_call_nature", "cd_a_hunter_poison_arrow", "cd_a_hunter_net", "cd_a_hunter_escape",
                "cd_a_ranger_entangle_arrow", "cd_a_ranger_evasion", "cd_a_ranger_thunder_arrow", "cd_a_ranger_thorn_bush",
                "cd_a_musketeer_quick_reload", "cd_a_musketeer_incendiary", "cd_a_musketeer_aimed_shot", "cd_a_musketeer_holster",

                "cd_a_ult_hunter_ult_shot", "cd_a_ult_hunter_pack", "cd_a_ult_hunter_sniper", "cd_a_ult_hunter_track",
                "cd_a_ult_ranger_wrath", "cd_a_ult_ranger_life_totem", "cd_a_ult_ranger_merge", "cd_a_ult_ranger_roots",
                "cd_a_ult_musketeer_barrage", "cd_a_ult_musketeer_grenade", "cd_a_ult_musketeer_concussion", "cd_a_ult_musketeer_execution",
                "cd_as_rogue_strong_poison", "cd_as_rogue_trip", "cd_as_rogue_blind",
                "cd_as_wanderer_barricade", "cd_as_wanderer_climb", "cd_as_wanderer_tripwire",
                "cd_as_assassin_mark", "cd_as_assassin_shuriken", "cd_as_assassin_rupture",
                "cd_as_ult_rogue_perfect_kill", "cd_as_ult_rogue_poison_veil", "cd_as_ult_rogue_confusion", "cd_as_ult_rogue_vanish",
                "cd_as_ult_wanderer_camp", "cd_as_ult_wanderer_dagger_rain", "cd_as_ult_wanderer_thorn_trail", "cd_as_ult_wanderer_ghosts",
                "cd_as_ult_assassin_blade_dance", "cd_as_ult_assassin_immobilize", "cd_as_ult_assassin_black_mist", "cd_as_ult_assassin_double"
        };
        for (String key : keys) {
            int val = PlayerLevels.getCooldown(player.getUUID(), key);
            player.getPersistentData().putInt(key, val);
            if (val > 0) {
                PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, val));
            }
        }
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown("lvluping_ranger_evasion_stacks", player.getPersistentData().getInt("lvluping_ranger_evasion_stacks")));
        syncSlideChargesToClient(player);
    }

    public static void handleAbilityUse(ServerPlayer player, int slot) {
        Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
        var data = player.getPersistentData();

        switch (slot) {
            case 0 -> {
                if (talents.contains("as_rogue_strong_poison") && data.getInt("cd_as_rogue_strong_poison") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_rogue_strong_poison", talents);
                        int ticks = AbilityUpgradeConfig.getInt("as_rogue_strong_poison", "poison_ticks", lvl, 80);
                        int amp = AbilityUpgradeConfig.getInt("as_rogue_strong_poison", "poison_amp", lvl, 0);
                        int cd = AbilityUpgradeConfig.getInt("as_rogue_strong_poison", "cooldown", lvl, 180);
                        data.putInt(AS_NEXT_HIT_EFFECT_KEY, 1);
                        data.putFloat(AS_NEXT_HIT_P1_KEY, ticks);
                        data.putFloat(AS_NEXT_HIT_P2_KEY, amp);
                        data.putLong(AS_NEXT_HIT_UNTIL_KEY, sl.getGameTime() + 200);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8f, 1.2f);
                        sl.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                        setCooldown(player, "cd_as_rogue_strong_poison", cd);
                    }
                    return;
                }
                if (talents.contains("as_wanderer_barricade") && data.getInt("cd_as_wanderer_barricade") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_wanderer_barricade", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_wanderer_barricade", "duration_ticks", lvl, 80);
                        int cd = AbilityUpgradeConfig.getInt("as_wanderer_barricade", "cooldown", lvl, 220);
                        Vec3 look = player.getLookAngle().normalize();
                        int bx = (int) Math.floor(player.getX() + look.x * 2.0);
                        int by = (int) Math.floor(player.getY());
                        int bz = (int) Math.floor(player.getZ() + look.z * 2.0);
                        data.putLong("lvluping_as_barricade_remove_at", sl.getGameTime() + dur);
                        data.putInt("lvluping_as_barricade_x", bx);
                        data.putInt("lvluping_as_barricade_y", by);
                        data.putInt("lvluping_as_barricade_z", bz);
                        data.putFloat(AS_WANDERER_BARRICADE_Y_ROT_KEY, player.getYRot());
                        placeAssassinBarricadeBarriers(sl, bx, by, bz, player.getYRot());
                        UUID vid = UUID.randomUUID();
                        data.putUUID(AS_WANDERER_BARRICADE_VISUAL_KEY, vid);
                        broadcastAssassinBarricadeShow(sl, vid, bx + 0.5, by, bz + 0.5, player.getYRot(), data.getLong("lvluping_as_barricade_remove_at"));
                        sl.playSound(null, bx, by, bz, SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.9f, 1.0f);
                        setCooldown(player, "cd_as_wanderer_barricade", cd);
                    }
                    return;
                }
                if (talents.contains("as_assassin_mark") && data.getInt("cd_as_assassin_mark") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 25.0, 25.0);
                        if (t == null) {
                            setCooldown(player, "cd_as_assassin_mark", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_assassin_mark", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_assassin_mark", "duration_ticks", lvl, 300);
                        int cd = AbilityUpgradeConfig.getInt("as_assassin_mark", "cooldown", lvl, 200);
                        data.putLong(AS_MARK_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putUUID(AS_MARK_TARGET_KEY, t.getUUID());
                        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
                        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.4f);
                        setCooldown(player, "cd_as_assassin_mark", cd);
                    }
                    return;
                }
                if (talents.contains("a_hunter_trap") && data.getInt("cd_a_hunter_trap") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_hunter_trap", talents);
                        int root = AbilityUpgradeConfig.getInt("a_hunter_trap", "root_ticks", lvl, 40);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("a_hunter_trap", "damage", lvl, 2.0);
                        int cd = AbilityUpgradeConfig.getInt("a_hunter_trap", "cooldown", lvl, 200);
                        Vec3 pos = getLookPointOnBlock(sl, player, 10.0);
                        data.putLong(A_HUNTER_TRAP_UNTIL_KEY, sl.getGameTime() + 400);
                        data.putDouble(A_HUNTER_TRAP_X_KEY, pos.x);
                        data.putDouble(A_HUNTER_TRAP_Y_KEY, pos.y);
                        data.putDouble(A_HUNTER_TRAP_Z_KEY, pos.z);
                        data.putDouble(A_HUNTER_TRAP_R_KEY, 1.7);
                        data.putFloat(A_HUNTER_TRAP_DMG_KEY, dmg);
                        data.putInt(A_HUNTER_TRAP_ROOT_KEY, root);
                        UUID trapVis = UUID.randomUUID();
                        data.putUUID(A_HUNTER_TRAP_VISUAL_KEY, trapVis);
                        broadcastHunterTrapShow(sl, trapVis, pos.x, pos.y, pos.z, player.getYRot(), data.getLong(A_HUNTER_TRAP_UNTIL_KEY));
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.9f, 1.4f);
                        setCooldown(player, "cd_a_hunter_trap", cd);
                    }
                    return;
                }
                if (talents.contains("a_ranger_entangle_arrow") && data.getInt("cd_a_ranger_entangle_arrow") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ranger_entangle_arrow", talents);
                        int slowTicks = AbilityUpgradeConfig.getInt("a_ranger_entangle_arrow", "slow_ticks", lvl, 80);
                        int slowAmp = AbilityUpgradeConfig.getInt("a_ranger_entangle_arrow", "slow_amp", lvl, 2);
                        int cd = AbilityUpgradeConfig.getInt("a_ranger_entangle_arrow", "cooldown", lvl, 160);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 2);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, slowTicks);
                        data.putFloat(A_NEXT_ARROW_P2_KEY, slowAmp);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 200);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.9f, 1.2f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
                        setCooldown(player, "cd_a_ranger_entangle_arrow", cd);
                    }
                    return;
                }
                if (talents.contains("a_musketeer_quick_reload") && data.getInt("cd_a_musketeer_quick_reload") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_musketeer_quick_reload", talents);
                        int cd = AbilityUpgradeConfig.getInt("a_musketeer_quick_reload", "cooldown", lvl, 180);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("a_musketeer_quick_reload", "damage", lvl, 6.0);
                        float speed = (float) AbilityUpgradeConfig.getDouble("a_musketeer_quick_reload", "speed", lvl, 3.0);

                        var a = net.minecraft.world.entity.EntityType.ARROW.create(sl);
                        if (a == null) {
                            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_a_musketeer_quick_reload", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        a.setOwner(player);
                        a.setPos(player.getX(), player.getEyeY() + PROJECTILE_SPAWN_OFFSET_EYE_Y, player.getZ());
                        a.setBaseDamage(Math.max(0.0, dmg));
                        Vec3 dir = player.getLookAngle().normalize();
                        a.shoot(dir.x, dir.y, dir.z, speed, 0.0f);
                        sl.addFreshEntity(a);

                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.9f, 1.4f);
                        sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.35, 0.45, 0.35, 0.12);
                        setCooldown(player, "cd_a_musketeer_quick_reload", cd);
                    }
                    return;
                }
                if (talents.contains("w_paladin_blessing") && data.getInt("cd_w_paladin_blessing") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_paladin_blessing", talents);
                        int cd = AbilityUpgradeConfig.getInt("w_paladin_blessing", "cooldown", lvl, 200);
                        if (player.isShiftKeyDown()) {
                            int n = AbilityUpgradeConfig.getInt("w_paladin_blessing", "cleanse_count", lvl, 1);
                            removeHarmfulEffects(player, n);
                        } else {
                            LivingEntity target = getTargetInFront(player, 20.0, 35.0);
                            if (target == null) target = player;
                            if (!target.isAlliedTo(player)) target = player;
                            int regenTicks = AbilityUpgradeConfig.getInt("w_paladin_blessing", "regen_ticks", lvl, 100);
                            int amp = AbilityUpgradeConfig.getInt("w_paladin_blessing", "regen_amp", lvl, 0);
                            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, amp, false, false));
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
                        setCooldown(player, "cd_w_paladin_blessing", cd);
                    }
                    return;
                }
                if (talents.contains("w_swordmaster_concentration") && data.getInt("cd_w_swordmaster_concentration") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_swordmaster_concentration", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_swordmaster_concentration", "duration_ticks", lvl, 60);
                        double mult = AbilityUpgradeConfig.getDouble("w_swordmaster_concentration", "damage_mult", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("w_swordmaster_concentration", "cooldown", lvl, 220);
                        data.putLong(W_SWORDMASTER_CONCENTRATION_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        data.putFloat(W_SWORDMASTER_CONCENTRATION_MULT_KEY, (float) mult);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.35f);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.2, 0.4, 0.02);
                        setCooldown(player, "cd_w_swordmaster_concentration", cd);
                    }
                    return;
                }
                if (talents.contains("w_barbarian_battle_cry") && data.getInt("cd_w_barbarian_battle_cry") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_barbarian_battle_cry", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_barbarian_battle_cry", "duration_ticks", lvl, 100);
                        int strAmp = AbilityUpgradeConfig.getInt("w_barbarian_battle_cry", "self_strength_amp", lvl, 0);
                        int speedAmp = AbilityUpgradeConfig.getInt("w_barbarian_battle_cry", "self_speed_amp", lvl, 0);
                        int weakAmp = AbilityUpgradeConfig.getInt("w_barbarian_battle_cry", "enemy_weakness_amp", lvl, 0);
                        double radius = AbilityUpgradeConfig.getDouble("w_barbarian_battle_cry", "radius", lvl, 5.0);
                        int cd = AbilityUpgradeConfig.getInt("w_barbarian_battle_cry", "cooldown", lvl, 220);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, strAmp, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, speedAmp, false, false));
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.0, radius))) {
                            if (e == player) continue;
                            if (player.distanceToSqr(e) > radius * radius) continue;
                            if (e.isAlliedTo(player)) continue;
                            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, weakAmp, false, false));
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.6f);
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.1, player.getZ(), 22, 0.55, 0.5, 0.55, 0.05);
                        setCooldown(player, "cd_w_barbarian_battle_cry", cd);
                    }
                    return;
                }

        // --- W_PARRY ---
        if (talents.contains("w_parry") && data.getInt("cd_parry") <= 0) {
            setCooldown(player, "lvluping_parry_window", PARRY_WINDOW);
            setCooldown(player, "cd_parry", PARRY_COOLDOWN);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

                // --- M_SUMMON_SERVANT / M_SUMMON_GUARD ---
                if ((talents.contains("m_summon_servant") || talents.contains("m_summon_guard")) && data.getInt("cd_m_summon") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        String abilityId = talents.contains("m_summon_guard") ? "m_summon_guard" : "m_summon_servant";
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), abilityId, talents);
                        double cost = AbilityUpgradeConfig.getDouble(abilityId, "mana", lvl, 60.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_summon", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        Mob summon;
                        if (talents.contains("m_summon_guard")) {
                            summon = net.minecraft.world.entity.EntityType.ZOMBIE.create(serverLevel);
                        } else {
                            summon = net.minecraft.world.entity.EntityType.SKELETON.create(serverLevel);
                        }
                        if (summon == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_summon", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        Vec3 forward = player.getLookAngle().normalize();
                        double sx = player.getX() + forward.x * 2.0;
                        double sy = player.getY();
                        double sz = player.getZ() + forward.z * 2.0;
                        Vec3 spawn = snapStandingPosition(serverLevel, player, new Vec3(sx, sy, sz));
                        sx = spawn.x;
                        sy = spawn.y;
                        sz = spawn.z;
                        summon.moveTo(sx, sy, sz, player.getYRot(), 0);
                        double hpMult = getSummonerBaseHpMult(player, talents);
                        int disciplineLvl = talents.contains("m_summon_discipline")
                                ? PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_discipline", talents) : 0;
                        int armorBonus = AbilityUpgradeConfig.getInt("m_summon_discipline", "armor_bonus", disciplineLvl, 0);
                        applySummonLoadout(abilityId, lvl, summon, hpMult, armorBonus);
                        serverLevel.addFreshEntity(summon);
                        int duration = AbilityUpgradeConfig.getInt(abilityId, "duration_ticks", lvl, (int) (20L * 30L));
                        long until = serverLevel.getGameTime() + duration;
                        int endLvl = talents.contains("m_summon_endurance")
                                ? PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_endurance", talents) : 0;
                        double summonDamageMult = AbilityUpgradeConfig.getDouble("m_summon_endurance", "damage_mult", endLvl, 1.0);
                        summon.getPersistentData().putDouble("lvluping_summon_damage_mult", summonDamageMult);
                        SummonerHandler.addSummon(serverLevel, player, summon, until);
                        serverLevel.playSound(null, sx, sy, sz, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.9f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.SMOKE, sx, sy + 0.8, sz, 15, 0.3, 0.4, 0.3, 0.02);
                        int cd = AbilityUpgradeConfig.getInt(abilityId, "cooldown", lvl, M_SUMMON_COOLDOWN);
                        setCooldown(player, "cd_m_summon", cd);
                    }
                    return;
                }

                // --- M_FIREBALL ---
                if (talents.contains("m_fireball") && data.getInt("cd_m_fireball") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_fireball", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_fireball", "mana", lvl, 30.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_fireball", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        double igniteR = AbilityUpgradeConfig.getDouble("m_fireball", "ignite_radius", lvl, 2.0);
                        Vec3 look = player.getLookAngle().normalize();
                        SmallFireball fb = net.minecraft.world.entity.EntityType.SMALL_FIREBALL.create(serverLevel);
                        if (fb != null) {
                            fb.setOwner(player);
                            fb.getPersistentData().putBoolean("lvluping_spell_fireball", true);
                            fb.getPersistentData().putDouble("lvluping_fireball_ignite_r", igniteR);
                            fb.moveTo(player.getX(), player.getEyeY() + PROJECTILE_SPAWN_OFFSET_EYE_Y, player.getZ(), player.getYRot(), player.getXRot());
                            fb.shoot(look.x, look.y, look.z, FIREBALL_SHOOT_SPEED, FIREBALL_SHOOT_INACCURACY);
                            serverLevel.addFreshEntity(fb);
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.2f);
                        int cd = AbilityUpgradeConfig.getInt("m_fireball", "cooldown", lvl, M_FIRE_COOLDOWN);
                        setCooldown(player, "cd_m_fireball", cd);
                    }
                    return;
                }

                // --- M_CLERIC_SMALL_HEAL ---
                if (talents.contains("m_cleric_small_heal") && data.getInt("cd_m_cleric_heal") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_small_heal", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_cleric_small_heal", "mana", lvl, 20.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_cleric_heal", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        LivingEntity target = getTargetInFront(player, M_CLERIC_SMALL_HEAL_AIM_RANGE_XZ, M_CLERIC_SMALL_HEAL_AIM_CONE_DEG);
                        if (target == null) target = player;
                        if (target != player && !target.isAlliedTo(player)) target = player;

                        long martyrUntil = player.getPersistentData().getLong("lvluping_cleric_martyr_until");
                        boolean martyrActive = martyrUntil > serverLevel.getGameTime();

                        float baseHeal = (float) AbilityUpgradeConfig.getDouble("m_cleric_small_heal", "heal", lvl, 3.0);
                        float heal = baseHeal;
                        heal *= getClericHealingAmpMult(player, talents);
                        if (martyrActive) {
                            if (target == player) {
                                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                                setCooldown(player, "cd_m_cleric_heal", ABILITY_FAIL_COOLDOWN);
                                return;
                            }
                            heal *= CLERIC_SMALL_HEAL_HEAL_AMP_MARTYR_MULT;
                        }

                        target.heal(heal);
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.7f, 1.1f);
                        serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.25, 0.35, 0.25, 0.05);

                        int cd = AbilityUpgradeConfig.getInt("m_cleric_small_heal", "cooldown", lvl, M_CLERIC_SMALL_HEAL_COOLDOWN);
                        setCooldown(player, "cd_m_cleric_heal", cd);
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
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BARRIER_EFFECT_DURATION_TICKS, BARRIER_DEF_RESISTANCE_AMPLIFIER, false, false));
                if (talents.contains("m_buff_atk")) {
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BARRIER_EFFECT_DURATION_TICKS, BARRIER_ATK_DAMAGE_BOOST_AMPLIFIER, false, false));
                        }
                    }
                }
            }
            case 1 -> {
                if (talents.contains("as_rogue_trip") && data.getInt("cd_as_rogue_trip") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 4.0, 35.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_rogue_trip", talents);
                        int stun = AbilityUpgradeConfig.getInt("as_rogue_trip", "stun_ticks", lvl, 40);
                        int slowAmp = AbilityUpgradeConfig.getInt("as_rogue_trip", "slow_amp", lvl, 2);
                        float mult = (float) AbilityUpgradeConfig.getDouble("as_rogue_trip", "damage_mult", lvl, 1.1);
                        int cd = AbilityUpgradeConfig.getInt("as_rogue_trip", "cooldown", lvl, 200);
                        if (t != null && t != player) {
                            float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult;
                            t.hurt(player.damageSources().playerAttack(player), dmg);
                            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, stun, 0, false, false));
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, Math.max(0, slowAmp), false, false));
                            sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
                            setCooldown(player, "cd_as_rogue_trip", cd);
                        } else {
                            setCooldown(player, "cd_as_rogue_trip", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("as_wanderer_climb") && data.getInt("cd_as_wanderer_climb") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_wanderer_climb", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_wanderer_climb", "duration_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("as_wanderer_climb", "cooldown", lvl, 180);
                        data.putLong(AS_WANDERER_WALL_CLIMB_UNTIL_KEY, sl.getGameTime() + dur);
                        setCooldown(player, "cd_as_wanderer_climb", cd);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 0.9f, 1.0f);
                    }
                    return;
                }
                if (talents.contains("as_assassin_shuriken") && data.getInt("cd_as_assassin_shuriken") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_assassin_shuriken", talents);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("as_assassin_shuriken", "damage", lvl, 4.0);
                        int bleedTicks = AbilityUpgradeConfig.getInt("as_assassin_shuriken", "bleed_ticks", lvl, 80);
                        float bleedDps = (float) AbilityUpgradeConfig.getDouble("as_assassin_shuriken", "bleed_dps", lvl, 1.2);
                        int cd = AbilityUpgradeConfig.getInt("as_assassin_shuriken", "cooldown", lvl, 160);
                        Snowball sb = net.minecraft.world.entity.EntityType.SNOWBALL.create(sl);
                        if (sb == null) {
                            setCooldown(player, "cd_as_assassin_shuriken", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        sb.setOwner(player);
                        sb.setPos(player.getX(), player.getEyeY(), player.getZ());
                        Vec3 dir = player.getLookAngle().normalize();
                        sb.shoot(dir.x, dir.y, dir.z, 1.8f, 0.02f);
                        sb.getPersistentData().putBoolean("lvluping_as_shuriken", true);
                        sb.getPersistentData().putFloat("lvluping_as_shuriken_dmg", dmg);
                        sb.getPersistentData().putInt("lvluping_as_shuriken_bleed_ticks", bleedTicks);
                        sb.getPersistentData().putFloat("lvluping_as_shuriken_bleed_dps", bleedDps);
                        sl.addFreshEntity(sb);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.7f, 1.8f);
                        setCooldown(player, "cd_as_assassin_shuriken", cd);
                    }
                    return;
                }
                if (talents.contains("w_swordmaster_steel_body") && data.getInt("cd_w_swordmaster_steel_body") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_swordmaster_steel_body", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_swordmaster_steel_body", "duration_ticks", lvl, 100);
                        double incomingMult = AbilityUpgradeConfig.getDouble("w_swordmaster_steel_body", "damage_mult_incoming", lvl, 0.5);
                        int cd = AbilityUpgradeConfig.getInt("w_swordmaster_steel_body", "cooldown", lvl, 260);
                        data.putLong(W_SWORDMASTER_STEEL_BODY_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        data.putFloat(W_SWORDMASTER_STEEL_BODY_INCOMING_MULT_KEY, (float) incomingMult);
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 0, false, false));
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                        setCooldown(player, "cd_w_swordmaster_steel_body", cd);
                    }
                    return;
                }
                if (talents.contains("a_hunter_call_nature") && data.getInt("cd_a_hunter_call_nature") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_hunter_call_nature", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_hunter_call_nature", "duration_ticks", lvl, 400);
                        double hp = AbilityUpgradeConfig.getDouble("a_hunter_call_nature", "wolf_hp", lvl, 16.0);
                        int cd = AbilityUpgradeConfig.getInt("a_hunter_call_nature", "cooldown", lvl, 300);
                        var wolf = net.minecraft.world.entity.EntityType.WOLF.create(sl);
                        if (wolf == null) {
                            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_a_hunter_call_nature", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        Vec3 fwd = player.getLookAngle().normalize();
                        Vec3 spawnPos = snapStandingPosition(sl, player, new Vec3(player.getX() + fwd.x * 1.5, player.getY(), player.getZ() + fwd.z * 1.5));
                        wolf.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0);
                        wolf.setOwnerUUID(player.getUUID());
                        if (wolf.getAttribute(Attributes.MAX_HEALTH) != null) {
                            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                            wolf.setHealth((float) hp);
                        }
                        wolf.getPersistentData().putUUID("lvluping_summon_owner", player.getUUID());
                        SummonerHandler.addSummon(sl, player, wolf, sl.getGameTime() + dur);
                        sl.addFreshEntity(wolf);
                        sl.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), SoundEvents.WOLF_AMBIENT, SoundSource.PLAYERS, 0.9f, 1.2f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, wolf.getX(), wolf.getY() + 0.8, wolf.getZ(), 18, 0.4, 0.4, 0.4, 0.02);
                        setCooldown(player, "cd_a_hunter_call_nature", cd);
                    }
                    return;
                }
                if (talents.contains("a_ranger_evasion") && data.getInt("cd_a_ranger_evasion") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ranger_evasion", talents);
                        int cd = AbilityUpgradeConfig.getInt("a_ranger_evasion", "cooldown_ticks", lvl, 200);
                        double dist = AbilityUpgradeConfig.getDouble("a_ranger_evasion", "back_distance", lvl, 5.0);
                        int stacks = data.getInt("lvluping_ranger_evasion_stacks");
                        if (stacks <= 0) {
                            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_a_ranger_evasion", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        stacks--;
                        data.putInt("lvluping_ranger_evasion_stacks", stacks);
                        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown("lvluping_ranger_evasion_stacks", stacks));
                        Vec3 back = player.getLookAngle().normalize().scale(-dist);
                        Vec3 dest = snapStandingPosition(sl, player, player.position().add(back));
                        player.teleportTo(sl, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);
                        sl.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 20, 0.4, 0.6, 0.4, 0.08);
                        setCooldown(player, "cd_a_ranger_evasion", cd);
                    }
                    return;
                }
                if (talents.contains("a_musketeer_incendiary") && data.getInt("cd_a_musketeer_incendiary") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_musketeer_incendiary", talents);
                        int fire = AbilityUpgradeConfig.getInt("a_musketeer_incendiary", "fire_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("a_musketeer_incendiary", "cooldown", lvl, 160);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 11);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, fire);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 200);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                        sl.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
                        setCooldown(player, "cd_a_musketeer_incendiary", cd);
                    }
                    return;
                }
                if (talents.contains("w_paladin_immolation") && data.getInt("cd_w_paladin_immolation") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_paladin_immolation", talents);
                        double radius = AbilityUpgradeConfig.getDouble("w_paladin_immolation", "radius", lvl, 3.0);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("w_paladin_immolation", "damage", lvl, 4.0);
                        double igniteR = AbilityUpgradeConfig.getDouble("w_paladin_immolation", "ignite_radius", lvl, 2.0);
                        int cd = AbilityUpgradeConfig.getInt("w_paladin_immolation", "cooldown", lvl, 160);
                        Vec3 pos = player.position();
                        double r2 = radius * radius;
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius + 1.0))) {
                            if (e == player) continue;
                            double dx = e.getX() - pos.x;
                            double dz = e.getZ() - pos.z;
                            if (dx * dx + dz * dz > r2) continue;
                            e.hurt(player.damageSources().playerAttack(player), dmg);
                            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 60));
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.1f);
                        serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 40, radius * 0.15, 0.1, radius * 0.15, 0.05);
                        CommonEventsHandler.igniteBlocksInHorizontalRadius(serverLevel, pos, igniteR);
                        setCooldown(player, "cd_w_paladin_immolation", cd);
                    }
                    return;
                }
                if (player.isShiftKeyDown() && talents.contains("w_barbarian_frenzy") && data.getInt("cd_w_barbarian_frenzy") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_barbarian_frenzy", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_barbarian_frenzy", "duration_ticks", lvl, 80);
                        double dmgMult = AbilityUpgradeConfig.getDouble("w_barbarian_frenzy", "damage_mult", lvl, 1.3);
                        double inMult = AbilityUpgradeConfig.getDouble("w_barbarian_frenzy", "incoming_damage_mult", lvl, 1.2);
                        double asMult = AbilityUpgradeConfig.getDouble("w_barbarian_frenzy", "attack_speed_mult", lvl, 1.15);
                        int cd = AbilityUpgradeConfig.getInt("w_barbarian_frenzy", "cooldown", lvl, 260);
                        data.putLong(W_BARBARIAN_FRENZY_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        data.putFloat(W_BARBARIAN_FRENZY_DAMAGE_MULT_KEY, (float) dmgMult);
                        data.putFloat(W_BARBARIAN_FRENZY_INCOMING_MULT_KEY, (float) inMult);
                        data.putFloat("lvluping_w_barbarian_frenzy_as_mult", (float) asMult);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.55f);
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 26, 0.5, 0.4, 0.5, 0.05);
                        setCooldown(player, "cd_w_barbarian_frenzy", cd);
                    }
                    return;
                }
                if (talents.contains("w_barbarian_bloodletting") && data.getInt("cd_w_barbarian_bloodletting") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_barbarian_bloodletting", talents);
                        int bleedDur = AbilityUpgradeConfig.getInt("w_barbarian_bloodletting", "bleed_duration_ticks", lvl, 80);
                        float bleedDps = (float) AbilityUpgradeConfig.getDouble("w_barbarian_bloodletting", "bleed_damage_per_sec", lvl, 1.0);
                        int cd = AbilityUpgradeConfig.getInt("w_barbarian_bloodletting", "cooldown", lvl, 160);
                        LivingEntity target = getTargetInFront(player, 6.0, 30.0);
                        if (target != null && target != player && !target.isAlliedTo(player)) {
                            target.getPersistentData().putLong("lvluping_barbarian_bleed_until", serverLevel.getGameTime() + bleedDur);
                            target.getPersistentData().putFloat("lvluping_barbarian_bleed_dps", bleedDps);
                            target.getPersistentData().putUUID("lvluping_barbarian_bleed_src", player.getUUID());
                            target.addEffect(new MobEffectInstance(MobEffects.WITHER, bleedDur, 0, false, false));
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 0.8f);
                            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.3, 0.3, 0.08);
                            setCooldown(player, "cd_w_barbarian_bloodletting", cd);
                        } else {
                            setCooldown(player, "cd_w_barbarian_bloodletting", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }

                // --- M_SUMMON_SACRIFICE ---
                if (talents.contains("m_summon_sacrifice") && data.getInt("cd_m_sacrifice") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_sacrifice", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_summon_sacrifice", "mana", lvl, 10.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_sacrifice", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        LivingEntity looked = getTargetInFront(player, M_SUMMON_SACRIFICE_AIM_RANGE_XZ, M_SUMMON_SACRIFICE_AIM_CONE_DEG);
                        Mob mob = (looked instanceof Mob m) ? m : null;
                        if (mob == null || !mob.getPersistentData().hasUUID("lvluping_summon_owner") || !player.getUUID().equals(mob.getPersistentData().getUUID("lvluping_summon_owner"))) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_sacrifice", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        mob.discard();
                        float heal = (float) AbilityUpgradeConfig.getDouble("m_summon_sacrifice", "heal", lvl, 5.0);
                        player.heal(heal);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.6f);
                        serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.4, 0.5, 0.4, 0.1);
                        int cd = AbilityUpgradeConfig.getInt("m_summon_sacrifice", "cooldown", lvl, M_SACRIFICE_COOLDOWN);
                        setCooldown(player, "cd_m_sacrifice", cd);
                    }
                    return;
                }

                // --- M_LIGHTNING ---
                if (talents.contains("m_lightning") && data.getInt("cd_m_lightning") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_lightning", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_lightning", "mana", lvl, 45.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_lightning", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int maxTargets = AbilityUpgradeConfig.getInt("m_lightning", "targets", lvl, 1);
                        var targets = getLightningTargets(player, serverLevel, maxTargets, M_LIGHTNING_AIM_RANGE_XZ, M_LIGHTNING_AIM_CONE_DEG);
                        if (targets.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_lightning", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        float dmg = (float) AbilityUpgradeConfig.getDouble("m_lightning", "damage", lvl, 1.0);
                        for (LivingEntity target : targets) {
                            var bolt = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, serverLevel);
                            bolt.moveTo(target.getX(), target.getY(), target.getZ());
                            bolt.setVisualOnly(true);
                            serverLevel.addFreshEntity(bolt);
                            target.hurt(player.damageSources().playerAttack(player), dmg);
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 25, 0.4, 0.6, 0.4, 0.15);
                        }
                        int cd = AbilityUpgradeConfig.getInt("m_lightning", "cooldown", lvl, M_FIRE_COOLDOWN);
                        setCooldown(player, "cd_m_lightning", cd);
                    }
                    return;
                }

                // --- M_CLERIC_BLESSING ---
                if (talents.contains("m_cleric_blessing") && data.getInt("cd_m_cleric_blessing") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_blessing", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_cleric_blessing", "mana", lvl, 25.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_cleric_blessing", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        LivingEntity target = getTargetInFront(player, M_CLERIC_BLESSING_AIM_RANGE_XZ, M_CLERIC_BLESSING_AIM_CONE_DEG);
                        if (target == null) target = player;
                        if (!target.isAlliedTo(player)) target = player;

                        int dur = AbilityUpgradeConfig.getInt("m_cleric_blessing", "duration_ticks", lvl, 100);
                        int cleanses = AbilityUpgradeConfig.getInt("m_cleric_blessing", "cleanses", lvl, 1);
                        double bonusPct = AbilityUpgradeConfig.getDouble("m_cleric_blessing", "damage_bonus_pct", lvl, 10.0);
                        float dmgMult = (float) (1.0 + bonusPct / 100.0);
                        long gameTime = serverLevel.getGameTime();
                        target.getPersistentData().putLong("lvluping_blessing_damage_until", gameTime + dur);
                        target.getPersistentData().putFloat("lvluping_blessing_damage_mult", dmgMult);

                        int removed = 0;
                        for (MobEffectInstance inst : target.getActiveEffects()) {
                            if (inst == null || inst.getEffect() == null) continue;
                            MobEffectCategory cat = inst.getEffect().value().getCategory();
                            if (cat == MobEffectCategory.HARMFUL) {
                                target.removeEffect(inst.getEffect());
                                removed++;
                                if (removed >= cleanses) break;
                            }
                        }

                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);

                        int cd = AbilityUpgradeConfig.getInt("m_cleric_blessing", "cooldown", lvl, M_CLERIC_BLESSING_COOLDOWN);
                        setCooldown(player, "cd_m_cleric_blessing", cd);
                    }
                    return;
                }
            }
            case 2 -> {
                if (talents.contains("as_slide") && data.getInt("cd_slide") <= 0) {
                    int maxCh = getSlideMaxCharges(player, talents);
                    int ch = data.getInt(LVLUPING_SLIDE_CHARGES_KEY);
                    if (ch <= 0) ch = maxCh;
                    if (ch <= 0) return;
                    ch--;
                    data.putInt(LVLUPING_SLIDE_CHARGES_KEY, ch);
                    PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(LVLUPING_SLIDE_CHARGES_KEY, ch));
                    Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(look.x * SLIDE_DELTA_MULT_XZ, 0, look.z * SLIDE_DELTA_MULT_XZ);
                    player.hurtMarked = true;
                    if (ch <= 0) {
                        setCooldown(player, "cd_slide", SLIDE_COOLDOWN);
                    }
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);
                    return;
                }
                if (talents.contains("a_musketeer_aimed_shot") && data.getInt("cd_a_musketeer_aimed_shot") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_musketeer_aimed_shot", talents);
                        float mult = (float) AbilityUpgradeConfig.getDouble("a_musketeer_aimed_shot", "damage_mult", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("a_musketeer_aimed_shot", "cooldown", lvl, 160);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 12);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, mult);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 200);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
                        sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.35, 0.45, 0.35, 0.12);
                        setCooldown(player, "cd_a_musketeer_aimed_shot", cd);
                    }
                    return;
                }
                // --- W_SPIN ---
                if (talents.contains("w_spin") && data.getInt("cd_w_spin") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_spin", talents);
                        double range = AbilityUpgradeConfig.getDouble("w_spin", "range", lvl, SPIN_RANGE);
                        int baseCd = AbilityUpgradeConfig.getInt("w_spin", "cooldown", lvl, SPIN_COOLDOWN);
                        int halfCdHits = AbilityUpgradeConfig.getInt("w_spin", "half_cd_hits", lvl, SPIN_HALF_CD_MIN_HITCOUNT);
                        int hitCount = 0;

                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range, SPIN_HITBOX_Y_THICKNESS, range))) {
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

                        int cd = baseCd;
                        if (hitCount >= halfCdHits) cd = Math.max(1, baseCd / 2);
                        setCooldown(player, "cd_w_spin", cd);
                    }
                    return;
                }

                // --- M_ICE_ARROW ---
                if (talents.contains("m_ice_arrow") && data.getInt("cd_m_ice") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ice_arrow", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ice_arrow", "mana", lvl, 25.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ice", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        LivingEntity target = getTargetInFront(player, M_ICE_ARROW_AIM_RANGE_XZ, M_ICE_ARROW_AIM_CONE_DEG);
                        int slowTicks = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_ticks", lvl, 80);
                        int slowAmp = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_amp", lvl, 3);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("m_ice_arrow", "damage", lvl, 6.0);
                        Snowball snowball = net.minecraft.world.entity.EntityType.SNOWBALL.create(serverLevel);
                        if (snowball != null) {
                            snowball.setOwner(player);
                            snowball.setPos(player.getX(), player.getEyeY() + PROJECTILE_SPAWN_OFFSET_EYE_Y, player.getZ());
                            Vec3 dir = target != null
                                    ? target.getEyePosition().subtract(snowball.position()).normalize()
                                    : player.getLookAngle().normalize();
                            snowball.shoot(dir.x, dir.y, dir.z, ICE_ARROW_SHOOT_SPEED, ICE_ARROW_SHOOT_INACCURACY);
                            snowball.getPersistentData().putBoolean("lvluping_ice_projectile", true);
                            snowball.getPersistentData().putFloat("lvluping_ice_damage", dmg);
                            snowball.getPersistentData().putInt("lvluping_ice_slow_ticks", slowTicks);
                            snowball.getPersistentData().putInt("lvluping_ice_slow_amp", slowAmp);
                            serverLevel.addFreshEntity(snowball);
                        }
                        int cd = AbilityUpgradeConfig.getInt("m_ice_arrow", "cooldown", lvl, M_ICE_COOLDOWN);
                        setCooldown(player, "cd_m_ice", cd);
                    }
                    return;
                }

                // --- W_SEISMIC ---
                if (talents.contains("w_seismic") && data.getInt("cd_w_seismic") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_seismic", talents);
                        Vec3 look = player.getLookAngle().normalize();
                        double range = AbilityUpgradeConfig.getDouble("w_seismic", "range", lvl, SEISMIC_RANGE);
                        double coneHalfAngleRad = Math.toRadians(SEISMIC_CONE_HALF_ANGLE_DEG);
                        double angleCos = Math.cos(coneHalfAngleRad);

                        float dmgMult = (float) AbilityUpgradeConfig.getDouble("w_seismic", "damage_mult", lvl, SEISMIC_DAMAGE_MULT);
                        float dmgBonus = (float) AbilityUpgradeConfig.getDouble("w_seismic", "damage_bonus", lvl, SEISMIC_DAMAGE_BONUS);
                        int slowTicks = AbilityUpgradeConfig.getInt("w_seismic", "slow_ticks", lvl, SEISMIC_SLOW_DURATION_TICKS);
                        int slowAmp = AbilityUpgradeConfig.getInt("w_seismic", "slow_amp", lvl, SEISMIC_SLOW_AMPLIFIER);
                        int cd = AbilityUpgradeConfig.getInt("w_seismic", "cooldown", lvl, SEISMIC_COOLDOWN);
                        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * dmgMult + dmgBonus;
                        Vec3 playerPos = player.position();

                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(range, SEISMIC_HITBOX_Y_THICKNESS, range))) {
                            if (e == player) continue;
                            Vec3 to = e.position().subtract(playerPos).normalize();
                            if (look.dot(to) > angleCos && player.distanceTo(e) <= range) {
                                e.hurt(player.damageSources().playerAttack(player), baseDamage);
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmp, false, false));
                                Vec3 knockback = to.scale(SEISMIC_KNOCKBACK_SCALE).add(0, SEISMIC_KNOCKBACK_Y, 0);
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
                        setCooldown(player, "cd_w_seismic", cd);
                    }
            return;
        }

        // --- A_DASH ---
        if (talents.contains("a_dash") && data.getInt("cd_dash") <= 0) {
            Vec3 look = player.getLookAngle();
                    player.setDeltaMovement(-look.x * DASH_DELTA_BACK_MULT, 0, -look.z * DASH_DELTA_BACK_MULT);
            player.hurtMarked = true;
            setCooldown(player, "cd_dash", DASH_COOLDOWN);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 2.0f);
            return;
        }

                // --- M_SUMMON ---
                if (talents.contains("m_summon_command") && data.getInt("cd_m_command") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_command", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_summon_command", "mana", lvl, 5.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        LivingEntity target = getTargetInFront(player, M_SUMMON_COMMAND_AIM_RANGE_XZ, M_SUMMON_COMMAND_AIM_CONE_DEG);
                        if (target == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        if (summons.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_command", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        for (Mob m : summons) {
                            m.setTarget(target);
                        }
                        SummonerHandler.setCommandTarget(player, target, 0);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.7f, 1.4f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.4, 0.6, 0.4, 0.12);
                        int cd = AbilityUpgradeConfig.getInt("m_summon_command", "cooldown", lvl, M_COMMAND_COOLDOWN);
                        setCooldown(player, "cd_m_command", cd);
                    }
                    return;
                }

                // --- M_CLERIC_LIGHT ---
                if (talents.contains("m_cleric_light") && data.getInt("cd_m_cleric_light") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_cleric_light", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_cleric_light", "mana", lvl, 30.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_cleric_light", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        LivingEntity lookTarget = getTargetInFront(player, M_CLERIC_LIGHT_AIM_RANGE_XZ, M_CLERIC_LIGHT_AIM_CONE_DEG);
                        Vec3 center = (lookTarget != null)
                                ? lookTarget.position()
                                : getLookPointOnBlock(serverLevel, player, LOOK_POINT_FALLBACK_RANGE);

                        int dur = AbilityUpgradeConfig.getInt("m_cleric_light", "duration_ticks", lvl, 120);
                        long until = serverLevel.getGameTime() + dur;

                        double radius = AbilityUpgradeConfig.getDouble("m_cleric_light", "radius", lvl, 6.0);
                        float healPulse = (float) AbilityUpgradeConfig.getDouble("m_cleric_light", "heal", lvl, 2.5);
                        float dmgPulse = (float) AbilityUpgradeConfig.getDouble("m_cleric_light", "damage", lvl, 5.0);

                        player.getPersistentData().putLong("lvluping_c_cleric_light_until", until);
                        player.getPersistentData().putDouble("lvluping_c_cleric_light_cx", center.x);
                        player.getPersistentData().putDouble("lvluping_c_cleric_light_cy", center.y);
                        player.getPersistentData().putDouble("lvluping_c_cleric_light_cz", center.z);
                        player.getPersistentData().putDouble("lvluping_c_cleric_light_radius", radius);
                        player.getPersistentData().putFloat("lvluping_c_cleric_light_heal", healPulse);
                        player.getPersistentData().putFloat("lvluping_c_cleric_light_damage", dmgPulse);

                        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, 60, radius * 0.2, radius * 0.2, radius * 0.2, 0.05);
                        int cd = AbilityUpgradeConfig.getInt("m_cleric_light", "cooldown", lvl, M_CLERIC_LIGHT_COOLDOWN);
                        setCooldown(player, "cd_m_cleric_light", cd);
                    }
                    return;
                }
            }
            case 3 -> {
                if (talents.contains("as_smoke") && data.getInt("cd_smoke") <= 0) {
                    ServerLevel level = player.serverLevel();
                    int invisTicks = SMOKE_EFFECT_DURATION_TICKS;
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisTicks, 0, false, false));
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
                    if (talents.contains("as_rogue_time_thief")) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_rogue_time_thief", talents);
                        int cdr = AbilityUpgradeConfig.getInt("as_rogue_time_thief", "cdr_ticks", lvl, 20);
                        String[] keys = {
                                "cd_as_rogue_strong_poison","cd_as_rogue_trip","cd_as_rogue_blind",
                                "cd_as_wanderer_barricade","cd_as_wanderer_climb","cd_as_wanderer_tripwire",
                                "cd_as_assassin_mark","cd_as_assassin_shuriken","cd_as_assassin_rupture"
                        };
                        for (String k : keys) {
                            int v = Math.max(0, player.getPersistentData().getInt(k) - cdr);
                            player.getPersistentData().putInt(k, v);
                            PlayerLevels.setCooldown(player.getUUID(), k, v);
                            PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(k, v));
                        }
                    }
                    return;
                }
                if (talents.contains("a_hunter_net") && data.getInt("cd_a_hunter_net") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_hunter_net", talents);
                        double r = AbilityUpgradeConfig.getDouble("a_hunter_net", "radius", lvl, 2.5);
                        int root = AbilityUpgradeConfig.getInt("a_hunter_net", "root_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("a_hunter_net", "cooldown", lvl, 220);
                        Snowball sb = net.minecraft.world.entity.EntityType.SNOWBALL.create(sl);
                        if (sb == null) {
                            setCooldown(player, "cd_a_hunter_net", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        sb.setOwner(player);
                        sb.setPos(player.getX(), player.getEyeY(), player.getZ());
                        Vec3 dir = player.getLookAngle().normalize();
                        sb.shoot(dir.x, dir.y, dir.z, 1.7f, 0.02f);
                        sb.getPersistentData().putBoolean("lvluping_hunter_net_projectile", true);
                        sb.getPersistentData().putFloat("lvluping_hunter_net_radius", (float) r);
                        sb.getPersistentData().putInt("lvluping_hunter_net_root", root);
                        sl.addFreshEntity(sb);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.1f);
                        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.35, 0.35, 0.35, 0.02);
                        setCooldown(player, "cd_a_hunter_net", cd);
                    }
                    return;
                }
                if (talents.contains("a_ranger_thorn_bush") && data.getInt("cd_a_ranger_thorn_bush") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ranger_thorn_bush", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_ranger_thorn_bush", "duration_ticks", lvl, 120);
                        double r = AbilityUpgradeConfig.getDouble("a_ranger_thorn_bush", "radius", lvl, 3.0);
                        float dps = (float) AbilityUpgradeConfig.getDouble("a_ranger_thorn_bush", "dps", lvl, 1.0);
                        int slowAmp = AbilityUpgradeConfig.getInt("a_ranger_thorn_bush", "slow_amp", lvl, 1);
                        int cd = AbilityUpgradeConfig.getInt("a_ranger_thorn_bush", "cooldown", lvl, 240);
                        Vec3 pos = getLookPointOnBlock(sl, player, 14.0);
                        data.putLong(A_RANGER_THORN_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(A_RANGER_THORN_X_KEY, pos.x);
                        data.putDouble(A_RANGER_THORN_Y_KEY, pos.y);
                        data.putDouble(A_RANGER_THORN_Z_KEY, pos.z);
                        data.putDouble(A_RANGER_THORN_R_KEY, r);
                        data.putFloat(A_RANGER_THORN_DPS_KEY, dps);
                        data.putInt(A_RANGER_THORN_SLOW_AMP_KEY, slowAmp);
                        UUID thornVid = UUID.randomUUID();
                        data.putUUID(A_RANGER_THORN_VISUAL_KEY, thornVid);
                        broadcastRangerThornShow(sl, thornVid, pos.x, pos.y, pos.z, player.getYRot(), r, data.getLong(A_RANGER_THORN_UNTIL_KEY));
                        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 0.6, pos.z, 25, r * 0.3, 0.2, r * 0.3, 0.02);
                        setCooldown(player, "cd_a_ranger_thorn_bush", cd);
                    }
                    return;
                }
                if (talents.contains("a_musketeer_holster") && data.getInt("cd_a_musketeer_holster") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_musketeer_holster", talents);
                        int cd = AbilityUpgradeConfig.getInt("a_musketeer_holster", "cooldown", lvl, 200);
                        Vec3 dir = player.getLookAngle().normalize();
                        for (int i = 0; i < 2; i++) {
                            var a = net.minecraft.world.entity.EntityType.ARROW.create(sl);
                            if (a == null) continue;
                            a.setOwner(player);
                            a.setPos(player.getX(), player.getEyeY() + PROJECTILE_SPAWN_OFFSET_EYE_Y, player.getZ());
                            float yawOff = (i == 0 ? -3.0f : 3.0f);
                            Vec3 sdir = dir.yRot((float) Math.toRadians(yawOff)).normalize();
                            a.shoot(sdir.x, sdir.y, sdir.z, 3.0f, 0.0f);
                            sl.addFreshEntity(a);
                        }
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.9f, 1.2f);
                        sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.35, 0.45, 0.35, 0.12);
                        setCooldown(player, "cd_a_musketeer_holster", cd);
                    }
                    return;
                }
                if (talents.contains("w_provocation") && data.getInt("cd_w_provocation") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_provocation", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_provocation", "duration_ticks", lvl, PROVOCATION_DURATION_TICKS);
                        int cd = AbilityUpgradeConfig.getInt("w_provocation", "cooldown", lvl, PROVOCATION_COOLDOWN);
                        long until = serverLevel.getGameTime() + dur;
                        player.getPersistentData().putLong("lvluping_provocation_until", until);
                        double absRatio = AbilityUpgradeConfig.getDouble("w_provocation", "absorption_max_hp_ratio", lvl, 0.2);
                        float beforeAbs = player.getAbsorptionAmount();
                        float absorbWant = (float) (player.getMaxHealth() * absRatio);
                        player.setAbsorptionAmount(Math.max(beforeAbs, absorbWant));
                        data.putFloat("lvluping_prov_absorb_before", beforeAbs);
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, W_PROVOCATION_GLOWING_AMPLIFIER, false, false));
                        org.mrutcka.lvluping.handler.ProvocationHandler.setProvokerTeam(player, true);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.7f);
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                        setCooldown(player, "cd_w_provocation", cd);
                        for (ServerPlayer p : serverLevel.players()) {
                            if (p != player) PacketDistributor.sendToPlayer(p, new S2CProvocationHint(true));
                        }
                    }
                    return;
                }
                // --- W_IRON_SKIN ---
                if (talents.contains("w_iron_skin") && data.getInt("cd_w_iron_skin") <= 0) {
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.BLINDNESS);
                    player.removeEffect(MobEffects.WEAKNESS);

                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, IRON_SKIN_EFFECT_DURATION_TICKS, IRON_SKIN_RESISTANCE_AMPLIFIER, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, IRON_SKIN_EFFECT_DURATION_TICKS, IRON_SKIN_SLOWDOWN_AMPLIFIER, false, false));

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0f, 0.5f);
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                                20, 0.4, 0.6, 0.4, 0.02);
                    }

                    setCooldown(player, "cd_w_iron_skin", IRON_SKIN_COOLDOWN);
                    return;
                }

                // --- M_TELEPORT ---
                if (talents.contains("m_teleport") && data.getInt("cd_m_teleport") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_teleport", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_teleport", "mana", lvl, 40.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_teleport", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        Vec3 look = player.getLookAngle().normalize();
                        double dist = AbilityUpgradeConfig.getDouble("m_teleport", "distance", lvl, 7.0);
                        Vec3 start = player.position();
                        Vec3 dest = start.add(look.scale(dist));
                        for (int i = 0; i < TELEPORT_NO_COLLISION_ATTEMPTS; i++) {
                            if (serverLevel.noCollision(player, player.getBoundingBox().move(dest.x - start.x, dest.y - start.y, dest.z - start.z))) break;
                            dist -= TELEPORT_BACKTRACK_STEP;
                            dest = start.add(look.scale(dist));
                        }
                        dest = snapStandingPosition(serverLevel, player, dest);
                        player.teleportTo(serverLevel, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
                        player.resetFallDistance();
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                        serverLevel.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 30, 0.5, 0.8, 0.5, 0.1);
                        int cd = AbilityUpgradeConfig.getInt("m_teleport", "cooldown", lvl, M_TELEPORT_COOLDOWN);
                        setCooldown(player, "cd_m_teleport", cd);
                    }
                    return;
                }
            }
            case 4 -> {
                if (talents.contains("as_ult_rogue_perfect_kill") && data.getInt("cd_as_ult_rogue_perfect_kill") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_rogue_perfect_kill", talents);
                        int invis = AbilityUpgradeConfig.getInt("as_ult_rogue_perfect_kill", "invis_ticks", lvl, 100);
                        float mult = (float) AbilityUpgradeConfig.getDouble("as_ult_rogue_perfect_kill", "next_hit_mult", lvl, 2.0);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_rogue_perfect_kill", "cooldown", lvl, 900);
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invis, 0, false, false));
                        data.putInt(AS_NEXT_HIT_EFFECT_KEY, 2);
                        data.putFloat(AS_NEXT_HIT_P1_KEY, mult);
                        data.putLong(AS_NEXT_HIT_UNTIL_KEY, sl.getGameTime() + invis);
                        setCooldown(player, "cd_as_ult_rogue_perfect_kill", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_rogue_poison_veil") && data.getInt("cd_as_ult_rogue_poison_veil") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_rogue_poison_veil", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_rogue_poison_veil", "duration_ticks", lvl, 100);
                        double r = AbilityUpgradeConfig.getDouble("as_ult_rogue_poison_veil", "radius", lvl, 4.0);
                        float dps = (float) AbilityUpgradeConfig.getDouble("as_ult_rogue_poison_veil", "dps", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_rogue_poison_veil", "cooldown", lvl, 900);
                        data.putLong(AS_ROGUE_POISON_VEIL_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(AS_ROGUE_POISON_VEIL_X_KEY, player.getX());
                        data.putDouble(AS_ROGUE_POISON_VEIL_Y_KEY, player.getY());
                        data.putDouble(AS_ROGUE_POISON_VEIL_Z_KEY, player.getZ());
                        data.putDouble(AS_ROGUE_POISON_VEIL_R_KEY, r);
                        data.putFloat(AS_ROGUE_POISON_VEIL_DPS_KEY, dps);
                        setCooldown(player, "cd_as_ult_rogue_poison_veil", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_rogue_confusion") && data.getInt("cd_as_ult_rogue_confusion") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 10.0, 30.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_rogue_confusion", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_rogue_confusion", "duration_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_rogue_confusion", "cooldown", lvl, 900);
                        if (t != null && t instanceof net.minecraft.world.entity.Mob mob) {
                            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2, false, false));
                            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 2, false, false));
                            setCooldown(player, "cd_as_ult_rogue_confusion", cd);
                        } else {
                            setCooldown(player, "cd_as_ult_rogue_confusion", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("as_ult_rogue_vanish") && data.getInt("cd_as_ult_rogue_vanish") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_rogue_vanish", talents);
                        double r = AbilityUpgradeConfig.getDouble("as_ult_rogue_vanish", "radius", lvl, 10.0);
                        int invis = AbilityUpgradeConfig.getInt("as_ult_rogue_vanish", "invis_ticks", lvl, 100);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_rogue_vanish", "cooldown", lvl, 900);
                        double nx = player.getX() + (sl.random.nextDouble() * 2 - 1) * r;
                        double nz = player.getZ() + (sl.random.nextDouble() * 2 - 1) * r;
                        Vec3 dest = snapStandingPosition(sl, player, new Vec3(nx, player.getY(), nz));
                        player.teleportTo(sl, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invis, 0, false, false));
                        setCooldown(player, "cd_as_ult_rogue_vanish", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_wanderer_camp") && data.getInt("cd_as_ult_wanderer_camp") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_wanderer_camp", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_wanderer_camp", "duration_ticks", lvl, 200);
                        double r = AbilityUpgradeConfig.getDouble("as_ult_wanderer_camp", "radius", lvl, 4.0);
                        float hps = (float) AbilityUpgradeConfig.getDouble("as_ult_wanderer_camp", "heal_per_sec", lvl, 1.0);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_wanderer_camp", "cooldown", lvl, 1000);
                        data.putLong(AS_WANDERER_CAMP_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(AS_WANDERER_CAMP_X_KEY, player.getX());
                        data.putDouble(AS_WANDERER_CAMP_Y_KEY, player.getY());
                        data.putDouble(AS_WANDERER_CAMP_Z_KEY, player.getZ());
                        data.putDouble(AS_WANDERER_CAMP_R_KEY, r);
                        data.putFloat(AS_WANDERER_CAMP_HPS_KEY, hps);
                        UUID vid = UUID.randomUUID();
                        data.putUUID(AS_WANDERER_CAMP_VISUAL_KEY, vid);
                        broadcastAssassinCampShow(sl, vid, player.getX(), player.getY(), player.getZ(), player.getYRot(), data.getLong(AS_WANDERER_CAMP_UNTIL_KEY));
                        setCooldown(player, "cd_as_ult_wanderer_camp", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_wanderer_dagger_rain") && data.getInt("cd_as_ult_wanderer_dagger_rain") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_wanderer_dagger_rain", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_wanderer_dagger_rain", "duration_ticks", lvl, 80);
                        int shots = AbilityUpgradeConfig.getInt("as_ult_wanderer_dagger_rain", "shots", lvl, 10);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_wanderer_dagger_rain", "cooldown", lvl, 1000);
                        data.putLong(AS_WANDERER_DAGGER_RAIN_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putInt(AS_WANDERER_DAGGER_RAIN_SHOTS_KEY, shots);
                        setCooldown(player, "cd_as_ult_wanderer_dagger_rain", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_wanderer_thorn_trail") && data.getInt("cd_as_ult_wanderer_thorn_trail") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_wanderer_thorn_trail", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_wanderer_thorn_trail", "duration_ticks", lvl, 120);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_wanderer_thorn_trail", "cooldown", lvl, 1000);
                        data.putLong(AS_WANDERER_THORN_TRAIL_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putFloat("lvluping_as_wanderer_thorn_trail_dps", (float) AbilityUpgradeConfig.getDouble("as_ult_wanderer_thorn_trail", "dps", lvl, 1.0));
                        setCooldown(player, "cd_as_ult_wanderer_thorn_trail", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_wanderer_ghosts") && data.getInt("cd_as_ult_wanderer_ghosts") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_wanderer_ghosts", talents);
                        int count = AbilityUpgradeConfig.getInt("as_ult_wanderer_ghosts", "count", lvl, 2);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_wanderer_ghosts", "duration_ticks", lvl, 160);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_wanderer_ghosts", "cooldown", lvl, 1000);
                        for (int i = 0; i < count; i++) {
                            var vex = net.minecraft.world.entity.EntityType.VEX.create(sl);
                            if (vex == null) continue;
                            vex.moveTo(player.getX() + sl.random.nextDouble() * 2 - 1, player.getY() + 1.0, player.getZ() + sl.random.nextDouble() * 2 - 1, player.getYRot(), 0);
                            vex.getPersistentData().putUUID("lvluping_summon_owner", player.getUUID());
                            sl.addFreshEntity(vex);
                            SummonerHandler.addSummon(sl, player, vex, sl.getGameTime() + dur);
                        }
                        setCooldown(player, "cd_as_ult_wanderer_ghosts", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_assassin_blade_dance") && data.getInt("cd_as_ult_assassin_blade_dance") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 7.0, 25.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_assassin_blade_dance", talents);
                        int hits = AbilityUpgradeConfig.getInt("as_ult_assassin_blade_dance", "hits", lvl, 4);
                        float mult = (float) AbilityUpgradeConfig.getDouble("as_ult_assassin_blade_dance", "hit_mult", lvl, 0.7);
                        int bleedTicks = AbilityUpgradeConfig.getInt("as_ult_assassin_blade_dance", "bleed_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_assassin_blade_dance", "cooldown", lvl, 1000);
                        if (t != null && t != player) {
                            data.putLong("lvluping_as_blade_dance_until", sl.getGameTime() + Math.max(40, hits * 3L));
                            data.putUUID("lvluping_as_blade_dance_target", t.getUUID());
                            data.putInt("lvluping_as_blade_dance_hits_left", Math.max(1, hits));
                            data.putFloat("lvluping_as_blade_dance_mult", mult);
                            data.putInt("lvluping_as_blade_dance_bleed_ticks", bleedTicks);
                            data.putLong("lvluping_as_blade_dance_next_hit_at", sl.getGameTime());
                            setCooldown(player, "cd_as_ult_assassin_blade_dance", cd);
                        } else {
                            setCooldown(player, "cd_as_ult_assassin_blade_dance", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("as_ult_assassin_immobilize") && data.getInt("cd_as_ult_assassin_immobilize") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 12.0, 25.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_assassin_immobilize", talents);
                        int root = AbilityUpgradeConfig.getInt("as_ult_assassin_immobilize", "root_ticks", lvl, 40);
                        float mult = (float) AbilityUpgradeConfig.getDouble("as_ult_assassin_immobilize", "bonus_mult", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_assassin_immobilize", "cooldown", lvl, 1000);
                        if (t != null && t != player) {
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 9, false, false));
                            Vec3 look = player.getLookAngle().normalize();
                            Vec3 dest = new Vec3(player.getX() + look.x * 1.2, player.getY(), player.getZ() + look.z * 1.2);
                            if (t instanceof ServerPlayer tsp) {
                                tsp.teleportTo(sl, dest.x, dest.y, dest.z, tsp.getYRot(), tsp.getXRot());
                            } else {
                                t.teleportTo(dest.x, dest.y, dest.z);
                            }
                            t.setDeltaMovement(Vec3.ZERO);
                            t.hurtMarked = true;
                            t.hurt(player.damageSources().playerAttack(player), (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult);
                            setCooldown(player, "cd_as_ult_assassin_immobilize", cd);
                        } else {
                            setCooldown(player, "cd_as_ult_assassin_immobilize", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("as_ult_assassin_black_mist") && data.getInt("cd_as_ult_assassin_black_mist") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_assassin_black_mist", talents);
                        int dur = AbilityUpgradeConfig.getInt("as_ult_assassin_black_mist", "duration_ticks", lvl, 100);
                        double r = AbilityUpgradeConfig.getDouble("as_ult_assassin_black_mist", "radius", lvl, 4.0);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_assassin_black_mist", "cooldown", lvl, 1000);
                        data.putLong(AS_ASSASSIN_BLACK_MIST_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(AS_ASSASSIN_BLACK_MIST_X_KEY, player.getX());
                        data.putDouble(AS_ASSASSIN_BLACK_MIST_Y_KEY, player.getY());
                        data.putDouble(AS_ASSASSIN_BLACK_MIST_Z_KEY, player.getZ());
                        data.putDouble(AS_ASSASSIN_BLACK_MIST_R_KEY, r);
                        setCooldown(player, "cd_as_ult_assassin_black_mist", cd);
                    }
                    return;
                }
                if (talents.contains("as_ult_assassin_double") && data.getInt("cd_as_ult_assassin_double") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_ult_assassin_double", talents);
                        int invis = AbilityUpgradeConfig.getInt("as_ult_assassin_double", "invis_ticks", lvl, 100);
                        int shadow = AbilityUpgradeConfig.getInt("as_ult_assassin_double", "shadow_ticks", lvl, 100);
                        double r = AbilityUpgradeConfig.getDouble("as_ult_assassin_double", "explosion_radius", lvl, 3.0);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("as_ult_assassin_double", "explosion_damage", lvl, 6.0);
                        int bleedTicks = AbilityUpgradeConfig.getInt("as_ult_assassin_double", "bleed_ticks", lvl, 80);
                        float bleedDps = (float) AbilityUpgradeConfig.getDouble("as_ult_assassin_double", "bleed_dps", lvl, 1.2);
                        int cd = AbilityUpgradeConfig.getInt("as_ult_assassin_double", "cooldown", lvl, 1000);

                        var clone = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(sl);
                        if (clone != null) {
                            clone.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                            clone.setCustomName(net.minecraft.network.chat.Component.literal("lvluping_clone:" + player.getUUID()));
                            clone.setCustomNameVisible(false);
                            clone.getPersistentData().putBoolean("lvluping_as_double_clone", true);
                            clone.getPersistentData().putUUID("lvluping_as_double_owner", player.getUUID());
                            clone.getPersistentData().putLong("lvluping_as_double_explode_at", sl.getGameTime() + shadow);
                            clone.getPersistentData().putDouble("lvluping_as_double_r", r);
                            clone.getPersistentData().putFloat("lvluping_as_double_dmg", dmg);
                            clone.getPersistentData().putInt("lvluping_as_double_bleed_ticks", bleedTicks);
                            clone.getPersistentData().putFloat("lvluping_as_double_bleed_dps", bleedDps);
                            clone.setInvisible(true);
                            sl.addFreshEntity(clone);
                            sl.sendParticles(ParticleTypes.SMOKE, clone.getX(), clone.getY() + 1.0, clone.getZ(), 18, 0.35, 0.55, 0.35, 0.02);
                        }
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invis, 0, false, false));
                        setCooldown(player, "cd_as_ult_assassin_double", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_hunter_track") && data.getInt("cd_a_ult_hunter_track") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 40.0, 20.0);
                        if (t == null || t == player) {
                            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_a_ult_hunter_track", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_hunter_track", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_ult_hunter_track", "duration_ticks", lvl, 300);
                        float mult = (float) AbilityUpgradeConfig.getDouble("a_ult_hunter_track", "damage_mult", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_hunter_track", "cooldown", lvl, 900);
                        data.putLong(A_HUNTER_TRACK_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putUUID(A_HUNTER_TRACK_TARGET_KEY, t.getUUID());
                        data.putFloat("lvluping_a_hunter_track_mult", mult);
                        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
                        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 0.8f, 1.5f);
                        sl.sendParticles(ParticleTypes.ENCHANT, t.getX(), t.getY() + 1.0, t.getZ(), 30, 0.4, 0.6, 0.4, 0.12);
                        setCooldown(player, "cd_a_ult_hunter_track", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_hunter_pack") && data.getInt("cd_a_ult_hunter_pack") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_hunter_pack", talents);
                        int count = AbilityUpgradeConfig.getInt("a_ult_hunter_pack", "count", lvl, 3);
                        int dur = AbilityUpgradeConfig.getInt("a_ult_hunter_pack", "duration_ticks", lvl, 400);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_hunter_pack", "cooldown", lvl, 900);
                        for (int i = 0; i < count; i++) {
                            var wolf = net.minecraft.world.entity.EntityType.WOLF.create(sl);
                            if (wolf == null) continue;
                            double ang = (Math.PI * 2) * (i / (double) Math.max(1, count));
                            Vec3 spawnPos = snapStandingPosition(sl, player, new Vec3(player.getX() + Math.cos(ang) * 1.6, player.getY(), player.getZ() + Math.sin(ang) * 1.6));
                            wolf.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0);
                            wolf.setOwnerUUID(player.getUUID());
                            wolf.getPersistentData().putUUID("lvluping_summon_owner", player.getUUID());
                            SummonerHandler.addSummon(sl, player, wolf, sl.getGameTime() + dur);
                            sl.addFreshEntity(wolf);
                        }
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.0f, 1.0f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 40, 0.9, 0.6, 0.9, 0.02);
                        setCooldown(player, "cd_a_ult_hunter_pack", cd);
                    }
                    return;
                }
                

                if (talents.contains("a_ult_ranger_merge") && data.getInt("cd_a_ult_ranger_merge") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_ranger_merge", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_ult_ranger_merge", "duration_ticks", lvl, 100);
                        float hps = (float) AbilityUpgradeConfig.getDouble("a_ult_ranger_merge", "heal_per_sec", lvl, 1.0);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_ranger_merge", "cooldown", lvl, 1000);
                        data.putLong(A_RANGER_MERGE_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(A_RANGER_MERGE_AX_KEY, player.getX());
                        data.putDouble(A_RANGER_MERGE_AY_KEY, player.getY());
                        data.putDouble(A_RANGER_MERGE_AZ_KEY, player.getZ());
                        data.putFloat(A_RANGER_MERGE_HPS_KEY, hps);
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0, false, false));
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 0.9f, 0.9f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 12, 0.6, 0.8, 0.6, 0.03);
                        setCooldown(player, "cd_a_ult_ranger_merge", cd);
                        java.util.UUID vid = java.util.UUID.randomUUID();
                        data.putUUID("lvluping_ranger_merge_tree_vis", vid);
                        broadcastRangerMergeTreeShow(sl, player, vid, player.getX(), player.getY(), player.getZ(), data.getLong(A_RANGER_MERGE_UNTIL_KEY));
                    }
                    return;
                }
                if (talents.contains("a_ult_ranger_life_totem") && data.getInt("cd_a_ult_ranger_life_totem") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_ranger_life_totem", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_ult_ranger_life_totem", "duration_ticks", lvl, 300);
                        double r = AbilityUpgradeConfig.getDouble("a_ult_ranger_life_totem", "radius", lvl, 6.0);
                        float total = (float) AbilityUpgradeConfig.getDouble("a_ult_ranger_life_totem", "team_heal_total", lvl, 0.2);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_ranger_life_totem", "cooldown", lvl, 1000);
                        Vec3 pos = getLookPointOnBlock(sl, player, 14.0);
                        data.putLong(A_RANGER_TOTEM_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putDouble(A_RANGER_TOTEM_X_KEY, pos.x);
                        data.putDouble(A_RANGER_TOTEM_Y_KEY, pos.y);
                        data.putDouble(A_RANGER_TOTEM_Z_KEY, pos.z);
                        data.putDouble(A_RANGER_TOTEM_R_KEY, r);
                        data.putFloat(A_RANGER_TOTEM_HEAL_TOTAL_KEY, total);
                        UUID totemVid = UUID.randomUUID();
                        data.putUUID(A_RANGER_TOTEM_VISUAL_KEY, totemVid);
                        broadcastRangerLifeTotemShow(sl, totemVid, pos.x, pos.y, pos.z, player.getYRot(), data.getLong(A_RANGER_TOTEM_UNTIL_KEY));
                        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.1f);
                        sl.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 18, r * 0.15, 0.35, r * 0.15, 0.03);
                        setCooldown(player, "cd_a_ult_ranger_life_totem", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_ranger_wrath") && data.getInt("cd_a_ult_ranger_wrath") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_ranger_wrath", talents);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_ranger_wrath", "cooldown", lvl, 1000);
                        data.putLong("lvluping_ranger_wrath_next_until", sl.getGameTime() + 400);
                        data.putBoolean("lvluping_ranger_wrath_pending", true);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.6f, 0.8f);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.6, 0.5, 0.06);
                        setCooldown(player, "cd_a_ult_ranger_wrath", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_ranger_roots") && data.getInt("cd_a_ult_ranger_roots") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_ranger_roots", talents);
                        int rootTicks = AbilityUpgradeConfig.getInt("a_ult_ranger_roots", "root_ticks", lvl, 80);
                        float dps = (float) AbilityUpgradeConfig.getDouble("a_ult_ranger_roots", "dps", lvl, 1.0);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_ranger_roots", "cooldown", lvl, 1000);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 14);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, rootTicks);
                        data.putFloat(A_NEXT_ARROW_P2_KEY, dps);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 400);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 0.85f, 1.1f);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 14, 0.4, 0.35, 0.4, 0.03);
                        setCooldown(player, "cd_a_ult_ranger_roots", cd);
                    }
                    return;
                }

                if (talents.contains("a_ult_musketeer_barrage") && data.getInt("cd_a_ult_musketeer_barrage") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_musketeer_barrage", talents);
                        int dur = AbilityUpgradeConfig.getInt("a_ult_musketeer_barrage", "duration_ticks", lvl, 60);
                        int shots = AbilityUpgradeConfig.getInt("a_ult_musketeer_barrage", "shots", lvl, 6);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_musketeer_barrage", "cooldown", lvl, 1000);
                        data.putLong(A_MUSK_BARRAGE_UNTIL_KEY, sl.getGameTime() + dur);
                        data.putInt(A_MUSK_BARRAGE_SHOTS_KEY, shots);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.6f);
                        sl.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.5, 0.6, 0.5, 0.12);
                        setCooldown(player, "cd_a_ult_musketeer_barrage", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_musketeer_grenade") && data.getInt("cd_a_ult_musketeer_grenade") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_musketeer_grenade", talents);
                        double r = AbilityUpgradeConfig.getDouble("a_ult_musketeer_grenade", "radius", lvl, 3.0);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("a_ult_musketeer_grenade", "damage", lvl, 6.0);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_musketeer_grenade", "cooldown", lvl, 1000);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 42);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, (float) r);
                        data.putFloat(A_NEXT_ARROW_P2_KEY, dmg);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 400);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.9f, 0.9f);
                        sl.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.5, 0.3, 0.5, 0.02);
                        setCooldown(player, "cd_a_ult_musketeer_grenade", cd);
                    }
                    return;
                }
                if (talents.contains("a_ult_musketeer_concussion") && data.getInt("cd_a_ult_musketeer_concussion") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "a_ult_musketeer_concussion", talents);
                        int root = AbilityUpgradeConfig.getInt("a_ult_musketeer_concussion", "root_ticks", lvl, 80);
                        int weak = AbilityUpgradeConfig.getInt("a_ult_musketeer_concussion", "weakness_ticks", lvl, 100);
                        int cd = AbilityUpgradeConfig.getInt("a_ult_musketeer_concussion", "cooldown", lvl, 1000);
                        data.putInt(A_NEXT_ARROW_EFFECT_KEY, 30);
                        data.putFloat(A_NEXT_ARROW_P1_KEY, root);
                        data.putFloat(A_NEXT_ARROW_P2_KEY, weak);
                        data.putLong(A_NEXT_ARROW_UNTIL_KEY, sl.getGameTime() + 400);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.4f);
                        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.4, 0.5, 0.04);
                        setCooldown(player, "cd_a_ult_musketeer_concussion", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_paladin_wings") && data.getInt("cd_w_ult_paladin_wings") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_paladin_wings", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_paladin_wings", "duration_ticks", lvl, 60);
                        int sa = AbilityUpgradeConfig.getInt("w_ult_paladin_wings", "speed_amp", lvl, 1);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_paladin_wings", "cooldown", lvl, 500);
                        long until = serverLevel.getGameTime() + dur;
                        data.putLong("lvluping_paladin_wings_until", until);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, sa, false, false));
                        if (!player.isCreative()) {
                            player.getAbilities().mayfly = true;
                            player.getAbilities().flying = true;
                            player.onUpdateAbilities();
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 1.2f);
                        setCooldown(player, "cd_w_ult_paladin_wings", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_paladin_sacrifice") && data.getInt("cd_w_ult_paladin_sacrifice") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_paladin_sacrifice", talents);
                        double ratio = AbilityUpgradeConfig.getDouble("w_ult_paladin_sacrifice", "hp_cost_ratio", lvl, 0.5);
                        double hr = AbilityUpgradeConfig.getDouble("w_ult_paladin_sacrifice", "heal_radius", lvl, 10.0);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_paladin_sacrifice", "cooldown", lvl, 700);
                        float hp = player.getHealth();
                        float loss = (float) (hp * ratio);
                        float nh = Math.max(1f, hp - loss);
                        player.setHealth(nh);
                        AABB box = player.getBoundingBox().inflate(hr, 4.0, hr);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
                            if (e == player) continue;
                            if (!e.isAlliedTo(player)) continue;
                            e.setHealth(e.getMaxHealth());
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.1f);
                        setCooldown(player, "cd_w_ult_paladin_sacrifice", cd);
                    }
                    return;
                }
                if (talents.contains("m_ult_light_ray") && data.getInt("cd_m_ult_light_ray") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_light_ray", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_ult_light_ray", "mana", lvl, 120.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_light_ray", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        LivingEntity t = getTargetInFront(player, CLERIC_LIGHT_RAY_TARGET_AIM_RANGE_XZ, CLERIC_LIGHT_RAY_AIM_CONE_DEG);
                        Vec3 center = (t != null) ? t.position() : getLookPointOnBlock(serverLevel, player, LOOK_POINT_FALLBACK_RANGE);

                        int dur = AbilityUpgradeConfig.getInt("m_ult_light_ray", "duration_ticks", lvl, 100);
                        long until = serverLevel.getGameTime() + dur;
                        double beamRadius = AbilityUpgradeConfig.getDouble("m_ult_light_ray", "beam_radius", lvl, CLERIC_LIGHT_RAY_BEAM_RADIUS);
                        double slowRadius = beamRadius;
                        double cy = center.y;
                        int minY = serverLevel.getMinBuildHeight();
                        int maxY = serverLevel.getMaxBuildHeight() - 1;
                        double yMin = Math.max(minY, cy - CLERIC_LIGHT_RAY_VERTICAL_REACH_BLOCKS);
                        double yMax = Math.min(maxY, cy + CLERIC_LIGHT_RAY_VERTICAL_REACH_BLOCKS);

                        float healPulse = (float) AbilityUpgradeConfig.getDouble("m_ult_light_ray", "heal", lvl, 2.0);
                        float dmgPulse = (float) AbilityUpgradeConfig.getDouble("m_ult_light_ray", "damage", lvl, 6.0);

                        UltimatesHandler.removeLightRayLightBlocks(serverLevel, player.getPersistentData());

                        player.getPersistentData().putLong("lvluping_c_light_ray_until", until);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_cx", center.x);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_cy", center.y);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_cz", center.z);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_slow_r", slowRadius);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_beam_r", beamRadius);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_ymin", yMin);
                        player.getPersistentData().putDouble("lvluping_c_light_ray_ymax", yMax);
                        player.getPersistentData().putFloat("lvluping_c_light_ray_heal", healPulse);
                        player.getPersistentData().putFloat("lvluping_c_light_ray_damage", dmgPulse);

                        UltimatesHandler.placeLightRayLightBlocks(serverLevel, player, center.x, center.z, yMin, yMax);

                        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
                        UltimatesHandler.playLightRayBeaconVisual(serverLevel, center.x, center.z, yMin, yMax);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_light_ray", "cooldown", lvl, M_ULT_LIGHT_RAY_COOLDOWN);
                        setCooldown(player, "cd_m_ult_light_ray", cd);
                    }
                    return;
                }

                // --- M_ULT_RESURRECTION ---
                if (talents.contains("m_ult_resurrection") && data.getInt("cd_m_ult_resurrection") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_resurrection", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_ult_resurrection", "mana", lvl, 160.0));
                        double radius = AbilityUpgradeConfig.getDouble("m_ult_resurrection", "radius", lvl, 10.0);
                        int durationTicks = AbilityUpgradeConfig.getInt("m_ult_resurrection", "duration_ticks", lvl, 100);
                        int regenAmp = AbilityUpgradeConfig.getInt("m_ult_resurrection", "regen_amp", lvl, 0);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_resurrection", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        AABB box = player.getBoundingBox().inflate(radius, Math.min(radius, 8.0), radius);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, box, le -> le == player || player.isAlliedTo(le))) {
                            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, regenAmp, false, true, true));
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9f, 1.35f);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 48, radius * 0.35, 0.5, radius * 0.35, 0.12);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_resurrection", "cooldown", lvl, M_ULT_RESURRECTION_COOLDOWN);
                        setCooldown(player, "cd_m_ult_resurrection", cd);
                    }
                    return;
                }

                // --- M_ULT_MARTYR ---
                if (talents.contains("m_ult_martyr") && data.getInt("cd_m_ult_martyr") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_martyr", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_ult_martyr", "mana", lvl, 140.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_martyr", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_martyr", "duration_ticks", lvl, 80);
                        long until = serverLevel.getGameTime() + dur;
                        player.getPersistentData().putLong("lvluping_cleric_martyr_until", until);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.6f, 1.6f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.6, 0.8, 0.6, 0.06);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_martyr", "cooldown", lvl, M_ULT_MARTYR_COOLDOWN);
                        setCooldown(player, "cd_m_ult_martyr", cd);
                    }
                    return;
                }

                // --- M_ULT_SLOW_SPHERE ---
                if (talents.contains("m_ult_slow_sphere") && data.getInt("cd_m_ult_slow_sphere") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_slow_sphere", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_ult_slow_sphere", "mana", lvl, 150.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_slow_sphere", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        LivingEntity t = getTargetInFront(player, CLERIC_SLOW_SPHERE_TARGET_AIM_RANGE_XZ, CLERIC_SLOW_SPHERE_AIM_CONE_DEG);
                        Vec3 center = (t != null) ? t.position() : getLookPointOnBlock(serverLevel, player, LOOK_POINT_FALLBACK_RANGE);

                        int dur = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "duration_ticks", lvl, 100);
                        long until = serverLevel.getGameTime() + dur;
                        double radius = AbilityUpgradeConfig.getDouble("m_ult_slow_sphere", "radius", lvl, 7.0);

                        int slowTicks = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "slow_ticks", lvl, 60);
                        int speedTicks = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "speed_ticks", lvl, 60);
                        int slowPct = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "slow_percent", lvl, 20);
                        int speedPct = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "speed_percent", lvl, 20);
                        int slowAmp = Math.min(5, Math.max(0, slowPct / 20 - 1));
                        int speedAmp = Math.min(5, Math.max(0, speedPct / 20 - 1));

                        player.getPersistentData().putLong("lvluping_c_slow_sphere_until", until);
                        player.getPersistentData().putDouble("lvluping_c_slow_sphere_cx", center.x);
                        player.getPersistentData().putDouble("lvluping_c_slow_sphere_cy", center.y);
                        player.getPersistentData().putDouble("lvluping_c_slow_sphere_cz", center.z);
                        player.getPersistentData().putDouble("lvluping_c_slow_sphere_radius", radius);
                        player.getPersistentData().putInt("lvluping_c_slow_sphere_slow_ticks", slowTicks);
                        player.getPersistentData().putInt("lvluping_c_slow_sphere_slow_amp", slowAmp);
                        player.getPersistentData().putInt("lvluping_c_slow_sphere_speed_ticks", speedTicks);
                        player.getPersistentData().putInt("lvluping_c_slow_sphere_speed_amp", speedAmp);

                        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 60, 0.6, 0.9, 0.6, 0.05);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_slow_sphere", "cooldown", lvl, M_ULT_SLOW_SPHERE_COOLDOWN);
                        setCooldown(player, "cd_m_ult_slow_sphere", cd);
                    }
                    return;
                }

                // --- M_ULT_DIVINE_PROTECTION --
                if (talents.contains("m_ult_divine_protection") && data.getInt("cd_m_ult_divine_protection") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_divine_protection", talents);
                        double cost = applyClericBaseMana(player, talents, AbilityUpgradeConfig.getDouble("m_ult_divine_protection", "mana", lvl, 170.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_divine_protection", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        int dur = AbilityUpgradeConfig.getInt("m_ult_divine_protection", "duration_ticks", lvl, 100);
                        long until = serverLevel.getGameTime() + dur;
                        double radius = AbilityUpgradeConfig.getDouble("m_ult_divine_protection", "radius", lvl, 10.0);

                        float shieldPct = (float) AbilityUpgradeConfig.getDouble("m_ult_divine_protection", "shield_percent", lvl, 0.30);
                        float healPerSec = (float) AbilityUpgradeConfig.getDouble("m_ult_divine_protection", "heal_per_sec", lvl, 1.0);
                        AABB area = player.getBoundingBox().inflate(radius, DIVINE_PROTECTION_HITBOX_Y_THICKNESS, radius);
                        for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
                            if (e == player) {
                                e.getPersistentData().putLong("lvluping_cleric_divine_protection_until", until);
                                e.getPersistentData().putFloat("lvluping_cleric_divine_shield_pct", shieldPct);
                                e.getPersistentData().putFloat("lvluping_cleric_divine_hps", healPerSec);
                                continue;
                            }
                            boolean allied = e.isAlliedTo(player);
                            if (!allied && e instanceof Mob mob && mob.getPersistentData().hasUUID("lvluping_summon_owner")
                                    && mob.getPersistentData().getUUID("lvluping_summon_owner").equals(player.getUUID())) {
                                allied = true;
                            }
                            if (allied) {
                                e.getPersistentData().putLong("lvluping_cleric_divine_protection_until", until);
                                e.getPersistentData().putFloat("lvluping_cleric_divine_shield_pct", shieldPct);
                                e.getPersistentData().putFloat("lvluping_cleric_divine_hps", healPerSec);
                                if (!(e instanceof Player)) {
                                    int regenAmp = healPerSec >= 1.5f ? 1 : 0;
                                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, regenAmp, false, false));
                                }
                            }
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 0.9, 0.6, 0.06);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_divine_protection", "cooldown", lvl, M_ULT_DIVINE_PROTECTION_COOLDOWN);
                        setCooldown(player, "cd_m_ult_divine_protection", cd);
                    }
                    return;
                }

                // --- M_ULT_METEOR ---
                if (talents.contains("m_ult_meteor") && data.getInt("cd_m_ult_meteor") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_meteor", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ult_meteor", "mana", lvl, 120.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_meteor", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        LivingEntity target = getTargetInFront(player, M_ULT_METEOR_AIM_RANGE_XZ, M_ULT_METEOR_AIM_CONE_DEG);
                        Vec3 pos = (target != null) ? target.position() : getLookPointOnBlock(serverLevel, player, LOOK_POINT_FALLBACK_RANGE);
                        int delay = AbilityUpgradeConfig.getInt("m_ult_meteor", "delay_ticks", lvl, 40);
                        player.getPersistentData().putLong("lvluping_m_meteor_at", serverLevel.getGameTime() + delay);
                        player.getPersistentData().putDouble("lvluping_m_meteor_x", pos.x);
                        player.getPersistentData().putDouble("lvluping_m_meteor_y", pos.y);
                        player.getPersistentData().putDouble("lvluping_m_meteor_z", pos.z);
                        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.8f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1.0, pos.z, 30, 0.8, 1.0, 0.8, 0.1);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_meteor", "cooldown", lvl, 850);
                        setCooldown(player, "cd_m_ult_meteor", cd);
                    }
                    return;
                }

                // --- M_ULT_ICE_BLOCK ---
                if (talents.contains("m_ult_ice_block") && data.getInt("cd_m_ult_ice_block") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_ice_block", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ult_ice_block", "mana", lvl, 90.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_ice_block", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        LivingEntity t = getTargetInFront(player, M_ULT_ICE_BLOCK_AIM_RANGE_XZ, M_ULT_ICE_BLOCK_AIM_CONE_DEG);
                        if (t == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_ice_block", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int freezeDur = AbilityUpgradeConfig.getInt("m_ult_ice_block", "duration_ticks", lvl, 100);
                        int slowTicks = AbilityUpgradeConfig.getInt("m_ult_ice_block", "slow_ticks", lvl, 200);
                        int slowAmp = AbilityUpgradeConfig.getInt("m_ult_ice_block", "slow_amp", lvl, 0);
                        t.getPersistentData().putLong("lvluping_m_ice_block_until", serverLevel.getGameTime() + freezeDur);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmp, false, false));
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, freezeDur, M_ULT_ICE_BLOCK_WEAKNESS_AMPLIFIER, false, false));
                        serverLevel.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, t.getX(), t.getY() + 1.0, t.getZ(), 60, 0.8, 1.0, 0.8, 0.04);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_ice_block", "cooldown", lvl, 700);
                        setCooldown(player, "cd_m_ult_ice_block", cd);
                    }
                    return;
                }

                // --- M_ULT_ANTI_MAGIC ---
                if (talents.contains("m_ult_anti_magic") && data.getInt("cd_m_ult_anti_magic") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_anti_magic", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ult_anti_magic", "mana", lvl, 110.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_anti_magic", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_anti_magic", "duration_ticks", lvl, 100);
                        double radius = AbilityUpgradeConfig.getDouble("m_ult_anti_magic", "radius", lvl, 6.0);
                        player.getPersistentData().putLong("lvluping_m_anti_magic_until", serverLevel.getGameTime() + dur);
                        player.getPersistentData().putDouble("lvluping_m_anti_magic_radius", radius);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.5f);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.8, 1.0, 0.8, 0.02);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_anti_magic", "cooldown", lvl, 750);
                        setCooldown(player, "cd_m_ult_anti_magic", cd);
                    }
                    return;
                }

                // --- M_ULT_ILLUSIONS ---
                if (talents.contains("m_ult_illusions") && data.getInt("cd_m_ult_illusions") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_illusions", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ult_illusions", "mana", lvl, 100.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_illusions", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_illusions", "duration_ticks", lvl, 160);
                        long until = serverLevel.getGameTime() + dur;
                        int illusionCount = AbilityUpgradeConfig.getInt("m_ult_illusions", "count", lvl, 3);
                        if (illusionCount < 1) illusionCount = 1;
                        for (int i = 0; i < illusionCount; i++) {
                            var illusion = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(serverLevel);
                            if (illusion == null) continue;
                            double ang = (i / (double) Math.max(1, illusionCount)) * Math.PI * 2.0;
                            double ox = Math.cos(ang) * M_ULT_ILLUSIONS_OFFSET_XZ;
                            double oz = Math.sin(ang) * M_ULT_ILLUSIONS_OFFSET_XZ;
                            illusion.moveTo(player.getX() + ox, player.getY() + M_ULT_ILLUSIONS_OFFSET_Y, player.getZ() + oz, player.getYRot(), player.getXRot());
                            illusion.setCustomName(net.minecraft.network.chat.Component.literal("lvluping_illusion:" + player.getUUID()));
                            illusion.setCustomNameVisible(false);
                            illusion.getPersistentData().putBoolean("lvluping_spell_illusion", true);
                            illusion.getPersistentData().putUUID("lvluping_illusion_owner", player.getUUID());
                            illusion.getPersistentData().putLong("lvluping_illusion_until", until);
                            illusion.getPersistentData().putDouble("lvluping_illusion_off_x", ox);
                            illusion.getPersistentData().putDouble("lvluping_illusion_off_z", oz);
                            illusion.setInvisible(true);

                            serverLevel.addFreshEntity(illusion);
                            serverLevel.sendParticles(ParticleTypes.PORTAL, illusion.getX(), illusion.getY() + 0.8, illusion.getZ(), 15, 0.4, 0.6, 0.4, 0.08);
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_illusions", "cooldown", lvl, 800);
                        setCooldown(player, "cd_m_ult_illusions", cd);
                    }
                    return;
                }

                // --- M_ULT_CHAOS ---
                if (talents.contains("m_ult_chaos") && data.getInt("cd_m_ult_chaos") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_chaos", talents);
                        double cost = applySpellcasterManaCost(player, talents, AbilityUpgradeConfig.getDouble("m_ult_chaos", "mana", lvl, 130.0));
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_chaos", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        double range = AbilityUpgradeConfig.getDouble("m_ult_chaos", "range", lvl, 10.0);
                        double radius = AbilityUpgradeConfig.getDouble("m_ult_chaos", "radius", lvl, 7.0);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("m_ult_chaos", "damage", lvl, 1.0);
                        int slowTicks = AbilityUpgradeConfig.getInt("m_ult_chaos", "slow_ticks", lvl, 0);
                        int slowAmp = AbilityUpgradeConfig.getInt("m_ult_chaos", "slow_amp", lvl, 0);
                        Vec3 center = getLookPointOnBlock(serverLevel, player, range);

                        double maxWaveRadius = radius;
                        int waveDurTicks = M_ULT_CHAOS_WAVE_DURATION_TICKS;
                        int burnTicks = M_ULT_CHAOS_BURN_TICKS;

                        data.putLong("lvluping_m_ult_chaos_wave_until", serverLevel.getGameTime() + waveDurTicks);
                        data.putLong("lvluping_m_ult_chaos_wave_dur", waveDurTicks);
                        data.putDouble("lvluping_m_ult_chaos_center_x", center.x);
                        data.putDouble("lvluping_m_ult_chaos_center_y", center.y);
                        data.putDouble("lvluping_m_ult_chaos_center_z", center.z);
                        data.putDouble("lvluping_m_ult_chaos_prev_r", 0.0);
                        data.putDouble("lvluping_m_ult_chaos_max_r", maxWaveRadius);
                        data.putFloat("lvluping_m_ult_chaos_dmg", dmg);
                        data.putInt("lvluping_m_ult_chaos_slow_ticks", slowTicks);
                        data.putInt("lvluping_m_ult_chaos_slow_amp", slowAmp);
                        data.putInt("lvluping_m_ult_chaos_burn_ticks", burnTicks);

                        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.8, center.z, 6, 0.8, 0.8, 0.8, 0.12);
                        serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.6, center.z, 8, 0.3, 0.5, 0.3, 0.02);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.9, center.z, 8, 0.3, 0.5, 0.3, 0.08);
                        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 10, 0.4, 0.6, 0.4, 0.04);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_chaos", "cooldown", lvl, 900);
                        setCooldown(player, "cd_m_ult_chaos", cd);
                    }
                    return;
                }

                // --- M_ULT_GATE ---
                if (talents.contains("m_ult_gate") && data.getInt("cd_m_ult_gate") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_gate", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_ult_gate", "mana", lvl, 140.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_gate", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int count = AbilityUpgradeConfig.getInt("m_ult_gate", "count", lvl, 3);
                        if (count < 1) count = 1;
                        int dur = AbilityUpgradeConfig.getInt("m_ult_gate", "duration_ticks", lvl, 400);
                        long until = serverLevel.getGameTime() + dur;
                        double hpMult = AbilityUpgradeConfig.getDouble("m_ult_gate", "hp_mult", lvl, 1.5);
                        int endLvl = talents.contains("m_summon_endurance")
                                ? PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_endurance", talents) : 0;
                        double summonDamageMult = AbilityUpgradeConfig.getDouble("m_summon_endurance", "damage_mult", endLvl, 1.0);

                        for (int i = 0; i < count; i++) {
                            Mob summon;
                            if (i % 3 == 0) summon = net.minecraft.world.entity.EntityType.BLAZE.create(serverLevel);
                            else if (i % 3 == 1) summon = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(serverLevel);
                            else summon = net.minecraft.world.entity.EntityType.WITHER_SKELETON.create(serverLevel);
                            if (summon == null) continue;

                            double angle = (i / (double) Math.max(1, count)) * Math.PI * 2.0;
                            double sx = player.getX() + Math.cos(angle) * M_ULT_GATE_SPAWN_RING_RADIUS;
                            double sz = player.getZ() + Math.sin(angle) * M_ULT_GATE_SPAWN_RING_RADIUS;
                            double sy = player.getY();
                            summon.moveTo(sx, sy, sz, player.getYRot(), 0);

                            if (summon.getAttribute(Attributes.MAX_HEALTH) != null) {
                                double hp = summon.getMaxHealth() * hpMult;
                                summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                                summon.setHealth((float) hp);
                            }
                            summon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, M_ULT_GATE_DAMAGE_RESISTANCE_AMPLIFIER, false, false));
                            summon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, M_ULT_GATE_MOVEMENT_SPEED_AMPLIFIER, false, false));
                            summon.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, M_ULT_GATE_GLOWING_AMPLIFIER, false, false));

                            serverLevel.addFreshEntity(summon);
                            summon.getPersistentData().putDouble("lvluping_summon_damage_mult", summonDamageMult);
                            SummonerHandler.addSummon(serverLevel, player, summon, until);
                            serverLevel.sendParticles(ParticleTypes.PORTAL, sx, sy + 1.0, sz, 30, 0.6, 0.8, 0.6, 0.08);
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7f, 1.2f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.9f);
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.8, 0.9, 0.8, 0.1);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_gate", "cooldown", lvl, 800);
                        setCooldown(player, "cd_m_ult_gate", cd);
                    }
                    return;
                }

                // --- M_ULT_ABSORPTION ---
                if (talents.contains("m_ult_absorption") && data.getInt("cd_m_ult_absorption") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_absorption", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_ult_absorption", "mana", lvl, 60.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_absorption", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        if (summons.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_absorption", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        float healPer = (float) AbilityUpgradeConfig.getDouble("m_ult_absorption", "heal_per", lvl, 6.0);
                        double manaPercent = AbilityUpgradeConfig.getDouble("m_ult_absorption", "mana_percent", lvl,
                                AbilityUpgradeConfig.getDouble("m_ult_absorption", "mana_per", lvl, 10.0));
                        int killed = 0;
                        for (Mob m : summons) {
                            if (!m.isAlive()) continue;
                            serverLevel.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 1.0, m.getZ(), 10, 0.3, 0.5, 0.3, 0.04);
                            m.discard();
                            killed++;
                        }
                        if (killed > 0) {
                            player.heal(healPer * killed);
                            Integer maxMana = ArsManaCompat.getMaxMana(player);
                            if (maxMana != null && maxMana > 0) {
                                ArsManaCompat.tryAddMana(player, (maxMana * (manaPercent / 100.0)) * killed);
                            } else {
                                ArsManaCompat.tryAddMana(player, manaPercent * killed);
                            }
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
                        serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.6, 0.5, 0.12);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_absorption", "cooldown", lvl, 650);
                        setCooldown(player, "cd_m_ult_absorption", cd);
                    }
                    return;
                }

                // --- M_ULT_TOTEM_FORM ---
                if (talents.contains("m_ult_totem_form") && data.getInt("cd_m_ult_totem_form") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_totem_form", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_ult_totem_form", "mana", lvl, 90.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_totem_form", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        if (summons.isEmpty()) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_totem_form", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_totem_form", "duration_ticks", lvl, 160);
                        double totemDamageMult = AbilityUpgradeConfig.getDouble("m_ult_totem_form", "damage_mult", lvl, 1.0);
                        long until = serverLevel.getGameTime() + dur;
                        for (Mob m : summons) {
                            if (!m.isAlive()) continue;
                            m.getPersistentData().putLong("lvluping_totem_until", until);
                            m.getPersistentData().putDouble("lvluping_totem_damage_mult", totemDamageMult);
                            m.setNoAi(true);
                            m.setInvulnerable(true);
                            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, M_ULT_TOTEM_FORM_GLOWING_AMPLIFIER, false, false));
                        }
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.8, 0.9, 0.8, 0.05);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_totem_form", "cooldown", lvl, 700);
                        setCooldown(player, "cd_m_ult_totem_form", cd);
                    }
                    return;
                }

                // --- M_ULT_POSSESSION ---
                if (talents.contains("m_ult_possession") && data.getInt("cd_m_ult_possession") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_possession", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_ult_possession", "mana", lvl, 80.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_possession", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_possession", "duration_ticks", lvl, 200);

                        double hpMult = AbilityUpgradeConfig.getDouble("m_ult_possession", "hp_mult", lvl, 1.8);
                        double damageMult = AbilityUpgradeConfig.getDouble("m_ult_possession", "damage_mult", lvl, 1.5);
                        int armorBonus = 1;

                        int servantLvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_servant", talents);
                        int guardLvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_summon_guard", talents);

                        List<Mob> summons = SummonerHandler.getAliveSummons(serverLevel, player);
                        boolean applied = false;
                        for (Mob m : summons) {
                            if (m == null || !m.isAlive()) continue;
                            if (!m.getPersistentData().hasUUID("lvluping_summon_owner")) continue;
                            if (!player.getUUID().equals(m.getPersistentData().getUUID("lvluping_summon_owner"))) continue;

                            boolean isServant = m.getType() == net.minecraft.world.entity.EntityType.SKELETON;
                            boolean isGuard = m.getType() == net.minecraft.world.entity.EntityType.ZOMBIE;
                            if (!isServant && !isGuard) continue;

                            if (isServant && servantLvl > 0) {
                                applySummonLoadout("m_summon_servant", servantLvl, m, hpMult, armorBonus);
                            } else if (isGuard && guardLvl > 0) {
                                applySummonLoadout("m_summon_guard", guardLvl, m, hpMult, armorBonus);
                            }

                            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, M_ULT_POSSESSION_DAMAGE_RESISTANCE_AMPLIFIER, false, false));
                            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, M_ULT_POSSESSION_MOVEMENT_SPEED_AMPLIFIER, false, false));
                            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, M_ULT_POSSESSION_DAMAGE_BOOST_AMPLIFIER, false, false));
                            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, M_ULT_POSSESSION_GLOWING_AMPLIFIER, false, false));
                            m.getPersistentData().putDouble("lvluping_summon_damage_mult", damageMult);
                            applied = true;
                        }

                        if (!applied) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_possession", ABILITY_FAIL_COOLDOWN);
                            return;
                        }

                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.8f);
                        serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 0.9, 0.6, 0.12);
                        int cd = AbilityUpgradeConfig.getInt("m_ult_possession", "cooldown", lvl, 750);
                        setCooldown(player, "cd_m_ult_possession", cd);
                    }
                    return;
                }

                // --- M_ULT_ELEMENTAL ---
                if (talents.contains("m_ult_elemental") && data.getInt("cd_m_ult_elemental") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "m_ult_elemental", talents);
                        double cost = AbilityUpgradeConfig.getDouble("m_ult_elemental", "mana", lvl, 120.0);
                        cost = getSummonerManaCost(player, talents, cost);
                        if (!ArsManaCompat.tryConsumeMana(player, cost)) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_elemental", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        int dur = AbilityUpgradeConfig.getInt("m_ult_elemental", "duration_ticks", lvl, 200);
                        long until = serverLevel.getGameTime() + dur;
                        double hpMult = AbilityUpgradeConfig.getDouble("m_ult_elemental", "hp_mult", lvl, 2.0);
                        double damageMult = AbilityUpgradeConfig.getDouble("m_ult_elemental", "damage_mult", lvl, 1.0);

                        boolean fire = !player.isCrouching();
                        Mob summon = fire
                                ? net.minecraft.world.entity.EntityType.BLAZE.create(serverLevel)
                                : net.minecraft.world.entity.EntityType.STRAY.create(serverLevel);
                        if (summon == null) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.6f);
                            setCooldown(player, "cd_m_ult_elemental", ABILITY_FAIL_COOLDOWN);
                            return;
                        }
                        Vec3 fwd = player.getLookAngle().normalize();
                        double sx = player.getX() + fwd.x * M_ULT_ELEMENTAL_SPAWN_FORWARD_OFFSET;
                        double sy = player.getY();
                        double sz = player.getZ() + fwd.z * M_ULT_ELEMENTAL_SPAWN_FORWARD_OFFSET;
                        summon.moveTo(sx, sy, sz, player.getYRot(), 0);

                        if (summon.getAttribute(Attributes.MAX_HEALTH) != null) {
                            double hp = summon.getMaxHealth() * hpMult;
                            summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                            summon.setHealth((float) hp);
                        }
                        summon.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, M_ULT_ELEMENTAL_DAMAGE_RESISTANCE_AMPLIFIER, false, false));
                        summon.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, M_ULT_ELEMENTAL_MOVEMENT_SPEED_AMPLIFIER, false, false));
                        summon.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, M_ULT_ELEMENTAL_GLOWING_AMPLIFIER, false, false));

                        serverLevel.addFreshEntity(summon);
                        summon.getPersistentData().putDouble("lvluping_summon_damage_mult", damageMult);
                        SummonerHandler.addSummon(serverLevel, player, summon, until);
                        serverLevel.playSound(null, sx, sy, sz, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.8f);
                        serverLevel.sendParticles(fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, sx, sy + 1.0, sz, 40, 0.8, 0.9, 0.8, 0.05);

                        int cd = AbilityUpgradeConfig.getInt("m_ult_elemental", "cooldown", lvl, 850);
                        setCooldown(player, "cd_m_ult_elemental", cd);
                    }
                    return;
                }

                // --- W_ULT_BERSERK ---
                if (talents.contains("w_ult_berserk") && data.getInt("cd_w_ult_berserk") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_berserk", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_berserk", "duration_ticks", lvl, ULT_BERSERK_DURATION);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_berserk", "cooldown", lvl, ULT_BERSERK_COOLDOWN);
                        int speedAmp = AbilityUpgradeConfig.getInt("w_ult_berserk", "speed_amp", lvl, W_ULT_BERSERK_MOVEMENT_SPEED_AMPLIFIER);
                        int regenAmp = AbilityUpgradeConfig.getInt("w_ult_berserk", "regen_amp", lvl, W_ULT_BERSERK_REGENERATION_AMPLIFIER);
                        int jumpAmp = AbilityUpgradeConfig.getInt("w_ult_berserk", "jump_amp", lvl, W_ULT_BERSERK_JUMP_AMPLIFIER);
                        long until = serverLevel.getGameTime() + dur;
                        player.getPersistentData().putLong("lvluping_berserk_until", until);
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur, W_ULT_BERSERK_FIRE_RESISTANCE_AMPLIFIER, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, regenAmp, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, speedAmp, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, jumpAmp, false, false));
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.5f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.8f);
                        for (int i = 0; i < 40; i++) {
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.5, 0.5, 0.5, 0.08);
                            serverLevel.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.3, 0.2, 0.3, 0.02);
                            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 0.8, player.getZ(), 1, 0.25, 0.25, 0.25, 0.05);
                        }
                        setCooldown(player, "cd_w_ult_berserk", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_swordmaster_hurricane") && data.getInt("cd_w_ult_swordmaster_hurricane") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_swordmaster_hurricane", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_swordmaster_hurricane", "duration_ticks", lvl, 80);
                        int speedAmp = AbilityUpgradeConfig.getInt("w_ult_swordmaster_hurricane", "speed_amp", lvl, 0);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_swordmaster_hurricane", "cooldown", lvl, 700);
                        double asMult = AbilityUpgradeConfig.getDouble("w_ult_swordmaster_hurricane", "attack_speed_mult", lvl, 1.5);
                        data.putLong(W_SWORDMASTER_HURRICANE_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        data.putFloat(W_SWORDMASTER_HURRICANE_AS_MULT_KEY, (float) asMult);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, speedAmp, false, false));
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.6f);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.55, 0.35, 0.55, 0.05);
                        setCooldown(player, "cd_w_ult_swordmaster_hurricane", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_swordmaster_omnislash") && data.getInt("cd_w_ult_swordmaster_omnislash") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_swordmaster_omnislash", talents);
                        int hits = AbilityUpgradeConfig.getInt("w_ult_swordmaster_omnislash", "hits", lvl, 5);
                        float hitMult = (float) AbilityUpgradeConfig.getDouble("w_ult_swordmaster_omnislash", "hit_damage_mult", lvl, 0.35);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_swordmaster_omnislash", "cooldown", lvl, 700);
                        LivingEntity target = getTargetInFront(player, 7.0, 25.0);
                        if (target == null) {
                            double best = Double.MAX_VALUE;
                            for (LivingEntity e : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(7.0, 2.0, 7.0))) {
                                if (e == player || e.isAlliedTo(player)) continue;
                                double d = player.distanceToSqr(e);
                                if (d < best) {
                                    best = d;
                                    target = e;
                                }
                            }
                        }
                        if (target != null && target != player && !target.isAlliedTo(player)) {
                            float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                            float total = Math.max(0.1f, base * hitMult * Math.max(1, hits));
                            target.invulnerableTime = 0;
                            target.hurt(player.damageSources().playerAttack(player), total);
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
                            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 24, 0.4, 0.4, 0.4, 0.03);
                            setCooldown(player, "cd_w_ult_swordmaster_omnislash", cd);
                        } else {
                            setCooldown(player, "cd_w_ult_swordmaster_omnislash", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("w_ult_swordmaster_blade_wall") && data.getInt("cd_w_ult_swordmaster_blade_wall") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_swordmaster_blade_wall", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_swordmaster_blade_wall", "duration_ticks", lvl, 60);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_swordmaster_blade_wall", "cooldown", lvl, 700);
                        data.putLong(W_SWORDMASTER_BLADE_WALL_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0f, 1.4f);
                        setCooldown(player, "cd_w_ult_swordmaster_blade_wall", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_swordmaster_perfect_cut") && data.getInt("cd_w_ult_swordmaster_perfect_cut") <= 0) {
                    int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_swordmaster_perfect_cut", talents);
                    float ratio = (float) AbilityUpgradeConfig.getDouble("w_ult_swordmaster_perfect_cut", "target_max_hp_ratio", lvl, 0.15);
                    float bossMult = (float) AbilityUpgradeConfig.getDouble("w_ult_swordmaster_perfect_cut", "boss_ratio_mult", lvl, 0.35);
                    int cd = AbilityUpgradeConfig.getInt("w_ult_swordmaster_perfect_cut", "cooldown", lvl, 700);
                    data.putBoolean(W_SWORDMASTER_PERFECT_CUT_READY_KEY, true);
                    data.putFloat(W_SWORDMASTER_PERFECT_CUT_RATIO_KEY, ratio);
                    data.putFloat(W_SWORDMASTER_PERFECT_CUT_BOSS_MULT_KEY, bossMult);
                    if (player.level() instanceof ServerLevel sl) {
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8f, 1.5f);
                    }
                    setCooldown(player, "cd_w_ult_swordmaster_perfect_cut", cd);
                    return;
                }
                if (talents.contains("w_ult_barbarian_taste_blood") && data.getInt("cd_w_ult_barbarian_taste_blood") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_barbarian_taste_blood", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_barbarian_taste_blood", "duration_ticks", lvl, 80);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_barbarian_taste_blood", "cooldown", lvl, 700);
                        double ratio = AbilityUpgradeConfig.getDouble("w_ult_barbarian_taste_blood", "lifesteal_ratio", lvl, 0.4);
                        data.putLong(W_BARBARIAN_TASTE_BLOOD_UNTIL_KEY, serverLevel.getGameTime() + dur);
                        data.putFloat(W_BARBARIAN_TASTE_BLOOD_RATIO_KEY, (float) ratio);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 0.7f);
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.4, 0.5, 0.09);
                        setCooldown(player, "cd_w_ult_barbarian_taste_blood", cd);
                    }
                    return;
                }
                if (talents.contains("w_ult_barbarian_feast") && data.getInt("cd_w_ult_barbarian_feast") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_barbarian_feast", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_barbarian_feast", "duration_ticks", lvl, 40);
                        float dps = (float) AbilityUpgradeConfig.getDouble("w_ult_barbarian_feast", "tick_damage", lvl, 2.0);
                        float ls = (float) AbilityUpgradeConfig.getDouble("w_ult_barbarian_feast", "lifesteal_ratio", lvl, 0.5);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_barbarian_feast", "cooldown", lvl, 750);
                        LivingEntity target = getTargetInFront(player, 4.0, 35.0);
                        if (target != null && target != player && !target.isAlliedTo(player)) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 10, false, false));
                            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 1, false, false));
                            int ticks = Math.max(1, dur / 10);
                            for (int i = 0; i < ticks; i++) {
                                target.hurt(player.damageSources().playerAttack(player), dps);
                                if (ls > 0f) player.heal(dps * ls);
                            }
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.7f);
                            setCooldown(player, "cd_w_ult_barbarian_feast", cd);
                        } else {
                            setCooldown(player, "cd_w_ult_barbarian_feast", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                // --- W_ULT_FINAL_COUNTDOWN ---
                if (talents.contains("w_ult_final_countdown") && data.getInt("cd_w_ult_final_countdown") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_final_countdown", talents);
                        int delay = AbilityUpgradeConfig.getInt("w_ult_final_countdown", "delay_ticks", lvl, ULT_FINAL_COUNTDOWN_DELAY);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_final_countdown", "cooldown", lvl, ULT_FINAL_COUNTDOWN_COOLDOWN);
                        LivingEntity target = getTargetInFront(player, W_ULT_FINAL_COUNTDOWN_AIM_RANGE_XZ, W_ULT_FINAL_COUNTDOWN_AIM_CONE_DEG);
                        if (target != null) {
                            long at = serverLevel.getGameTime() + delay;
                            player.getPersistentData().putLong("lvluping_final_countdown_at", at);
                            player.getPersistentData().putUUID("lvluping_final_countdown_target", target.getUUID());
                            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
                            for (ServerPlayer p : serverLevel.players()) {
                                PacketDistributor.sendToPlayer(p, new org.mrutcka.lvluping.network.S2CJudgementHammerEffect(tx, ty, tz, delay, target.getUUID()));
                            }
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0f, 0.7f);
                            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5f, 1.5f);
                            for (int i = 0; i < 25; i++) {
                                serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.4, 0.6, 0.4, 0.15);
                                serverLevel.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0.2, 0.3, 0.2, 0.05);
                            }
                            setCooldown(player, "cd_w_ult_final_countdown", cd);
                        } else {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 0.5f);
                            setCooldown(player, "cd_w_ult_final_countdown", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }

                if (talents.contains("w_ult_invulnerability") && data.getInt("cd_w_ult_invulnerability") <= 0) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "w_ult_invulnerability", talents);
                        int dur = AbilityUpgradeConfig.getInt("w_ult_invulnerability", "duration_ticks", lvl, 100);
                        double radius = AbilityUpgradeConfig.getDouble("w_ult_invulnerability", "radius", lvl, 4.0);
                        float healPs = (float) AbilityUpgradeConfig.getDouble("w_ult_invulnerability", "heal_per_sec", lvl, 1.0);
                        float shieldRatio = (float) AbilityUpgradeConfig.getDouble("w_ult_invulnerability", "ally_shield_max_hp_ratio", lvl, 0.08);
                        int cd = AbilityUpgradeConfig.getInt("w_ult_invulnerability", "cooldown", lvl, 500);
                        long t = serverLevel.getGameTime();
                        if (player.getVehicle() != null) {
                            player.stopRiding();
                        }
                        data.putLong(W_LIGHT_FORM_UNTIL_KEY, t + dur);
                        data.putDouble(W_LIGHT_FORM_RADIUS_KEY, radius);
                        data.putFloat(W_LIGHT_FORM_HEAL_KEY, healPs);
                        data.putFloat(W_LIGHT_FORM_SHIELD_RATIO_KEY, shieldRatio);
                        data.putDouble(W_LIGHT_FORM_AX_KEY, player.getX());
                        data.putDouble(W_LIGHT_FORM_AY_KEY, player.getY());
                        data.putDouble(W_LIGHT_FORM_AZ_KEY, player.getZ());
                        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
                        UltimatesHandler.applyLightFormMoveLock(player);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.15f);
                        for (int i = 0; i < 40; i++) {
                            double ang = (Math.PI * 2) * i / 40.0;
                            double rx = Math.cos(ang) * radius;
                            double rz = Math.sin(ang) * radius;
                            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX() + rx, player.getY() + 0.12, player.getZ() + rz, 1, 0.04, 0.02, 0.04, 0.002);
                            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX() + rx * 0.92, player.getY() + 0.1, player.getZ() + rz * 0.92, 1, 0.02, 0.01, 0.02, 0.001);
                        }
                        setCooldown(player, "cd_w_ult_invulnerability", cd);
                    }
                    return;
                }
            }
            case 5 -> {
                if (talents.contains("as_rogue_blind") && data.getInt("cd_as_rogue_blind") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 6.0, 35.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_rogue_blind", talents);
                        int blind = AbilityUpgradeConfig.getInt("as_rogue_blind", "blind_ticks", lvl, 80);
                        int cd = AbilityUpgradeConfig.getInt("as_rogue_blind", "cooldown", lvl, 180);
                        if (t != null && t != player) {
                            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind, 0, false, false));
                            sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                            setCooldown(player, "cd_as_rogue_blind", cd);
                        } else {
                            setCooldown(player, "cd_as_rogue_blind", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
                if (talents.contains("as_wanderer_tripwire") && data.getInt("cd_as_wanderer_tripwire") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_wanderer_tripwire", talents);
                        double r = AbilityUpgradeConfig.getDouble("as_wanderer_tripwire", "radius", lvl, 2.0);
                        float dmg = (float) AbilityUpgradeConfig.getDouble("as_wanderer_tripwire", "damage", lvl, 3.0);
                        int cd = AbilityUpgradeConfig.getInt("as_wanderer_tripwire", "cooldown", lvl, 200);
                        data.putLong("lvluping_as_tripwire_until", sl.getGameTime() + 200);
                        data.putDouble("lvluping_as_tripwire_x", player.getX());
                        data.putDouble("lvluping_as_tripwire_y", player.getY());
                        data.putDouble("lvluping_as_tripwire_z", player.getZ());
                        data.putDouble("lvluping_as_tripwire_r", r);
                        data.putFloat("lvluping_as_tripwire_dmg", dmg);
                        UUID vid = UUID.randomUUID();
                        data.putUUID(AS_WANDERER_TRIPWIRE_VISUAL_KEY, vid);
                        broadcastAssassinTripwireShow(sl, vid, player.getX(), player.getY(), player.getZ(), player.getYRot(), data.getLong("lvluping_as_tripwire_until"));
                        setCooldown(player, "cd_as_wanderer_tripwire", cd);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 0.8f, 1.0f);
                    }
                    return;
                }
                if (talents.contains("as_assassin_rupture") && data.getInt("cd_as_assassin_rupture") <= 0) {
                    if (player.level() instanceof ServerLevel sl) {
                        LivingEntity t = getTargetInFront(player, 6.0, 35.0);
                        int lvl = PlayerLevels.getAbilityLevel(player.getUUID(), "as_assassin_rupture", talents);
                        float mult = (float) AbilityUpgradeConfig.getDouble("as_assassin_rupture", "bleed_tick_damage_mult", lvl, 1.5);
                        int cd = AbilityUpgradeConfig.getInt("as_assassin_rupture", "cooldown", lvl, 180);
                        if (t != null && t != player) {
                            long until = t.getPersistentData().getLong("lvluping_as_bleed_until");
                            float dps = t.getPersistentData().getFloat("lvluping_as_bleed_dps");
                            if (until > sl.getGameTime() && dps > 0f) {
                                float secs = (until - sl.getGameTime()) / 20f;
                                t.hurt(player.damageSources().playerAttack(player), Math.max(0f, dps * secs * mult));
                                t.getPersistentData().remove("lvluping_as_bleed_until");
                                t.getPersistentData().remove("lvluping_as_bleed_dps");
                                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + 1.0, t.getZ(), 16, 0.3, 0.3, 0.3, 0.1);
                            }
                            setCooldown(player, "cd_as_assassin_rupture", cd);
                        } else {
                            setCooldown(player, "cd_as_assassin_rupture", ABILITY_FAIL_COOLDOWN);
                        }
                    }
                    return;
                }
            }
        }
    }
}

