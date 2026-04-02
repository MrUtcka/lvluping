package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.AbilityUpgradeConfig;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.handler.AttributeHandler;

import java.util.UUID;

public record C2SUpgradeAbility(String abilityId) implements CustomPacketPayload {
    public static final Type<C2SUpgradeAbility> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "upgrade_ability"));

    public static final StreamCodec<FriendlyByteBuf, C2SUpgradeAbility> STREAM_CODEC = CustomPacketPayload.codec(
            C2SUpgradeAbility::write, C2SUpgradeAbility::new);

    private C2SUpgradeAbility(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(abilityId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SUpgradeAbility msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            UUID uuid = player.getUUID();
            var owned = PlayerLevels.getPlayerTalents(uuid);
            if (!owned.contains(msg.abilityId)) return;
            if (!AbilityUpgradeConfig.has(msg.abilityId)) return;

            int current = PlayerLevels.getAbilityLevel(uuid, msg.abilityId, owned);
            int max = AbilityUpgradeConfig.getMaxLevel(msg.abilityId);
            if (current <= 0) current = 1;
            if (current >= max) return;
            int next = current + 1;

            int cost = AbilityUpgradeConfig.getUpgradePointCost(msg.abilityId, next);
            int spentTalents = owned.stream().map(org.mrutcka.lvluping.data.Talent::getById).filter(java.util.Objects::nonNull).mapToInt(t -> t.cost).sum();
            int spentStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream().mapToInt(Integer::intValue).sum();
            int spentUpgrades = PlayerLevels.getSpentUpgradePoints(uuid);
            int available = PlayerLevels.getLevel(player) - (spentTalents + spentStats + spentUpgrades);
            if (available < cost) return;

            PlayerLevels.setAbilityLevel(uuid, msg.abilityId, next);
            AttributeHandler.applyStats(player, false);

            PacketDistributor.sendToPlayer(player, new S2CSyncTalents(
                    PlayerLevels.getLevel(player),
                    PlayerLevels.getStars(uuid),
                    PlayerLevels.getPlayerTalents(uuid),
                    PlayerLevels.getPlayerStatsMap(uuid),
                    PlayerLevels.getPlayerAbilityLevels(uuid),
                    PlayerLevels.getRace(uuid).id
            ));
            PlayerLevels.save(player.getServer());
        });
    }
}

