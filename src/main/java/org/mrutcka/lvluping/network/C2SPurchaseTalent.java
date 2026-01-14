package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.PlayerLevels;
import org.mrutcka.lvluping.data.Talent;
import java.util.Set;
import java.util.Objects;

public class C2SPurchaseTalent {
    private final String talentId;

    public C2SPurchaseTalent(String id) { this.talentId = id; }
    public C2SPurchaseTalent(FriendlyByteBuf buf) { this.talentId = buf.readUtf(); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(talentId); }

    public static void handle(C2SPurchaseTalent msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Talent target = Talent.getById(msg.talentId);
            if (target == null) return;

            Set<String> ownedIds = PlayerLevels.getPlayerTalents(player.getUUID());

            int spentPoints = ownedIds.stream()
                    .map(Talent::getById)
                    .filter(Objects::nonNull)
                    .mapToInt(t -> t.cost)
                    .sum();

            int availablePoints = PlayerLevels.getLevel(player) - spentPoints;
            if (availablePoints < target.cost) return;

            if (!ownedIds.contains(target.id)) {
                boolean hasParent = (target.parent == null || ownedIds.contains(target.parent.id));
                boolean groupOk = !Talent.isGroupBlocked(target, ownedIds);

                if (hasParent && groupOk) {
                    PlayerLevels.unlockTalent(player.getUUID(), target.id);
                    ModNetworking.CHANNEL.send(
                            new S2CSyncTalents(PlayerLevels.getLevel(player), PlayerLevels.getPlayerTalents(player.getUUID())),
                            PacketDistributor.PLAYER.with(player)
                    );
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}