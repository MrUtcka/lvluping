package org.mrutcka.lvluping.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mrutcka.lvluping.LvlupingMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public final class AbilityUpgradeConfig {
    private static volatile Map<String, AbilityDef> defs = new HashMap<>();

    public static void load() {
        try {
            InputStream in = AbilityUpgradeConfig.class.getClassLoader().getResourceAsStream("lvluping/ability_upgrades.json");
            if (in == null) {
                defs = new HashMap<>();
                return;
            }
            try (Reader r = new InputStreamReader(in)) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                JsonObject abilities = root.has("abilities") ? root.getAsJsonObject("abilities") : new JsonObject();
                Map<String, AbilityDef> out = new HashMap<>();
                for (String key : abilities.keySet()) {
                    JsonObject obj = abilities.getAsJsonObject(key);
                    int maxLevel = obj.has("maxLevel") ? obj.get("maxLevel").getAsInt() : 1;
                    JsonObject params = obj.has("params") ? obj.getAsJsonObject("params") : new JsonObject();
                    JsonObject costs = obj.has("upgradeCost") ? obj.getAsJsonObject("upgradeCost") : null;
                    out.put(key, new AbilityDef(maxLevel, params, costs));
                }
                defs = out;
            }
        } catch (Exception e) {
            LvlupingMod.LOGGER.error("Failed to load ability_upgrades.json: " + e.getMessage());
            defs = new HashMap<>();
        }
    }

    public static boolean has(String abilityId) {
        return defs.containsKey(abilityId);
    }

    public static int getMaxLevel(String abilityId) {
        AbilityDef d = defs.get(abilityId);
        return d == null ? 1 : Math.max(1, d.maxLevel);
    }
    
    public static boolean isUpgradeable(String abilityId) {
        return getMaxLevel(abilityId) > 1;
    }

    public static int getUpgradePointCost(String abilityId, int nextLevel) {
        AbilityDef d = defs.get(abilityId);
        if (d == null || d.upgradeCost == null) return 1;
        if (d.upgradeCost.has(String.valueOf(nextLevel))) return d.upgradeCost.get(String.valueOf(nextLevel)).getAsInt();
        if (d.upgradeCost.has("default")) return d.upgradeCost.get("default").getAsInt();
        return 1;
    }

    public static double getDouble(String abilityId, String param, int level, double fallback) {
        AbilityDef d = defs.get(abilityId);
        if (d == null) return fallback;
        JsonElement el = d.params.get(param);
        if (el == null) return fallback;
        return resolveNumber(el, level, fallback);
    }

    public static int getInt(String abilityId, String param, int level, int fallback) {
        return (int) Math.round(getDouble(abilityId, param, level, fallback));
    }

    private static double resolveNumber(JsonElement el, int level, double fallback) {
        if (el == null) return fallback;
        if (el.isJsonPrimitive()) return el.getAsDouble();
        if (el.isJsonArray()) {
            var arr = el.getAsJsonArray();
            int idx = Math.max(1, level) - 1;
            if (idx < arr.size()) return arr.get(idx).getAsDouble();
            if (!arr.isEmpty()) return arr.get(arr.size() - 1).getAsDouble();
        }
        return fallback;
    }

    private record AbilityDef(int maxLevel, JsonObject params, JsonObject upgradeCost) {}
}

