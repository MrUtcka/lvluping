package org.mrutcka.lvluping.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLevels {

    private static final Map<UUID, Integer> levels = new HashMap<>();

    public static int getLevel(ServerPlayer player) {
        return levels.getOrDefault(player.getUUID(), 0);
    }

    public static void setLevel(ServerPlayer player, int level) {
        levels.put(player.getUUID(), level);
    }

    public static void addLevel(ServerPlayer player) {
        levels.put(player.getUUID(), getLevel(player) + 1);
    }

    public static void load(MinecraftServer server) {
        try {
            Path path = server.getWorldPath(LevelResource.ROOT)
                    .resolve("serverdata")
                    .resolve("lvluping.dat");

            if (!path.toFile().exists()) return;

            CompoundTag tag = NbtIo.read(path);
            levels.clear();

            for (String key : tag.getAllKeys()) {
                levels.put(UUID.fromString(key), tag.getInt(key));
            }

            System.out.println("[Lvluping] Loaded player levels!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(MinecraftServer server) {
        try {
            Path serverDataDir = server.getWorldPath(LevelResource.ROOT).resolve("serverdata");
            serverDataDir.toFile().mkdirs();

            Path path = serverDataDir.resolve("lvluping.dat");
            CompoundTag tag = new CompoundTag();

            for (Map.Entry<UUID, Integer> e : levels.entrySet()) {
                tag.putInt(e.getKey().toString(), e.getValue());
            }

            NbtIo.write(tag, path);

            System.out.println("[Lvluping] Saved player levels!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
