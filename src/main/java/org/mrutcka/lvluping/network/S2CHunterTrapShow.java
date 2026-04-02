package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CHunterTrapShow(UUID id, double x, double y, double z, float yRot, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CHunterTrapShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "hunter_trap_show"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> buf.writeUUID(u),
            buf -> buf.readUUID()
    );

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            buf -> buf.readLong()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CHunterTrapShow> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, S2CHunterTrapShow::id,
            ByteBufCodecs.DOUBLE, S2CHunterTrapShow::x,
            ByteBufCodecs.DOUBLE, S2CHunterTrapShow::y,
            ByteBufCodecs.DOUBLE, S2CHunterTrapShow::z,
            ByteBufCodecs.FLOAT, S2CHunterTrapShow::yRot,
            LONG_CODEC, S2CHunterTrapShow::untilGameTime,
            S2CHunterTrapShow::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CHunterTrapShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.HunterTrapClient.show(
                msg.id(), msg.x(), msg.y(), msg.z(), msg.yRot(), msg.untilGameTime()));
    }
}
