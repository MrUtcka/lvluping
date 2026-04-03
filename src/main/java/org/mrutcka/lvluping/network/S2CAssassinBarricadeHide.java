package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CAssassinBarricadeHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CAssassinBarricadeHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "assassin_barricade_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CAssassinBarricadeHide> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeUUID(msg.id()),
            buf -> new S2CAssassinBarricadeHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAssassinBarricadeHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.AssassinBarricadeClient.hide(msg.id()));
    }
}

