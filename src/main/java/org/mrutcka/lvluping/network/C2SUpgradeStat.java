package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.*;
import org.mrutcka.lvluping.handler.AttributeHandler;

import java.util.Objects;
import java.util.UUID;

public class C2SUpgradeStat {
    private final String statId;

    public C2SUpgradeStat(String id) { this.statId = id; }
    public C2SUpgradeStat(FriendlyByteBuf buf) { this.statId = buf.readUtf(); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(this.statId); }

    public static void handle(C2SUpgradeStat msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) return;

            AttributeStat stat = AttributeStat.getById(msg.statId);
            if (stat == null) return;

            UUID uuid = player.getUUID();
            int currentStatLvl = PlayerLevels.getStatLevel(uuid, stat.id);

            if (currentStatLvl >= stat.maxLevel) return;

            int spentOnTalents = PlayerLevels.getPlayerTalents(uuid).stream()
                    .map(Talent::getById)
                    .filter(Objects::nonNull)
                    .mapToInt(t -> t.cost)
                    .sum();

            int spentOnStats = PlayerLevels.getPlayerStatsMap(uuid).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            if (PlayerLevels.getLevel(player) - (spentOnTalents + spentOnStats) >= 1) {
                PlayerLevels.upgradeStat(uuid, stat.id);
                AttributeHandler.applyStats(player, false);

                ModNetworking.CHANNEL.send(new S2CSyncTalents(
                        PlayerLevels.getLevel(player),
                        PlayerLevels.getStars(uuid),
                        PlayerLevels.getPlayerTalents(uuid),
                        PlayerLevels.getPlayerStatsMap(uuid)
                ), PacketDistributor.PLAYER.with(player));

            }
        });
        ctx.setPacketHandled(true);
    }
}