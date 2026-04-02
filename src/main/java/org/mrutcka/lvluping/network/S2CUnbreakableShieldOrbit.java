package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

import java.util.UUID;

public record S2CUnbreakableShieldOrbit(UUID targetUuid, long untilGameTime) implements CustomPacketPayload {
    public static final Type<S2CUnbreakableShieldOrbit> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "unbreakable_shield_orbit"));

    private static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
            (buf, u) -> buf.writeUUID(u),
            buf -> buf.readUUID()
    );

    public static final StreamCodec<FriendlyByteBuf, S2CUnbreakableShieldOrbit> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC, S2CUnbreakableShieldOrbit::targetUuid,
            ByteBufCodecs.VAR_LONG, S2CUnbreakableShieldOrbit::untilGameTime,
            S2CUnbreakableShieldOrbit::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(S2CUnbreakableShieldOrbit msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> org.mrutcka.lvluping.client.UnbreakableShieldClient.addEffect(msg.targetUuid(), msg.untilGameTime()));
    }
}
