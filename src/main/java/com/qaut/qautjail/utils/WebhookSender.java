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

    // ==============================
    // 📡 SEND WEBHOOK
    // ==============================
    private void sendWebhook(JsonObject payload) {
        try {
            boolean enabled = plugin.getConfig().getBoolean("discord.enable", false);
            String url = plugin.getConfig().getString("discord.webhook", "");

            if (!enabled) {
                Bukkit.getLogger().info("[Wardon 🌙] " + plugin.getLanguageManager().getMessage("embed.webhook.disabled"));
                return;
            }

            if (url == null || url.isEmpty()) {
                Bukkit.getLogger().warning("[Wardon 🌙] " + plugin.getLanguageManager().getMessage("embed.webhook.missing_url"));
                return;
            }

            URL webhookUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) webhookUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                Bukkit.getLogger().info("[Wardon 🌙] " + plugin.getLanguageManager().getMessage("embed.webhook.sent"));
            } else {
                Bukkit.getLogger().warning("[Wardon 🌙] " +
                        plugin.getLanguageManager().getMessage("embed.webhook.failed")
                                .replace("{code}", String.valueOf(responseCode)));
            }

            connection.disconnect();

        } catch (Exception e) {
            Bukkit.getLogger().warning("[Wardon 🌙] " +
                    plugin.getLanguageManager().getMessage("embed.webhook.error")
                            .replace("{error}", e.getMessage()));
        }
    }

    // ==============================
    // 🔒 PLAYER JAILED EMBED
    // ==============================
    public void sendJailEmbed(String player, UUID uuid, String jailedBy, String reason, String jail, long duration) {
        JsonObject json = new JsonObject();
        json.addProperty("username", "Wardon 🌙 • Logger");
        json.addProperty("avatar_url", "https://mc-heads.net/avatar/" + uuid);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty("title", "🔒 " + plugin.getLanguageManager().getMessage("embed.jailed.title"));
        embed.addProperty("color", 0xE74C3C); // Red

        JsonArray fields = new JsonArray();
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.jailed.player"), player, true));
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.jailed.jail"), jail, true));
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.jailed.duration"), formatDuration(duration), true));
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.jailed.reason"), reason, false));
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.jailed.by"), jailedBy, false));
        embed.add("fields", fields);

        JsonObject thumb = new JsonObject();
        thumb.addProperty("url", "https://mc-heads.net/avatar/" + uuid);
        embed.add("thumbnail", thumb);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", plugin.getLanguageManager().getMessage("embed.footer"));
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json);
    }

    // ==============================
    // ✅ PLAYER UNJAILED EMBED (manual vs auto)
    // ==============================
    public void sendUnjailEmbed(String player, String unjailedBy, String avatarUrl, boolean auto) {
        JsonObject json = new JsonObject();
        json.addProperty("username", "Wardon 🌙 • Logger");
        json.addProperty("avatar_url", avatarUrl);

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        // عنوان مختلف حسب السبب
        String titleKey = auto ? "embed.unjailed.auto_title" : "embed.unjailed.manual_title";
        embed.addProperty("title", "✅ " + plugin.getLanguageManager().getMessage(titleKey));
        embed.addProperty("color", 0x2ECC71); // Green

        JsonArray fields = new JsonArray();
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.unjailed.player"), player, true));
        fields.add(createField(plugin.getLanguageManager().getMessage("embed.unjailed.by"), unjailedBy, true));

        // نوع الإفراج (يدوي / تلقائي)
        fields.add(createField(
                plugin.getLanguageManager().getMessage("embed.unjailed.type"),
                plugin.getLanguageManager().getMessage(auto ? "embed.unjailed.type_auto" : "embed.unjailed.type_manual"),
                true
        ));

        embed.add("fields", fields);

        JsonObject thumb = new JsonObject();
        thumb.addProperty("url", avatarUrl);
        embed.add("thumbnail", thumb);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", plugin.getLanguageManager().getMessage("embed.footer"));
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json);
    }

    // ==============================
    // ⚙️ HELPERS
    // ==============================
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

    // ==============================
// 🚨 PLUGIN UPDATE EMBED
// ==============================
    public void sendUpdateEmbed(String currentVersion, String latestVersion) {
        JsonObject json = new JsonObject();
        json.addProperty("username", "Wardon 🌙 • Update");
        json.addProperty("avatar_url", "https://i.imgur.com/ZyK9Y5F.png"); // أيقونة ثابتة

        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty("title", "🚨 تحديث جديد لبلوقن Wardon");
        embed.addProperty("description",
                "يوجد تحديث جديد متوفر لبلوقن **Wardon Jail**\n\n" +
                        "🔹 الإصدار الحالي: **" + currentVersion + "**\n" +
                        "🆕 الإصدار الجديد: **" + latestVersion + "**\n\n" +
                        "⚠ يرجى تحديث البلوقن لتجنب المشاكل."
        );

        embed.addProperty("color", 0xF1C40F); // أصفر تحذير

        JsonObject footer = new JsonObject();
        footer.addProperty("text", plugin.getLanguageManager().getMessage("embed.footer"));
        embed.add("footer", footer);

        embeds.add(embed);
        json.add("embeds", embeds);

        sendWebhook(json); // ✅ استدعاء خاص من الداخل (مسموح)
    }

}
