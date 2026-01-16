package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.*;
import org.mrutcka.lvluping.handler.AttributeHandler;

import java.util.Objects;
import java.util.UUID;

public record C2SUpgradeStat(String statId) implements CustomPacketPayload {
    public static final Type<C2SUpgradeStat> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "upgrade_stat"));

    public static final StreamCodec<FriendlyByteBuf, C2SUpgradeStat> STREAM_CODEC = CustomPacketPayload.codec(
            C2SUpgradeStat::write, C2SUpgradeStat::new);

    private C2SUpgradeStat(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(statId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SUpgradeStat msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;

            AttributeStat stat = AttributeStat.getById(msg.statId);
            if (stat == null) return;

            UUID uuid = serverPlayer.getUUID();
            int currentTotalLvl = PlayerLevels.getStatLevel(uuid, stat.id);

            if (currentTotalLvl >= stat.maxLevel) return;

            int spentOnTalents = PlayerLevels.getPlayerTalents(uuid).stream()
                    .map(Talent::getById)
                    .filter(Objects::nonNull)
                    .mapToInt(t -> t.cost)
                    .sum();

            int spentOnStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            if (PlayerLevels.getLevel(serverPlayer) - (spentOnTalents + spentOnStats) >= 1) {
                PlayerLevels.upgradeStat(uuid, stat.id);
                AttributeHandler.applyStats(serverPlayer, false);

                PacketDistributor.sendToPlayer(serverPlayer, new S2CSyncTalents(
                        PlayerLevels.getLevel(serverPlayer),
                        PlayerLevels.getStars(uuid),
                        PlayerLevels.getPlayerTalents(uuid),
                        PlayerLevels.getPlayerStatsMap(uuid),
                        PlayerLevels.getRace(uuid).id
                ));
            }
        });
    }
}