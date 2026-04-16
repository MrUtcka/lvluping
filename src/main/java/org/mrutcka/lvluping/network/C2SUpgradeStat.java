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
            if (PlayerLevels.getStatLevel(uuid, stat.id) >= stat.maxLevel) return;

            if (stat == AttributeStat.MANA) {
                if (PlayerLevels.getAvailableUpgradePoints(uuid) < 1) return;
                PlayerLevels.addSpentStatPoints(uuid, 1);
            } else {
                if (!PlayerStatTrainingData.tryConsumeOneLevelTrainingProgress(serverPlayer, stat)) {
                    if (PlayerLevels.getAvailableUpgradePoints(uuid) < 1) return;
                    PlayerLevels.addSpentStatPoints(uuid, 1);
                }
            }

            PlayerLevels.upgradeStat(uuid, stat.id);
            AttributeHandler.applyStats(serverPlayer, false);

            PacketDistributor.sendToPlayer(serverPlayer, PlayerLevels.createSyncPayload(serverPlayer));
            PlayerStatTrainingData.syncToClient(serverPlayer);
            PlayerLevels.save(serverPlayer.getServer());
        });
    }
}
