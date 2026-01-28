package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import com.qaut.qautjail.utils.JailManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnJailCommand implements CommandExecutor {

    private final QauTJail plugin;

    public UnJailCommand(QauTJail plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ✅ تحقق من عدد الوسائط
        if (args.length != 1) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("unjail_usage"));
            return true;
        }

        // ✅ جلب اللاعب الهدف
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("player_not_found"));
            return true;
        }

        JailManager jailManager = plugin.getJailManager();

        // ✅ تحقق أن اللاعب مسجون بالفعل
        if (!jailManager.isJailed(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("unjail_not_jailed"));
            return true;
        }

        // ✅ الإفراج عن اللاعب (يدوي مع توحيد الإشعارات داخل JailManager)
        jailManager.releasePlayerManual(target, sender.getName());

        // ✅ رسالة للإداري فقط
        sender.sendMessage(plugin.getLanguageManager().format("unjail_admin", target.getName()));


        // ✅ تسجيل الحدث في اللوق
        plugin.getLogManager().logEvent("UNJAIL",
                sender.getName() + " released player '" + target.getName() + "'");

        return true;
    }
}
