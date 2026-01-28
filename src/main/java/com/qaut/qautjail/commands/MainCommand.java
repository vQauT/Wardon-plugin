package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MainCommand implements CommandExecutor {

    private final QauTJail plugin;

    public MainCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ✅ /wardon reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            String lang = plugin.getLang();
            plugin.ensureLangFile(lang);
            plugin.getLanguageManager().loadLanguage(lang);

            sender.sendMessage(plugin.getLanguageManager().getMessage("reload_success"));
            return true;
        }

        // ✅ /wardon setlang <ar|en>
        if (args.length == 2 && args[0].equalsIgnoreCase("setlang")) {
            String lang = args[1].toLowerCase();

            if (!lang.equals("ar") && !lang.equals("en")) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("setlang_usage"));
                return true;
            }

            plugin.getConfig().set("lang", lang);
            plugin.saveConfig();

            plugin.ensureLangFile(lang);
            plugin.getLanguageManager().loadLanguage(lang);

            // 🔹 عرض اسم اللغة من ملف الترجمة
            String display = plugin.getLanguageManager().getMessage("language." + lang);
            sender.sendMessage(plugin.getLanguageManager().format("setlang_success", display));
            return true;
        }

        // ✅ رسالة المساعدة
        sender.sendMessage(plugin.getLanguageManager().getMessage("wardon_help"));
        return true;
    }
}
