package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.mrutcka.lvluping.LvlupingMod;

public record S2CProvocationHint(boolean active) implements CustomPacketPayload {
    public static final Type<S2CProvocationHint> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "provocation_hint"));

    public static final StreamCodec<FriendlyByteBuf, S2CProvocationHint> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeBoolean(msg.active()),
            buf -> new S2CProvocationHint(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(S2CProvocationHint msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.ProvocationHintClient.setProvocationActive(msg.active()));
    }
}
