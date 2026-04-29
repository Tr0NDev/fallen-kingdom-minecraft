package org.example.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.example.storage.PluginDataStore;

public class WorldDifficultyListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    private final PluginDataStore dataStore;

    public WorldDifficultyListener(PluginDataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }

        String[] parts = event.getMessage().trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String command = stripSlash(parts[0]);
        if (!isDifficultyCommand(command)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        handleDifficultyChange(event.getPlayer(), parts, event, true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (event == null || event.getCommand() == null) {
            return;
        }

        String[] parts = event.getCommand().trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String command = stripSlash(parts[0]);
        if (!isDifficultyCommand(command)) {
            return;
        }

        handleDifficultyChange(event.getSender(), parts, event, false);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (dataStore == null || event == null || event.getWorld() == null) {
            return;
        }

        Difficulty difficulty = dataStore.getWorldDifficulty(event.getWorld().getName());
        if (difficulty == null) {
            return;
        }

        try {
            event.getWorld().setDifficulty(difficulty);
        } catch (Exception ignored) {
        }
    }

    private boolean handleDifficultyChange(CommandSender sender, String[] parts, Object cancellableEvent, boolean playerCommand) {
        if (sender == null || parts == null || parts.length < 2) {
            sendUsage(sender);
            cancel(cancellableEvent);
            return true;
        }

        Difficulty difficulty = parseDifficulty(parts[1]);
        if (difficulty == null) {
            sendUsage(sender);
            cancel(cancellableEvent);
            return true;
        }

        World world = resolveTargetWorld(sender);
        if (world == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No world available to apply the difficulty.");
            cancel(cancellableEvent);
            return true;
        }

        if (dataStore != null) {
            dataStore.setWorldDifficulty(world, difficulty);
        } else {
            world.setDifficulty(difficulty);
        }

        sender.sendMessage(PREFIX + ChatColor.GREEN + "Difficulty for " + ChatColor.YELLOW + world.getName()
                + ChatColor.GREEN + " set to " + ChatColor.YELLOW + difficulty.name().toLowerCase() + ChatColor.GREEN + ".");
        cancel(cancellableEvent);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        if (sender != null) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: /difficulty <peaceful|easy|normal|hard>");
        }
    }

    private void cancel(Object cancellableEvent) {
        if (cancellableEvent instanceof PlayerCommandPreprocessEvent) {
            ((PlayerCommandPreprocessEvent) cancellableEvent).setCancelled(true);
        } else if (cancellableEvent instanceof ServerCommandEvent) {
            ((ServerCommandEvent) cancellableEvent).setCancelled(true);
        }
    }

    private Difficulty parseDifficulty(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim().toLowerCase();
        if ("0".equals(normalized) || "peaceful".equals(normalized) || "p".equals(normalized)) {
            return Difficulty.PEACEFUL;
        }
        if ("1".equals(normalized) || "easy".equals(normalized) || "e".equals(normalized)) {
            return Difficulty.EASY;
        }
        if ("2".equals(normalized) || "normal".equals(normalized) || "n".equals(normalized)) {
            return Difficulty.NORMAL;
        }
        if ("3".equals(normalized) || "hard".equals(normalized) || "h".equals(normalized)) {
            return Difficulty.HARD;
        }

        try {
            return Difficulty.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private World resolveTargetWorld(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }

        if (dataStore != null) {
            Location startLocation = dataStore.getGameStartLocation();
            if (startLocation != null && startLocation.getWorld() != null) {
                return startLocation.getWorld();
            }

            Location switchLocation = dataStore.getMapSwitchLocation();
            if (switchLocation != null && switchLocation.getWorld() != null) {
                return switchLocation.getWorld();
            }
        }

        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0);
        }

        return null;
    }

    private boolean isDifficultyCommand(String command) {
        return "difficulty".equalsIgnoreCase(command) || "diff".equalsIgnoreCase(command);
    }

    private String stripSlash(String command) {
        if (command == null) {
            return "";
        }

        return command.startsWith("/") ? command.substring(1) : command;
    }
}