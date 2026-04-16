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
    private static final String ABILITY_USE_RATE_KEY = "lvluping_last_use_ability_gt";
    private static final int ABILITY_USE_MIN_INTERVAL_TICKS = 2;

    public static final StreamCodec<FriendlyByteBuf, C2SUseAbility> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> buf.writeVarInt(msg.slot()),
            buf -> new C2SUseAbility(buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SUseAbility msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                long nowGt = player.level().getGameTime();
                long lastGt = player.getPersistentData().getLong(ABILITY_USE_RATE_KEY);
                if (lastGt > 0 && nowGt - lastGt < ABILITY_USE_MIN_INTERVAL_TICKS) {
                    return;
                }
                player.getPersistentData().putLong(ABILITY_USE_RATE_KEY, nowGt);
                try {
                    TalentAbilityHandler.handleAbilityUse(player, msg.slot());
                } catch (Throwable t) {
                    LvlupingMod.LOGGER.error("LVLuping: ability use failed (player={}, slot={})",
                            player.getScoreboardName(), msg.slot(), t);
                }
            }
        });
    }
}