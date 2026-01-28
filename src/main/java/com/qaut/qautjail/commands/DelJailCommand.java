package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class DelJailCommand implements CommandExecutor {

    private final QauTJail plugin;

    public DelJailCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_only"));
            return true;
        }

        if (!player.hasPermission("wardon.deljail")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLanguageManager().getMessage("deljail_usage"));
            return true;
        }

        String jailName = args[0];
        File jailFile = new File(plugin.getDataFolder(), "Jails/" + jailName + ".yml");

        if (!jailFile.exists()) {
            player.sendMessage(plugin.getLanguageManager().getMessage("jail_not_found"));
            return true;
        }

        if (plugin.getJailManager().isJailOccupied(jailName)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("deljail_occupied"));
            return true;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(jailFile);

        if (config.contains("sign.world")) {
            String worldName = config.getString("sign.world");
            int x = config.getInt("sign.x");
            int y = config.getInt("sign.y");
            int z = config.getInt("sign.z");

            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                Location signLoc = new Location(Bukkit.getWorld(worldName), x, y, z);
                if (signLoc.getBlock().getState() instanceof Sign sign) {
                    for (int i = 0; i < 4; i++) sign.setLine(i, "");
                    sign.update();
                    player.sendMessage(plugin.getLanguageManager().getMessage("sign_cleared"));
                }
            }
        }

        if (jailFile.delete()) {
            player.sendMessage(
                    plugin.getLanguageManager()
                            .getMessage("deljail_success")
                            .replace("{jail}", jailName)
            );

            plugin.getLogManager().logEvent("DELJAIL",
                    player.getName() + " deleted jail '" + jailName + "'");
        } else {
            player.sendMessage(
                    plugin.getLanguageManager()
                            .getMessage("deljail_failed")
                            .replace("{jail}", jailName)
            );
        }

        return true;
    }
}
