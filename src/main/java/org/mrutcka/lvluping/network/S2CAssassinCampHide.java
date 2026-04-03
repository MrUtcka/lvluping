package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CAssassinCampHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CAssassinCampHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "assassin_camp_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CAssassinCampHide> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeUUID(msg.id()),
            buf -> new S2CAssassinCampHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAssassinCampHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.AssassinCampClient.hide(msg.id()));
    }
}

