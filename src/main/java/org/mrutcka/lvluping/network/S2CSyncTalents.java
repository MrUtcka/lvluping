package org.mrutcka.lvluping.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import org.mrutcka.lvluping.client.TalentScreen;

import java.util.HashSet;
import java.util.Set;

public class S2CSyncTalents {
    private final int level;
    private final Set<String> talents;

    public S2CSyncTalents(int level, Set<String> talents) {
        this.level = level;
        this.talents = talents;
    }

    public S2CSyncTalents(FriendlyByteBuf buf) {
        this.level = buf.readInt();
        int size = buf.readInt();
        this.talents = new HashSet<>();
        for (int i = 0; i < size; i++) talents.add(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(level);
        buf.writeInt(talents.size());
        for (String t : talents) buf.writeUtf(t);
    }

    public static void handle(S2CSyncTalents msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                TalentScreen.clientLevel = msg.level;
                TalentScreen.clientTalents = msg.talents;
            });
        });
        ctx.setPacketHandled(true);
    }
}