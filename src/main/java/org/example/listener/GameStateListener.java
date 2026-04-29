package org.example.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.example.game.GameStateManager;

public class GameStateListener implements Listener {

    private final GameStateManager gameStateManager;

    public GameStateListener(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!gameStateManager.isPaused()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getX() == event.getTo().getX()
                && event.getFrom().getY() == event.getTo().getY()
                && event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        event.setTo(event.getFrom());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!gameStateManager.isPaused()) {
            return;
        }

        if (event.getPlayer().isOp()) {
            return;
        }

        event.getPlayer().addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.BLINDNESS,
                Integer.MAX_VALUE,
                1,
                false,
                false), true);
    }
}
