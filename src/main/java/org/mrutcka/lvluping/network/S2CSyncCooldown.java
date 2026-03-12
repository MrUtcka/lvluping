package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;

public record S2CSyncCooldown(String key, int value) implements CustomPacketPayload {
    public static final Type<S2CSyncCooldown> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "sync_cooldown"));

    public static final StreamCodec<FriendlyByteBuf, S2CSyncCooldown> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, S2CSyncCooldown::key,
            ByteBufCodecs.VAR_INT, S2CSyncCooldown::value,
            S2CSyncCooldown::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(S2CSyncCooldown msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                player.getPersistentData().putInt(msg.key(), msg.value());
            }
        });
    }
}