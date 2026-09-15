package com.fistpractice.configuration;

import com.fistpractice.FistPractice;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GameModeManager {

    private final FistPractice plugin;
    private final Map<String, GameMode> modes = new ConcurrentHashMap<>();

    public GameModeManager(FistPractice plugin) {
        this.plugin = plugin;
    }

    public void loadModes() {
        modes.clear();
        File folder = new File(plugin.getDataFolder(), "modes");
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            // First run - copy bundled defaults out of the jar
            for (String def : new String[]{"nodebuff.yml", "boxing.yml"}) {
                plugin.saveResource("modes/" + def, false);
            }
            files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        }
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                GameMode mode = GameMode.fromYaml(yaml);
                modes.put(mode.getId(), mode);
                plugin.getLogger().info("Loaded game mode: " + mode.getId());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load mode file " + file.getName(), e);
            }
        }
    }

    public GameMode get(String id) {
        return id == null ? null : modes.get(id.toUpperCase());
    }

    public boolean exists(String id) {
        return get(id) != null;
    }

    public Map<String, GameMode> getAll() {
        return modes;
    }
}
