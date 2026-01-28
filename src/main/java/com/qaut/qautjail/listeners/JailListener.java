package com.qaut.qautjail.listeners;

import com.qaut.qautjail.QauTJail;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class JailListener implements Listener {

    private final QauTJail plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public JailListener(QauTJail plugin) {
        this.plugin = plugin;
    }

    // ----------------------------
    // 🔹 ربط اللوحة بالسجن
    // ----------------------------
    @EventHandler
    public void onSignRightClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String pendingJail = plugin.getJailManager().getPendingSign(uuid);
        if (pendingJail != null) {
            File jailFile = new File(plugin.getDataFolder(), "Jails/" + pendingJail + ".yml");
            YamlConfiguration jailConfig = YamlConfiguration.loadConfiguration(jailFile);

            // ✅ منع ربط أكثر من ساين بنفس السجن
            if (jailConfig.contains("sign.world")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("sign_already_linked"));
                plugin.getJailManager().clearPendingSign(uuid);
                event.setCancelled(true);
                return;
            }

            jailConfig.set("sign.world", sign.getWorld().getName());
            jailConfig.set("sign.x", sign.getX());
            jailConfig.set("sign.y", sign.getY());
            jailConfig.set("sign.z", sign.getZ());

            try {
                jailConfig.save(jailFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            sign.setLine(0, "");
            sign.setLine(1, ChatColor.DARK_RED + "" + ChatColor.BOLD + "Not Jail");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();

            player.sendMessage(plugin.getLanguageManager().getMessage("sign_linked"));
            plugin.getJailManager().clearPendingSign(uuid);
            event.setCancelled(true);
        }
    }

    // ----------------------------
    // 🔹 منع كسر أو تعديل الساين إلا إذا انكسر البلوك الأساسي
    // ----------------------------
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // منع المسجونين من كسر أي شيء
        if (plugin.getJailManager().isJailed(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage("jail_block_break"));
            return;
        }

        // تحقق إن البلوك ساين أو البلوك اللي تحت الساين انكسر
        File jailsFolder = new File(plugin.getDataFolder(), "Jails");
        if (!jailsFolder.exists()) return;

        for (File f : Objects.requireNonNull(jailsFolder.listFiles())) {
            YamlConfiguration jc = YamlConfiguration.loadConfiguration(f);
            if (jc.contains("sign.world")) {
                String w = jc.getString("sign.world");
                int x = jc.getInt("sign.x");
                int y = jc.getInt("sign.y");
                int z = jc.getInt("sign.z");

                if (!block.getWorld().getName().equals(w)) continue;

                // ✅ اللوحة نفسها
                if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getMessage("sign_break_blocked"));
                    return;
                }

                // ✅ البلوك اللي الساين راكبة عليه
                Block signBlock = block.getWorld().getBlockAt(x, y, z);
                if (signBlock.getState() instanceof Sign sign) {
                    Block attachedBlock = getAttachedBlock(sign);
                    if (attachedBlock != null && attachedBlock.equals(block)) {
                        // انكسر البلوك الأساسي → نحذف الساين من النظام
                        jc.set("sign", null);
                        try {
                            jc.save(f);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        player.sendMessage(plugin.getLanguageManager().getMessage("sign_removed"));
                        return;
                    }
                }
            }
        }
    }

    private Block getAttachedBlock(Sign sign) {
        // يحدد البلوك اللي الساين راكبة عليه
        Block block = sign.getBlock();
        switch (sign.getBlockData().toString()) {
            case "minecraft:oak_wall_sign":
            case "minecraft:birch_wall_sign":
            case "minecraft:spruce_wall_sign":
            case "minecraft:jungle_wall_sign":
            case "minecraft:acacia_wall_sign":
            case "minecraft:dark_oak_wall_sign":
            case "minecraft:crimson_wall_sign":
            case "minecraft:warped_wall_sign":
                return block.getRelative(((org.bukkit.block.data.type.WallSign) sign.getBlockData()).getFacing().getOppositeFace());
            default:
                return block.getRelative(org.bukkit.block.BlockFace.DOWN);
        }
    }
    // ----------------------------
    // 🔹 منع المسجون من التقاط الأشياء من الأرض
    // ----------------------------
    @EventHandler
    public void onItemPickup(org.bukkit.event.player.PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();

        // ✅ إذا اللاعب مسجون → نمنعه من التقاط أي شيء
        if (plugin.getJailManager().isJailed(player.getUniqueId())) {
            event.setCancelled(true);

            // فقط رسالة بسيطة تنبيهية (كل 3 ثواني مثلاً حتى لا تزعج)
            long now = System.currentTimeMillis();
            if (!cooldowns.containsKey(player.getUniqueId()) || now - cooldowns.get(player.getUniqueId()) > 3000) {
                player.sendMessage(plugin.getLanguageManager().getMessage("jail_item_pickup_blocked"));
                cooldowns.put(player.getUniqueId(), now);
            }
        }
    }

    // ----------------------------
    // 🔹 باقي الأحداث بدون تعديل
    // ----------------------------
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Sign sign = (Sign) event.getBlock().getState();
        File jailsFolder = new File(plugin.getDataFolder(), "Jails");
        if (!jailsFolder.exists()) return;

        for (File f : Objects.requireNonNull(jailsFolder.listFiles())) {
            YamlConfiguration jc = YamlConfiguration.loadConfiguration(f);
            if (jc.contains("sign.world")) {
                String w = jc.getString("sign.world");
                int x = jc.getInt("sign.x");
                int y = jc.getInt("sign.y");
                int z = jc.getInt("sign.z");

                if (sign.getWorld().getName().equals(w)
                        && sign.getX() == x && sign.getY() == y && sign.getZ() == z) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("sign_edit_blocked"));
                    return;
                }
            }
        }
    }



    @EventHandler(ignoreCancelled = true)
    public void onSignEdit(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        Player player = event.getPlayer();

        File jailsFolder = new File(plugin.getDataFolder(), "Jails");
        if (!jailsFolder.exists()) return;

        for (File file : Objects.requireNonNull(jailsFolder.listFiles())) {
            if (!file.getName().endsWith(".yml")) continue;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (!config.contains("sign.world")) continue;
            String world = config.getString("sign.world");
            int x = config.getInt("sign.x");
            int y = config.getInt("sign.y");
            int z = config.getInt("sign.z");

            if (world == null) continue;

            Location signLoc = new Location(Bukkit.getWorld(world), x, y, z);

            if (signLoc.getBlock().equals(event.getClickedBlock())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getLanguageManager().getMessage("sign_edit_blocked"));
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getJailManager().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getJailManager().handleQuit(event.getPlayer());
    }
}
