package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CHunterTrapHide(UUID id) implements CustomPacketPayload {
    public static final Type<S2CHunterTrapHide> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "hunter_trap_hide"));

    public static final StreamCodec<FriendlyByteBuf, S2CHunterTrapHide> STREAM_CODEC = StreamCodec.of(
            (buf, m) -> buf.writeUUID(m.id()),
            buf -> new S2CHunterTrapHide(buf.readUUID())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CHunterTrapHide msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.HunterTrapClient.hide(msg.id()));
    }
}
