package org.mrutcka.lvluping.handler;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.network.S2CSyncCooldown;

import java.util.Set;

public class TalentAbilityHandler {

    private static final int SLIDE_COOLDOWN = 200;
    private static final int SMOKE_COOLDOWN = 400;
    private static final int DASH_COOLDOWN = 160;
    private static final int PARRY_COOLDOWN = 100;
    private static final int BUFF_COOLDOWN = 600;

    private static final int PARRY_WINDOW = 20;
    private static final int BARRIER_WINDOW = 200;

    public static boolean isDagger(Item item) {
        return item == Items.IRON_SWORD;
    }

    private static void setCooldown(ServerPlayer player, String key, int ticks) {
        player.getPersistentData().putInt(key, ticks);
        PacketDistributor.sendToPlayer(player, new S2CSyncCooldown(key, ticks));
    }

    public static void syncAllCooldowns(ServerPlayer player) {
        String[] keys = {"cd_slide", "cd_smoke", "cd_dash", "cd_parry", "cd_buff"};
        for (String key : keys) {
            int val = player.getPersistentData().getInt(key);
            if (val > 0) {
                PacketDistributor.sendToPlayer(player, new org.mrutcka.lvluping.network.S2CSyncCooldown(key, val));
            }
        }
    }

    public static void handleAbilityUse(ServerPlayer player) {
        Set<String> talents = PlayerLevels.getPlayerTalents(player.getUUID());
        var data = player.getPersistentData();

        // --- W_PARRY ---
        if (talents.contains("w_parry") && data.getInt("cd_parry") <= 0) {
            setCooldown(player, "lvluping_parry_window", PARRY_WINDOW);
            setCooldown(player, "cd_parry", PARRY_COOLDOWN);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // --- M_BARRIER SHIELD ATTACK ---
        if (talents.contains("m_barrier") && data.getInt("cd_buff") <= 0) {
            setCooldown(player, "cd_buff", BUFF_COOLDOWN);
            setCooldown(player, "lvluping_barrier_window", BARRIER_WINDOW);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);

            if (talents.contains("m_buff_def")) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
                if (talents.contains("m_buff_atk")) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
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
}

