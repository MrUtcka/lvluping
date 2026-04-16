package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.client.ClientStatTrainingHud;

public record S2CSyncStatTraining(
        int dmgProg, int dmgNeed,
        int spdProg, int spdNeed,
        int hpProg, int hpNeed,
        boolean dmgMaxed, boolean spdMaxed, boolean hpMaxed,
        int globalFatigue,
        int fatigueTier,
        int fatigueCap
) implements CustomPacketPayload {

    public static final Type<S2CSyncStatTraining> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "sync_stat_training"));

    public static final StreamCodec<FriendlyByteBuf, S2CSyncStatTraining> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeVarInt(msg.dmgProg());
                buf.writeVarInt(msg.dmgNeed());
                buf.writeVarInt(msg.spdProg());
                buf.writeVarInt(msg.spdNeed());
                buf.writeVarInt(msg.hpProg());
                buf.writeVarInt(msg.hpNeed());
                buf.writeBoolean(msg.dmgMaxed());
                buf.writeBoolean(msg.spdMaxed());
                buf.writeBoolean(msg.hpMaxed());
                buf.writeVarInt(msg.globalFatigue());
                buf.writeVarInt(msg.fatigueTier());
                buf.writeVarInt(msg.fatigueCap());
            },
            buf -> new S2CSyncStatTraining(
                    buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CSyncStatTraining msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientStatTrainingHud.applyPacket(msg));
    }
}
