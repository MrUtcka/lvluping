package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.mrutcka.lvluping.client.TalentScreen;
import java.util.*;

public class S2CSyncTalents {
    private final int level, stars;
    private final Set<String> talents;
    private final Map<String, Integer> stats; // Новое поле для статов

    public S2CSyncTalents(int level, int stars, Set<String> talents, Map<String, Integer> stats) {
        this.level = level;
        this.stars = stars;
        this.talents = talents;
        this.stats = stats;
    }

    public S2CSyncTalents(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.stars = buf.readInt();

        this.talents = new HashSet<>();
        int tSize = buf.readInt();
        for (int i = 0; i < tSize; i++) this.talents.add(buf.readUtf());

        this.stats = new HashMap<>();
        int sSize = buf.readInt();
        for (int i = 0; i < sSize; i++) this.stats.put(buf.readUtf(), buf.readInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(stars);

        buf.writeInt(talents.size());
        for (String t : talents) buf.writeUtf(t);

        buf.writeInt(stats.size());
        stats.forEach((id, lvl) -> {
            buf.writeUtf(id);
            buf.writeInt(lvl);
        });
    }

    public static void handle(S2CSyncTalents msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            TalentScreen.clientLevel = msg.level;
            TalentScreen.clientStars = msg.stars;
            TalentScreen.clientTalents = msg.talents;
            TalentScreen.clientStats = msg.stats;
        });
        ctx.setPacketHandled(true);
    }
}