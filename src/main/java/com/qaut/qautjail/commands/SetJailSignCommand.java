package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class SetJailSignCommand implements CommandExecutor {

    private final QauTJail plugin;

    public SetJailSignCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return true;
        }

        if (!player.hasPermission("wardon.setjailsign")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLanguageManager().getMessage("setjailsign_usage"));
            return true;
        }

        String jailName = args[0];

        File jailFile = new File(plugin.getDataFolder(), "Jails/" + jailName + ".yml");
        if (!jailFile.exists()) {
            player.sendMessage(plugin.getLanguageManager()
                    .getMessage("setjailsign_notfound")
                    .replace("{jail}", jailName));
            return true;
        }

        plugin.getJailManager().setPendingSign(player.getUniqueId(), jailName);
        player.sendMessage(plugin.getLanguageManager()
                .getMessage("setjailsign_ready")
                .replace("{jail}", jailName));

        return true;
    }
}
