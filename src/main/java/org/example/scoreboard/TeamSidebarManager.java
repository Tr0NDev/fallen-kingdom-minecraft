package org.example.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.example.settings.SettingsManager;
import org.example.team.TeamData;
import org.example.team.TeamManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TeamSidebarManager {

    private static final String HEALTH_OBJECTIVE_NAME = "tr0nfkhealth";
    private static final String CAPTURE_OBJECTIVE_NAME = "capturebar";

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final SettingsManager settingsManager;
    private BukkitTask task;
    private Scoreboard mainScoreboard;
    private Objective healthObjective;
    private final Map<UUID, Scoreboard> captureScoreboards = new HashMap<UUID, Scoreboard>();
    private final Set<UUID> sidebarOverrides = new HashSet<UUID>();

    public TeamSidebarManager(JavaPlugin plugin, TeamManager teamManager, SettingsManager settingsManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.settingsManager = settingsManager;
    }

    public void start() {
        stop();
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager != null) {
            mainScoreboard = scoreboardManager.getNewScoreboard();

            // Create teams initially
            updateTeams();
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                updateAllPlayers();
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clearCaptureProgressExcept(java.util.Collections.<UUID>emptySet());
    }

    public void refreshTeams() {
        updateTeams();
    }

    public Scoreboard getMainScoreboard() {
        return mainScoreboard;
    }

    public void setSidebarOverride(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }

        if (enabled) {
            sidebarOverrides.add(playerId);
        } else {
            sidebarOverrides.remove(playerId);
        }
    }

    public boolean isSidebarOverride(UUID playerId) {
        return playerId != null && sidebarOverrides.contains(playerId);
    }

    private void updateAllPlayers() {
        if (mainScoreboard == null) {
            return;
        }

        // Handle health objective
        if (settingsManager.isHealthBelowNameEnabled()) {
            if (healthObjective == null) {
                healthObjective = mainScoreboard.registerNewObjective(HEALTH_OBJECTIVE_NAME, "health");
                healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
                healthObjective.setDisplayName(ChatColor.RED + "❤");
            }
            // Update health scores for all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                healthObjective.getScore(player.getName()).setScore((int) player.getHealth());
            }
        } else {
            if (healthObjective != null) {
                healthObjective.unregister();
                healthObjective = null;
            }
        }

        // Update teams
        updateTeams();

        // Apply scoreboard to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (captureScoreboards.containsKey(player.getUniqueId()) || sidebarOverrides.contains(player.getUniqueId())) {
                continue;
            }
            player.setScoreboard(mainScoreboard);
        }

        // Update display names
        teamManager.updatePlayerDisplayNames();
    }

    private void updateTeams() {
        if (mainScoreboard == null) {
            return;
        }

        // Remove old teams
        for (org.bukkit.scoreboard.Team team : mainScoreboard.getTeams()) {
            team.unregister();
        }

        // Add teams
        for (Map.Entry<Integer, TeamData> entry : teamManager.getTeams().entrySet()) {
            TeamData teamData = entry.getValue();
            org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.registerNewTeam(teamData.name);
            // Only set color prefix if colored names are enabled
            if (settingsManager.isColoredNamesEnabled()) {
                scoreboardTeam.setPrefix(teamManager.getTeamChatColor(teamData.color) + "");
            }
            scoreboardTeam.setDisplayName(teamData.name);
            scoreboardTeam.setCanSeeFriendlyInvisibles(true);
            scoreboardTeam.setAllowFriendlyFire(settingsManager.isFriendlyFireEnabled());
            // Add players to team
            for (UUID playerId : teamData.members) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    scoreboardTeam.addEntry(player.getName());
                }
            }
        }
    }

    public void showCaptureProgress(Player capturer, TeamData defendingTeam, int percent) {
        if (capturer == null || !capturer.isOnline()) {
            return;
        }

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }

        Scoreboard board = captureScoreboards.get(capturer.getUniqueId());
        if (board == null) {
            board = scoreboardManager.getNewScoreboard();
            captureScoreboards.put(capturer.getUniqueId(), board);
        }

        rebuildCaptureBoard(board, defendingTeam, Math.max(0, Math.min(100, percent)));
        capturer.setScoreboard(board);
    }

    public void clearCaptureProgress(Player player) {
        if (player == null) {
            return;
        }

        captureScoreboards.remove(player.getUniqueId());
        if (mainScoreboard != null && player.isOnline()) {
            player.setScoreboard(mainScoreboard);
        }
    }

    public void clearCaptureProgressExcept(Set<UUID> keepPlayers) {
        java.util.Set<UUID> toRemove = new java.util.HashSet<UUID>(captureScoreboards.keySet());
        for (UUID playerId : toRemove) {
            if (keepPlayers != null && keepPlayers.contains(playerId)) {
                continue;
            }

            captureScoreboards.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && mainScoreboard != null) {
                player.setScoreboard(mainScoreboard);
            }
        }
    }

    private void rebuildCaptureBoard(Scoreboard board, TeamData defendingTeam, int percent) {
        if (board == null) {
            return;
        }

        // Reset teams and recreate from team data for colored names + tab consistency.
        for (org.bukkit.scoreboard.Team oldTeam : board.getTeams()) {
            oldTeam.unregister();
        }
        for (Map.Entry<Integer, TeamData> entry : teamManager.getTeams().entrySet()) {
            TeamData teamData = entry.getValue();
            String teamKey = "t" + entry.getKey();
            org.bukkit.scoreboard.Team scoreboardTeam = board.registerNewTeam(teamKey);
            if (settingsManager.isColoredNamesEnabled()) {
                scoreboardTeam.setPrefix(teamManager.getTeamChatColor(teamData.color) + "");
            }
            scoreboardTeam.setDisplayName(teamData.name);
            scoreboardTeam.setCanSeeFriendlyInvisibles(true);
            scoreboardTeam.setAllowFriendlyFire(settingsManager.isFriendlyFireEnabled());
            for (UUID playerId : teamData.members) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    scoreboardTeam.addEntry(player.getName());
                }
            }
        }

        Objective oldCaptureObjective = board.getObjective(CAPTURE_OBJECTIVE_NAME);
        if (oldCaptureObjective != null) {
            oldCaptureObjective.unregister();
        }

        Objective captureObjective = board.registerNewObjective(CAPTURE_OBJECTIVE_NAME, "dummy");
        captureObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        captureObjective.setDisplayName(ChatColor.RED + "Base Capture");

        String percentLine = ChatColor.GOLD + "Cap: " + ChatColor.YELLOW + percent + "%";
        captureObjective.getScore(percentLine).setScore(3);

        int filled = Math.max(0, Math.min(5, (int) Math.round((percent / 100.0D) * 5.0D)));
        StringBuilder bar = new StringBuilder();
        for (int index = 0; index < filled; index++) {
            bar.append("|");
        }
        StringBuilder empty = new StringBuilder();
        for (int index = filled; index < 5; index++) {
            empty.append("|");
        }
        String barLine = ChatColor.GREEN + bar.toString() + ChatColor.DARK_GRAY + empty.toString();
        captureObjective.getScore(barLine).setScore(2);

        String defenderName = defendingTeam == null || defendingTeam.name == null ? "Unknown" : defendingTeam.name;
        if (defenderName.length() > 12) {
            defenderName = defenderName.substring(0, 12);
        }
        String teamLine = ChatColor.RED + defenderName;
        captureObjective.getScore(teamLine).setScore(1);

        if (settingsManager.isHealthBelowNameEnabled()) {
            Objective oldHealthObjective = board.getObjective(HEALTH_OBJECTIVE_NAME);
            if (oldHealthObjective != null) {
                oldHealthObjective.unregister();
            }
            Objective boardHealthObjective = board.registerNewObjective(HEALTH_OBJECTIVE_NAME, "health");
            boardHealthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            boardHealthObjective.setDisplayName(ChatColor.RED + "❤");
            for (Player player : Bukkit.getOnlinePlayers()) {
                boardHealthObjective.getScore(player.getName()).setScore((int) player.getHealth());
            }
        }
    }
}
