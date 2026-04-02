package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CRangerThornHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CRangerThornHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "ranger_thorn_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CRangerThornHide> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeUUID(m.id()),
            buf -> new S2CRangerThornHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CRangerThornHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.ThornBushClient.hide(msg.id()));
    }
}
