package com.qaut.qautjail.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // الاقتراحات عند كتابة /wardon <...>
            List<String> subCommands = Arrays.asList("reload", "setlang");
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setlang")) {
            // الاقتراحات عند كتابة /wardon setlang <...>
            List<String> langs = Arrays.asList("ar", "en");
            for (String lang : langs) {
                if (lang.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(lang);
                }
            }
        }

        return completions;
    }
}
