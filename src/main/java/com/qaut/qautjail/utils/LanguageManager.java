package com.qaut.qautjail.utils;

import com.qaut.qautjail.QauTJail;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.File;

public class LanguageManager {

    private final QauTJail plugin;
    private YamlConfiguration messages = null;
    private String currentLang = "en";

    // ============================================================
    // 🔄 تحديث ملفات اللغة تلقائيًا عند تشغيل السيرفر
    // ============================================================
    public static void updateLanguagesOnStartup(QauTJail plugin) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        updateLang(plugin, "en");
        updateLang(plugin, "ar");
    }

    private static void updateLang(QauTJail plugin, String lang) {
        try {
            File file = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

            // إذا الملف غير موجود → انسخه كامل
            if (!file.exists()) {
                plugin.saveResource("lang/" + lang + ".yml", false);
                plugin.getLogger().info("[Lang] Created " + lang + ".yml");
                return;
            }

            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);

            // ✅ تحميل ملف اللغة الافتراضي من داخل الـ JAR بشكل صحيح
            InputStream stream = plugin.getResource("lang/" + lang + ".yml");
            if (stream == null) {
                plugin.getLogger().warning("[Lang] Default " + lang + ".yml not found in jar");
                return;
            }

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );

            boolean updated = false;

            for (String key : defaults.getKeys(true)) {
                if (!current.contains(key)) {
                    current.set(key, defaults.get(key));
                    updated = true;
                }
            }

            if (updated) {
                current.save(file);
                plugin.getLogger().info("[Lang] Updated missing keys in " + lang + ".yml");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[Lang] Failed to update " + lang + ".yml: " + e.getMessage());
        }
    }


    // ============================================================
    // 🧠 Constructor
    // ============================================================
    public LanguageManager(QauTJail plugin, String initialLang) {
        this.plugin = plugin;
        loadLanguage(initialLang);
    }

    // ============================================================
    // 🌍 تحميل اللغة
    // ============================================================
    public void loadLanguage(String langCode) {
        this.currentLang = (langCode == null ? "en" : langCode.toLowerCase());

        try {
            File langFile = new File(plugin.getDataFolder(), "lang/" + currentLang + ".yml");
            if (!langFile.exists()) {
                plugin.getLogger().warning("[Lang] File not found: " + langFile.getName() + " -> fallback to en.yml");
                langFile = new File(plugin.getDataFolder(), "lang/en.yml");
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

    // ============================================================
    // 💬 جلب النصوص مع الألوان والمتغيرات
    // ============================================================
    public String getMessage(String key, Object... args) {
        if (messages == null) return key;

        if (!messages.contains(key)) {
            plugin.getLogger().warning("[Lang] Missing key: " + key + " (" + currentLang + ")");
            return key;
        }

        String msg = messages.getString(key, key);

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }

        msg = msg.replace("\n", "\n&r");
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    // Alias
    public String format(String key, Object... args) {
        return getMessage(key, args);
    }

    // For non-Minecraft outputs (e.g., Discord), no color codes or resets.
    public String getPlainMessage(String key, Object... args) {
        if (messages == null) return key;

        if (!messages.contains(key)) {
            plugin.getLogger().warning("[Lang] Missing key: " + key + " (" + currentLang + ")");
            return key;
        }

        String msg = messages.getString(key, key);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return msg;
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
