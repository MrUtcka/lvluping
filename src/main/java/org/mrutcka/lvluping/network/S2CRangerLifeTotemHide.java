package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CRangerLifeTotemHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CRangerLifeTotemHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "ranger_life_totem_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CRangerLifeTotemHide> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeUUID(m.id()),
            buf -> new S2CRangerLifeTotemHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CRangerLifeTotemHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.LifeTotemClient.hide(msg.id()));
    }
}
