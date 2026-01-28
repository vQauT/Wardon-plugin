package com.qaut.qautjail.utils;

import com.qaut.qautjail.QauTJail;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class JailManager implements Listener {

    private final QauTJail plugin;
    private final Map<UUID, JailData> jailedPlayers = new HashMap<>();
    private final Map<UUID, String> pendingSigns = new HashMap<>();

    public JailManager(QauTJail plugin) {
        this.plugin = plugin;
    }

    // سبب الإفراج
    public enum ReleaseCause {
        MANUAL,            // /unjail
        SENTENCE_COMPLETE, // انتهاء المدة
        SHUTDOWN           // عند الإيقاف
    }

    // البيانات
    public static class JailData {
        public final Location oldLocation;
        public final ItemStack[] inventoryContents;
        public final ItemStack[] armorContents;
        public final ItemStack offhand;
        public final Location oldRespawn;

        public long releaseTime;
        public long remainingTime;
        public final boolean onlineOnly;
        public boolean pendingRelease;

        public final String reason;
        public final String jailName;
        public final Location jailLocation;
        public final Location signLocation;
        public int taskId;

        public JailData(Location oldLocation, ItemStack[] inventory, ItemStack[] armor, ItemStack offhand,
                        long releaseTime, long remainingTime, boolean onlineOnly,
                        String reason, String jailName, Location jailLocation, Location signLocation,
                        Location oldRespawn) {
            this.oldLocation = oldLocation;
            this.inventoryContents = inventory;
            this.armorContents = armor;
            this.offhand = offhand;
            this.releaseTime = releaseTime;
            this.remainingTime = remainingTime;
            this.onlineOnly = onlineOnly;
            this.pendingRelease = false;
            this.reason = reason;
            this.jailName = jailName;
            this.jailLocation = jailLocation;
            this.signLocation = signLocation;
            this.oldRespawn = oldRespawn;
        }
    }

    public void setPendingSign(UUID uuid, String jailName) { pendingSigns.put(uuid, jailName); }
    public String getPendingSign(UUID uuid) { return pendingSigns.get(uuid); }
    public void clearPendingSign(UUID uuid) { pendingSigns.remove(uuid); }

    public boolean isJailed(UUID uuid) { return jailedPlayers.containsKey(uuid); }
    public Map<UUID, JailData> getJailedPlayers() { return jailedPlayers; }

    // ========================== سجن اللاعب ==========================
    public boolean jailPlayer(Player player, String jailName, long durationMillis, String reason,
                              Location jailLocation, Location signLocation) {

        UUID id = player.getUniqueId();
        if (jailedPlayers.containsKey(id)) return false;

        boolean onlineOnly = plugin.getConfig().getBoolean("onlinejail", false);

        Location oldLoc = player.getLocation();
        ItemStack[] inv = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        long now = System.currentTimeMillis();
        long releaseAt = now + durationMillis;
        long remaining = onlineOnly ? durationMillis : 0L;

        Location oldRespawn = player.getBedSpawnLocation();
        if (oldRespawn == null) oldRespawn = player.getWorld().getSpawnLocation();

        player.setBedSpawnLocation(jailLocation, true);

        JailData data = new JailData(oldLoc, inv, armor, offhand,
                releaseAt, remaining, onlineOnly, reason, jailName, jailLocation, signLocation, oldRespawn);

        jailedPlayers.put(id, data);

        // تنظيف الإنفنتوري ونقل اللاعب
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.teleport(jailLocation);

        // تحديث اللوحة
        if (signLocation != null && signLocation.getBlock().getState() instanceof Sign sign) {
            sign.setLine(0, ChatColor.DARK_GRAY + "⛓ " + ChatColor.GOLD + "" + ChatColor.BOLD + "Wardon Jail");
            sign.setLine(1, ChatColor.AQUA + player.getName());
            sign.setLine(2, ChatColor.YELLOW + formatTime(durationMillis));
            sign.setLine(3, ChatColor.RED + reason);
            sign.update();
        }

        // تشغيل العد التنازلي
        startCountdown(id, player.getName(), data);

        return true;
    }

    // ========================== العداد ==========================
    private void startCountdown(UUID uuid, String playerName, JailData data) {
        if (data.taskId != 0) Bukkit.getScheduler().cancelTask(data.taskId);

        data.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            long remaining = data.onlineOnly
                    ? Math.max(0, data.remainingTime -= 1000)
                    : data.releaseTime - System.currentTimeMillis();

            if (remaining <= 0) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) releasePlayerInternal(p, ReleaseCause.SENTENCE_COMPLETE, null, false);
                else data.pendingRelease = true;
                Bukkit.getScheduler().cancelTask(data.taskId);
                data.taskId = 0;
                return;
            }

            // تحديث الساين
            if (data.signLocation != null && data.signLocation.getBlock().getState() instanceof Sign sign) {
                sign.setLine(0, ChatColor.DARK_GRAY + "" + ChatColor.GOLD + "" + ChatColor.BOLD + "Wardon 🌙");
                sign.setLine(1, ChatColor.WHITE + playerName);
                sign.setLine(2, ChatColor.AQUA + formatTime(remaining));
                sign.setLine(3, ChatColor.RED + data.reason);
                sign.update();
            }

        }, 0L, 20L);
    }

    // ========================== فك السجن ==========================
    public void releasePlayerManual(Player player, String moderator) {
        releasePlayerInternal(player, ReleaseCause.MANUAL, moderator, false);
    }

    public void releasePlayer(Player player) {
        releasePlayerInternal(player, ReleaseCause.SENTENCE_COMPLETE, null, false);
    }

    public void releasePlayer(Player player, boolean shutdown) {
        releasePlayerInternal(player, shutdown ? ReleaseCause.SHUTDOWN : ReleaseCause.SENTENCE_COMPLETE, null, shutdown);
    }

    private void releasePlayerInternal(Player player, ReleaseCause cause, String moderator, boolean shutdown) {
        JailData data = jailedPlayers.remove(player.getUniqueId());
        if (data == null) return;

        if (data.taskId != 0) {
            Bukkit.getScheduler().cancelTask(data.taskId);
            data.taskId = 0;
        }

        if (data.oldRespawn != null)
            player.setBedSpawnLocation(data.oldRespawn, true);

        if (!shutdown)
            player.teleport(data.oldLocation);

        player.getInventory().setContents(data.inventoryContents);
        player.getInventory().setArmorContents(data.armorContents);
        player.getInventory().setItemInOffHand(data.offhand);

        // تحديث اللوحة
        if (data.signLocation != null && data.signLocation.getBlock().getState() instanceof Sign sign) {
            sign.setLine(0, "");
            sign.setLine(1, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[ Empty Cell ]");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
        }

        player.sendMessage(plugin.getLanguageManager().getMessage("unjail_player"));

        if (plugin.getConfig().getBoolean("broadcastunjail", true)) {
            String msg = plugin.getLanguageManager().format("unjail_broadcast", player.getName());
            msg = ChatColor.translateAlternateColorCodes('&', msg);
            Bukkit.broadcastMessage(msg);
        }

        // ✅ إرسال Webhook فقط في الحالات غير الـ Shutdown
        if (cause != ReleaseCause.SHUTDOWN) {
            boolean auto = (cause == ReleaseCause.SENTENCE_COMPLETE);
            String releasedBy = auto
                    ? plugin.getLanguageManager().getMessage("system.auto")
                    : (moderator != null ? moderator : "Unknown");

            try {
                new WebhookSender(plugin).sendUnjailEmbed(
                        player.getName(),
                        releasedBy,
                        "https://mc-heads.net/avatar/" + player.getUniqueId(),
                        auto
                );
                plugin.getLogger().info("[Wardon 🌙] Sent unjail webhook for " + player.getName() + " (auto=" + auto + ")");
            } catch (Exception e) {
                plugin.getLogger().warning("[Wardon 🌙] Failed to send unjail webhook: " + e.getMessage());
            }
        }

        plugin.getLogger().info("✅ Player " + player.getName() + " released from jail: " + data.jailName);
    }

    // ========================== مساعدات ==========================
    private String formatTime(long millis) {
        long s = millis / 1000;
        long m = s / 60;
        long h = m / 60;
        long d = h / 24;
        s %= 60; m %= 60; h %= 24;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public boolean isJailOccupied(String jailName) {
        for (JailData data : jailedPlayers.values()) {
            if (data.jailName.equalsIgnoreCase(jailName)) return true;
        }
        return false;
    }

    public void unjailOffline(UUID uuid) {
        JailData data = jailedPlayers.remove(uuid);
        if (data == null) return;

        if (data.taskId != 0) Bukkit.getScheduler().cancelTask(data.taskId);

        if (data.signLocation != null && data.signLocation.getBlock().getState() instanceof Sign sign) {
            sign.setLine(0, "");
            sign.setLine(1, ChatColor.DARK_GREEN + "⛓ " + ChatColor.GREEN + "" + ChatColor.BOLD + "[ Empty Cell ]");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
        }

        plugin.getLogger().info("✅ Offline player released: " + uuid);
    }

    public void releaseAllOnShutdown() {
        for (UUID uuid : new HashSet<>(jailedPlayers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) releasePlayerInternal(p, ReleaseCause.SHUTDOWN, null, true);
        }
        jailedPlayers.clear();
        plugin.getLogger().info("✅ All jailed players released on shutdown.");
    }
    // ✅ عند دخول اللاعب
    public void handleJoin(Player player) {
        UUID id = player.getUniqueId();
        JailData data = jailedPlayers.get(id);
        if (data == null) return;

        // إذا انتهت مدته وهو أوفلاين → يفرج عنه تلقائيًا
        if (!data.onlineOnly && System.currentTimeMillis() >= data.releaseTime) {
            releasePlayer(player);
            return;
        }

        // تأكد أن اللاعب داخل السجن الصحيح
        if (player.getWorld() != data.jailLocation.getWorld()
                || player.getLocation().distanceSquared(data.jailLocation) > 1) {
            player.teleport(data.jailLocation);
        }

        // إعادة تشغيل العداد لو كان متوقف
        if (data.taskId == 0) {
            startCountdown(id, player.getName(), data);
        }

        player.sendMessage(ChatColor.RED + "❗ You are still serving your jail sentence.");
    }

    // ✅ عند خروج اللاعب
    public void handleQuit(Player player) {
        // لا حاجة لشيء هنا لأن العداد يوقف تلقائيًا
    }

    // منع التقاط العناصر
    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        if (isJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + plugin.getLanguageManager().getMessage("jail_item_pickup_blocked"));
        }
    }
}
