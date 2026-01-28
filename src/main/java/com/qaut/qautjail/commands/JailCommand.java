package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import com.qaut.qautjail.utils.WebhookSender;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;

public class JailCommand implements CommandExecutor {

    private final QauTJail plugin;

    public JailCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("jail_usage"));
            return true;
        }

        // 🎯 Get target player
        String mention = args[0];
        String playerName = mention.startsWith("@") ? mention.substring(1) : mention;
        Player target = Bukkit.getPlayerExact(playerName);

        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
            return true;
        }

        if (plugin.getJailManager().isJailed(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().format("jail_already", target.getName()));
            return true;
        }

        // 🏛 Jail name
        String jailName = args[1];

        // 🚫 Check if jail is occupied
        if (plugin.getJailManager().isJailOccupied(jailName)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("jail_occupied"));
            return true;
        }

        // ⏱ Time
        String timeArg = args[2];
        long duration = parseTime(timeArg);
        if (duration <= 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("time_invalid"));
            return true;
        }

        // 🧾 Reason
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));

        // 📂 Jail file
        File jailFile = new File(plugin.getDataFolder(), "Jails/" + jailName + ".yml");
        if (!jailFile.exists()) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("jail_not_found"));
            return true;
        }
        YamlConfiguration jailConfig = YamlConfiguration.loadConfiguration(jailFile);

        // 📍 Jail location
        Location jailLoc = new Location(
                Bukkit.getWorld(jailConfig.getString("world")),
                jailConfig.getDouble("x"),
                jailConfig.getDouble("y"),
                jailConfig.getDouble("z"),
                (float) jailConfig.getDouble("yaw"),
                (float) jailConfig.getDouble("pitch")
        );

        // 📍 Sign location (if exists)
        Location signLoc = null;
        if (jailConfig.contains("sign.world")) {
            signLoc = new Location(
                    Bukkit.getWorld(jailConfig.getString("sign.world")),
                    jailConfig.getInt("sign.x"),
                    jailConfig.getInt("sign.y"),
                    jailConfig.getInt("sign.z")
            );
        }

        // 🚨 Jail player
        boolean ok = plugin.getJailManager().jailPlayer(target, jailName, duration, reason, jailLoc, signLoc);
        if (ok) {
            // ✅ Discord Webhook
            new WebhookSender(plugin).sendJailEmbed(
                    target.getName(),
                    target.getUniqueId(),
                    sender.getName(),
                    reason,
                    jailName,
                    duration
            );

            String displayTimeArg = formatTimeForLang(timeArg, plugin.getLanguageManager().getCurrentLang());
            String amsg = plugin.getLanguageManager().format(
                    "broadcast_jail",
                    target.getName(),
                    jailName,
                    displayTimeArg,
                    reason
            );

            Bukkit.broadcastMessage(amsg);

            target.sendMessage(plugin.getLanguageManager().format(
                    "jail_player",
                    displayTimeArg,
                    reason
            ));


            // ✅ Broadcast (if enabled)
            if (plugin.getConfig().getBoolean("broadcastjail", true)) {
                String msg = plugin.getLanguageManager().format(
                        "broadcast_jail",
                        target.getName(),
                        jailName,
                        displayTimeArg,
                        reason
                );
                Bukkit.broadcastMessage(msg);
            }


            // ✅ Log to file
            plugin.getLogManager().logEvent("JAIL",
                    sender.getName() + " jailed " + target.getName() +
                            " for " + timeArg + " (Reason: " + reason + ", Jail: " + jailName + ")");
        }

        return true;
    }

    /**
     * ⏱ Converts time string (e.g., 1m, 1h) to milliseconds.
     */
    private long parseTime(String arg) {
        long total = 0;
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < arg.length(); i++) {
            char c = arg.charAt(i);

            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                if (number.length() == 0) continue;
                long value = Long.parseLong(number.toString());
                number.setLength(0);

                switch (c) {
                    case 's': total += value * 1000L; break;                        // seconds
                    case 'm':
                        if (i + 1 < arg.length() && arg.charAt(i + 1) == 'o') {
                            total += value * 30L * 24 * 60 * 60 * 1000L; // month
                            i++;
                        } else {
                            total += value * 60 * 1000L; // minute
                        }
                        break;
                    case 'h': total += value * 60 * 60 * 1000L; break;              // hour
                    case 'd': total += value * 24 * 60 * 60 * 1000L; break;         // day
                    case 'w': total += value * 7L * 24 * 60 * 60 * 1000L; break;    // week
                    case 'y': total += value * 365L * 24 * 60 * 60 * 1000L; break;  // year
                    default: return -1; // unknown unit
                }
            }
        }
        return total > 0 ? total : -1;
    }

    private String formatTimeForLang(String timeArg, String lang) {
        if (!"ar".equalsIgnoreCase(lang) || timeArg == null || timeArg.isEmpty()) {
            return timeArg;
        }

        StringBuilder out = new StringBuilder();
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < timeArg.length(); i++) {
            char c = timeArg.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
                continue;
            }

            if (number.length() == 0) continue;

            int value;
            try {
                value = Integer.parseInt(number.toString());
            } catch (NumberFormatException e) {
                return timeArg;
            }

            String unit = arabicUnit(value, c, timeArg, i);
            if (unit == null) return timeArg;

            if (out.length() > 0) out.append(" و ");
            out.append(value).append(' ').append(unit);
            number.setLength(0);
        }

        return out.length() == 0 ? timeArg : out.toString();
    }

    private String arabicUnit(int value, char unitChar, String timeArg, int index) {
        if (unitChar == 'm' && index + 1 < timeArg.length() && timeArg.charAt(index + 1) == 'o') {
            return arabicPlural(value, "شهر", "شهرين", "أشهر");
        }

        switch (unitChar) {
            case 's':
                return arabicPlural(value, "ثانية", "ثانيتين", "ثوانٍ");
            case 'm':
                return arabicPlural(value, "دقيقة", "دقيقتين", "دقائق");
            case 'h':
                return arabicPlural(value, "ساعة", "ساعتين", "ساعات");
            case 'd':
                return arabicPlural(value, "يوم", "يومين", "أيام");
            case 'w':
                return arabicPlural(value, "أسبوع", "أسبوعين", "أسابيع");
            case 'y':
                return arabicPlural(value, "سنة", "سنتين", "سنوات");
            default:
                return null;
        }
    }

    private String arabicPlural(int value, String singular, String dual, String plural) {
        if (value == 1) return singular;
        if (value == 2) return dual;
        if (value >= 3 && value <= 10) return plural;
        return singular;
    }

}
