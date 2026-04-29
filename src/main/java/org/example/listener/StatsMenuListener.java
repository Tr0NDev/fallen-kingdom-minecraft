package org.example.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.command.FkSettingsCommand;

public class StatsMenuListener implements Listener {
    private final FkSettingsCommand settingsCommand;

    public StatsMenuListener(FkSettingsCommand settingsCommand, JavaPlugin plugin) {
        this.settingsCommand = settingsCommand;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals(FkSettingsCommand.GLOBAL_STATS_TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals(FkSettingsCommand.GLOBAL_STATS_TITLE)) {
            event.setCancelled(true);
        }
    }
}