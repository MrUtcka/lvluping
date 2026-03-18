package org.mrutcka.lvluping.handler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.mrutcka.lvluping.LvlupingMod;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class HealEventsHandler {

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        long expire = entity.getPersistentData().getLong("lvluping_w_armor_break_until");
        if (expire > 0 && serverLevel.getGameTime() <= expire) {
            event.setAmount(event.getAmount() * 0.6f);
        } else if (expire > 0 && serverLevel.getGameTime() > expire) {
            entity.getPersistentData().remove("lvluping_w_armor_break_until");
        }
    }
}

