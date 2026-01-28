package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DelJailTabCompleter implements TabCompleter {

    private final QauTJail plugin;

    public DelJailTabCompleter(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (!sender.hasPermission("wardon.deljail")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return suggestions;
        }

        if (args.length == 1) {
            File jailsFolder = new File(plugin.getDataFolder(), "Jails");

            if (!jailsFolder.exists() || jailsFolder.listFiles() == null || jailsFolder.listFiles().length == 0) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("no_jails_found"));
                return suggestions;
            }

            for (File file : jailsFolder.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    String jailName = file.getName().replace(".yml", "");
                    if (jailName.toLowerCase().startsWith(args[0].toLowerCase())) {
                        suggestions.add(jailName);
                    }
                }
            }
        }

        return suggestions;
    }
}
