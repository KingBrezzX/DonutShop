package com.kingbrezz.donutshop;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LanguageManager {

    private final DonutShop plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();

    public LanguageManager(DonutShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        languages.clear();

        File directory = new File(plugin.getDataFolder(), "lang");

        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create language directory.");
        }

        String[] supported = {
                "id",
                "en",
                "zh",
                "vi",
                "de"
        };

        for (String language : supported) {
            saveLanguageIfMissing(language);

            File file = new File(directory, language + ".yml");

            if (file.exists()) {
                languages.put(
                        language,
                        YamlConfiguration.loadConfiguration(file)
                );
            }
        }
    }

    private void saveLanguageIfMissing(String language) {
        File file = new File(
                plugin.getDataFolder(),
                "lang/" + language + ".yml"
        );

        if (file.exists()) {
            return;
        }

        plugin.saveResource(
                "lang/" + language + ".yml",
                false
        );
    }

    public String get(String path) {
        String language = plugin.getConfig()
                .getString("language", "id")
                .toLowerCase();

        FileConfiguration configuration =
                languages.getOrDefault(
                        language,
                        languages.get("id")
                );

        if (configuration == null) {
            return path;
        }

        return color(
                configuration.getString(path, path)
        );
    }

    public String get(String path, Map<String, String> placeholders) {
        String message = get(path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue()
            );
        }

        return message;
    }

    public String get(String path, String key, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key, value);

        return get(path, placeholders);
    }

    public void send(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    private String color(String text) {
        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public Map<String, FileConfiguration> getLanguages() {
        return Collections.unmodifiableMap(languages);
    }

    public boolean isSupported(String language) {
        return languages.containsKey(language.toLowerCase());
    }
}
