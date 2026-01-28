package com.qaut.qautjail.listeners;

import com.qaut.qautjail.QauTJail;
import com.qaut.qautjail.utils.UpdateChecker;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateJoinListener implements Listener {

    private final QauTJail plugin;

    public UpdateJoinListener(QauTJail plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("wardon.*")) return;
        if (!UpdateChecker.hasUpdate()) return;

        String current = plugin.getPluginMeta().getVersion();
        String latest = UpdateChecker.getLatestVersion();
        String prefix = plugin.getLanguageManager().getMessage("update.prefix");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            String available = plugin.getLanguageManager().getMessage("update.available");
            String currentMsg = plugin.getLanguageManager()
                    .getMessage("update.current")
                    .replace("{0}", current);
            String latestMsg = plugin.getLanguageManager()
                    .getMessage("update.latest")
                    .replace("{0}", latest);
            player.sendMessage(prefix + " " + available + "\n" + currentMsg + "\n" + latestMsg);

            TextComponent button = new TextComponent(
                    plugin.getLanguageManager().getMessage("update.button")
            );
            button.setClickEvent(
                    new ClickEvent(
                            ClickEvent.Action.OPEN_URL,
                            "https://modrinth.com/plugin/wardon"
                    )
            );

            player.spigot().sendMessage(button);

        }, 40L); // بعد ثانيتين من الدخول

    }
}
