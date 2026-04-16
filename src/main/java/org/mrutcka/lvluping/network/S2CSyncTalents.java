package org.mrutcka.lvluping.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.mrutcka.lvluping.LvlupingMod;
import java.util.*;

public record S2CSyncTalents(int level, int stars, Set<String> talents, Map<String, Integer> stats, Map<String, Integer> abilityLevels, String raceId, int bonusPoints, int statPointsSpent, Set<String> adminGrantedTalentIds, int talentBudgetDebt) implements CustomPacketPayload {

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
                buf.writeInt(msg.abilityLevels.size());
                msg.abilityLevels.forEach((id, lvl) -> {
                    buf.writeUtf(id);
                    buf.writeInt(lvl);
                });
                buf.writeUtf(msg.raceId);
                buf.writeInt(msg.bonusPoints);
                buf.writeInt(msg.statPointsSpent);
                buf.writeInt(msg.adminGrantedTalentIds.size());
                msg.adminGrantedTalentIds.forEach(buf::writeUtf);
                buf.writeInt(msg.talentBudgetDebt);
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
                Map<String, Integer> abilityLevels = new HashMap<>();
                int aSize = buf.readInt();
                for (int i = 0; i < aSize; i++) abilityLevels.put(buf.readUtf(), buf.readInt());
                String race = buf.readUtf();
                int bonusPoints = buf.readInt();
                int statPointsSpent = buf.readInt();
                Set<String> adminGranted = new HashSet<>();
                int agSize = buf.readInt();
                for (int i = 0; i < agSize; i++) adminGranted.add(buf.readUtf());
                int talentBudgetDebt = buf.readInt();
                return new S2CSyncTalents(lvl, stars, talents, stats, abilityLevels, race, bonusPoints, statPointsSpent, adminGranted, talentBudgetDebt);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(S2CSyncTalents msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            org.mrutcka.lvluping.client.ClientPacketHandler.handleSync(msg);
        });
    }
}