package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.data.*;
import org.mrutcka.lvluping.handler.AttributeHandler;

import java.util.Objects;
import java.util.UUID;

public record C2SPurchaseTalent(String talentId) implements CustomPacketPayload {

    public static final Type<C2SPurchaseTalent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "purchase_talent"));

    public static final StreamCodec<FriendlyByteBuf, C2SPurchaseTalent> STREAM_CODEC = CustomPacketPayload.codec(
            C2SPurchaseTalent::write, C2SPurchaseTalent::new);

    private C2SPurchaseTalent(FriendlyByteBuf buf) {
        this(buf.readUtf());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(talentId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SPurchaseTalent msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer serverPlayer)) return;

            Talent t = Talent.getById(msg.talentId);
            if (t == null) return;

            UUID uuid = serverPlayer.getUUID();
            var owned = PlayerLevels.getPlayerTalents(uuid);
            int stars = PlayerLevels.getStars(uuid);

            boolean hasClass = owned.contains("warrior_base") || owned.contains("archer_base") || owned.contains("mage_base") || owned.contains("assassin_base");
            boolean isClassPick = msg.talentId.equals("warrior_base") || msg.talentId.equals("archer_base") || msg.talentId.equals("mage_base") || msg.talentId.equals("assassin_base");

            if (isClassPick && !hasClass) {
                if (PlayerLevels.isRaceForbidden(uuid, t)) {
                    PlayerLevels.syncTalentsToClient(serverPlayer);
                    return;
                }
                owned.add("start");
                owned.add(t.id);
                if (org.mrutcka.lvluping.data.AbilityUpgradeConfig.has(t.id)) {
                    int existing = PlayerLevels.getPlayerAbilityLevels(uuid).getOrDefault(t.id, 0);
                    if (existing <= 0) PlayerLevels.setAbilityLevel(uuid, t.id, 1);
                }

                PlayerLevels.syncTalentsToClient(serverPlayer);
                PlayerLevels.save(serverPlayer.getServer());
                return;
            }

            if (isClassPick && hasClass) {
                return;
            }

            long purchasedCount = owned.stream().filter(id -> !isFreeClassTalent(id)).count();

            if (purchasedCount < PlayerLevels.getTalentLimit(stars)
                    && !owned.contains(t.id)
                    && !PlayerLevels.isBranchBlocked(uuid, t)
                    && !PlayerLevels.isRaceForbidden(uuid, t)) {

                if (t.parentsSatisfiedForPurchase(owned)) {
                    int spentOnTalents = owned.stream()
                            .map(Talent::getById)
                            .filter(Objects::nonNull)
                            .filter(ta -> !isFreeClassTalent(ta.id))
                            .mapToInt(ta -> ta.cost)
                            .sum();

                    int spentOnStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream()
                            .mapToInt(Integer::intValue)
                            .sum();

                    int spentUpgrades = PlayerLevels.getSpentUpgradePoints(uuid);
                    int available = PlayerLevels.getLevel(serverPlayer) - (spentOnTalents + spentOnStats + spentUpgrades);
                    if (available >= t.cost) {
                        PlayerLevels.unlockTalent(uuid, t.id);
                        if (org.mrutcka.lvluping.data.AbilityUpgradeConfig.has(t.id)) {
                            int existing = PlayerLevels.getPlayerAbilityLevels(uuid).getOrDefault(t.id, 0);
                            if (existing <= 0) PlayerLevels.setAbilityLevel(uuid, t.id, 1);
                        }

                        PlayerLevels.syncTalentsToClient(serverPlayer);

                        AttributeHandler.applyStats(serverPlayer, false);
                        PlayerLevels.save(serverPlayer.getServer());
                    }
                }
            }
        });
    }

    private static boolean isFreeClassTalent(String id) {
        return "start".equals(id) || "warrior_base".equals(id) || "archer_base".equals(id) || "mage_base".equals(id) || "assassin_base".equals(id);
    }
}