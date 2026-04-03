package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CAssassinCampShow(UUID id, double x, double y, double z, float yRot, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CAssassinCampShow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "assassin_camp_show"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeUUID(v),
            buf -> buf.readUUID()
    );

    private static final StreamCodec<FriendlyByteBuf, Long> LONG_CODEC = StreamCodec.of(
            (buf, v) -> buf.writeLong(v),
            buf -> buf.readLong()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CAssassinCampShow> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, S2CAssassinCampShow::id,
            ByteBufCodecs.DOUBLE, S2CAssassinCampShow::x,
            ByteBufCodecs.DOUBLE, S2CAssassinCampShow::y,
            ByteBufCodecs.DOUBLE, S2CAssassinCampShow::z,
            ByteBufCodecs.FLOAT, S2CAssassinCampShow::yRot,
            LONG_CODEC, S2CAssassinCampShow::untilGameTime,
            S2CAssassinCampShow::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CAssassinCampShow msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.AssassinCampClient.show(
                msg.id(), msg.x(), msg.y(), msg.z(), msg.yRot(), msg.untilGameTime()
        ));
    }
}

