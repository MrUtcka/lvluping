package org.mrutcka.lvluping.data;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlayerLevels {
    private static final Map<UUID, Integer> playerLevels = new HashMap<>();
    private static final Map<UUID, Integer> playerStars = new HashMap<>();
    private static final Map<UUID, Set<String>> playerTalents = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> playerStats = new HashMap<>();

    // --- Геттеры (Получение данных) ---

    public static int getLevel(ServerPlayer p) {
        return playerLevels.getOrDefault(p.getUUID(), 0);
    }

    public static int getStars(UUID uuid) {
        return playerStars.getOrDefault(uuid, 2);
    }

    public static Set<String> getPlayerTalents(UUID uuid) {
        return playerTalents.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    public static Map<String, Integer> getStatsMap(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static int getStatLevel(UUID uuid, String statId) {
        return getStatsMap(uuid).getOrDefault(statId, 0);
    }

    // Метод, который искал компилятор
    public static int getMaxLevel(String statId) {
        Stat s = Stat.getById(statId);
        return s != null ? s.maxLevel : 0;
    }

    public static int getTalentLimit(int stars) {
        return switch (stars) {
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            case 5 -> 8;
            case 6 -> 12;
            case 7 -> 16;
            default -> stars > 7 ? 20 : 1;
        };
    }

    // --- Сеттеры (Для команд /lvl и /star) ---

    public static void setLevel(UUID uuid, int level) {
        playerLevels.put(uuid, Math.max(0, level));
    }

    public static void setStars(UUID uuid, int stars) {
        playerStars.put(uuid, Math.clamp(stars, 1, 7));
    }

    // --- Логика прогрессии ---

    public static boolean isBranchBlocked(UUID uuid, Talent t) {
        if (t.branch == null || t.branch.isEmpty()) return false;
        Set<String> owned = getPlayerTalents(uuid);
        if (owned.contains(t.id)) return false;

        for (String id : owned) {
            Talent o = Talent.getById(id);
            // Если у таланта та же ветка и тот же родитель - это конкурент
            if (o != null && t.branch.equals(o.branch) && t.parent == o.parent) {
                return true;
            }
        }
        return false;
    }

    public static void unlockTalent(UUID uuid, String id) {
        getPlayerTalents(uuid).add(id);
    }

    public static void upgradeStat(UUID uuid, String statId) {
        Map<String, Integer> stats = getStatsMap(uuid);
        int current = stats.getOrDefault(statId, 0);
        if (current < getMaxLevel(statId)) {
            stats.put(statId, current + 1);
        }
    }

    // --- Сохранение и Загрузка (NBT) ---

    public static void save(MinecraftServer server) {
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        CompoundTag root = new CompoundTag();

        // Объединяем всех UUID из всех мап для сохранения
        Set<UUID> allPlayers = new HashSet<>(playerLevels.keySet());
        allPlayers.addAll(playerStars.keySet());

        for (UUID uuid : allPlayers) {
            CompoundTag pData = new CompoundTag();
            pData.putInt("level", playerLevels.getOrDefault(uuid, 0));
            pData.putInt("stars", playerStars.getOrDefault(uuid, 2));

            ListTag tList = new ListTag();
            getPlayerTalents(uuid).forEach(s -> tList.add(StringTag.valueOf(s)));
            pData.put("talents", tList);

            CompoundTag sTag = new CompoundTag();
            getStatsMap(uuid).forEach(sTag::putInt);
            pData.put("stats", sTag);

            root.put(uuid.toString(), pData);
        }

        try (OutputStream out = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(root, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(MinecraftServer server) {
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        if (!Files.exists(path)) return;

        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            playerLevels.clear();
            playerStars.clear();
            playerTalents.clear();
            playerStats.clear();

            for (String key : root.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                CompoundTag pData = root.getCompound(key);

                playerLevels.put(uuid, pData.getInt("level"));
                playerStars.put(uuid, pData.getInt("stars"));

                Set<String> talents = getPlayerTalents(uuid);
                ListTag tList = pData.getList("talents", 8);
                for (int i = 0; i < tList.size(); i++) talents.add(tList.getString(i));

                Map<String, Integer> stats = getStatsMap(uuid);
                CompoundTag sTag = pData.getCompound("stats");
                for (String skey : sTag.getAllKeys()) stats.put(skey, sTag.getInt(skey));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}