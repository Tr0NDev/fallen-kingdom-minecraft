package org.example.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.example.game.GameStateManager;
import org.example.settings.SettingsManager;
import org.example.storage.PluginDataStore;
import org.example.team.TeamData;
import org.example.team.TeamManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStateListener implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final int NO_BASE = -1;
    private final TeamManager teamManager;
    private final SettingsManager settingsManager;
    private final GameStateManager gameStateManager;
    private final PluginDataStore dataStore;
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> insideBaseState = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> insideChestRoomState = new ConcurrentHashMap<UUID, Integer>();

    public PlayerStateListener(JavaPlugin plugin, TeamManager teamManager, SettingsManager settingsManager, GameStateManager gameStateManager, PluginDataStore dataStore) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.settingsManager = settingsManager;
        this.gameStateManager = gameStateManager;
        this.dataStore = dataStore;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode mode = event.getNewGameMode();
        if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR && mode != GameMode.SURVIVAL) {
            return;
        }

        String modeText;
        if (mode == GameMode.CREATIVE) {
            modeText = "creative";
        } else if (mode == GameMode.SPECTATOR) {
            modeText = "spectator";
        } else {
            modeText = "survival";
        }

        Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + event.getPlayer().getName() + " has switched to " + modeText + " mode.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(ChatColor.GREEN + "[+] " + ChatColor.AQUA + event.getPlayer().getName());
        UUID playerId = event.getPlayer().getUniqueId();
        restoreLastKnownLocation(event.getPlayer());
        Location location = event.getPlayer().getLocation();
        insideBaseState.put(playerId, getBaseTeamIdAtLocation(location));
        insideChestRoomState.put(playerId, getChestRoomTeamIdAtLocation(location));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.RED + "[-] " + ChatColor.AQUA + event.getPlayer().getName());
        if (dataStore != null) {
            dataStore.setPlayerLastLocation(event.getPlayer().getUniqueId(), event.getPlayer().getLocation());
        }
        insideBaseState.remove(event.getPlayer().getUniqueId());
        insideChestRoomState.remove(event.getPlayer().getUniqueId());

        if (settingsManager.isAutoPauseOnQuitEnabled()
                && gameStateManager != null
                && gameStateManager.isRunning()
                && !gameStateManager.isPaused()) {
            gameStateManager.pause();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String victim = ChatColor.AQUA + event.getEntity().getName();
        Player killer = event.getEntity().getKiller();

        if (killer != null) {
            event.setDeathMessage(ChatColor.DARK_RED + "[x] " + victim + ChatColor.GRAY + " was eliminated by "
                    + ChatColor.RED + "(" + killer.getName() + ")" + ChatColor.GRAY + ".");
            return;
        }

        event.setDeathMessage(ChatColor.DARK_RED + "[x] " + victim + ChatColor.GRAY + " died.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getWorld() != null
            && event.getTo().getWorld() != null
            && event.getFrom().getWorld().equals(event.getTo().getWorld())
            && event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        updateBaseState(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getPlayer() == null) {
            return;
        }

        updateBaseState(event.getPlayer(), event.getTo());
    }

    private void updateBaseState(Player player, Location location) {
        if (!settingsManager.isBaseMessagesEnabled() && !settingsManager.isChestRoomMessagesEnabled()) {
            insideBaseState.remove(player.getUniqueId());
            insideChestRoomState.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        Integer previousBaseId = insideBaseState.get(player.getUniqueId());
        int currentBaseId = getBaseTeamIdAtLocation(location);
        Integer previousChestRoomId = insideChestRoomState.get(playerId);
        int currentChestRoomId = getChestRoomTeamIdAtLocation(location);

        if (previousBaseId == null) {
            insideBaseState.put(playerId, NO_BASE);
            previousBaseId = Integer.valueOf(NO_BASE);
        }

        if (previousChestRoomId == null) {
            insideChestRoomState.put(playerId, NO_BASE);
            previousChestRoomId = Integer.valueOf(NO_BASE);
        }

        boolean baseChanged = previousBaseId.intValue() != currentBaseId;
        boolean chestChanged = previousChestRoomId.intValue() != currentChestRoomId;

        if (!baseChanged && !chestChanged) {
            return;
        }

        insideBaseState.put(playerId, currentBaseId);
        insideChestRoomState.put(playerId, currentChestRoomId);

        if (chestChanged) {
            if (!settingsManager.isChestRoomMessagesEnabled()) {
                insideChestRoomState.put(playerId, currentChestRoomId);
            } else {
                if (previousChestRoomId.intValue() != NO_BASE) {
                    TeamData previousChestTeam = teamManager.getTeams().get(previousChestRoomId);
                    if (previousChestTeam != null) {
                        notifyChestRoomTransition(player, previousChestTeam, false);
                    }
                }

                if (currentChestRoomId != NO_BASE) {
                    TeamData currentChestTeam = teamManager.getTeams().get(currentChestRoomId);
                    if (currentChestTeam != null) {
                        notifyChestRoomTransition(player, currentChestTeam, true);
                    }
                }
            }

            return;
        }

        if (!settingsManager.isBaseMessagesEnabled()) {
            insideBaseState.put(playerId, currentBaseId);
            return;
        }

        if (previousBaseId.intValue() != NO_BASE) {
            TeamData previousBaseTeam = teamManager.getTeams().get(previousBaseId);
            if (previousBaseTeam != null) {
                notifyBaseTransition(player, previousBaseTeam, false);
            }
        }

        if (currentBaseId != NO_BASE) {
            TeamData currentBaseTeam = teamManager.getTeams().get(currentBaseId);
            if (currentBaseTeam != null) {
                notifyBaseTransition(player, currentBaseTeam, true);
            }
        }
    }

    private int getBaseTeamIdAtLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return NO_BASE;
        }

        TeamData closestBase = null;
        double closestNormalizedDistance = Double.MAX_VALUE;

        for (TeamData team : teamManager.getTeams().values()) {
            if (!isInsideBaseRadius(location, team)) {
                continue;
            }

            double deltaX = Math.abs(location.getBlockX() - team.baseCenter.getBlockX());
            double deltaZ = Math.abs(location.getBlockZ() - team.baseCenter.getBlockZ());
            double normalizedDistance = Math.max(deltaX, deltaZ) / Math.max(1.0D, team.baseRadius);

            if (normalizedDistance < closestNormalizedDistance) {
                closestNormalizedDistance = normalizedDistance;
                closestBase = team;
            }
        }

        return closestBase == null ? NO_BASE : closestBase.id;
    }

    private int getChestRoomTeamIdAtLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return NO_BASE;
        }

        TeamData closestChestRoom = null;
        double closestCenterDistance = Double.MAX_VALUE;

        for (TeamData team : teamManager.getTeams().values()) {
            if (!isInsideChestRoom(location, team)) {
                continue;
            }

            double deltaX = location.getX() - team.baseCenter.getX();
            double deltaZ = location.getZ() - team.baseCenter.getZ();
            double centerDistance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));

            if (centerDistance < closestCenterDistance) {
                closestCenterDistance = centerDistance;
                closestChestRoom = team;
            }
        }

        return closestChestRoom == null ? NO_BASE : closestChestRoom.id;
    }

    private boolean isInsideBaseRadius(Location location, TeamData team) {
        if (team == null || team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
            return false;
        }

        if (!location.getWorld().equals(team.baseCenter.getWorld())) {
            return false;
        }

        double deltaX = Math.abs(location.getBlockX() - team.baseCenter.getBlockX());
        double deltaZ = Math.abs(location.getBlockZ() - team.baseCenter.getBlockZ());
        return deltaX <= team.baseRadius && deltaZ <= team.baseRadius;
    }

    private boolean isInsideChestRoom(Location location, TeamData team) {
        if (team == null) {
            return false;
        }

        TeamManager.CaptureBounds bounds = teamManager.getCaptureBounds(team);
        if (bounds == null) {
            return false;
        }

        return teamManager.isInsideCaptureBounds(location, bounds);
    }

    private void notifyBaseTransition(Player player, TeamData team, boolean entering) {
        ChatColor teamColor = teamManager.getTeamChatColor(team.color);
        String baseName = teamColor + team.name;
        String message = entering
                ? ChatColor.GREEN + "You entered " + baseName + ChatColor.GREEN + " base."
                : ChatColor.RED + "You left " + baseName + ChatColor.RED + " base.";
        player.sendMessage(PREFIX + message);
    }

    private void notifyChestRoomTransition(Player player, TeamData team, boolean entering) {
        TeamData playerTeam = teamManager.getTeamByPlayer(player.getUniqueId());
        boolean ownChestRoom = playerTeam != null && playerTeam.id == team.id;
        ChatColor teamColor = teamManager.getTeamChatColor(team.color);
        String chestName = teamColor + team.name;

        if (entering && !ownChestRoom) {
            sendCaptureHint(player, team, "chest room");
        }

        String publicMessage = entering
                ? ChatColor.GREEN + player.getName() + " entered " + chestName + ChatColor.GREEN + " chest room."
                : ChatColor.RED + player.getName() + " left " + chestName + ChatColor.RED + " chest room.";
        Bukkit.broadcastMessage(PREFIX + publicMessage);
    }

    private boolean isOwnTeam(Player player, TeamData team) {
        TeamData playerTeam = teamManager.getTeamByPlayer(player.getUniqueId());
        return playerTeam != null && playerTeam.id == team.id;
    }

    private void sendCaptureHint(Player player, TeamData defendingTeam, String locationName) {
        if (!settingsManager.isTntCapEnabled()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Assault cap is not active yet. You cannot capture this " + locationName + ".");
            return;
        }

        int required = Math.max(1, settingsManager.getBaseCaptureRequiredPlayers());
        player.sendMessage(PREFIX + ChatColor.YELLOW + "To capture this " + locationName + ", you need "
                + ChatColor.GOLD + required + ChatColor.YELLOW + " player" + (required == 1 ? "" : "s") + " from your team inside the zone.");
    }

    private void restoreLastKnownLocation(final Player player) {
        if (player == null || dataStore == null) {
            return;
        }

        Location last = dataStore.getPlayerLastLocation(player.getUniqueId());
        if (last == null) {
            return;
        }

        final Location target = last.clone();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.teleport(target);
                }
            }
        }, 2L);
    }
}
