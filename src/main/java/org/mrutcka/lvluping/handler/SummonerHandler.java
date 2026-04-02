package org.mrutcka.lvluping.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SummonerHandler {
    private static final String KEY_LIST = "lvluping_summons";
    private static final String KEY_CMD_TARGET = "lvluping_summon_cmd_target";
    private static final String KEY_OWNER = "lvluping_summon_owner";

    public static void tick(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            var data = player.getPersistentData();
            boolean possessing = data.getLong("lvluping_possession_until") > time && data.hasUUID("lvluping_possession_mob");
            UUID possessingMob = possessing ? data.getUUID("lvluping_possession_mob") : null;
            ListTag list = data.getList(KEY_LIST, 8);
            if (list.isEmpty()) {
                list = discoverSummonsNear(level, player);
                if (!list.isEmpty()) data.put(KEY_LIST, list);
            }
            if (list.isEmpty()) continue;

            List<StringTag> keep = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getString(i);
                UUID uuid;
                try {
                    uuid = UUID.fromString(raw);
                } catch (Exception e) {
                    continue;
                }
                Entity entity = findEntity(level, uuid, player.getX(), player.getZ());
                if (!(entity instanceof Mob mob)) continue;
                if (!mob.isAlive()) {
                    level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1.0, mob.getZ(), 12, 0.35, 0.6, 0.35, 0.02);
                    level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 0.8, mob.getZ(), 16, 0.4, 0.5, 0.4, 0.01);
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.5f, 1.6f);
                    continue;
                }
                long until = mob.getPersistentData().getLong("lvluping_summon_until");
                UUID owner = mob.getPersistentData().hasUUID(KEY_OWNER) ? mob.getPersistentData().getUUID(KEY_OWNER) : null;
                if (owner == null || !owner.equals(player.getUUID())) continue;
                if (until > 0 && until <= time) {
                    level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 0.35, 0.6, 0.35, 0.02);
                    level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 0.8, mob.getZ(), 24, 0.4, 0.5, 0.4, 0.01);
                    level.sendParticles(ParticleTypes.ENCHANT, mob.getX(), mob.getY() + 1.0, mob.getZ(), 12, 0.4, 0.6, 0.4, 0.1);
                    level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 1.4f);
                    mob.discard();
                    continue;
                }
                ensureAllied(level, player, mob);
                LivingEntity desired = pickDesiredTarget(level, player, time);

                if (possessing && possessingMob != null && mob.getUUID().equals(possessingMob)) {
                    keep.add(StringTag.valueOf(uuid.toString()));
                    continue;
                }

                if (mob.getPersistentData().getBoolean("lvluping_spell_illusion")) {
                    tickIllusion(level, player, mob, desired);
                    keep.add(StringTag.valueOf(uuid.toString()));
                    continue;
                }

                long totemUntil = mob.getPersistentData().getLong("lvluping_totem_until");
                if (totemUntil > 0) {
                    if (totemUntil <= time) {
                        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 0.35, 0.6, 0.35, 0.02);
                        level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + 0.8, mob.getZ(), 24, 0.4, 0.5, 0.4, 0.01);
                        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.6f, 1.6f);
                        mob.discard();
                        continue;
                    }
                    mob.setNoAi(true);
                    mob.setInvulnerable(true);
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, 0, 0);
                    mob.setAggressive(desired != null);
                    if (desired != null && desired.isAlive() && desired != player) {
                        mob.setTarget(desired);
                        totemShoot(level, mob, desired);
                    } else {
                        mob.setTarget(null);
                    }
                    keep.add(StringTag.valueOf(uuid.toString()));
                    continue;
                }

                LivingEntity current = mob.getTarget();
                if (current == player) {
                    mob.setTarget(null);
                    current = null;
                }
                if (current != null && !isAllowedTarget(player, current, desired)) {
                    mob.setTarget(null);
                }
                if (desired != null && (mob.getTarget() == null || mob.getTarget() != desired)) {
                    mob.setTarget(desired);
                    mob.setAggressive(true);
                } else if (desired == null) {
                    mob.setAggressive(false);
                    mob.setTarget(null);
                    followOwner(player, mob);
                }
                keep.add(StringTag.valueOf(uuid.toString()));
            }

            ListTag out = new ListTag();
            keep.forEach(out::add);
            data.put(KEY_LIST, out);
        }
    }

    private static void totemShoot(ServerLevel level, Mob totem, LivingEntity target) {
        CompoundTag pd = totem.getPersistentData();
        int cd = pd.getInt("lvluping_totem_shoot_cd");
        if (cd > 0) {
            pd.putInt("lvluping_totem_shoot_cd", cd - 1);
            return;
        }
        pd.putInt("lvluping_totem_shoot_cd", 4);

        Arrow arrow = net.minecraft.world.entity.EntityType.ARROW.create(level);
        if (arrow == null) return;
        arrow.setOwner(totem);
        arrow.setPos(totem.getX(), totem.getEyeY() - 0.1, totem.getZ());
        Vec3 from = arrow.position();
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 dir = to.subtract(from);
        arrow.shoot(dir.x, dir.y, dir.z, 1.8f, 2.5f);
        double totemDamageMult = pd.getDouble("lvluping_totem_damage_mult");
        if (totemDamageMult <= 0.0) totemDamageMult = 1.0;
        arrow.setBaseDamage(3.0 * totemDamageMult);
        arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        level.addFreshEntity(arrow);
        level.sendParticles(ParticleTypes.CRIT, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void tickIllusion(ServerLevel level, ServerPlayer owner, Mob illusion, LivingEntity target) {
        illusion.setNoAi(true);
        illusion.setAggressive(target != null);
        illusion.setTarget(null);
        CompoundTag pd = illusion.getPersistentData();
        double offX = pd.getDouble("lvluping_illusion_off_x");
        double offZ = pd.getDouble("lvluping_illusion_off_z");

        double yawRad = Math.toRadians(owner.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double worldOffX = offX * cos - offZ * sin;
        double worldOffZ = offX * sin + offZ * cos;
        illusion.moveTo(owner.getX() + worldOffX, owner.getY() + 0.1, owner.getZ() + worldOffZ, owner.getYRot(), owner.getXRot());
        if (target == null || !target.isAlive() || target == owner) return;

        int cd = pd.getInt("lvluping_illusion_cast_cd");
        if (cd > 0) {
            pd.putInt("lvluping_illusion_cast_cd", cd - 1);
            return;
        }
        pd.putInt("lvluping_illusion_cast_cd", 18);

        var talents = PlayerLevels.getPlayerTalents(owner.getUUID());
        boolean canFire = talents.contains("m_fireball");
        boolean canLightning = talents.contains("m_lightning");
        boolean canIce = talents.contains("m_ice_arrow");
        int variants = (canFire ? 1 : 0) + (canLightning ? 1 : 0) + (canIce ? 1 : 0);
        if (variants == 0) return;

        int pick = level.random.nextInt(variants);
        String spell = canFire && pick-- == 0 ? "fire"
                : canLightning && pick-- == 0 ? "lightning"
                : "ice";

        Vec3 eye = new Vec3(illusion.getX(), illusion.getEyeY() - 0.1, illusion.getZ());

        if ("fire".equals(spell)) {
            int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "m_fireball", talents);
            float dmg = (float) AbilityUpgradeConfig.getDouble("m_fireball", "damage", lvl, 8.0);
            SmallFireball fb = net.minecraft.world.entity.EntityType.SMALL_FIREBALL.create(level);
            if (fb != null) {
                fb.setPos(eye.x, eye.y, eye.z);
                fb.setOwner(illusion);
                Vec3 to = target.getEyePosition().subtract(eye).normalize().scale(0.7);
                fb.setDeltaMovement(to);
                fb.getPersistentData().putFloat("lvluping_magic_damage", dmg);
                level.addFreshEntity(fb);
            }
            level.sendParticles(ParticleTypes.FLAME, eye.x, eye.y, eye.z, 8, 0.15, 0.15, 0.15, 0.01);
            return;
        }

        if ("lightning".equals(spell)) {
            int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "m_lightning", talents);
            float dmg = (float) AbilityUpgradeConfig.getDouble("m_lightning", "damage", lvl, 10.0);
            target.hurt(level.damageSources().magic(), dmg);
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.35, 0.6, 0.35, 0.1);
            return;
        }

        int lvl = PlayerLevels.getAbilityLevel(owner.getUUID(), "m_ice_arrow", talents);
        float dmg = (float) AbilityUpgradeConfig.getDouble("m_ice_arrow", "damage", lvl, 7.0);
        int slowTicks = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_ticks", lvl, 60);
        int slowAmp = AbilityUpgradeConfig.getInt("m_ice_arrow", "slow_amp", lvl, 1);
        Snowball snowball = net.minecraft.world.entity.EntityType.SNOWBALL.create(level);
        if (snowball != null) {
            snowball.setOwner(illusion);
            snowball.setPos(eye.x, eye.y, eye.z);
            Vec3 to = target.getEyePosition().subtract(eye).normalize();
            snowball.shoot(to.x, to.y, to.z, 1.5f, 0.5f);
            snowball.getPersistentData().putBoolean("lvluping_ice_projectile", true);
            snowball.getPersistentData().putFloat("lvluping_ice_damage", dmg);
            snowball.getPersistentData().putInt("lvluping_ice_slow_ticks", slowTicks);
            snowball.getPersistentData().putInt("lvluping_ice_slow_amp", slowAmp);
            level.addFreshEntity(snowball);
        }
    }

    private static void ensureAllied(ServerLevel level, ServerPlayer owner, Mob mob) {
        Scoreboard scoreboard = level.getScoreboard();
        String raw = owner.getUUID().toString().replace("-", "");
        String teamName = "luS_" + raw.substring(0, Math.min(12, raw.length()));
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
        scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(mob.getScoreboardName(), team);
    }

    private static void followOwner(ServerPlayer owner, Mob mob) {
        double d2 = mob.distanceToSqr(owner);
        if (d2 > 30 * 30) {
            Vec3 pos = owner.position();
            mob.moveTo(pos.x + (owner.getRandom().nextDouble() - 0.5) * 2.0, pos.y, pos.z + (owner.getRandom().nextDouble() - 0.5) * 2.0, mob.getYRot(), mob.getXRot());
            return;
        }
        if (d2 > 4.5 * 4.5) {
            mob.getNavigation().moveTo(owner, 1.1);
        } else {
            mob.getNavigation().stop();
        }
    }

    private static ListTag discoverSummonsNear(ServerLevel level, ServerPlayer player) {
        ListTag out = new ListTag();
        AABB search = player.getBoundingBox().inflate(96, 64, 96);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, search)) {
            CompoundTag pd = mob.getPersistentData();
            if (pd.hasUUID(KEY_OWNER) && player.getUUID().equals(pd.getUUID(KEY_OWNER)) && mob.isAlive()) {
                ensureAllied(level, player, mob);
                out.add(StringTag.valueOf(mob.getUUID().toString()));
            }
        }
        return out;
    }

    public static boolean isAllowedTargetForOwner(ServerLevel level, UUID ownerUuid, LivingEntity target) {
        if (target == null) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner == null) return false;
        if (target.getUUID().equals(ownerUuid)) return false;
        LivingEntity desired = pickDesiredTarget(level, owner, level.getGameTime());
        return isAllowedTarget(owner, target, desired);
    }

    public static void setCommandTarget(ServerPlayer player, LivingEntity target, long untilTick) {
        var data = player.getPersistentData();
        data.putUUID(KEY_CMD_TARGET, target.getUUID());
    }

    public static void addSummon(ServerLevel level, ServerPlayer player, Mob mob, long untilTick) {
        CompoundTag pd = mob.getPersistentData();
        pd.putUUID(KEY_OWNER, player.getUUID());
        pd.putLong("lvluping_summon_until", untilTick);

        ensureAllied(level, player, mob);

        ListTag list = player.getPersistentData().getList(KEY_LIST, 8);
        list.add(StringTag.valueOf(mob.getUUID().toString()));
        player.getPersistentData().put(KEY_LIST, list);
    }

    public static List<Mob> getAliveSummons(ServerLevel level, ServerPlayer player) {
        List<Mob> out = new ArrayList<>();
        ListTag list = player.getPersistentData().getList(KEY_LIST, 8);
        if (list.isEmpty()) {
            AABB search = player.getBoundingBox().inflate(96, 64, 96);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, search)) {
                CompoundTag pd = mob.getPersistentData();
                if (pd.hasUUID(KEY_OWNER) && player.getUUID().equals(pd.getUUID(KEY_OWNER)) && mob.isAlive()) {
                    ensureAllied(level, player, mob);
                    out.add(mob);
                }
            }
            return out;
        }
        for (int i = 0; i < list.size(); i++) {
            UUID uuid;
            try {
                uuid = UUID.fromString(list.getString(i));
            } catch (Exception e) {
                continue;
            }
            Entity entity = findEntity(level, uuid, player.getX(), player.getZ());
            if (entity instanceof Mob mob && mob.isAlive()) {
                CompoundTag pd = mob.getPersistentData();
                if (pd.hasUUID(KEY_OWNER) && player.getUUID().equals(pd.getUUID(KEY_OWNER))) {
                    out.add(mob);
                }
            }
        }
        return out;
    }

    private static Entity findEntity(ServerLevel level, UUID uuid, double centerX, double centerZ) {
        AABB search = new AABB(centerX - 80, -64, centerZ - 80, centerX + 80, 320, centerZ + 80);
        for (Entity e : level.getEntitiesOfClass(Entity.class, search)) {
            if (e.getUUID().equals(uuid)) return e;
        }
        return null;
    }

    private static LivingEntity pickDesiredTarget(ServerLevel level, ServerPlayer player, long time) {
        var data = player.getPersistentData();
        if (data.hasUUID(KEY_CMD_TARGET)) {
            UUID uuid = data.getUUID(KEY_CMD_TARGET);
            LivingEntity e = findLivingByUuid(level, uuid, player.getX(), player.getZ());
            if (e != null && e.isAlive() && e != player) return e;
            data.remove(KEY_CMD_TARGET);
        }
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && attacker.level() == level) return attacker;
        LivingEntity victim = player.getLastHurtMob();
        if (victim != null && victim.isAlive() && victim.level() == level) return victim;
        return null;
    }

    private static boolean isAllowedTarget(ServerPlayer owner, LivingEntity current, LivingEntity desired) {
        if (current == owner) return false;
        if (desired != null && current == desired) return true;
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && attacker == current) return true;
        LivingEntity victim = owner.getLastHurtMob();
        if (victim != null && victim == current) return true;
        return false;
    }

    private static LivingEntity findLivingByUuid(ServerLevel level, UUID uuid, double centerX, double centerZ) {
        AABB search = new AABB(centerX - 96, -64, centerZ - 96, centerX + 96, 320, centerZ + 96);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search)) {
            if (e.getUUID().equals(uuid)) return e;
        }
        return null;
    }
}

