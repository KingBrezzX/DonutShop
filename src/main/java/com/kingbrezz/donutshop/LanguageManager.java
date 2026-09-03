package com.kingbrezz.donutshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads, validates and serves all supported language resources. */
public final class LanguageManager {
    public static final List<String> SUPPORTED = List.of("id", "en", "zh", "vi", "de");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final DonutShop plugin;
    private final Map<String, FileConfiguration> languages = new LinkedHashMap<>();

    public LanguageManager(DonutShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        languages.clear();

        File directory = new File(plugin.getDataFolder(), "lang");
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().severe("Unable to create language directory: " + directory);
            return;
        }

        boolean createMissing = plugin.getConfig().getBoolean("persistence.create-missing-language-files", true);
        for (String locale : SUPPORTED) {
            File file = new File(directory, locale + ".yml");
            if (!file.exists() && createMissing) {
                plugin.saveResource("lang/" + locale + ".yml", false);
            }
            languages.put(locale, YamlConfiguration.loadConfiguration(file));
        }

        validateLanguageParity();
    }

    private void validateLanguageParity() {
        FileConfiguration english = languages.get("en");
        if (english == null) {
            plugin.getLogger().severe("English language resource could not be loaded.");
            return;
        }

        for (String locale : SUPPORTED) {
            FileConfiguration target = languages.get(locale);
            List<String> missing = new ArrayList<>();
            for (String key : english.getKeys(true)) {
                if (english.isString(key) && (target == null || !target.isString(key))) {
                    missing.add(key);
                }
            }
            if (!missing.isEmpty()) {
                plugin.getLogger().severe("Language " + locale + " is missing keys: " + missing);
            }
        }
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> placeholders) {
        String requested = plugin.getConfig().getString("language", "id").toLowerCase(Locale.ROOT);
        FileConfiguration selected = languages.get(requested);
        if (selected == null) {
            selected = languages.get("id");
        }

        String value = selected == null ? null : selected.getString(path);
        if (value == null) {
            FileConfiguration english = languages.get("en");
            value = english == null ? path : english.getString(path, path);
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return LEGACY.serialize(LEGACY.deserialize(value));
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(component(get(path)));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(component(get(path, placeholders)));
    }

    private static Component component(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public boolean isSupported(String locale) {
        return locale != null && SUPPORTED.contains(locale.toLowerCase(Locale.ROOT));
    }
}
