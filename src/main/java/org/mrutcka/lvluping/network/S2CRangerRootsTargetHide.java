package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

public record S2CRangerRootsTargetHide(int entityId) implements CustomPacketPayload {
    public static final Type<S2CRangerRootsTargetHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "ranger_roots_target_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CRangerRootsTargetHide> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CRangerRootsTargetHide::entityId,
            S2CRangerRootsTargetHide::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CRangerRootsTargetHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.RootsTargetClient.hide(msg.entityId()));
    }
}
