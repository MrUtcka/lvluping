package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CAssassinBarricadeShow(UUID id, double x, double y, double z, float yRot, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CAssassinBarricadeShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "assassin_barricade_show"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUUID(v),
            buf -> buf.readUUID()
    );

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            buf -> buf.readLong()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CAssassinBarricadeShow> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, S2CAssassinBarricadeShow::id,
            ByteBufCodecs.DOUBLE, S2CAssassinBarricadeShow::x,
            ByteBufCodecs.DOUBLE, S2CAssassinBarricadeShow::y,
            ByteBufCodecs.DOUBLE, S2CAssassinBarricadeShow::z,
            ByteBufCodecs.FLOAT, S2CAssassinBarricadeShow::yRot,
            LONG_CODEC, S2CAssassinBarricadeShow::untilGameTime,
            S2CAssassinBarricadeShow::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAssassinBarricadeShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.AssassinBarricadeClient.show(
                msg.id(), msg.x(), msg.y(), msg.z(), msg.yRot(), msg.untilGameTime()
        ));
    }
}

