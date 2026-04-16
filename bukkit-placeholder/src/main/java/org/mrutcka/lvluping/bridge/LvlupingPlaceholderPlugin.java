package org.mrutcka.lvluping.bridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class LvlupingPlaceholderPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI не найден — плейсхолдеры lvluping не зарегистрированы.");
            return;
        }
        new LvlupingPlaceholderExpansion().register();
        getLogger().info("PlaceholderAPI: плейсхолдеры %lvluping_level%, %lvluping_stars%, %lvluping_max_level%");
    }
}
