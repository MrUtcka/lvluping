package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.handler.TalentAbilityHandler;

public record C2SUseAbility(int slot) implements CustomPacketPayload {
    public static final Type<C2SUseAbility> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "use_ability"));

    public static final StreamCodec<FriendlyByteBuf, C2SUseAbility> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeVarInt(msg.slot()),
            buf -> new C2SUseAbility(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SUseAbility msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                TalentAbilityHandler.handleAbilityUse(player, msg.slot());
            }
        });
    }
}