package org.example.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class FlyGuiListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8Fly Speed");

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getName() == null || !event.getInventory().getName().equals(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int speedMultiplier = 0;

        if (clicked.getType() == Material.LEATHER_BOOTS) {
            speedMultiplier = 1;
        } else if (clicked.getType() == Material.CHAINMAIL_BOOTS) {
            speedMultiplier = 2;
        } else if (clicked.getType() == Material.IRON_BOOTS) {
            speedMultiplier = 3;
        } else if (clicked.getType() == Material.DIAMOND_BOOTS) {
            speedMultiplier = 5;
        }

        if (speedMultiplier > 0) {
            setFlySpeed(player, speedMultiplier);
            player.closeInventory();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Fly speed set to " + ChatColor.YELLOW + speedMultiplier + "x" + ChatColor.GREEN + ".");
        }
    }

    private void setFlySpeed(Player player, int speedMultiplier) {
        float SPEED_INCREMENT = 0.1f;
        float MAX_SPEED = 1.0f;
        float speed = Math.min(SPEED_INCREMENT * speedMultiplier, MAX_SPEED);
        player.setFlySpeed(speed);
        player.setAllowFlight(true);
        player.setFlying(true);
    }
}
