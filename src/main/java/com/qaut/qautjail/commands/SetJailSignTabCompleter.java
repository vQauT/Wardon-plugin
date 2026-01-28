package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SetJailSignTabCompleter implements TabCompleter {

    private final QauTJail plugin;

    public SetJailSignTabCompleter(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            File jailsFolder = new File(plugin.getDataFolder(), "Jails");
            if (jailsFolder.exists() && jailsFolder.isDirectory()) {
                for (File file : jailsFolder.listFiles()) {
                    if (file.getName().endsWith(".yml")) {
                        String jailName = file.getName().replace(".yml", "");
                        if (jailName.toLowerCase().startsWith(args[0].toLowerCase())) {
                            suggestions.add(jailName);
                        }
                    }
                }
            }
        }

        return suggestions;
    }
}
