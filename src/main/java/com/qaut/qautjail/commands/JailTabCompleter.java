package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JailTabCompleter implements TabCompleter {

    private final QauTJail plugin;

    public JailTabCompleter(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // 🧍‍♂️ Player names
            Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));

        } else if (args.length == 2) {
            // 🏛 Jail names
            File jailFolder = new File(plugin.getDataFolder(), "Jails");
            if (jailFolder.exists() && jailFolder.isDirectory()) {
                for (File f : jailFolder.listFiles()) {
                    if (f.getName().endsWith(".yml")) {
                        suggestions.add(f.getName().replace(".yml", ""));
                    }
                }
            }

        } else if (args.length == 3) {
            // ⏱ Time suggestions (from translation)
            suggestions.add(plugin.getLanguageManager().getMessage("time.30s"));
            suggestions.add(plugin.getLanguageManager().getMessage("time.5m"));
            suggestions.add(plugin.getLanguageManager().getMessage("time.10m"));
            suggestions.add(plugin.getLanguageManager().getMessage("time.1h"));
            suggestions.add(plugin.getLanguageManager().getMessage("time.1d"));

        } else if (args.length == 4) {
            // 📜 Reasons (from translation)
            suggestions.add(plugin.getLanguageManager().getMessage("reason.cheating"));
            suggestions.add(plugin.getLanguageManager().getMessage("reason.spam"));
            suggestions.add(plugin.getLanguageManager().getMessage("reason.insult"));
            suggestions.add(plugin.getLanguageManager().getMessage("reason.hack"));
        }

        return suggestions;
    }
}
