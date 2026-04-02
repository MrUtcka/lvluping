package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

public record S2CRangerRootsTargetShow(int entityId, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CRangerRootsTargetShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "ranger_roots_target_show"));

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            buf -> buf.readLong()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CRangerRootsTargetShow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CRangerRootsTargetShow::entityId,
            LONG_CODEC, S2CRangerRootsTargetShow::untilGameTime,
            S2CRangerRootsTargetShow::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CRangerRootsTargetShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.RootsTargetClient.show(msg.entityId(), msg.untilGameTime()));
    }
}
