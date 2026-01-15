package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.mrutcka.lvluping.client.TalentScreen;
import java.util.*;

public class S2CSyncTalents {
    private final int level, stars;
    private final Set<String> talents;

    public S2CSyncTalents(int level, int stars, Set<String> talents) {
        this.level = level; this.stars = stars; this.talents = talents;
    }

    public S2CSyncTalents(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        this.stars = buf.readInt();

        this.talents = new HashSet<>();
        int tSize = buf.readInt();
        for (int i = 0; i < tSize; i++) this.talents.add(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(stars);

        buf.writeInt(talents.size());
        for (String t : talents) buf.writeUtf(t);
    }

    public static void handle(S2CSyncTalents msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            TalentScreen.clientLevel = msg.level;
            TalentScreen.clientStars = msg.stars;
            TalentScreen.clientTalents = msg.talents;
        });
        ctx.setPacketHandled(true);
    }
}