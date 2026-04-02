package org.mrutcka.lvluping.handler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.PlayerLevels;

import java.util.Set;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class HealEventsHandler {

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        long martyrUntil = entity.getPersistentData().getLong("lvluping_cleric_martyr_until");
        if (martyrUntil > serverLevel.getGameTime()) {
            event.setAmount(0f);
            return;
        }

        long expire = entity.getPersistentData().getLong("lvluping_w_armor_break_until");
        if (expire > 0 && serverLevel.getGameTime() <= expire) {
            event.setAmount(event.getAmount() * 0.6f);
        } else if (expire > 0 && serverLevel.getGameTime() > expire) {
            entity.getPersistentData().remove("lvluping_w_armor_break_until");
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            Set<String> talents = PlayerLevels.getPlayerTalents(sp.getUUID());
            if (talents.contains("w_paladin_healing_touch")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "w_paladin_healing_touch", talents);
                double mult = AbilityUpgradeConfig.getDouble("w_paladin_healing_touch", "incoming_heal_mult", lvl, 1.15);
                event.setAmount((float) (event.getAmount() * mult));
            }
            if (talents.contains("w_barbarian_indestructible_body")) {
                int lvl = PlayerLevels.getAbilityLevel(sp.getUUID(), "w_barbarian_indestructible_body", talents);
                double mult = AbilityUpgradeConfig.getDouble("w_barbarian_indestructible_body", "heal_mult", lvl, 0.8);
                event.setAmount((float) (event.getAmount() * mult));
            }
        }
    }
}

