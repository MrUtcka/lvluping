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
    private static final Map<UUID, Race> playerRaces = new HashMap<>();

    public static int getLevel(ServerPlayer p) {
        return playerLevels.getOrDefault(p.getUUID(), 0);
    }

    public static int getStars(UUID uuid) {
        return playerStars.getOrDefault(uuid, 2);
    }

    public static Set<String> getPlayerTalents(UUID uuid) {
        return playerTalents.computeIfAbsent(uuid, k -> {
            Set<String> s = new HashSet<>();
            s.add("start");
            return s;
        });
    }

    public static Map<String, Integer> getPlayerStatsMap(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static int getStatLevel(UUID uuid, String statId) {
        int base = getPlayerStatsMap(uuid).getOrDefault(statId, 0);
        Race race = getRace(uuid);
        return base + race.bonuses.getOrDefault(statId, 0);
    }

    public static Race getRace(UUID uuid) {
        return playerRaces.getOrDefault(uuid, Race.HUMAN);
    }

    public static void setRace(UUID uuid, Race race) {
        playerRaces.put(uuid, race);
    }

    public static void unlockTalent(UUID uuid, String id) { getPlayerTalents(uuid).add(id); }
    public static void upgradeStat(UUID uuid, String id) {
        Map<String, Integer> stats = getPlayerStatsMap(uuid);
        stats.put(id, stats.getOrDefault(id, 0) + 1);
    }

    public static void setLevel(UUID uuid, int level) {
        int stars = getStars(uuid);
        int maxLvl = getMaxLevel(stars);
        playerLevels.put(uuid, Math.min(level, maxLvl));
    }
    public static void setStars(UUID uuid, int s) { playerStars.put(uuid, s); }
    public static void setStoredHealth(UUID uuid, float h) { playerStoredHealth.put(uuid, h); }
    public static float getStoredHealth(UUID uuid) { return playerStoredHealth.getOrDefault(uuid, -1f); }

    public static int getMaxLevel(int stars) {
        return stars * 10;
    }

    public static int getTalentLimit(int stars) {
        return switch (stars) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 5;
            case 5 -> 8;
            case 6 -> 12;
            case 7 -> 16;
            default -> 0;
        };
    }

    public static boolean isBranchBlocked(UUID uuid, Talent t) {
        if (t.branch.isEmpty()) return false;
        Set<String> owned = getPlayerTalents(uuid);
        for (String id : owned) {
            Talent ot = Talent.getById(id);
            if (ot != null && ot != t && ot.branch.equals(t.branch)) return true;
        }
        return false;
    }

    public static void save(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        CompoundTag root = new CompoundTag();
        Set<UUID> allPlayers = new HashSet<>(playerLevels.keySet());
        allPlayers.addAll(playerStars.keySet());

        for (UUID uuid : allPlayers) {
            CompoundTag pData = new CompoundTag();
            pData.putInt("level", playerLevels.getOrDefault(uuid, 0));
            pData.putInt("stars", playerStars.getOrDefault(uuid, 2));
            pData.putString("race", getRace(uuid).id);
            pData.putFloat("currentHealth", playerStoredHealth.getOrDefault(uuid, 20f));

            ListTag tList = new ListTag();
            for (String t : getPlayerTalents(uuid)) tList.add(StringTag.valueOf(t));
            pData.put("talents", tList);

            CompoundTag sData = new CompoundTag();
            getPlayerStatsMap(uuid).forEach(sData::putInt);
            pData.put("attributes", sData);

            root.put(uuid.toString(), pData);
        }

        try (OutputStream out = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(root, out);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void load(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        if (!Files.exists(path)) return;

        try (InputStream in = Files.newInputStream(path)) {
            CompoundTag root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            for (String key : root.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                CompoundTag pData = root.getCompound(key);
                playerLevels.put(uuid, pData.getInt("level"));
                playerStars.put(uuid, pData.getInt("stars"));
                playerRaces.put(uuid, Race.getById(pData.getString("race")));
                playerStoredHealth.put(uuid, pData.getFloat("currentHealth"));

                Set<String> talents = getPlayerTalents(uuid);
                talents.clear();
                ListTag tList = pData.getList("talents", 8);
                for (int i = 0; i < tList.size(); i++) talents.add(tList.getString(i));

                Map<String, Integer> statsMap = getPlayerStatsMap(uuid);
                statsMap.clear();
                CompoundTag sData = pData.getCompound("attributes");
                for (String sKey : sData.getAllKeys()) statsMap.put(sKey, sData.getInt(sKey));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}