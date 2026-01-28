package com.qaut.qautjail.utils;

import com.qaut.qautjail.QauTJail;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class LanguageManager {

    private final QauTJail plugin;
    private YamlConfiguration messages = null;
    private String currentLang = "en";

    public LanguageManager(QauTJail plugin, String initialLang) {
        this.plugin = plugin;
        loadLanguage(initialLang);
    }

    public void loadLanguage(String langCode) {
        this.currentLang = (langCode == null ? "en" : langCode.toLowerCase());

        try {
            File langFolder = new File(plugin.getDataFolder(), "lang");
            if (!langFolder.exists()) langFolder.mkdirs();

            File langFile = new File(langFolder, currentLang + ".yml");
            if (!langFile.exists()) {
                plugin.getLogger().warning("[Lang] File not found: " + langFile.getName() + " -> fallback to en.yml");
                langFile = new File(langFolder, "en.yml");
            }

            if (!langFile.exists()) {
                plugin.getLogger().severe("[Lang] Missing en.yml fallback! Using raw keys.");
                messages = null;
                return;
            }

            messages = YamlConfiguration.loadConfiguration(langFile);
            plugin.getLogger().info("[Lang] Loaded language: " + currentLang);

        } catch (Exception e) {
            plugin.getLogger().severe("[Lang] Failed to load language: " + e.getMessage());
            messages = null;
        }
    }

    // ✅ ترجمة النصوص + الألوان
    public String getMessage(String key, Object... args) {
        if (messages == null) return key;

        String msg = messages.getString(key, key);

        // استبدال القيم داخل الأقواس {0}, {1}, ...
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }

        // ✅ تفعيل أكواد الألوان (&a → §a)
        msg = ChatColor.translateAlternateColorCodes('&', msg);

        return msg;
    }

    public String format(String key, Object... args) {
        return getMessage(key, args);
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
