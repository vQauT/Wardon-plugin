package com.qaut.qautjail.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.qaut.qautjail.QauTJail;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class WebhookSender {

    private final QauTJail plugin;

    public WebhookSender(QauTJail plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    // 📡 إرسال Webhook (أساسي)
    // ============================================================
    private void sendWebhook(JsonObject payload) {
        try {
            boolean enabled = plugin.getConfig().getBoolean("discord.enable", false);
            String url = plugin.getConfig().getString("discord.webhook", "");

            if (!enabled) return;
            if (url == null || url.isEmpty()) return;

            URL webhookUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) webhookUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            connection.getInputStream().close();
            connection.disconnect();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Wardon] Discord Webhook Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 🔔 Embed التحديث
    // ============================================================
    public void sendUpdateEmbed(String current, String latest) {

        JsonObject json = new JsonObject();
        json.addProperty("username",
                plugin.getLanguageManager().getMessage("embed.update.username")
        );
        json.addProperty(
                "avatar_url",
                "https://cdn.discordapp.com/attachments/1163745191765737472/1424322484206960741/logo.png?ex=697b23ae&is=6979d22e&hm=89b531dc85da570558bb94d63e7c6f5b38143a3929930434b081a48bda547c33&"
        );

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty(
                "title",
                plugin.getLanguageManager().getMessage("embed.update.title")
        );

        String description = plugin.getLanguageManager()
                .getPlainMessage("embed.update.description")
                .replace("{current}", current)
                .replace("{latest}", latest);

        embed.addProperty("description", description);
        embed.addProperty("color", 0xF1C40F); // أصفر تحذيري

        JsonObject footer = new JsonObject();
        footer.addProperty(
                "text",
                plugin.getLanguageManager().getMessage("embed.footer")
        );
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json);
    }

    // ============================================================
    // 🔒 Embed سجن لاعب
    // ============================================================
    public void sendJailEmbed(String player, UUID uuid, String jailedBy,
                              String reason, String jail, long duration) {

        JsonObject json = new JsonObject();
        json.addProperty("username", "Wardon Logger");
        json.addProperty("avatar_url", "https://cdn.discordapp.com/attachments/1163745191765737472/1424322484206960741/logo.png?ex=697b23ae&is=6979d22e&hm=89b531dc85da570558bb94d63e7c6f5b38143a3929930434b081a48bda547c33&");

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty(
                "title",
                plugin.getLanguageManager().getMessage("embed.jailed.title")
        );
        embed.addProperty("color", 0xE74C3C);

        JsonArray fields = new JsonArray();
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.jailed.player"),
                player, true
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.jailed.jail"),
                jail, true
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.jailed.duration"),
                formatDuration(duration), true
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.jailed.reason"),
                reason, false
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.jailed.by"),
                jailedBy, false
        ));

        embed.add("fields", fields);

        JsonObject thumb = new JsonObject();
        thumb.addProperty("url", "https://mc-heads.net/avatar/" + uuid);
        embed.add("thumbnail", thumb);

        JsonObject footer = new JsonObject();
        footer.addProperty(
                "text",
                plugin.getLanguageManager().getMessage("embed.footer")
        );
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json);
    }

    // ============================================================
    // ✅ Embed الإفراج
    // ============================================================
    public void sendUnjailEmbed(String player, String unjailedBy,
                                String avatarUrl, boolean auto) {

        JsonObject json = new JsonObject();
        json.addProperty("username", "Wardon Logger");
        json.addProperty("avatar_url", avatarUrl);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        String titleKey = auto
                ? "embed.unjailed.auto_title"
                : "embed.unjailed.manual_title";

        embed.addProperty(
                "title",
                plugin.getLanguageManager().getMessage(titleKey)
        );
        embed.addProperty("color", 0x2ECC71);

        JsonArray fields = new JsonArray();
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.unjailed.player"),
                player, true
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.unjailed.by"),
                unjailedBy, true
        ));
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.unjailed.type"),
                plugin.getLanguageManager().getMessage(
                        auto
                                ? "embed.unjailed.type_auto"
                                : "embed.unjailed.type_manual"
                ),
                true
        ));

        embed.add("fields", fields);

        JsonObject footer = new JsonObject();
        footer.addProperty(
                "text",
                plugin.getLanguageManager().getMessage("embed.footer")
        );
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json);
    }

    // ============================================================
    // 🧩 Helpers
    // ============================================================
    private JsonObject createField(String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        return field;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
