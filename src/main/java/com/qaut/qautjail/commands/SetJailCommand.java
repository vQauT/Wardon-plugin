package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class SetJailCommand implements CommandExecutor {

    private final QauTJail plugin;

    public SetJailCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getLanguageManager().getMessage("setjail_usage"));
            return true;
        }

        String jailName = args[0];
        Location loc = player.getLocation();

        File jailFile = new File(plugin.getDataFolder(), "Jails/" + jailName + ".yml");
        if (jailFile.exists()) {
            player.sendMessage(plugin.getLanguageManager().format("setjail_exists", jailName));
            return true;
        }

        YamlConfiguration jailConfig = new YamlConfiguration();

        jailConfig.set("name", jailName);
        jailConfig.set("world", loc.getWorld().getName());
        jailConfig.set("x", loc.getX());
        jailConfig.set("y", loc.getY());
        jailConfig.set("z", loc.getZ());
        jailConfig.set("yaw", loc.getYaw());
        jailConfig.set("pitch", loc.getPitch());

        try {
            jailConfig.save(jailFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(plugin.getLanguageManager().getMessage("save_error"));
            return true;
        }

        player.sendMessage(plugin.getLanguageManager().format("setjail_success", jailName));
        return true;
    }
}
