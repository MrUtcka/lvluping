package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CMergeTreeShow(UUID id, double x, double y, double z, float yRot, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CMergeTreeShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "merge_tree_show"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> buf.writeUUID(u),
            buf -> buf.readUUID()
    );

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            FriendlyByteBuf::readLong
    );

    public static final StreamCodec<FriendlyByteBuf, S2CMergeTreeShow> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, S2CMergeTreeShow::id,
            ByteBufCodecs.DOUBLE, S2CMergeTreeShow::x,
            ByteBufCodecs.DOUBLE, S2CMergeTreeShow::y,
            ByteBufCodecs.DOUBLE, S2CMergeTreeShow::z,
            ByteBufCodecs.FLOAT, S2CMergeTreeShow::yRot,
            LONG_CODEC, S2CMergeTreeShow::untilGameTime,
            S2CMergeTreeShow::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CMergeTreeShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.MergeTreeClient.show(
                msg.id(), msg.x(), msg.y(), msg.z(), msg.yRot(), msg.untilGameTime()));
    }
}
