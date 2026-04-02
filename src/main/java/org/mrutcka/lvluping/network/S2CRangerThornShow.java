package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CRangerThornShow(UUID id, double x, double y, double z, float yRot, double radius, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CRangerThornShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "ranger_thorn_show"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> buf.writeUUID(u),
            buf -> buf.readUUID()
    );

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            buf -> buf.readLong()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CRangerThornShow> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeUUID(msg.id());
                buf.writeDouble(msg.x());
                buf.writeDouble(msg.y());
                buf.writeDouble(msg.z());
                buf.writeFloat(msg.yRot());
                buf.writeDouble(msg.radius());
                buf.writeLong(msg.untilGameTime());
            },
            buf -> new S2CRangerThornShow(
                    buf.readUUID(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readDouble(),
                    buf.readLong()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CRangerThornShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.ThornBushClient.show(
                msg.id(), msg.x(), msg.y(), msg.z(), msg.yRot(), msg.radius(), msg.untilGameTime()));
    }
}
