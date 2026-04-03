package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CAssassinTripwireHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CAssassinTripwireHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "assassin_tripwire_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CAssassinTripwireHide> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeUUID(msg.id()),
            buf -> new S2CAssassinTripwireHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAssassinTripwireHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.AssassinTripwireClient.hide(msg.id()));
    }
}

