package org.mrutcka.lvluping.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import org.mrutcka.lvluping.client.TalentScreen;
import org.mrutcka.lvluping.data.Race;
import org.mrutcka.lvluping.data.Talent;

import java.util.*;

public record S2CSyncTalents(int level, int stars, Set<String> talents, Map<String, Integer> stats, String raceId) implements CustomPacketPayload {

    public static final Type<S2CSyncTalents> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LvlupingMod.MODID, "sync_talents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncTalents> STREAM_CODEC = StreamCodec.of(
            (buf, msg) -> {
                buf.writeInt(msg.level);
                buf.writeInt(msg.stars);
                buf.writeInt(msg.talents.size());
                msg.talents.forEach(buf::writeUtf);
                buf.writeInt(msg.stats.size());
                msg.stats.forEach((id, lvl) -> {
                    buf.writeUtf(id);
                    buf.writeInt(lvl);
                });
                buf.writeUtf(msg.raceId);
            },
            buf -> {
                int lvl = buf.readInt();
                int stars = buf.readInt();
                Set<String> talents = new HashSet<>();
                int tSize = buf.readInt();
                for (int i = 0; i < tSize; i++) talents.add(buf.readUtf());
                Map<String, Integer> stats = new HashMap<>();
                int sSize = buf.readInt();
                for (int i = 0; i < sSize; i++) stats.put(buf.readUtf(), buf.readInt());
                String race = buf.readUtf();
                return new S2CSyncTalents(lvl, stars, talents, stats, race);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(S2CSyncTalents msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            TalentScreen.clientLevel = msg.level();
            TalentScreen.clientStars = msg.stars();
            TalentScreen.clientTalents = msg.talents();
            TalentScreen.clientStats = msg.stats();
            TalentScreen.clientRace = Race.getById(msg.raceId());
        });
    }
}