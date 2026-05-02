package org.example.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.example.storage.PluginDataStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedSpawnListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    private final PluginDataStore dataStore;
    private final Map<UUID, Location> bedSpawnLocations = new HashMap<UUID, Location>();

    public BedSpawnListener(PluginDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        if (!isBed(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        Location bedLocation = block.getLocation();
        player.setBedSpawnLocation(bedLocation, true);
        bedSpawnLocations.put(player.getUniqueId(), bedLocation.clone());
        player.sendMessage(PREFIX + ChatColor.GREEN + "Your spawn point has been set to this bed.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isBed(block.getType())) {
            return;
        }

        Location brokenLocation = block.getLocation();

        for (Player online : Bukkit.getOnlinePlayers()) {
            Location saved = bedSpawnLocations.get(online.getUniqueId());
            if (saved == null) {
                continue;
            }

            if (!isSameBedLocation(saved, brokenLocation)) {
                continue;
            }

            bedSpawnLocations.remove(online.getUniqueId());

            Location fallback = dataStore != null ? dataStore.getGameStartLocation() : null;
            if (fallback != null && fallback.getWorld() != null) {
                online.setBedSpawnLocation(fallback, true);
                online.sendMessage(PREFIX + ChatColor.RED + "Your bed was destroyed. ");
            } else {
                online.setBedSpawnLocation(null, true);
                online.sendMessage(PREFIX + ChatColor.RED + "Your bed was destroyed. ");
            }
        }
    }

    private boolean isSameBedLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private boolean isBed(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.equals("BED") || name.equals("BED_BLOCK") || name.endsWith("_BED");
    }
}