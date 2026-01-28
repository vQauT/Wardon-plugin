package com.qaut.qautjail;

import com.qaut.qautjail.commands.*;
import com.qaut.qautjail.listeners.JailListener;
import com.qaut.qautjail.utils.JailManager;
import com.qaut.qautjail.utils.LanguageManager;
import com.qaut.qautjail.utils.LogManager;
import com.qaut.qautjail.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

// ✅ مكتبة WebSocket الجديدة
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;

public class QauTJail extends JavaPlugin {

    private JailManager jailManager;
    private LanguageManager languageManager;
    private LogManager logManager;

    // 🌐 نظام التتبع والاتصال المباشر (Live Activity + Server Counter)
    private WebSocketClient wsClient;
    private final String WS_URL = "ws://154.38.178.36:1090";
    private String serverUUID;
    private long lastHeartbeat = 0;

    // ============================================================
    // 🗂️ ملفات اللغة والتهيئة
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

        // 🌍 حفظ UUID خاص بكل سيرفر
        this.serverUUID = getConfig().getString("serverUUID", null);
        if (this.serverUUID == null) {
            this.serverUUID = UUID.randomUUID().toString();
            getConfig().set("serverUUID", this.serverUUID);
            saveConfig();
        }

        // 🌐 تحميل اللغة
        String langCode = getLang();
        ensureLangFile(langCode);
        ensureLangFile("en");

        // ⚙️ تهيئة الـ Managers
        this.languageManager = new LanguageManager(this, langCode);
        this.jailManager = new JailManager(this);
        this.logManager = new LogManager(this);
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
        UpdateChecker.check(this); // 🔔 فحص التحديث

        // 🔗 الاتصال مع نظام الإحصائيات والأنشطة الحية
        connectWebSocket();
    }

    // ============================================================
    // 📴 عند إيقاف البلوقن
    // ============================================================
    @Override
    public void onDisable() {
        jailManager.releaseAllOnShutdown();

        sendStatus("offline");
        if (wsClient != null && wsClient.isOpen()) {
            try {
                wsClient.close();
            } catch (Exception ignored) {}
        }

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

    // ============================================================
    // 🌐 نظام Live Activity + Telemetry (WebSocket فعلي)
    // ============================================================
    private void connectWebSocket() {
        try {
            getLogger().info("🔌 Trying to connect to JailAPI at " + WS_URL);
            URI uri = new URI(WS_URL);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    getLogger().info("🌐 Connected to JailAPI Live server!");
                    sendRegisterPacket();
                }

                @Override
                public void onMessage(String message) {
                    getLogger().info("📩 Message from JailAPI: " + message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    getLogger().warning("🔌 Connection closed: " + reason + " | Code: " + code);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    getLogger().warning("❌ WebSocket Error: " + ex.getMessage());
                    ex.printStackTrace();
                    scheduleReconnect();
                }
            };

            wsClient.connect();
            getLogger().info("🚀 Connecting... waiting for handshake...");

        } catch (Exception e) {
            getLogger().severe("❌ Failed to start WebSocket connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void scheduleReconnect() {
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            getLogger().info("🔁 Reconnecting to JailAPI...");
            connectWebSocket();
        }, 20L * 10); // بعد 10 ثواني
    }

    private void sendRegisterPacket() {
        try {
            if (wsClient != null && wsClient.isOpen()) {
                String json = String.format(
                        "{\"type\":\"register\",\"serverId\":\"%s\",\"name\":\"%s\",\"port\":%d,\"version\":\"%s\"}",
                        serverUUID,
                        Bukkit.getServer().getName(),
                        Bukkit.getServer().getPort(),
                        Bukkit.getBukkitVersion()
                );
                wsClient.send(json);
                sendStatus("online");
            }
        } catch (Exception e) {
            getLogger().warning("❌ Failed to send register packet: " + e.getMessage());
        }
    }

    private void sendHeartbeat() {
        try {
            if (wsClient != null && wsClient.isOpen()) {
                int online = Bukkit.getOnlinePlayers().size();
                String json = String.format(
                        "{\"type\":\"heartbeat\",\"serverId\":\"%s\",\"onlinePlayers\":%d}",
                        serverUUID, online
                );
                wsClient.send(json);
                lastHeartbeat = System.currentTimeMillis();
            }
        } catch (Exception e) {
            getLogger().warning("❌ Failed to send heartbeat: " + e.getMessage());
        }
    }

    private void sendStatus(String status) {
        try {
            if (wsClient != null && wsClient.isOpen()) {
                String json = String.format(
                        "{\"type\":\"status\",\"status\":\"%s\",\"serverId\":\"%s\"}",
                        status, serverUUID
                );
                wsClient.send(json);
            }
        } catch (Exception e) {
            getLogger().warning("❌ Failed to send status: " + e.getMessage());
        }
    }
}
