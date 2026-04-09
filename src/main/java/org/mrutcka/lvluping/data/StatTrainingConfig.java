package org.mrutcka.lvluping.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.mrutcka.lvluping.LvlupingMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StatTrainingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static volatile ZoneBox speedZone = ZoneBox.disabled();
    public static volatile ZoneBox healthPvpZone = ZoneBox.disabled();

    public static volatile String minecellsTrainingEntityId = "minecells:protector";

    public static volatile int damageUnitsPerLevel = 12_000;
    public static volatile int damageFromMannequinHit = 2;
    public static volatile int damageFromHayHit = 2;
    public static volatile int meleeTrainingCooldownTicks = 12;

    public static volatile int speedUnitsPerLevel = 18_000;
    public static volatile int speedUnitsPerBlock = 3;

    public static volatile int healthUnitsPerLevel = 9_000;
    public static volatile int healthFromPvPHit = 25;

    public static volatile int fatigueWarnMelee = 4_000;
    public static volatile int fatigueDebuffMelee = 6_500;
    public static volatile int fatigueWarnBow = 4_000;
    public static volatile int fatigueDebuffBow = 6_500;
    public static volatile int fatigueWarnSpeed = 5_500;
    public static volatile int fatigueDebuffSpeed = 8_000;
    public static volatile int fatigueWarnPvp = 4_000;
    public static volatile int fatigueDebuffPvp = 6_500;

    public static volatile int fatigueDecayPerSecond = 8;

    public static volatile int slownessTicks = 30 * 60 * 20;
    public static volatile int slownessAmpMin = 1;
    public static volatile int slownessAmpMax = 2;
    public static volatile int weaknessTicksShort = 30 * 60 * 20;
    public static volatile int weaknessTicksLong = 60 * 60 * 20;

    public record ZoneBox(
            boolean enabled,
            String dimension,
            int minX, int maxX,
            int minZ, int maxZ,
            int minY, int maxY
    ) {
        static ZoneBox disabled() {
            return new ZoneBox(false, "minecraft:overworld", 0, 0, 0, 0, -64, 320);
        }

        public boolean contains(ServerLevel level, double x, double y, double z) {
            if (!enabled) return false;
            if (!level.dimension().location().toString().equals(dimension)) return false;
            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y);
            int bz = (int) Math.floor(z);
            return bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ && by >= minY && by <= maxY;
        }

        public static ZoneBox fromJson(JsonObject o, ZoneBox defaults) {
            if (o == null) return defaults;
            boolean en = o.has("enabled") && o.get("enabled").getAsBoolean();
            String dim = o.has("dimension") ? o.get("dimension").getAsString() : defaults.dimension;
            int minX = intOr(o, "minX", defaults.minX);
            int maxX = intOr(o, "maxX", defaults.maxX);
            int minZ = intOr(o, "minZ", defaults.minZ);
            int maxZ = intOr(o, "maxZ", defaults.maxZ);
            int minY = intOr(o, "minY", defaults.minY);
            int maxY = intOr(o, "maxY", defaults.maxY);
            return new ZoneBox(en, dim, Math.min(minX, maxX), Math.max(minX, maxX), Math.min(minZ, maxZ), Math.max(minZ, maxZ), minY, maxY);
        }
    }

    private static int intOr(JsonObject o, String k, int def) {
        if (!o.has(k)) return def;
        try {
            return o.get(k).getAsInt();
        } catch (Exception e) {
            return def;
        }
    }

    public static Path configPath(MinecraftServer server) {
        return LvlupingServerData.root(server).resolve("stat_training.json");
    }

    public static void ensureDefaultFile(MinecraftServer server) {
        Path path = configPath(server);
        if (Files.exists(path)) return;
        try {
            LvlupingServerData.ensureRootExists(server);
            try (InputStream in = StatTrainingConfig.class.getClassLoader().getResourceAsStream("lvluping/stat_training_default.json")) {
                if (in != null) {
                    Files.copy(in, path);
                    LvlupingMod.LOGGER.info("LVLuping: создан stat_training.json из шаблона");
                } else {
                    Files.writeString(path, GSON.toJson(new JsonObject()), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            LvlupingMod.LOGGER.warn("LVLuping: не удалось создать stat_training.json: {}", e.toString());
        }
    }

    public static void load(MinecraftServer server) {
        ensureDefaultFile(server);
        Path path = configPath(server);
        if (!Files.exists(path)) {
            applyDefaults();
            return;
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) {
                applyDefaults();
                return;
            }
            if (root.has("minecellsTrainingEntityId")) {
                minecellsTrainingEntityId = root.get("minecellsTrainingEntityId").getAsString();
            }
            if (root.has("damage")) {
                JsonObject d = root.getAsJsonObject("damage");
                damageUnitsPerLevel = intOr(d, "unitsPerLevel", damageUnitsPerLevel);
                damageFromMannequinHit = intOr(d, "fromMannequinHit", damageFromMannequinHit);
                damageFromHayHit = intOr(d, "fromHayHit", damageFromHayHit);
                meleeTrainingCooldownTicks = intOr(d, "meleeCooldownTicks", meleeTrainingCooldownTicks);
            }
            if (root.has("speed")) {
                JsonObject s = root.getAsJsonObject("speed");
                speedUnitsPerLevel = intOr(s, "unitsPerLevel", speedUnitsPerLevel);
                speedUnitsPerBlock = intOr(s, "unitsPerBlock", speedUnitsPerBlock);
            }
            if (root.has("health")) {
                JsonObject h = root.getAsJsonObject("health");
                healthUnitsPerLevel = intOr(h, "unitsPerLevel", healthUnitsPerLevel);
                healthFromPvPHit = intOr(h, "fromPvPHit", healthFromPvPHit);
            }
            if (root.has("fatigue")) {
                JsonObject f = root.getAsJsonObject("fatigue");
                fatigueWarnMelee = intOr(f, "warnMelee", fatigueWarnMelee);
                fatigueDebuffMelee = intOr(f, "debuffMelee", fatigueDebuffMelee);
                fatigueWarnBow = intOr(f, "warnBow", fatigueWarnBow);
                fatigueDebuffBow = intOr(f, "debuffBow", fatigueDebuffBow);
                fatigueWarnSpeed = intOr(f, "warnSpeed", fatigueWarnSpeed);
                fatigueDebuffSpeed = intOr(f, "debuffSpeed", fatigueDebuffSpeed);
                fatigueWarnPvp = intOr(f, "warnPvp", fatigueWarnPvp);
                fatigueDebuffPvp = intOr(f, "debuffPvp", fatigueDebuffPvp);
                fatigueDecayPerSecond = intOr(f, "decayPerSecond", fatigueDecayPerSecond);
                slownessTicks = intOr(f, "slownessTicks", slownessTicks);
                slownessAmpMin = intOr(f, "slownessAmpMin", slownessAmpMin);
                slownessAmpMax = intOr(f, "slownessAmpMax", slownessAmpMax);
                weaknessTicksShort = intOr(f, "weaknessTicksShort", weaknessTicksShort);
                weaknessTicksLong = intOr(f, "weaknessTicksLong", weaknessTicksLong);
            }
            if (root.has("speedZone")) {
                speedZone = ZoneBox.fromJson(root.getAsJsonObject("speedZone"), speedZone);
            }
            if (root.has("healthPvpZone")) {
                healthPvpZone = ZoneBox.fromJson(root.getAsJsonObject("healthPvpZone"), healthPvpZone);
            }
        } catch (Exception e) {
            LvlupingMod.LOGGER.error("LVLuping: ошибка stat_training.json — {}", e.toString());
            applyDefaults();
        }
    }

    private static void applyDefaults() {
        speedZone = ZoneBox.disabled();
        healthPvpZone = ZoneBox.disabled();
    }

    /** Сохранить зоны и числовые поля (для команд). */
    public static void saveToFile(MinecraftServer server) throws Exception {
        Path path = configPath(server);
        LvlupingServerData.ensureRootExists(server);
        JsonObject root = new JsonObject();
        root.addProperty("minecellsTrainingEntityId", minecellsTrainingEntityId);
        JsonObject dmg = new JsonObject();
        dmg.addProperty("unitsPerLevel", damageUnitsPerLevel);
        dmg.addProperty("fromMannequinHit", damageFromMannequinHit);
        dmg.addProperty("fromHayHit", damageFromHayHit);
        dmg.addProperty("meleeCooldownTicks", meleeTrainingCooldownTicks);
        root.add("damage", dmg);
        JsonObject spd = new JsonObject();
        spd.addProperty("unitsPerLevel", speedUnitsPerLevel);
        spd.addProperty("unitsPerBlock", speedUnitsPerBlock);
        root.add("speed", spd);
        JsonObject hp = new JsonObject();
        hp.addProperty("unitsPerLevel", healthUnitsPerLevel);
        hp.addProperty("fromPvPHit", healthFromPvPHit);
        root.add("health", hp);
        JsonObject fat = new JsonObject();
        fat.addProperty("warnMelee", fatigueWarnMelee);
        fat.addProperty("debuffMelee", fatigueDebuffMelee);
        fat.addProperty("warnBow", fatigueWarnBow);
        fat.addProperty("debuffBow", fatigueDebuffBow);
        fat.addProperty("warnSpeed", fatigueWarnSpeed);
        fat.addProperty("debuffSpeed", fatigueDebuffSpeed);
        fat.addProperty("warnPvp", fatigueWarnPvp);
        fat.addProperty("debuffPvp", fatigueDebuffPvp);
        fat.addProperty("decayPerSecond", fatigueDecayPerSecond);
        fat.addProperty("slownessTicks", slownessTicks);
        fat.addProperty("slownessAmpMin", slownessAmpMin);
        fat.addProperty("slownessAmpMax", slownessAmpMax);
        fat.addProperty("weaknessTicksShort", weaknessTicksShort);
        fat.addProperty("weaknessTicksLong", weaknessTicksLong);
        root.add("fatigue", fat);
        root.add("speedZone", zoneToJson(speedZone));
        root.add("healthPvpZone", zoneToJson(healthPvpZone));
        Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static JsonObject zoneToJson(ZoneBox z) {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", z.enabled());
        o.addProperty("dimension", z.dimension);
        o.addProperty("minX", z.minX);
        o.addProperty("maxX", z.maxX);
        o.addProperty("minZ", z.minZ);
        o.addProperty("maxZ", z.maxZ);
        o.addProperty("minY", z.minY);
        o.addProperty("maxY", z.maxY);
        return o;
    }

    public static boolean minecellsEntityMatches(net.minecraft.resources.ResourceLocation typeId) {
        return typeId != null && typeId.toString().equals(minecellsTrainingEntityId);
    }

    private StatTrainingConfig() {}
}
