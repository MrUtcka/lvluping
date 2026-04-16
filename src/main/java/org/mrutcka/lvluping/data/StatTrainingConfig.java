package org.mrutcka.lvluping.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.mrutcka.lvluping.LvlupingMod;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StatTrainingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static volatile ZoneBox speedZone = ZoneBox.disabled();
    public static volatile ZoneBox healthPvpZone = ZoneBox.disabled();

    public static volatile String minecellsTrainingEntityId = "minecells:protector";

    public static volatile int damageUnitsPerLevel = 12;
    public static volatile int damageFromMannequinHit = 5;
    public static volatile int damageFromHayHit = 42;
    public static volatile int meleeTrainingCooldownTicks = 12;

    public static volatile int speedUnitsPerLevel = 18_000;
    public static volatile int speedUnitsPerBlock = 3;

    public static volatile int healthUnitsPerLevel = 9_000;
    public static volatile int healthFromPvPHit = 25;

    public static volatile int tfThreshold1 = 4_000;
    public static volatile int tfThreshold2 = 8_000;
    public static volatile int tfThreshold3 = 12_000;
    public static volatile int tfThreshold4 = 16_000;
    public static volatile int tfDecayPerSecondWhenIdle = 8;
    public static volatile double tfApproachFraction1 = 0.82;
    public static volatile double tfApproachFraction2 = 0.82;
    public static volatile double tfApproachFraction3 = 0.82;
    public static volatile double tfApproachFraction4 = 0.88;
    public static volatile double tfTier3ApplyChance = 1.0;
    public static volatile int tfPostTrainDeathPenaltyHours = 24;
    public static volatile int[] tfEffectMinutes = {30, 50, 90, 120};

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

    private static double doubleOr(JsonObject o, String k, double def) {
        if (!o.has(k)) return def;
        try {
            return o.get(k).getAsDouble();
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
            if (root.has("trainingFatigue")) {
                JsonObject tf = root.getAsJsonObject("trainingFatigue");
                tfThreshold1 = intOr(tf, "threshold1", tfThreshold1);
                tfThreshold2 = intOr(tf, "threshold2", tfThreshold2);
                tfThreshold3 = intOr(tf, "threshold3", tfThreshold3);
                tfThreshold4 = intOr(tf, "threshold4", tfThreshold4);
                tfDecayPerSecondWhenIdle = intOr(tf, "decayPerSecondWhenNotTraining", tfDecayPerSecondWhenIdle);
                tfApproachFraction1 = doubleOr(tf, "approachFraction1", tfApproachFraction1);
                tfApproachFraction2 = doubleOr(tf, "approachFraction2", tfApproachFraction2);
                tfApproachFraction3 = doubleOr(tf, "approachFraction3", tfApproachFraction3);
                tfApproachFraction4 = doubleOr(tf, "approachFraction4", tfApproachFraction4);
                if (tf.has("effectMinutes") && tf.get("effectMinutes").isJsonArray()) {
                    JsonArray arr = tf.getAsJsonArray("effectMinutes");
                    for (int i = 0; i < Math.min(4, arr.size()); i++) {
                        try {
                            tfEffectMinutes[i] = arr.get(i).getAsInt();
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    if (tf.has("effectMinutesTier1")) tfEffectMinutes[0] = intOr(tf, "effectMinutesTier1", tfEffectMinutes[0]);
                    if (tf.has("effectMinutesTier2")) tfEffectMinutes[1] = intOr(tf, "effectMinutesTier2", tfEffectMinutes[1]);
                    if (tf.has("effectMinutesTier3")) tfEffectMinutes[2] = intOr(tf, "effectMinutesTier3", tfEffectMinutes[2]);
                    if (tf.has("effectMinutesTier4")) tfEffectMinutes[3] = intOr(tf, "effectMinutesTier4", tfEffectMinutes[3]);
                }
                tfTier3ApplyChance = doubleOr(tf, "tier3ApplyChance", tfTier3ApplyChance);
                if (tf.has("postTrainDeathPenaltyHours")) {
                    tfPostTrainDeathPenaltyHours = Math.max(1, intOr(tf, "postTrainDeathPenaltyHours", tfPostTrainDeathPenaltyHours));
                }
            } else if (root.has("fatigue")) {
                migrateLegacyFatigue(root.getAsJsonObject("fatigue"));
            }
            if (root.has("speedZone")) {
                speedZone = ZoneBox.fromJson(root.getAsJsonObject("speedZone"), speedZone);
            }
            if (root.has("healthPvpZone")) {
                healthPvpZone = ZoneBox.fromJson(root.getAsJsonObject("healthPvpZone"), healthPvpZone);
            }
            normalizeThresholds();
        } catch (Exception e) {
            LvlupingMod.LOGGER.error("LVLuping: ошибка stat_training.json — {}", e.toString());
            applyDefaults();
        }
    }

    private static void migrateLegacyFatigue(JsonObject f) {
        int maxWarn = Math.max(
                Math.max(intOr(f, "warnMelee", 4000), intOr(f, "warnBow", 4000)),
                Math.max(intOr(f, "warnSpeed", 5500), intOr(f, "warnPvp", 4000)));
        int maxDeb = Math.max(
                Math.max(intOr(f, "debuffMelee", 6500), intOr(f, "debuffBow", 6500)),
                Math.max(intOr(f, "debuffSpeed", 8000), intOr(f, "debuffPvp", 6500)));
        tfThreshold1 = maxWarn;
        tfThreshold2 = maxWarn + (maxDeb - maxWarn) / 3;
        tfThreshold3 = maxWarn + 2 * (maxDeb - maxWarn) / 3;
        tfThreshold4 = maxDeb + (maxDeb - maxWarn) / 2;
        tfDecayPerSecondWhenIdle = intOr(f, "decayPerSecond", tfDecayPerSecondWhenIdle);
        normalizeThresholds();
    }

    private static void normalizeThresholds() {
        if (tfThreshold2 <= tfThreshold1) tfThreshold2 = tfThreshold1 + 1;
        if (tfThreshold3 <= tfThreshold2) tfThreshold3 = tfThreshold2 + 1;
        if (tfThreshold4 <= tfThreshold3) tfThreshold4 = tfThreshold3 + 1;
        for (int i = 0; i < tfEffectMinutes.length; i++) {
            tfEffectMinutes[i] = Math.max(1, tfEffectMinutes[i]);
        }
        tfTier3ApplyChance = Math.max(0.0, Math.min(1.0, tfTier3ApplyChance));
        tfPostTrainDeathPenaltyHours = Math.max(1, tfPostTrainDeathPenaltyHours);
    }

    private static void applyDefaults() {
        speedZone = ZoneBox.disabled();
        healthPvpZone = ZoneBox.disabled();
    }

    public static int tfEffectTicksForTier(int tier1to3) {
        int idx = Math.min(2, Math.max(0, tier1to3 - 1));
        return tfEffectMinutes[idx] * 60 * 20;
    }

    public static int tfPostTrainDeathPenaltyTicks() {
        return tfPostTrainDeathPenaltyHours * 60 * 60 * 20;
    }

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
        JsonObject tf = new JsonObject();
        tf.addProperty("threshold1", tfThreshold1);
        tf.addProperty("threshold2", tfThreshold2);
        tf.addProperty("threshold3", tfThreshold3);
        tf.addProperty("threshold4", tfThreshold4);
        tf.addProperty("decayPerSecondWhenNotTraining", tfDecayPerSecondWhenIdle);
        tf.addProperty("approachFraction1", tfApproachFraction1);
        tf.addProperty("approachFraction2", tfApproachFraction2);
        tf.addProperty("approachFraction3", tfApproachFraction3);
        tf.addProperty("approachFraction4", tfApproachFraction4);
        JsonArray mins = new JsonArray();
        for (int m : tfEffectMinutes) mins.add(m);
        tf.add("effectMinutes", mins);
        tf.addProperty("tier3ApplyChance", tfTier3ApplyChance);
        tf.addProperty("postTrainDeathPenaltyHours", tfPostTrainDeathPenaltyHours);
        root.add("trainingFatigue", tf);
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
