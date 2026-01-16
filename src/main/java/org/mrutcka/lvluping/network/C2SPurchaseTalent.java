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

            long purchasedCount = owned.stream().filter(id -> !id.equals("start")).count();

            if (purchasedCount < PlayerLevels.getTalentLimit(stars)
                    && !owned.contains(t.id)
                    && !PlayerLevels.isBranchBlocked(uuid, t)) {

                if (t.parent == null || owned.contains(t.parent.id)) {

                    int spentOnTalents = owned.stream()
                            .map(Talent::getById)
                            .filter(Objects::nonNull)
                            .mapToInt(ta -> ta.cost)
                            .sum();

                    int spentOnStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream()
                            .mapToInt(Integer::intValue)
                            .sum();

                    if (PlayerLevels.getLevel(serverPlayer) - (spentOnTalents + spentOnStats) >= t.cost) {
                        PlayerLevels.unlockTalent(uuid, t.id);

                        PacketDistributor.sendToPlayer(serverPlayer, new S2CSyncTalents(
                                PlayerLevels.getLevel(serverPlayer),
                                stars,
                                PlayerLevels.getPlayerTalents(uuid),
                                PlayerLevels.getPlayerStatsMap(uuid),
                                PlayerLevels.getRace(uuid).id
                        ));

                        PlayerLevels.save(serverPlayer.getServer());
                    }
                }
            }
        });
    }
}