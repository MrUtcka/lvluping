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

        registrar.playToServer(
                C2SUseAbility.TYPE,
                C2SUseAbility.STREAM_CODEC,
                C2SUseAbility::handle
        );

        registrar.playToServer(
                C2SUpgradeAbility.TYPE,
                C2SUpgradeAbility.STREAM_CODEC,
                C2SUpgradeAbility::handle
        );

        registrar.playToServer(
                C2SPossessionInput.TYPE,
                C2SPossessionInput.STREAM_CODEC,
                C2SPossessionInput::handle
        );

        registrar.playToClient(
                S2CSyncCooldown.TYPE,
                S2CSyncCooldown.STREAM_CODEC,
                S2CSyncCooldown::handle
        );

        registrar.playToClient(
                S2CProvocationHint.TYPE,
                S2CProvocationHint.STREAM_CODEC,
                S2CProvocationHint::handle
        );

        registrar.playToClient(
                S2CJudgementHammerEffect.TYPE,
                S2CJudgementHammerEffect.STREAM_CODEC,
                S2CJudgementHammerEffect::handle
        );

        registrar.playToClient(
                S2CUnbreakableShieldOrbit.TYPE,
                S2CUnbreakableShieldOrbit.STREAM_CODEC,
                S2CUnbreakableShieldOrbit::handle
        );

        registrar.playToClient(
                S2CHunterTrapShow.TYPE,
                S2CHunterTrapShow.STREAM_CODEC,
                S2CHunterTrapShow::handle
        );

        registrar.playToClient(
                S2CHunterTrapHide.TYPE,
                S2CHunterTrapHide.STREAM_CODEC,
                S2CHunterTrapHide::handle
        );

        registrar.playToClient(
                S2CMergeTreeShow.TYPE,
                S2CMergeTreeShow.STREAM_CODEC,
                S2CMergeTreeShow::handle
        );

        registrar.playToClient(
                S2CMergeTreeHide.TYPE,
                S2CMergeTreeHide.STREAM_CODEC,
                S2CMergeTreeHide::handle
        );

        registrar.playToClient(
                S2CRangerThornShow.TYPE,
                S2CRangerThornShow.STREAM_CODEC,
                S2CRangerThornShow::handle
        );

        registrar.playToClient(
                S2CRangerThornHide.TYPE,
                S2CRangerThornHide.STREAM_CODEC,
                S2CRangerThornHide::handle
        );

        registrar.playToClient(
                S2CRangerLifeTotemShow.TYPE,
                S2CRangerLifeTotemShow.STREAM_CODEC,
                S2CRangerLifeTotemShow::handle
        );

        registrar.playToClient(
                S2CRangerLifeTotemHide.TYPE,
                S2CRangerLifeTotemHide.STREAM_CODEC,
                S2CRangerLifeTotemHide::handle
        );

        registrar.playToClient(
                S2CRangerRootsTargetShow.TYPE,
                S2CRangerRootsTargetShow.STREAM_CODEC,
                S2CRangerRootsTargetShow::handle
        );

        registrar.playToClient(
                S2CRangerRootsTargetHide.TYPE,
                S2CRangerRootsTargetHide.STREAM_CODEC,
                S2CRangerRootsTargetHide::handle
        );

        registrar.playToClient(
                S2CAssassinBarricadeShow.TYPE,
                S2CAssassinBarricadeShow.STREAM_CODEC,
                S2CAssassinBarricadeShow::handle
        );
        registrar.playToClient(
                S2CAssassinBarricadeHide.TYPE,
                S2CAssassinBarricadeHide.STREAM_CODEC,
                S2CAssassinBarricadeHide::handle
        );
        registrar.playToClient(
                S2CAssassinTripwireShow.TYPE,
                S2CAssassinTripwireShow.STREAM_CODEC,
                S2CAssassinTripwireShow::handle
        );
        registrar.playToClient(
                S2CAssassinTripwireHide.TYPE,
                S2CAssassinTripwireHide.STREAM_CODEC,
                S2CAssassinTripwireHide::handle
        );
        registrar.playToClient(
                S2CAssassinCampShow.TYPE,
                S2CAssassinCampShow.STREAM_CODEC,
                S2CAssassinCampShow::handle
        );
        registrar.playToClient(
                S2CAssassinCampHide.TYPE,
                S2CAssassinCampHide.STREAM_CODEC,
                S2CAssassinCampHide::handle
        );
    }
}