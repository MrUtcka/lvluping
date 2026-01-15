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
    private static final Map<UUID, Float> playerStoredHealth = new HashMap<>();

    public static int getLevel(ServerPlayer p) {
        return playerLevels.getOrDefault(p.getUUID(), 0);
    }
    public static int getMaxLevel(int stars) { return stars * 10; }
    public static int getStars(UUID uuid) {
        return playerStars.getOrDefault(uuid, 2);
    }
    public static Set<String> getPlayerTalents(UUID uuid) { return playerTalents.computeIfAbsent(uuid, k -> new HashSet<>()); }
    public static Map<String, Integer> getPlayerStatsMap(UUID uuid) { return playerStats.computeIfAbsent(uuid, k -> new HashMap<>()); }
    public static int getStatLevel(UUID uuid, String statId) { return playerStats.computeIfAbsent(uuid, k -> new HashMap<>()).getOrDefault(statId, 0); }
    public static void setStoredHealth(UUID uuid, float health) { playerStoredHealth.put(uuid, health); }
    public static float getStoredHealth(UUID uuid) { return playerStoredHealth.getOrDefault(uuid, -1f); }

    public static void upgradeStat(UUID uuid, String statId) {
        Map<String, Integer> stats = playerStats.computeIfAbsent(uuid, k -> new HashMap<>());
        AttributeStat stat = AttributeStat.getById(statId);
        if (stat != null) {
            int current = stats.getOrDefault(statId, 0);
            if (current < stat.maxLevel) {
                stats.put(statId, current + 1);
            }
        }
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

    public static void setLevel(UUID uuid, int level) {
        int stars = getStars(uuid);
        int maxAllowed = getMaxLevel(stars);
        playerLevels.put(uuid, Math.clamp(level, 0, maxAllowed));
    }

    public static void setStars(UUID uuid, int stars) {
        playerStars.put(uuid, Math.clamp(stars, 1, 7));
    }

    public static boolean isBranchBlocked(UUID uuid, Talent t) {
        if (t.branch == null || t.branch.isEmpty()) return false;
        Set<String> owned = getPlayerTalents(uuid);
        if (owned.contains(t.id)) return false;

        for (String id : owned) {
            Talent o = Talent.getById(id);
            if (o != null && t.branch.equals(o.branch) && t.parent == o.parent) {
                return true;
            }
        }
        return false;
    }

    public static void unlockTalent(UUID uuid, String id) {
        getPlayerTalents(uuid).add(id);
    }

    public static void save(MinecraftServer server) {
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        CompoundTag root = new CompoundTag();

        Set<UUID> allPlayers = new HashSet<>(playerLevels.keySet());
        allPlayers.addAll(playerStars.keySet());

        for (UUID uuid : allPlayers) {
            CompoundTag pData = new CompoundTag();
            pData.putFloat("currentHealth", playerStoredHealth.getOrDefault(uuid, 20f));
            pData.putInt("level", playerLevels.getOrDefault(uuid, 0));
            pData.putInt("stars", playerStars.getOrDefault(uuid, 2));

            ListTag tList = new ListTag();
            getPlayerTalents(uuid).forEach(s -> tList.add(StringTag.valueOf(s)));
            pData.put("talents", tList);

            CompoundTag sData = new CompoundTag();
            getPlayerStatsMap(uuid).forEach(sData::putInt);
            pData.put("attributes", sData);

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

        playerLevels.clear();
        playerStars.clear();
        playerTalents.clear();
        playerStats.clear();
        playerStoredHealth.clear();

        try {
            CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());

            for (String key : root.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                CompoundTag pData = root.getCompound(key);

                playerLevels.put(uuid, pData.getInt("level"));
                playerStars.put(uuid, pData.getInt("stars"));

                if (pData.contains("currentHealth")) {
                    playerStoredHealth.put(uuid, pData.getFloat("currentHealth"));
                }

                Set<String> talents = getPlayerTalents(uuid);
                ListTag tList = pData.getList("talents", 8);
                for (int i = 0; i < tList.size(); i++) talents.add(tList.getString(i));

                if (pData.contains("attributes")) {
                    CompoundTag sData = pData.getCompound("attributes");
                    Map<String, Integer> statsMap = getPlayerStatsMap(uuid);
                    for (String sKey : sData.getAllKeys()) {
                        statsMap.put(sKey, sData.getInt(sKey));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}