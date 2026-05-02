package org.example.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.example.game.GameStateManager;

public class PauseProtectionListener implements Listener {

    private final GameStateManager gameStateManager;

    public PauseProtectionListener(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameStateManager.isPaused() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (gameStateManager.isPaused() && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!gameStateManager.isPaused()) return;
        if (event.getPlayer().isOp()) return;

        if (event.getClickedBlock() != null) {
            String name = event.getClickedBlock().getType().name();
            if (name.equals("BED") || name.equals("BED_BLOCK") || name.endsWith("_BED")) {
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (gameStateManager.isPaused() && event.getEntity() instanceof Player && !((Player) event.getEntity()).isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (gameStateManager.isPaused()) {
            if (event.getDamager() instanceof Player && !((Player) event.getDamager()).isOp()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameStateManager.isPaused()) {
            Player player = event.getPlayer();
            if (player.isOp()) {
                return;
            }
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                event.setCancelled(true);
            }
        }
    }
}