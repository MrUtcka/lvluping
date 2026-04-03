package org.mrutcka.lvluping.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.mrutcka.lvluping.LvlupingMod;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlayerLevels {
    private static final Map<UUID, Integer> playerLevels = new HashMap<>();
    private static final Map<UUID, Integer> playerStars = new HashMap<>();
    private static final Map<UUID, Set<String>> playerTalents = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> playerStats = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> playerCooldowns = new HashMap<>();
    private static final Map<UUID, Float> playerStoredHealth = new HashMap<>();
    private static final Map<UUID, Race> playerRaces = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> playerAbilityLevels = new HashMap<>();

    public static int getLevel(ServerPlayer p) {
        return playerLevels.getOrDefault(p.getUUID(), 0);
    }

    public static int getStars(UUID uuid) {
        return playerStars.getOrDefault(uuid, 2);
    }

    public static Set<String> getPlayerTalents(UUID uuid) {
        return playerTalents.computeIfAbsent(uuid, k -> {
            return new HashSet<>();
        });
    }

    public static Map<String, Integer> getPlayerStatsMap(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static Map<String, Integer> getPlayerCooldowns(UUID uuid) {
        return playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static Map<String, Integer> getPlayerAbilityLevels(UUID uuid) {
        return playerAbilityLevels.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public static int getAbilityLevel(UUID uuid, String abilityId, Set<String> ownedTalents) {
        int raw = getPlayerAbilityLevels(uuid).getOrDefault(abilityId, 0);
        if (raw > 0) return raw;
        if (ownedTalents != null && ownedTalents.contains(abilityId) && AbilityUpgradeConfig.has(abilityId)) return 1;
        return 0;
    }

    public static void setAbilityLevel(UUID uuid, String abilityId, int level) {
        if (level <= 0) getPlayerAbilityLevels(uuid).remove(abilityId);
        else getPlayerAbilityLevels(uuid).put(abilityId, level);
    }

    public static int getSpentUpgradePoints(UUID uuid) {
        int total = 0;
        Map<String, Integer> levels = getPlayerAbilityLevels(uuid);
        for (var e : levels.entrySet()) {
            String id = e.getKey();
            int lvl = e.getValue() == null ? 0 : e.getValue();
            if (lvl <= 1) continue;
            for (int next = 2; next <= lvl; next++) {
                total += AbilityUpgradeConfig.getUpgradePointCost(id, next);
            }
        }
        return total;
    }

    public static int getCooldown(UUID uuid, String key) {
        return getPlayerCooldowns(uuid).getOrDefault(key, 0);
    }

    public static void setCooldown(UUID uuid, String key, int ticks) {
        getPlayerCooldowns(uuid).put(key, ticks);
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

    public static void migrateAssassinEvoBuffs(Set<String> talents) { }

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
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 8;
            case 5 -> 11;
            case 6 -> 15;
            case 7 -> 21;
            default -> 0;
        };
    }

    public static boolean isBranchBlocked(UUID uuid, Talent t) {
        Set<String> owned = getPlayerTalents(uuid);

        Talent root = Talent.subclassRootFor(t);
        if (root != null) {
            for (Talent b : Talent.subclassBasesFor(t)) {
                if (b != root && owned.contains(b.id)) return true;
            }
        }

        if (t.branch.isEmpty()) return false;
        for (String id : owned) {
            Talent ot = Talent.getById(id);
            if (ot != null && ot != t && ot.branch.equals(t.branch)) {
                if (!Talent.isSameHierarchy(t, ot)) return true;
            }
        }
        return false;
    }

    public static boolean isRaceForbidden(UUID uuid, Talent t) {
        Race playerRace = getRace(uuid);
        for (Race forbidden : t.forbiddenRaces) {
            if (forbidden == playerRace) return true;
        }
        return false;
    }

    public static void applyStartingBonus(ServerPlayer player) {
        UUID uuid = player.getUUID();
        String playerName = player.getGameProfile().getName();

        if (playerRaces.containsKey(uuid)) return;

        Path configPath = player.getServer().getWorldPath(LevelResource.ROOT).resolve("lvluping_preset.json");

        Race raceToSet = Race.HUMAN;
        int starsToSet = 2;

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray players = json.getAsJsonArray("players");

                for (int i = 0; i < players.size(); i++) {
                    JsonObject pObj = players.get(i).getAsJsonObject();
                    if (pObj.get("name").getAsString().equalsIgnoreCase(playerName)) {
                        raceToSet = Race.getById(pObj.get("race").getAsString());
                        starsToSet = pObj.get("stars").getAsInt();
                        break;
                    }
                }
            } catch (Exception e) {
                LvlupingMod.LOGGER.error("Ошибка чтения lvluping_preset.json: " + e.getMessage());
            }
        }

        playerRaces.put(uuid, raceToSet != null ? raceToSet : Race.HUMAN);
        playerStars.put(uuid, starsToSet);

        LvlupingMod.LOGGER.info("Применены стартовые параметры для {}: Раса={}, Звезды={}",
                playerName, playerRaces.get(uuid).label, starsToSet);
    }

    public static void save(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("lvluping_data.dat");
        CompoundTag root = new CompoundTag();
        Set<UUID> allPlayers = new HashSet<>(playerLevels.keySet());
        allPlayers.addAll(playerStars.keySet());
        allPlayers.addAll(playerCooldowns.keySet());
        allPlayers.addAll(playerAbilityLevels.keySet());

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

            CompoundTag cData = new CompoundTag();
            getPlayerCooldowns(uuid).forEach(cData::putInt);
            pData.put("cooldowns", cData);

            CompoundTag aData = new CompoundTag();
            getPlayerAbilityLevels(uuid).forEach(aData::putInt);
            pData.put("ability_upgrades", aData);

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
                migrateAssassinEvoBuffs(talents);

                Map<String, Integer> statsMap = getPlayerStatsMap(uuid);
                statsMap.clear();
                CompoundTag sData = pData.getCompound("attributes");
                for (String sKey : sData.getAllKeys()) statsMap.put(sKey, sData.getInt(sKey));

                Map<String, Integer> cooldownMap = getPlayerCooldowns(uuid);
                cooldownMap.clear();
                CompoundTag cData = pData.contains("cooldowns") ? pData.getCompound("cooldowns") : new CompoundTag();
                for (String cKey : cData.getAllKeys()) cooldownMap.put(cKey, cData.getInt(cKey));

                Map<String, Integer> abilityMap = getPlayerAbilityLevels(uuid);
                abilityMap.clear();
                CompoundTag aData = pData.contains("ability_upgrades") ? pData.getCompound("ability_upgrades") : new CompoundTag();
                for (String aKey : aData.getAllKeys()) abilityMap.put(aKey, aData.getInt(aKey));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}