package org.mrutcka.lvluping.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SummonerHandler {
    private static final String KEY_LIST = "lvluping_summons";

    public static void tick(ServerLevel level) {
        long time = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            var data = player.getPersistentData();
            ListTag list = data.getList(KEY_LIST, 8);
            if (list.isEmpty()) continue;

            List<StringTag> keep = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getString(i);
                UUID uuid;
                try {
                    uuid = UUID.fromString(raw);
                } catch (Exception e) {
                    continue;
                }
                Entity entity = findEntity(level, uuid, player.getX(), player.getZ());
                if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
                long until = mob.getPersistentData().getLong("lvluping_summon_until");
                UUID owner = mob.getPersistentData().hasUUID("lvluping_summon_owner") ? mob.getPersistentData().getUUID("lvluping_summon_owner") : null;
                if (owner == null || !owner.equals(player.getUUID())) continue;
                if (until > 0 && until <= time) {
                    mob.discard();
                    continue;
                }
                keep.add(StringTag.valueOf(uuid.toString()));
            }

            ListTag out = new ListTag();
            keep.forEach(out::add);
            data.put(KEY_LIST, out);
        }
    }

    public static void addSummon(ServerPlayer player, Mob mob, long untilTick) {
        CompoundTag pd = mob.getPersistentData();
        pd.putUUID("lvluping_summon_owner", player.getUUID());
        pd.putLong("lvluping_summon_until", untilTick);

        ListTag list = player.getPersistentData().getList(KEY_LIST, 8);
        list.add(StringTag.valueOf(mob.getUUID().toString()));
        player.getPersistentData().put(KEY_LIST, list);
    }

    public static List<Mob> getAliveSummons(ServerLevel level, ServerPlayer player) {
        List<Mob> out = new ArrayList<>();
        ListTag list = player.getPersistentData().getList(KEY_LIST, 8);
        if (list.isEmpty()) return out;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid;
            try {
                uuid = UUID.fromString(list.getString(i));
            } catch (Exception e) {
                continue;
            }
            Entity entity = findEntity(level, uuid, player.getX(), player.getZ());
            if (entity instanceof Mob mob && mob.isAlive()) {
                CompoundTag pd = mob.getPersistentData();
                if (pd.hasUUID("lvluping_summon_owner") && player.getUUID().equals(pd.getUUID("lvluping_summon_owner"))) {
                    out.add(mob);
                }
            }
        }
        return out;
    }

    private static Entity findEntity(ServerLevel level, UUID uuid, double centerX, double centerZ) {
        AABB search = new AABB(centerX - 80, -64, centerZ - 80, centerX + 80, 320, centerZ + 80);
        for (Entity e : level.getEntitiesOfClass(Entity.class, search)) {
            if (e.getUUID().equals(uuid)) return e;
        }
        return null;
    }
}

