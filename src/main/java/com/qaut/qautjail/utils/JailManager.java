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

    // ============================================================
    // 📌 Pending Signs (setjailsign)
    // ============================================================
    public void setPendingSign(UUID uuid, String jailName) {
        pendingSigns.put(uuid, jailName);
    }

    public String getPendingSign(UUID uuid) {
        return pendingSigns.get(uuid);
    }

    public void clearPendingSign(UUID uuid) {
        pendingSigns.remove(uuid);
    }

    // ============================================================
    // 🔓 Release Cause
    // ============================================================
    public enum ReleaseCause {
        MANUAL,
        SENTENCE_COMPLETE,
        SHUTDOWN
    }

    // ============================================================
    // 📦 Jail Data
    // ============================================================
    public static class JailData {

        public final Location oldLocation;
        public final ItemStack[] inventory;
        public final ItemStack[] armor;
        public final ItemStack offhand;
        public final Location oldRespawn;

        public final String jailName;
        public final String reason;
        public final Location jailLocation;
        public final Location signLocation;

        public final boolean onlineOnly;

        public long releaseTime;
        public long remainingTime;
        public int taskId = 0;

        public boolean pendingRelease = false;
        public boolean released = false; // 🔑 المفتاح الأساسي لمنع التكرار

        public JailData(
                Location oldLocation,
                ItemStack[] inventory,
                ItemStack[] armor,
                ItemStack offhand,
                Location oldRespawn,
                String jailName,
                String reason,
                Location jailLocation,
                Location signLocation,
                boolean onlineOnly,
                long releaseTime,
                long remainingTime
        ) {
            this.oldLocation = oldLocation;
            this.inventory = inventory;
            this.armor = armor;
            this.offhand = offhand;
            this.oldRespawn = oldRespawn;
            this.jailName = jailName;
            this.reason = reason;
            this.jailLocation = jailLocation;
            this.signLocation = signLocation;
            this.onlineOnly = onlineOnly;
            this.releaseTime = releaseTime;
            this.remainingTime = remainingTime;
        }
    }

    // ============================================================
    // 🔍 Utils
    // ============================================================
    public boolean isJailed(UUID uuid) {
        return jailedPlayers.containsKey(uuid);
    }

    public Map<UUID, JailData> getJailedPlayers() {
        return jailedPlayers;
    }

    public boolean isJailOccupied(String jailName) {
        for (JailData data : jailedPlayers.values()) {
            if (data.jailName.equalsIgnoreCase(jailName)) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    // 🚨 Jail Player
    // ============================================================
    public boolean jailPlayer(Player player, String jailName, long duration, String reason,
                              Location jailLocation, Location signLocation) {

        if (isJailed(player.getUniqueId())) return false;

        boolean onlineOnly = plugin.getConfig().getBoolean("onlinejail", false);

        Location oldLoc = player.getLocation();
        ItemStack[] inv = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        Location oldRespawn = player.getBedSpawnLocation();
        if (oldRespawn == null) oldRespawn = player.getWorld().getSpawnLocation();

        long now = System.currentTimeMillis();
        long releaseAt = now + duration;
        long remaining = onlineOnly ? duration : 0L;

        JailData data = new JailData(
                oldLoc, inv, armor, offhand, oldRespawn,
                jailName, reason, jailLocation, signLocation,
                onlineOnly, releaseAt, remaining
        );

        jailedPlayers.put(player.getUniqueId(), data);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setBedSpawnLocation(jailLocation, true);
        player.teleport(jailLocation);

        updateSign(data, player.getName(), duration);
        startCountdown(player.getUniqueId(), player.getName(), data);

        return true;
    }

    // ============================================================
    // ⏱ Countdown
    // ============================================================
    private void startCountdown(UUID uuid, String playerName, JailData data) {

        data.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {

            if (data.released) {
                Bukkit.getScheduler().cancelTask(data.taskId);
                data.taskId = 0;
                return;
            }

            long remaining = data.onlineOnly
                    ? Math.max(0, data.remainingTime -= 1000)
                    : data.releaseTime - System.currentTimeMillis();

            if (remaining <= 0) {

                if (data.pendingRelease || data.released) {
                    Bukkit.getScheduler().cancelTask(data.taskId);
                    data.taskId = 0;
                    return;
                }

                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    releasePlayerInternal(p, ReleaseCause.SENTENCE_COMPLETE, null, false);
                } else {
                    data.pendingRelease = true;
                }

                Bukkit.getScheduler().cancelTask(data.taskId);
                data.taskId = 0;
                return;
            }

            updateSign(data, playerName, remaining);

        }, 20L, 20L);
    }

    // ============================================================
    // 🔓 Manual Release
    // ============================================================
    public void releasePlayerManual(Player player, String moderator) {
        JailData data = jailedPlayers.get(player.getUniqueId());
        if (data != null) {
            data.pendingRelease = true;
        }
        releasePlayerInternal(player, ReleaseCause.MANUAL, moderator, false);
    }

    // ============================================================
    // 🔓 Public Auto Release
    // ============================================================
    public void releasePlayer(Player player) {
        releasePlayerInternal(player, ReleaseCause.SENTENCE_COMPLETE, null, false);
    }

    // ============================================================
    // 🔓 Internal Release (ONE TIME ONLY)
    // ============================================================
    private void releasePlayerInternal(Player player, ReleaseCause cause, String moderator, boolean shutdown) {

        JailData data = jailedPlayers.get(player.getUniqueId());
        if (data == null || data.released) return;

        data.released = true;
        jailedPlayers.remove(player.getUniqueId());

        if (data.taskId != 0) Bukkit.getScheduler().cancelTask(data.taskId);

        player.getInventory().setContents(data.inventory);
        player.getInventory().setArmorContents(data.armor);
        player.getInventory().setItemInOffHand(data.offhand);

        if (data.oldRespawn != null)
            player.setBedSpawnLocation(data.oldRespawn, true);

        if (!shutdown)
            player.teleport(data.oldLocation);

        clearSign(data);

        player.sendMessage(plugin.getLanguageManager().getMessage("unjail_player"));

        if (plugin.getConfig().getBoolean("broadcastunjail", true)) {
            Bukkit.broadcastMessage(
                    plugin.getLanguageManager().format("unjail_broadcast", player.getName())
            );
        }

        // ✅ Webhook مرة واحدة فقط
        if (cause != ReleaseCause.SHUTDOWN) {
            boolean auto = (cause == ReleaseCause.SENTENCE_COMPLETE);
            String by = auto
                    ? plugin.getLanguageManager().getMessage("system.auto")
                    : moderator;

            new WebhookSender(plugin).sendUnjailEmbed(
                    player.getName(),
                    by,
                    "https://mc-heads.net/avatar/" + player.getUniqueId(),
                    auto
            );
        }
    }

    // ============================================================
    // 🌙 Offline / Shutdown
    // ============================================================
    public void unjailOffline(UUID uuid) {
        JailData data = jailedPlayers.remove(uuid);
        if (data == null) return;

        if (data.taskId != 0) Bukkit.getScheduler().cancelTask(data.taskId);
        clearSign(data);
    }

    public void releaseAllOnShutdown() {
        for (UUID uuid : new HashSet<>(jailedPlayers.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                releasePlayerInternal(p, ReleaseCause.SHUTDOWN, null, true);
            }
        }
        jailedPlayers.clear();
    }

    // ============================================================
    // 🪧 Sign helpers
    // ============================================================
    private void updateSign(JailData data, String player, long time) {
        if (data.signLocation == null) return;
        if (!(data.signLocation.getBlock().getState() instanceof Sign sign)) return;

        sign.setLine(0, ChatColor.GOLD + "Wardon 🌙");
        sign.setLine(1, ChatColor.WHITE + player);
        sign.setLine(2, ChatColor.AQUA + formatTime(time));
        sign.setLine(3, ChatColor.RED + data.reason);
        sign.update();
    }

    private void clearSign(JailData data) {
        if (data.signLocation == null) return;
        if (!(data.signLocation.getBlock().getState() instanceof Sign sign)) return;

        sign.setLine(0, "");
        sign.setLine(1, ChatColor.GREEN + "[ Empty Cell ]");
        sign.setLine(2, "");
        sign.setLine(3, "");
        sign.update();
    }

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

    // ============================================================
    // 🚫 Item Pickup
    // ============================================================
    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent e) {
        if (isJailed(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(
                    plugin.getLanguageManager().getMessage("jail_item_pickup_blocked")
            );
        }
    }

    // ============================================================
    // 👤 Player Join
    // ============================================================
    public void handleJoin(Player player) {
        JailData data = jailedPlayers.get(player.getUniqueId());
        if (data == null || data.released) return;

        if (!data.onlineOnly && System.currentTimeMillis() >= data.releaseTime) {
            releasePlayerInternal(player, ReleaseCause.SENTENCE_COMPLETE, null, false);
            return;
        }

        if (!player.getWorld().equals(data.jailLocation.getWorld())
                || player.getLocation().distanceSquared(data.jailLocation) > 2) {
            player.teleport(data.jailLocation);
        }

        if (data.taskId == 0) {
            startCountdown(player.getUniqueId(), player.getName(), data);
        }

        player.sendMessage(
                plugin.getLanguageManager().getMessage("jail_still_serving")
        );
    }

    // ============================================================
    // 🚪 Player Quit
    // ============================================================
    public void handleQuit(Player player) {
        // no-op
    }
}
