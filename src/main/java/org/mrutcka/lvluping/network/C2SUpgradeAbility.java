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
import org.mrutcka.lvluping.handler.TalentAbilityHandler;

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
            int spentTalents = owned.stream().map(org.mrutcka.lvluping.data.Talent::getById).filter(java.util.Objects::nonNull).filter(t -> !isFreeClassTalent(t.id)).mapToInt(t -> t.cost).sum();
            int spentStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream().mapToInt(Integer::intValue).sum();
            int spentUpgrades = PlayerLevels.getSpentUpgradePoints(uuid);
            int available = PlayerLevels.getLevel(player) - (spentTalents + spentStats + spentUpgrades);
            if (available < cost) return;

            PlayerLevels.setAbilityLevel(uuid, msg.abilityId, next);
            AttributeHandler.applyStats(player, false);

            if ("m_summon_servant".equals(msg.abilityId)) {
                TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_servant");
            } else if ("m_summon_guard".equals(msg.abilityId)) {
                TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_guard");
            } else if ("m_summon_discipline".equals(msg.abilityId)) {
                if (owned.contains("m_summon_servant")) TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_servant");
                if (owned.contains("m_summon_guard")) TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_guard");
            } else if ("m_summoner_base".equals(msg.abilityId)) {
                if (owned.contains("m_summon_servant")) TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_servant");
                if (owned.contains("m_summon_guard")) TalentAbilityHandler.refreshOwnedSummonLoadouts(player, "m_summon_guard");
            }

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

    private static boolean isFreeClassTalent(String id) {
        return "start".equals(id)
                || "warrior_base".equals(id)
                || "archer_base".equals(id)
                || "mage_base".equals(id)
                || "assassin_base".equals(id);
    }
}

