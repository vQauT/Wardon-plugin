package com.qaut.qautjail.commands;

import com.qaut.qautjail.QauTJail;
import com.qaut.qautjail.utils.JailManager;
import com.qaut.qautjail.utils.WebhookSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        // ✅ الإفراج عن اللاعب
        jailManager.releasePlayer(target);

        // ✅ رسائل مترجمة للإداري واللاعب
        sender.sendMessage(plugin.getLanguageManager().format("unjail_admin", target.getName()));
        target.sendMessage(plugin.getLanguageManager().getMessage("unjail_player"));

        // ✅ إرسال إشعار Webhook إلى ديسكورد
        new WebhookSender(plugin).sendUnjailEmbed(
                target.getName(),
                sender.getName(),
                "https://mc-heads.net/avatar/" + target.getUniqueId(),
                false // 👈 لأن الإفراج هنا يدوي
        );


        // ✅ تسجيل الحدث في اللوق
        plugin.getLogManager().logEvent("UNJAIL",
                sender.getName() + " released player '" + target.getName() + "'");

        // ✅ بث عام في حال تم تفعيله من الكونفيغ
        if (plugin.getConfig().getBoolean("broadcastunjail", true)) {
            String msg = plugin.getLanguageManager().format("unjail_broadcast", target.getName());
            msg = ChatColor.translateAlternateColorCodes('&', msg);
            Bukkit.broadcastMessage(msg);
        }

        return true;
    }
}
