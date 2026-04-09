package org.mrutcka.lvluping.handler;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.PlayerStatTrainingData;
import org.mrutcka.lvluping.data.StatTrainingConfig;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public final class StatTrainingHandler {

    private StatTrainingHandler() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(victim.level() instanceof ServerLevel sl)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        var typeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (StatTrainingConfig.minecellsEntityMatches(typeId)) {
            long t = sl.getGameTime();
            long last = attacker.getPersistentData().getLong("lvluping_train_melee_cd");
            if (t - last >= StatTrainingConfig.meleeTrainingCooldownTicks) {
                attacker.getPersistentData().putLong("lvluping_train_melee_cd", t);
                PlayerStatTrainingData.addDamageProgress(
                        attacker,
                        StatTrainingConfig.damageFromMannequinHit,
                        PlayerStatTrainingData.FatigueTrack.MELEE,
                        StatTrainingConfig.damageFromMannequinHit * 3
                );
            }
        }

        if (victim instanceof ServerPlayer victimPlayer && victimPlayer != attacker) {
            var zone = StatTrainingConfig.healthPvpZone;
            if (zone.enabled()
                    && zone.contains(sl, attacker.getX(), attacker.getY(), attacker.getZ())
                    && zone.contains(sl, victimPlayer.getX(), victimPlayer.getY(), victimPlayer.getZ())) {
                PlayerStatTrainingData.addHealthProgress(attacker, StatTrainingConfig.healthFromPvPHit);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().level() instanceof ServerLevel sl)) return;
        Projectile proj = event.getProjectile();
        if (!(proj.getOwner() instanceof ServerPlayer sp)) return;
        if (event.getRayTraceResult() instanceof BlockHitResult bhr) {
            if (sl.getBlockState(bhr.getBlockPos()).is(Blocks.HAY_BLOCK) && isBowLikeProjectile(proj)) {
                PlayerStatTrainingData.addDamageProgress(
                        sp,
                        StatTrainingConfig.damageFromHayHit,
                        PlayerStatTrainingData.FatigueTrack.BOW,
                        StatTrainingConfig.damageFromHayHit * 4
                );
            }
        }
    }

    private static boolean isBowLikeProjectile(Projectile proj) {
        EntityType<?> t = proj.getType();
        return t == EntityType.ARROW || t == EntityType.SPECTRAL_ARROW;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide()) return;
        if (sp.tickCount % 20 == 0) {
            PlayerStatTrainingData.decayFatigue(sp.getUUID());
        }
        if (sp.tickCount % 5 != 0) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;
        var zone = StatTrainingConfig.speedZone;
        if (!zone.enabled() || !zone.contains(sl, sp.getX(), sp.getY(), sp.getZ())) {
            return;
        }
        var tag = sp.getPersistentData();
        if (!tag.contains("lvluping_spdx")) {
            tag.putDouble("lvluping_spdx", sp.getX());
            tag.putDouble("lvluping_spdz", sp.getZ());
            return;
        }
        double lx = tag.getDouble("lvluping_spdx");
        double lz = tag.getDouble("lvluping_spdz");
        double px = sp.getX();
        double pz = sp.getZ();
        double dx = px - lx;
        double dz = pz - lz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist >= 0.35) {
            int blocks = Mth.floor(dist);
            if (blocks > 0) {
                PlayerStatTrainingData.addSpeedProgress(sp, blocks * StatTrainingConfig.speedUnitsPerBlock);
            }
            tag.putDouble("lvluping_spdx", px);
            tag.putDouble("lvluping_spdz", pz);
        }
    }
}
