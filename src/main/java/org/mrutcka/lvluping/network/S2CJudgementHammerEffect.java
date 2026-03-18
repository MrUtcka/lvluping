package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CJudgementHammerEffect(double x, double y, double z, int ticksRemaining, UUID targetUuid) implements CustomPacketPayload {
    public static final Type<S2CJudgementHammerEffect> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "judgement_hammer_effect"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> buf.writeUUID(u),
            buf -> buf.readUUID()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CJudgementHammerEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, S2CJudgementHammerEffect::x,
            ByteBufCodecs.DOUBLE, S2CJudgementHammerEffect::y,
            ByteBufCodecs.DOUBLE, S2CJudgementHammerEffect::z,
            ByteBufCodecs.VAR_INT, S2CJudgementHammerEffect::ticksRemaining,
            UUID_CODEC, S2CJudgementHammerEffect::targetUuid,
            S2CJudgementHammerEffect::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(S2CJudgementHammerEffect msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.JudgementHammerClient.addEffect(msg.x(), msg.y(), msg.z(), msg.ticksRemaining(), msg.targetUuid()));
    }
}
