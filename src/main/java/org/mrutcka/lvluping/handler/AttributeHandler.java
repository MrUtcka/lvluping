package org.mrutcka.lvluping.handler;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.AttributeStat;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.UUID;

public class AttributeHandler {
    private static final ResourceLocation PALADIN_HP_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "paladin_hp");
    private static final ResourceLocation SWORDMASTER_AS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "swordmaster_attack_speed");
    private static final ResourceLocation BARBARIAN_AS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "barbarian_attack_speed");
    private static final ResourceLocation BARBARIAN_HP_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "barbarian_hp");
    private static final ResourceLocation SWORDMASTER_HAND_AS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "swordmaster_hand_attack_speed");
    private static final ResourceLocation SWORDMASTER_HARDENED_AS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "swordmaster_hardened_attack_speed");
    private static final ResourceLocation SWORDMASTER_AGILITY_MS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "swordmaster_agility_move_speed");
    private static final ResourceLocation WANDERER_BASE_MS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "wanderer_base_move_speed");
    private static final ResourceLocation WANDERER_ENDURANCE_MS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "wanderer_endurance_move_speed");
    private static final ResourceLocation ROGUE_EDGE_HP_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "rogue_edge_hp");
    private static final ResourceLocation ROGUE_EDGE_DMG_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "rogue_edge_damage");
    private static final ResourceLocation ROGUE_TRAINED_AS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "rogue_trained_attack_speed");
    private static final ResourceLocation AS_EVO_LETHALITY_DMG_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "as_evo_lethality_damage");
    private static final ResourceLocation AS_EVO_MOBILITY_MS_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "as_evo_mobility_move_speed");
    private static final ResourceLocation AS_EVO_ENDURANCE_HP_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "as_evo_endurance_hp");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_health");
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_damage");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_speed");

    private static final double HEALTH_PER_STAT_LEVEL = 1.0;
    private static final double DAMAGE_PER_STAT_LEVEL = 0.25;
    private static final double SPEED_PER_STAT_LEVEL = 0.0025;

    public static void applyStats(ServerPlayer player, boolean isHeal) {
        UUID uuid = player.getUUID();

        int healthLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.HEALTH.id);
        int damageLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.DAMAGE.id);
        int speedLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.SPEED.id);

        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_ID);
            healthAttr.removeModifier(PALADIN_HP_ID);
            healthAttr.removeModifier(BARBARIAN_HP_ID);
            healthAttr.removeModifier(ROGUE_EDGE_HP_ID);
            healthAttr.removeModifier(AS_EVO_ENDURANCE_HP_ID);
            if (healthLvl > 0) {
                healthAttr.addTransientModifier(new AttributeModifier(HEALTH_ID, (double) healthLvl * HEALTH_PER_STAT_LEVEL, AttributeModifier.Operation.ADD_VALUE));
            }
            var owned = PlayerLevels.getPlayerTalents(uuid);
            if (owned.contains("w_paladin_base")) {
                double m = AbilityUpgradeConfig.getDouble("w_paladin_base", "hp_mult", 1, 1.2) - 1.0;
                if (m > 1.0e-4) {
                    healthAttr.addTransientModifier(new AttributeModifier(PALADIN_HP_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("w_barbarian_base")) {
                double m = AbilityUpgradeConfig.getDouble("w_barbarian_base", "hp_mult", 1, 1.1) - 1.0;
                if (m > 1.0e-4) {
                    healthAttr.addTransientModifier(new AttributeModifier(BARBARIAN_HP_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_rogue_edge")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "as_rogue_edge", owned);
                double m = AbilityUpgradeConfig.getDouble("as_rogue_edge", "hp_mult", lvl, 0.85) - 1.0;
                if (Math.abs(m) > 1.0e-4) {
                    healthAttr.addTransientModifier(new AttributeModifier(ROGUE_EDGE_HP_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_evo_endurance")) {
                double m = AbilityUpgradeConfig.getDouble("as_evo_endurance", "hp_mult", 1, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    healthAttr.addTransientModifier(new AttributeModifier(AS_EVO_ENDURANCE_HP_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }

        if (isHeal) {
            player.setHealth(player.getMaxHealth());
        } else {
            float savedHealth = PlayerLevels.getStoredHealth(uuid);
            if (savedHealth > 0) {
                player.setHealth(Math.min(savedHealth, player.getMaxHealth()));
            } else {
                player.setHealth(player.getMaxHealth());
            }
        }

        var attackAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(DAMAGE_ID);
            attackAttr.removeModifier(ROGUE_EDGE_DMG_ID);
            attackAttr.removeModifier(AS_EVO_LETHALITY_DMG_ID);
            if (damageLvl > 0) {
                attackAttr.addTransientModifier(new AttributeModifier(DAMAGE_ID, (double) damageLvl * DAMAGE_PER_STAT_LEVEL, AttributeModifier.Operation.ADD_VALUE));
            }
            var owned = PlayerLevels.getPlayerTalents(uuid);
            if (owned.contains("as_rogue_edge")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "as_rogue_edge", owned);
                double m = AbilityUpgradeConfig.getDouble("as_rogue_edge", "damage_mult", lvl, 1.3) - 1.0;
                if (m > 1.0e-4) {
                    attackAttr.addTransientModifier(new AttributeModifier(ROGUE_EDGE_DMG_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_evo_lethality")) {
                double m = AbilityUpgradeConfig.getDouble("as_evo_lethality", "melee_damage_mult", 1, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    attackAttr.addTransientModifier(new AttributeModifier(AS_EVO_LETHALITY_DMG_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }

        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_ID);
            speedAttr.removeModifier(SWORDMASTER_AGILITY_MS_ID);
            speedAttr.removeModifier(WANDERER_BASE_MS_ID);
            speedAttr.removeModifier(WANDERER_ENDURANCE_MS_ID);
            speedAttr.removeModifier(AS_EVO_MOBILITY_MS_ID);
            if (speedLvl > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_ID, (double) speedLvl * SPEED_PER_STAT_LEVEL, AttributeModifier.Operation.ADD_VALUE));
            }
            var owned = PlayerLevels.getPlayerTalents(uuid);
            if (owned.contains("w_swordmaster_agility")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "w_swordmaster_agility", owned);
                double m = AbilityUpgradeConfig.getDouble("w_swordmaster_agility", "move_speed_mult", lvl, 1.03) - 1.0;
                if (m > 1.0e-4) {
                    speedAttr.addTransientModifier(new AttributeModifier(SWORDMASTER_AGILITY_MS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_wanderer_base")) {
                double m = AbilityUpgradeConfig.getDouble("as_wanderer_base", "move_speed_mult", 1, 1.1) - 1.0;
                if (m > 1.0e-4) {
                    speedAttr.addTransientModifier(new AttributeModifier(WANDERER_BASE_MS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_wanderer_endurance")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "as_wanderer_endurance", owned);
                double m = AbilityUpgradeConfig.getDouble("as_wanderer_endurance", "move_speed_mult", lvl, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    speedAttr.addTransientModifier(new AttributeModifier(WANDERER_ENDURANCE_MS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_evo_mobility")) {
                double m = AbilityUpgradeConfig.getDouble("as_evo_mobility", "move_speed_mult", 1, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    speedAttr.addTransientModifier(new AttributeModifier(AS_EVO_MOBILITY_MS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }

        var attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            attackSpeedAttr.removeModifier(SWORDMASTER_AS_ID);
            attackSpeedAttr.removeModifier(BARBARIAN_AS_ID);
            attackSpeedAttr.removeModifier(SWORDMASTER_HAND_AS_ID);
            attackSpeedAttr.removeModifier(SWORDMASTER_HARDENED_AS_ID);
            attackSpeedAttr.removeModifier(ROGUE_TRAINED_AS_ID);
            var owned = PlayerLevels.getPlayerTalents(uuid);
            if (owned.contains("w_swordmaster_base")) {
                double m = AbilityUpgradeConfig.getDouble("w_swordmaster_base", "attack_speed_mult", 1, 1.1) - 1.0;
                if (m > 1.0e-4) {
                    attackSpeedAttr.addTransientModifier(new AttributeModifier(SWORDMASTER_AS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("w_barbarian_base")) {
                double m = AbilityUpgradeConfig.getDouble("w_barbarian_base", "attack_speed_mult", 1, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    attackSpeedAttr.addTransientModifier(new AttributeModifier(BARBARIAN_AS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("w_swordmaster_hand_dexterity")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "w_swordmaster_hand_dexterity", owned);
                double m = AbilityUpgradeConfig.getDouble("w_swordmaster_hand_dexterity", "attack_speed_mult", lvl, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    attackSpeedAttr.addTransientModifier(new AttributeModifier(SWORDMASTER_HAND_AS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("w_swordmaster_hardened_mastery")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "w_swordmaster_hardened_mastery", owned);
                double m = AbilityUpgradeConfig.getDouble("w_swordmaster_hardened_mastery", "attack_speed_mult", lvl, 1.05) - 1.0;
                if (m > 1.0e-4) {
                    attackSpeedAttr.addTransientModifier(new AttributeModifier(SWORDMASTER_HARDENED_AS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
            if (owned.contains("as_rogue_trained")) {
                int lvl = PlayerLevels.getAbilityLevel(uuid, "as_rogue_trained", owned);
                double m = AbilityUpgradeConfig.getDouble("as_rogue_trained", "attack_speed_mult", lvl, 1.08) - 1.0;
                if (m > 1.0e-4) {
                    attackSpeedAttr.addTransientModifier(new AttributeModifier(ROGUE_TRAINED_AS_ID, m, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            }
        }
    }
}