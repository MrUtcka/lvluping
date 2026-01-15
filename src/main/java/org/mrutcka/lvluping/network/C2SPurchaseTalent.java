package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.*;
import java.util.Objects;
import java.util.UUID;

public class C2SPurchaseTalent {
    private final String talentId;
    public C2SPurchaseTalent(String id) { this.talentId = id; }
    public C2SPurchaseTalent(FriendlyByteBuf buf) { this.talentId = buf.readUtf(); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(this.talentId); }

    public static void handle(C2SPurchaseTalent msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender(); if (player == null) return;
            Talent t = Talent.getById(msg.talentId); if (t == null) return;

            UUID uuid = player.getUUID();
            var owned = PlayerLevels.getPlayerTalents(uuid);
            int stars = PlayerLevels.getStars(uuid);
            long count = owned.stream().filter(id -> !id.equals("start")).count();

            if (count < PlayerLevels.getTalentLimit(stars) && !owned.contains(t.id) && !PlayerLevels.isBranchBlocked(uuid, t)) {
                if (t.parent == null || owned.contains(t.parent.id)) {

                    int spentOnTalents = owned.stream()
                            .map(Talent::getById)
                            .filter(Objects::nonNull)
                            .mapToInt(ta -> ta.cost)
                            .sum();

                    int spentOnStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream()
                            .mapToInt(Integer::intValue)
                            .sum();

                    if (PlayerLevels.getLevel(player) - (spentOnTalents + spentOnStats) >= t.cost) {
                        PlayerLevels.unlockTalent(uuid, t.id);

                        ModNetworking.CHANNEL.send(new S2CSyncTalents(
                                PlayerLevels.getLevel(player),
                                stars,
                                PlayerLevels.getPlayerTalents(uuid),
                                PlayerLevels.getPlayerStatsMap(uuid)
                        ), PacketDistributor.PLAYER.with(player));
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}