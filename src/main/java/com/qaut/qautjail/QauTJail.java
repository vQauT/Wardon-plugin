package com.qaut.qautjail;

import com.qaut.qautjail.commands.*;
import com.qaut.qautjail.listeners.JailListener;
import com.qaut.qautjail.utils.JailManager;
import com.qaut.qautjail.utils.LanguageManager;
import com.qaut.qautjail.utils.LogManager;
import com.qaut.qautjail.utils.UpdateChecker;
import com.qaut.qautjail.listeners.UpdateJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class QauTJail extends JavaPlugin {

    private JailManager jailManager;
    private LanguageManager languageManager;
    private LogManager logManager;

    // ============================================================
    // 🗂️ ملفات اللغة
    // ============================================================
    public void ensureLangFile(String code) {
        File langFile = new File(getDataFolder(), "lang/" + code + ".yml");
        if (!langFile.exists()) {
            saveResource("lang/" + code + ".yml", false);
        }
    }

    public String getLang() {
        return getConfig().getString("lang", "en").toLowerCase();
    }

    // ============================================================
    // 🚀 عند تشغيل البلوقن
    // ============================================================
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        // 📁 إنشاء مجلدات أساسية
        new File(getDataFolder(), "Jails").mkdirs();
        new File(getDataFolder(), "lang").mkdirs();
        new File(getDataFolder(), "logs").mkdirs();

        // 🌐 تحميل اللغة
        String langCode = getLang();
        ensureLangFile(langCode);
        ensureLangFile("en");

// 🔄 تحديث ملفات اللغة تلقائيًا عند التشغيل
        LanguageManager.updateLanguagesOnStartup(this);
        this.languageManager = new LanguageManager(this, langCode);
        this.jailManager = new JailManager(this);
        this.logManager = new LogManager(this);
        this.languageManager = new LanguageManager(this, langCode);

        getLogger().info("✅ LogManager initialized successfully!");


        // 💬 تسجيل الأوامر
        getCommand("setjail").setExecutor(new SetJailCommand(this));
        getCommand("setjailsign").setExecutor(new SetJailSignCommand(this));
        getCommand("setjailsign").setTabCompleter(new SetJailSignTabCompleter(this));
        getCommand("deljail").setExecutor(new DelJailCommand(this));
        getCommand("deljail").setTabCompleter(new DelJailTabCompleter(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnJailCommand(this));
        getCommand("jaillist").setExecutor(new JailListCommand(this));
        getCommand("jail").setTabCompleter(new JailTabCompleter(this));

        MainCommand mainCommand = new MainCommand(this);
        getCommand("wardon").setExecutor(mainCommand);
        getCommand("wardon").setTabCompleter(new MainTabCompleter());

        // 🎧 تسجيل الأحداث
        getServer().getPluginManager().registerEvents(new JailListener(this), this);
        getServer().getPluginManager().registerEvents(jailManager, this);
        getServer().getPluginManager().registerEvents(
                new UpdateJoinListener(this), this
        );
        // ⏱️ مهمة دورية للتحقق من المسجونين الأوفلاين
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                for (UUID uuid : jailManager.getJailedPlayers().keySet()) {
                    var data = jailManager.getJailedPlayers().get(uuid);
                    if (!data.onlineOnly && System.currentTimeMillis() >= data.releaseTime) {
                        Bukkit.getScheduler().runTask(this, () -> jailManager.unjailOffline(uuid));
                    }
                }
            } catch (Exception ignored) {}
        }, 20L * 60, 20L * 60);

        getLogger().info("Loaded language: " + langCode);
        getLogger().info("Wardon 🌙 enabled!");

        // 🔔 فحص التحديثات (GitHub)
        UpdateChecker.check(this);
    }

    // ============================================================
    // 📴 عند إيقاف البلوقن
    // ============================================================
    @Override
    public void onDisable() {
        jailManager.releaseAllOnShutdown();
        getLogger().info("Wardon 🌙 disabled!");
    }

    // ============================================================
    // 🧩 Getters
    // ============================================================
    public JailManager getJailManager() {
        return jailManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }
}
