package org.mrutcka.lvluping.client;

import net.minecraft.client.resources.language.I18n;

import java.util.concurrent.ThreadLocalRandom;

public final class ModSplashTexts {

    private static final String[] KEYS = {
            "splash.lvluping.1",
            "splash.lvluping.2",
            "splash.lvluping.3",
            "splash.lvluping.4",
            "splash.lvluping.5",
            "splash.lvluping.6",
            "splash.lvluping.7",
            "splash.lvluping.8",
            "splash.lvluping.9",
            "splash.lvluping.10",
            "splash.lvluping.11",
            "splash.lvluping.12",
            "splash.lvluping.13",
            "splash.lvluping.14",
            "splash.lvluping.15"
    };

    private ModSplashTexts() {}

    public static String pickRandom() {
        String key = KEYS[ThreadLocalRandom.current().nextInt(KEYS.length)];
        return I18n.get(key);
    }
}
