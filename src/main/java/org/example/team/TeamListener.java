package org.example.team;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TeamListener implements Listener {

    private final TeamManager teamManager;

    public TeamListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        teamManager.handleChat(event);
        if (event.isCancelled()) {
            return;
        }
        teamManager.formatChat(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        teamManager.handleMenuClick(event);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        teamManager.handleBlockPlace(event);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        teamManager.handleBlockBreak(event);
    }
}
