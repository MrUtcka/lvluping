package org.mrutcka.lvluping.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public final class LvlupingServerData {
    public static final String DATA_SUBDIR = "lvluping";
    public static final String PRESET_FILE = "lvluping_preset.json";
    public static final String PLAYER_DATA_FILE = "lvluping_data.dat";

    private LvlupingServerData() {}

    public static Path root(MinecraftServer server) {
        return server.getServerDirectory().resolve(DATA_SUBDIR).toAbsolutePath().normalize();
    }

    public static void ensureRootExists(MinecraftServer server) throws IOException {
        Files.createDirectories(root(server));
    }

    public static Path presetPath(MinecraftServer server) {
        return root(server).resolve(PRESET_FILE);
    }

    public static Path playerDataPath(MinecraftServer server) {
        return root(server).resolve(PLAYER_DATA_FILE);
    }

    public static Path legacyWorldRootDataFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(PLAYER_DATA_FILE);
    }

    public static Path legacyWorldRootPresetFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(PRESET_FILE);
    }

    public static void migratePlayerDataIfNeeded(MinecraftServer server) {
        Path target = playerDataPath(server);
        if (Files.exists(target)) return;
        Path legacy = legacyWorldRootDataFile(server);
        if (!Files.exists(legacy)) return;
        try {
            ensureRootExists(server);
            Files.copy(legacy, target, StandardCopyOption.COPY_ATTRIBUTES);
            org.mrutcka.lvluping.LvlupingMod.LOGGER.info(
                    "LVLuping: перенесён {} → {}", legacy.toAbsolutePath(), target.toAbsolutePath());
        } catch (IOException e) {
            org.mrutcka.lvluping.LvlupingMod.LOGGER.warn(
                    "LVLuping: не удалось скопировать данные из папки мира: {}", e.toString());
        }
    }

    public static void migratePresetIfNeeded(MinecraftServer server) {
        Path target = presetPath(server);
        if (Files.exists(target)) return;
        Path legacy = legacyWorldRootPresetFile(server);
        if (!Files.exists(legacy)) return;
        try {
            ensureRootExists(server);
            Files.copy(legacy, target, StandardCopyOption.COPY_ATTRIBUTES);
            org.mrutcka.lvluping.LvlupingMod.LOGGER.info(
                    "LVLuping: перенесён пресет {} → {}", legacy.toAbsolutePath(), target.toAbsolutePath());
        } catch (IOException e) {
            org.mrutcka.lvluping.LvlupingMod.LOGGER.warn(
                    "LVLuping: не удалось скопировать пресет из папки мира: {}", e.toString());
        }
    }
}
