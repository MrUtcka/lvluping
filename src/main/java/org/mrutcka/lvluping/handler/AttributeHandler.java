package org.mrutcka.lvluping.handler;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AttributeStat;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.UUID;

public class AttributeHandler {
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_health");
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_damage");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "stat_speed");

    public static void applyStats(ServerPlayer player, boolean isHeal) {
        UUID uuid = player.getUUID();

        int healthLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.HEALTH.id);
        int damageLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.DAMAGE.id);
        int speedLvl = PlayerLevels.getStatLevel(uuid, AttributeStat.SPEED.id);

        var healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_ID);
            if (healthLvl > 0) {
                healthAttr.addTransientModifier(new AttributeModifier(HEALTH_ID, (double) healthLvl * 2.0, AttributeModifier.Operation.ADD_VALUE));
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
            if (damageLvl > 0) {
                attackAttr.addTransientModifier(new AttributeModifier(DAMAGE_ID, damageLvl * 0.5, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_ID);
            if (speedLvl > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_ID, speedLvl * 0.02, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }
}