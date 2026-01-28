package com.qaut.qautjail.utils;
import com.qaut.qautjail.QauTJail;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker {

    private static final String VERSION_URL =
            "https://wardon.xyz/latest-version.txt"; // ← عدل الرابط لو تحب

    private static boolean hasUpdate = false;
    private static String latestVersion = "";

    public static void check(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(VERSION_URL);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(url.openStream()));

                latestVersion = reader.readLine().trim();
                String current = plugin.getDescription().getVersion();

                if (!current.equalsIgnoreCase(latestVersion)) {
                    hasUpdate = true;

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        notifyAdmins((QauTJail) plugin, current, latestVersion);
                        notifyDiscord(plugin, current, latestVersion);
                    });
                }

            } catch (Exception e) {
                // تجاهل أي خطأ (ما نبي نكسر البلوقن)
            }
        });
    }

    public static boolean hasUpdate() {
        return hasUpdate;
    }

    /* ================== */
    /* إشعار داخل اللعبة */
    /* ================== */
    private static void notifyAdmins(QauTJail plugin, String current, String latest) {

        String prefix = plugin.getLanguageManager().getMessage("update.prefix");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("wardon.admin")) continue;

            p.sendMessage(prefix + " " +
                    plugin.getLanguageManager().getMessage("update.available"));

            p.sendMessage(prefix + " " +
                    plugin.getLanguageManager().getMessage("update.current")
                            .replace("{0}", current));

            p.sendMessage(prefix + " " +
                    plugin.getLanguageManager().getMessage("update.latest")
                            .replace("{0}", latest));

            p.sendMessage(prefix + " " +
                    plugin.getLanguageManager().getMessage("update.hint"));
        }
    }


    /* ================== */
    /* إشعار ديسكورد */
    /* ================== */
    private static void notifyDiscord(JavaPlugin plugin, String current, String latest) {
        if (!plugin.getConfig().getBoolean("update-check.discord")) return;

        WebhookSender sender = new WebhookSender((QauTJail) plugin);
        sender.sendUpdateEmbed(current, latest);
    }

}
