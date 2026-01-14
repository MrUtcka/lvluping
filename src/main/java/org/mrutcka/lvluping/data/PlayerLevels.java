package org.mrutcka.lvluping.data;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.*;

public class PlayerLevels {
    private static final Map<UUID, Integer> levels = new HashMap<>();
    private static final Map<UUID, Set<String>> unlockedTalents = new HashMap<>();

    public static int getLevel(ServerPlayer player) {
        return levels.getOrDefault(player.getUUID(), 0);
    }

    public static int getSpentPoints(UUID uuid) {
        return unlockedTalents.getOrDefault(uuid, Set.of()).size();
    }

    public static boolean hasTalent(UUID uuid, String id) {
        return unlockedTalents.getOrDefault(uuid, Set.of()).contains(id);
    }

    public static Set<String> getPlayerTalents(UUID uuid) {
        return unlockedTalents.getOrDefault(uuid, Set.of());
    }

    public static void unlockTalent(UUID uuid, String id) {
        unlockedTalents.computeIfAbsent(uuid, k -> new HashSet<>()).add(id);
    }

    public static void setLevel(ServerPlayer player, int level) {
        levels.put(player.getUUID(), level);
    }

    public static void addLevel(ServerPlayer player) {
        setLevel(player, getLevel(player) + 1);
    }

    public static void load(MinecraftServer server) {
        try {
            Path path = server.getWorldPath(LevelResource.ROOT).resolve("serverdata").resolve("lvluping.dat");
            if (!path.toFile().exists()) return;

            CompoundTag root = NbtIo.read(path);
            levels.clear();
            unlockedTalents.clear();

            for (String key : root.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                CompoundTag pTag = root.getCompound(key);
                levels.put(uuid, pTag.getInt("level"));

                ListTag tList = pTag.getList("talents", Tag.TAG_STRING);
                Set<String> ts = new HashSet<>();
                for (int i = 0; i < tList.size(); i++) {
                    ts.add(tList.getString(i));
                }
                unlockedTalents.put(uuid, ts);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save(MinecraftServer server) {
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("serverdata");
            dir.toFile().mkdirs();
            CompoundTag root = new CompoundTag();

            for (UUID uuid : levels.keySet()) {
                CompoundTag pTag = new CompoundTag();
                pTag.putInt("level", levels.get(uuid));

                ListTag tList = new ListTag();
                unlockedTalents.getOrDefault(uuid, Set.of()).forEach(s -> tList.add(StringTag.valueOf(s)));
                pTag.put("talents", tList);

                root.put(uuid.toString(), pTag);
            }
            NbtIo.write(root, dir.resolve("lvluping.dat"));
        } catch (Exception e) { e.printStackTrace(); }
    }
}