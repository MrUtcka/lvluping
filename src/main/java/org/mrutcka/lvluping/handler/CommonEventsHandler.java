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
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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

        if (!player.level().isClientSide) {
            decrementCooldown(player, "cd_slide");
            decrementCooldown(player, "cd_smoke");
            decrementCooldown(player, "cd_dash");
            decrementCooldown(player, "cd_parry");
            decrementCooldown(player, "cd_buff");
            decrementCooldown(player, "cd_w_shield");
            decrementCooldown(player, "cd_w_seismic");
            decrementCooldown(player, "cd_w_iron_skin");
            decrementCooldown(player, "cd_w_spin");
            decrementCooldown(player, "cd_w_heavy_step");
            decrementCooldown(player, "cd_w_unbreakable");
            decrementCooldown(player, "cd_w_armor_breaker");

            decrementWindow(player, "lvluping_parry_window");
            decrementWindow(player, "lvluping_barrier_window");
            decrementWindow(player, "lvluping_shield_window");
            decrementCooldown(player, "cd_w_provocation");
            decrementCooldown(player, "cd_w_ult_berserk");
            decrementCooldown(player, "cd_w_ult_brotherhood");
            decrementCooldown(player, "cd_w_ult_final_countdown");
            decrementCooldown(player, "cd_w_ult_invulnerability");
            decrementCooldown(player, "cd_m_fire");
            decrementCooldown(player, "cd_m_ice");
            decrementCooldown(player, "cd_m_teleport");
            decrementCooldown(player, "cd_m_summon");
            decrementCooldown(player, "cd_m_sacrifice");
            decrementCooldown(player, "cd_m_command");

            if (UltimatesHandler.isInvulnerabilityActive(player)) {
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;
                player.setPos(player.xOld, player.yOld, player.zOld);
            }
            if (UltimatesHandler.isBerserkActive(player) && player.isBlocking()) {
                player.stopUsingItem();
            }

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
        }
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

    private static void decrementCooldown(Player player, String key) {
        int val = PlayerLevels.getCooldown(player.getUUID(), key);
        if (val > 0) {
            val -= 1;
            PlayerLevels.setCooldown(player.getUUID(), key, val);
            player.getPersistentData().putInt(key, val);
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
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && UltimatesHandler.isInvulnerabilityActive(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (UltimatesHandler.isInvulnerabilityActive(attacker)) {
            event.setCanceled(true);
            return;
        }
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
        // Финальный отсчёт: урон от молнии отменяем (урон уже нанесён способностью)
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

        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            ServerPlayer provoker = ProvocationHandler.getActiveProvoker(serverLevel);
            var attacker = event.getSource().getEntity();
            if (provoker != null && attacker != null && attacker != provoker && event.getEntity() != provoker) {
                event.setAmount(event.getAmount() * 0.5f);
            }
        }

        if (event.getEntity() instanceof Player victim) {
            if (UltimatesHandler.isInvulnerabilityActive(victim)) {
                event.setCanceled(true);
                return;
            }

            Set<String> talents = PlayerLevels.getPlayerTalents(victim.getUUID());

            // W_UNBREAKABLE — одноразовый тотем: при смертельном ударе, если не в КД, не умираем + реген
            if (talents.contains("w_unbreakable") && PlayerLevels.getCooldown(victim.getUUID(), "cd_w_unbreakable") <= 0) {
                if (victim.getHealth() - event.getAmount() <= 0f) {
                    event.setCanceled(true);
                    victim.setHealth(2f);
                    victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
                    PlayerLevels.setCooldown(victim.getUUID(), "cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN);
                    victim.getPersistentData().putInt("cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN);
                    if (victim instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_w_unbreakable", TalentAbilityHandler.UNBREAKABLE_COOLDOWN));
                    }
                    victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1f);
                    if (victim.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1, victim.getZ(), 30, 0.4, 0.5, 0.4, 0.2);
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
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.2f);

                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1, victim.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
                }

                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
                }
            }

            // --- W_BARRIER ---
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

            // --- W_SHIELD_STRIKE ---
            if (talents.contains("w_shield_strike") && victim.getPersistentData().getInt("lvluping_shield_window") > 0) {
                event.setCanceled(true);
                victim.getPersistentData().putInt("lvluping_shield_window", 0);
                PlayerLevels.setCooldown(victim.getUUID(), "cd_w_shield", TalentAbilityHandler.SHIELD_COOLDOWN);
                victim.getPersistentData().putInt("cd_w_shield", TalentAbilityHandler.SHIELD_COOLDOWN);
                if (victim instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_w_shield", TalentAbilityHandler.SHIELD_COOLDOWN));
                }

                victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.9f);

                if (victim.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                            8, 0.3, 0.3, 0.3, 0.2);
                }
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

            // --- W_ULT_BERSERK ---
            if (UltimatesHandler.isBerserkActive(attacker)) {
                float maxH = attacker.getMaxHealth();
                float curH = attacker.getHealth();
                float missingRatio = maxH > 0 ? 1f - (curH / maxH) : 0f;
                float multiplier = 1f + missingRatio * 1.2f;
                event.setNewDamage(event.getNewDamage() * multiplier);
                attacker.heal(maxH * 0.02f);
                if (attacker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
                }
            }

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
                    if (combo > 1 && attacker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 0.5, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                comboMultiplier = 1.0f + (combo * 0.1f);
                event.setNewDamage(event.getNewDamage() * comboMultiplier);
            }


            // --- W_STUN ---
            if (talents.contains("w_stun") && attacker.getRandom().nextFloat() < 0.2f) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 128, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 128, false, false));

                attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.5f);

                if (attacker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.5, target.getZ(), 1, 0, 0, 0, 0);
                }
            }

            // --- W_BLOODLUST ---
            if (talents.contains("w_bloodlust")) {
                int hits = attacker.getPersistentData().getInt("lvluping_w_hits");
                hits++;
                if (hits >= 5) {
                    hits = 0;
                    float healAmount = event.getNewDamage() * 0.3f;
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

            // --- W_ARMOR_BREAKER ---
            if (talents.contains("w_armor_breaker") && PlayerLevels.getCooldown(attacker.getUUID(), "cd_w_armor_breaker") <= 0) {
                if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                    float newDamage = event.getNewDamage() * 1.5f;
                    event.setNewDamage(newDamage);

                    if (attacker.level() instanceof ServerLevel sl) {
                        long expire = sl.getGameTime() + 100;
                        target.getPersistentData().putLong("lvluping_w_armor_break_until", expire);

                        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8f, 0.7f);
                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(),
                                10, 0.3, 0.3, 0.3, 0.15);
                    }

                    PlayerLevels.setCooldown(attacker.getUUID(), "cd_w_armor_breaker", 200);
                    attacker.getPersistentData().putInt("cd_w_armor_breaker", 200);
                    if (attacker instanceof ServerPlayer sp) {
                        PacketDistributor.sendToPlayer(sp, new S2CSyncCooldown("cd_w_armor_breaker", 200));
                    }
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