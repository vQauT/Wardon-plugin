package com.qaut.qautjail.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qaut.qautjail.QauTJail;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class UpdateChecker {

    private static final String GITHUB_API =
            "https://api.github.com/repos/vQauT/Wardon-plugin/releases/latest";
    private static final String DOWNLOAD_URL =
            "https://modrinth.com/plugin/wardon";

    private static boolean hasUpdate = false;
    private static String latestVersion = "";

    public static void check(QauTJail plugin) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                URI.create(GITHUB_API).toURL().openStream()
                        )
                );

                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                // tag_name = v1.3.0
                latestVersion = json.get("tag_name").getAsString().replace("v", "");
                String currentVersion = plugin.getPluginMeta().getVersion();

                plugin.getLogger().warning(
                        "[UpdateChecker DEBUG] Current=" + currentVersion + " | Latest=" + latestVersion
                );

                if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                    hasUpdate = true;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        notifyAdmins(plugin, currentVersion, latestVersion);
                        notifyDiscord(plugin, currentVersion, latestVersion);
                    });
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check updates from GitHub.");
            }
        });
    }

    public static boolean hasUpdate() {
        return hasUpdate;
    }
    public static String getLatestVersion() {
        return latestVersion;
    }

    // ==========================
    // 📢 إشعار داخل السيرفر
    // ==========================
    private static void notifyAdmins(QauTJail plugin, String current, String latest) {

        String prefix = plugin.getLanguageManager().getMessage("update.prefix");

        boolean needsUpdate = !current.equalsIgnoreCase(latest);

        for (Player p : Bukkit.getOnlinePlayers()) {

            if (!p.hasPermission("wardon.main") && !p.isOp()) continue;

            // الحالة العامة (البادئة مرة واحدة فقط)
            String status = plugin.getLanguageManager().getMessage("update.status")
                    .replace("{current}", current)
                    .replace("{latest}", latest);

            if (!needsUpdate) {
                // ✅ محدث
                p.sendMessage(prefix + " " + status);
                p.sendMessage(plugin.getLanguageManager().getMessage("update.uptodate"));
                continue;
            }

            // ❌ يحتاج تحديث (البادئة مرة واحدة فقط)
            String available = plugin.getLanguageManager().getMessage("update.available");
            String currentMsg = plugin.getLanguageManager().getMessage("update.current")
                    .replace("{0}", current);
            String latestMsg = plugin.getLanguageManager().getMessage("update.latest")
                    .replace("{0}", latest);
            p.sendMessage(prefix + " " + available + "\n" + currentMsg + "\n" + latestMsg);

            // 🔘 زر [ تحديث الآن ]
            TextComponent button = new TextComponent(
                    plugin.getLanguageManager().getMessage("update.button")
            );

            button.setClickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL)
            );

            button.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new Text(plugin.getLanguageManager().getMessage("update.hover"))
                    )
            );

            p.spigot().sendMessage(button);
        }
    }

    // ==========================
    // 🌍 إشعار ديسكورد
    // ==========================
    private static void notifyDiscord(QauTJail plugin, String current, String latest) {

        if (!plugin.getConfig().getBoolean("update-check.discord", true)) return;

        new com.qaut.qautjail.utils.WebhookSender(plugin)
                .sendUpdateEmbed(current, latest);
    }
}
