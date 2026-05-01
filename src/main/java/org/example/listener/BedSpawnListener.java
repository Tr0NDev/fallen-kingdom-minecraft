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
import org.bukkit.event.player.PlayerInteractEvent;

public class BedSpawnListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    @EventHandler(priority = EventPriority.HIGHEST)
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

        event.setCancelled(true);

        Player player = event.getPlayer();
        Location bedLocation = block.getLocation();
        player.setBedSpawnLocation(bedLocation, true);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Your spawn point has been set to this bed.");
        Bukkit.getConsoleSender().sendMessage("[BedSpawn] Spawn set for " + player.getName() + " at " + bedLocation);
    }

    private boolean isBed(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.equals("BED") || name.equals("BED_BLOCK") || name.endsWith("_BED");
    }
}