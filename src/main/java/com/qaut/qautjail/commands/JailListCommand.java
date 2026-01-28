package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class JailListCommand implements CommandExecutor {

    private final QauTJail plugin;

    public JailListCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        File jailFolder = new File(plugin.getDataFolder(), "Jails");
        File[] files = jailFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null || files.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLanguageManager().getMessage("no_jails_found")));
            return true;
        }

        // 🔹 ترتيب الملفات من الأقدم إلى الأحدث
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLanguageManager().getMessage("jaillist_header")));

        for (File file : files) {
            YamlConfiguration jailConfig = YamlConfiguration.loadConfiguration(file);
            String jailName = file.getName().replace(".yml", "");

            // 🔹 دعم للمسارين المختلفين لتخزين العالم والإحداثيات
            String world = jailConfig.getString("location.world",
                    jailConfig.getString("world", plugin.getLanguageManager().getMessage("jaillist_unknown_world")));

            double x = jailConfig.getDouble("location.x", jailConfig.getDouble("x", 0));
            double y = jailConfig.getDouble("location.y", jailConfig.getDouble("y", 0));
            double z = jailConfig.getDouble("location.z", jailConfig.getDouble("z", 0));

            String coords = String.format("%.0f, %.0f, %.0f", x, y, z);

            // 🔹 صياغة الإحداثيات لتكون قابلة للنقر
            TextComponent line = new TextComponent(ChatColor.translateAlternateColorCodes('&',
                    plugin.getLanguageManager().getMessage("jaillist_entry")
                            .replace("{jail}", jailName)
                            .replace("{world}", world)
                            .replace("{coords}", coords)));

            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + x + " " + y + " " + z));

            sender.spigot().sendMessage(line);
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLanguageManager().getMessage("jaillist_footer")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getLanguageManager().getMessage("jaillist_hint")));

        return true;
    }
}
