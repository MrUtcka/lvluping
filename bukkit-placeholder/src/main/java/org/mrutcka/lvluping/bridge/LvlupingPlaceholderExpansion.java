package org.mrutcka.lvluping.bridge;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

public class LvlupingPlaceholderExpansion extends PlaceholderExpansion {

    private static final String PLAYER_LEVELS = "org.mrutcka.lvluping.data.PlayerLevels";

    @Override
    public @NotNull String getIdentifier() {
        return "lvluping";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lvluping";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();
        String p = params == null ? "" : params.trim().toLowerCase();
        return switch (p) {
            case "level", "lvl" -> str(callInt(uuid, "getLevel"));
            case "stars", "star", "rank" -> str(callInt(uuid, "getStars"));
            case "max_level", "maxlevel", "max_lvl" -> {
                int stars = callInt(uuid, "getStars");
                yield str(callIntStars("getMaxLevel", stars));
            }
            default -> null;
        };
    }

    private static String str(int v) {
        return v < 0 ? "" : Integer.toString(v);
    }

    private static int callInt(UUID uuid, String method) {
        try {
            Class<?> c = Class.forName(PLAYER_LEVELS);
            Method m = c.getMethod(method, UUID.class);
            Object o = m.invoke(null, uuid);
            if (o instanceof Integer i) return i;
        } catch (Throwable ignored) {
            return -1;
        }
        return -1;
    }

    private static int callIntStars(String method, int stars) {
        try {
            Class<?> c = Class.forName(PLAYER_LEVELS);
            Method m = c.getMethod(method, int.class);
            Object o = m.invoke(null, stars);
            if (o instanceof Integer i) return i;
        } catch (Throwable ignored) {
            return -1;
        }
        return -1;
    }
}
