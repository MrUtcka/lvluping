package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;
import org.mrutcka.lvluping.data.*;
import java.util.Objects;

public class C2SUpgradeStat {
    private final String statId;

    public C2SUpgradeStat(String id) { this.statId = id; }
    public C2SUpgradeStat(FriendlyByteBuf buf) { this.statId = buf.readUtf(); }
    public void encode(FriendlyByteBuf buf) { buf.writeUtf(statId); }

    public static void handle(C2SUpgradeStat msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Stat stat = Stat.getById(msg.statId);
            if (stat == null) return;

            int currentLvl = PlayerLevels.getStatLevel(player.getUUID(), stat.id);
            if (currentLvl >= stat.maxLevel) return;

            // Считаем общие траты
            int spentTalents = PlayerLevels.getPlayerTalents(player.getUUID()).stream()
                    .map(Talent::getById).filter(Objects::nonNull).mapToInt(t -> t.cost).sum();
            int spentStats = PlayerLevels.getStatsMap(player.getUUID()).values().stream().mapToInt(i -> i).sum();

            if (PlayerLevels.getLevel(player) - (spentTalents + spentStats) >= 1) {
                PlayerLevels.upgradeStat(player.getUUID(), stat.id);

                // Синхронизация
                ModNetworking.CHANNEL.send(new S2CSyncTalents(
                        PlayerLevels.getLevel(player),
                        PlayerLevels.getStars(player.getUUID()),
                        PlayerLevels.getPlayerTalents(player.getUUID()),
                        PlayerLevels.getStatsMap(player.getUUID())
                ), PacketDistributor.PLAYER.with(player));
            }
        });
        ctx.setPacketHandled(true);
    }
}