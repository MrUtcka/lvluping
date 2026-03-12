package org.mrutcka.lvluping.handler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.S2CSyncCooldown;


import java.util.Set;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class CommonEventsHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        BarrierRender(player);

        decrementData(player, "cd_slide");
        decrementData(player, "cd_smoke");
        decrementData(player, "cd_dash");
        decrementData(player, "cd_parry");
        decrementData(player, "cd_buff");

        //if (!player.level().isClientSide) {
        decrementData(player, "lvluping_parry_window");
        decrementData(player, "lvluping_barrier_window");
        // }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAbilityHandler.syncAllCooldowns(player);
        }
    }

    private static void decrementData(Player player, String key) {
        int val = player.getPersistentData().getInt(key);
        if (val > 0) {
            player.getPersistentData().putInt(key, val - 1);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        Set<String> talents = PlayerLevels.getPlayerTalents(attacker.getUUID());

        // --- A_DAGGER ---
        if (TalentAbilityHandler.isDagger(attacker.getMainHandItem().getItem())) {
            if (!talents.contains("a_dagger")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (PlayerLevels.getPlayerTalents(player.getUUID()).contains("a_power")) {
                int charge = event.getCharge();

                if (charge >= 20) {
                }
            }
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player victim) {
            if (victim.getPersistentData().getInt("lvluping_parry_window") > 0) {
                event.setCanceled(true);
                victim.getPersistentData().putInt("lvluping_parry_window", 0);

                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);

                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
                }
            }

            if (victim.getPersistentData().getInt("lvluping_barrier_window") > 0) {
                event.setCanceled(true);
                victim.getPersistentData().putInt("lvluping_barrier_window", 0);

                victim.level().playSound(null, victim.getX(), victim.getY() + 1, victim.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
                victim.level().playSound(null, victim.getX(), victim.getY() + 1, victim.getZ(),
                        SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);

                if (victim.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 1, victim.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            Set<String> talents = PlayerLevels.getPlayerTalents(attacker.getUUID());
            LivingEntity target = event.getEntity();
            long currentTime = attacker.level().getGameTime();

            // --- AS_CRIT ---
            if (talents.contains("as_crit")) {
                Vec3 lookA = attacker.getLookAngle().normalize();
                Vec3 lookT = target.getLookAngle().normalize();

                if (lookA.dot(lookT) > 0.7) {
                    event.setNewDamage(event.getOriginalDamage() * 2.0f);
                    attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
                }
            }

            // --- A_POWER ---
            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                if (talents.contains("a_power")) {
                    if (arrow.isCritArrow()) {
                        event.setNewDamage(event.getOriginalDamage() * 2f);
                    }
                }
            }

            // --- W_COMBO ---
            float comboMultiplier = 1.0f;
            if (talents.contains("w_combo")) {
                long lastHit = attacker.getPersistentData().getLong("lvluping_last_hit");
                int combo = attacker.getPersistentData().getInt("lvluping_combo");

                if (currentTime - lastHit < 20) {
                    attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2f, 0.5f + (combo * 0.1f));
                    combo = Math.min(combo + 1, 10);
                } else {
                    combo = 1;
                }

                attacker.getPersistentData().putInt("lvluping_combo", combo);
                attacker.getPersistentData().putLong("lvluping_last_hit", currentTime);

                if (attacker instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("lvluping_combo", combo));
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("lvluping_last_hit", (int)currentTime));
                }
                comboMultiplier = 1.0f + (combo * 0.1f);
                event.setNewDamage(event.getNewDamage() * comboMultiplier);
            }


            // --- W_STUN ---
            if (talents.contains("w_stun") && attacker.getRandom().nextFloat() < 0.2f) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 128));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 128));

                attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.5f);

                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.5, target.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
    }

    private static void BarrierRender(Player player) {
        if (!player.level().isClientSide && player.level() instanceof ServerLevel serverLevel) {
            int barrierTicks = player.getPersistentData().getInt("lvluping_barrier_window");
            if (barrierTicks > 0) {
                double time = serverLevel.getGameTime() * 0.15;
                for (int i = 0; i < 4; i++) {
                    double angle = time + (i * Math.PI);
                    double x = player.getX() + Math.cos(angle) * 1.5;
                    double z = player.getZ() + Math.sin(angle) * 1.5;
                    double y = player.getY() + 1;

                    serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.01);
                }
            }
        }
    }
}