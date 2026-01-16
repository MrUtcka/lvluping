package org.mrutcka.lvluping.network;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.mrutcka.lvluping.LvlupingMod;

@EventBusSubscriber(modid = LvlupingMod.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(LvlupingMod.MODID)
                .versioned("1");

        registrar.playToClient(
                S2CSyncTalents.TYPE,
                S2CSyncTalents.STREAM_CODEC,
                S2CSyncTalents::handle
        );

        registrar.playToServer(
                C2SPurchaseTalent.TYPE,
                C2SPurchaseTalent.STREAM_CODEC,
                C2SPurchaseTalent::handle
        );

        registrar.playToServer(
                C2SUpgradeStat.TYPE,
                C2SUpgradeStat.STREAM_CODEC,
                C2SUpgradeStat::handle
        );
    }
}