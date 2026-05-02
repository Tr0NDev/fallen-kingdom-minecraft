package org.example.scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
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
import org.example.game.GameStateManager;

public class CapSidebarManager {

    private static final String OBJECTIVE_NAME = "fkcapside";
    private static final String HEALTH_OBJECTIVE_NAME = "fkhealth";

    private final JavaPlugin plugin;
    private final SettingsManager settingsManager;
    private final TeamManager teamManager;
    private final TeamSidebarManager teamSidebarManager;
    private final GameStateManager gameStateManager;
    private final Set<UUID> enabledViewers = new HashSet<UUID>();
    private final Map<UUID, Scoreboard> scoreboards = new HashMap<UUID, Scoreboard>();
    private BukkitTask task;

    public CapSidebarManager(JavaPlugin plugin, SettingsManager settingsManager, TeamManager teamManager, TeamSidebarManager teamSidebarManager, GameStateManager gameStateManager) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.teamManager = teamManager;
        this.teamSidebarManager = teamSidebarManager;
        this.gameStateManager = gameStateManager;
    }

    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }

        boolean enabled = !enabledViewers.contains(player.getUniqueId());
        setEnabled(player, enabled);
        return enabled;
    }

    public void setEnabled(Player player, boolean enabled) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (enabled) {
            enabledViewers.add(playerId);
            if (teamSidebarManager != null) {
                teamSidebarManager.setSidebarOverride(playerId, true);
            }
            updatePlayerBoard(player);
            return;
        }

        enabledViewers.remove(playerId);
        scoreboards.remove(playerId);
        if (teamSidebarManager != null) {
            teamSidebarManager.setSidebarOverride(playerId, false);
            Scoreboard mainScoreboard = teamSidebarManager.getMainScoreboard();
            if (mainScoreboard != null && player.isOnline()) {
                player.setScoreboard(mainScoreboard);
            }
        }
    }

    public boolean isEnabled(UUID playerId) {
        return playerId != null && enabledViewers.contains(playerId);
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                updateAll();
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID viewerId : new HashSet<UUID>(enabledViewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player != null) {
                setEnabled(player, false);
            }
        }
        enabledViewers.clear();
        scoreboards.clear();
    }

    private void updateAll() {
        for (UUID viewerId : new HashSet<UUID>(enabledViewers)) {
            Player player = Bukkit.getPlayer(viewerId);
            if (player == null || !player.isOnline()) {
                enabledViewers.remove(viewerId);
                scoreboards.remove(viewerId);
                if (teamSidebarManager != null) {
                    teamSidebarManager.setSidebarOverride(viewerId, false);
                }
                continue;
            }

            updatePlayerBoard(player);
        }
    }

    private void updatePlayerBoard(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            return;
        }

        Scoreboard board = scoreboards.get(player.getUniqueId());
        if (board == null) {
            board = scoreboardManager.getNewScoreboard();
            scoreboards.put(player.getUniqueId(), board);
        }

        rebuildBoard(board, player);
        player.setScoreboard(board);
    }

    private void rebuildBoard(Scoreboard board, Player viewer) {
        if (board == null || viewer == null) {
            return;
        }

        for (org.bukkit.scoreboard.Team oldTeam : board.getTeams()) {
            oldTeam.unregister();
        }

        for (Map.Entry<Integer, TeamData> entry : teamManager.getTeams().entrySet()) {
            TeamData teamData = entry.getValue();
            org.bukkit.scoreboard.Team scoreboardTeam = board.registerNewTeam("cap_t" + entry.getKey());
            if (settingsManager.isColoredNamesEnabled()) {
                scoreboardTeam.setPrefix(teamManager.getTeamChatColor(teamData.color) + "");
            }
            scoreboardTeam.setDisplayName(teamData.name);
            scoreboardTeam.setCanSeeFriendlyInvisibles(true);
            scoreboardTeam.setAllowFriendlyFire(settingsManager.isFriendlyFireEnabled());
            for (UUID playerId : teamData.members) {
                Player member = Bukkit.getPlayer(playerId);
                if (member != null && member.isOnline()) {
                    scoreboardTeam.addEntry(member.getName());
                }
            }
        }

        Objective health = board.getObjective(HEALTH_OBJECTIVE_NAME);
        if (health != null) {
            health.unregister();
        }

        if (settingsManager.isHealthBelowNameEnabled()) {
            health = board.registerNewObjective(HEALTH_OBJECTIVE_NAME, "health");
            health.setDisplaySlot(DisplaySlot.BELOW_NAME);
            health.setDisplayName(ChatColor.RED + "❤");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != null && online.isOnline()) {
                    health.getScore(online.getName()).setScore((int) online.getHealth());
                }
            }
        }

        Objective old = board.getObjective(OBJECTIVE_NAME);
        if (old != null) {
            old.unregister();
        }

        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.RED + "Caps");

        int currentDay = settingsManager.getCurrentDay();
        List<String> lines = new ArrayList<String>();
        lines.add(makeDayLine("Day", currentDay));

        String nextDay;
        if (gameStateManager != null && gameStateManager.isRunning()) {
            nextDay = gameStateManager.getFormattedTimeUntilNextDay();
        } else {
            nextDay = "00:20:00";
        }
        lines.add(ChatColor.GRAY + "Next day: " + ChatColor.AQUA + nextDay);

        lines.add(makeDayLine("Nether cap", settingsManager.getNetherCapDay(), currentDay));
        lines.add(makeDayLine("End cap", settingsManager.getEndCapDay(), currentDay));
        lines.add(makeDayLine("PvP cap", settingsManager.getPvpCapDay(), currentDay));
        lines.add(makeDayLine("Assault cap", settingsManager.getTntCapDay(), currentDay));

        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
    }

    private String makeDayLine(String label, int capDay, int currentDay) {
        if (capDay <= 0) {
            return ChatColor.GRAY + label + ": " + ChatColor.GREEN + "\u2713" + ChatColor.RESET + ChatColor.GREEN;
        }

        if (currentDay >= capDay) {
            return ChatColor.GRAY + label + ": " + ChatColor.GREEN + "\u2713" + ChatColor.RESET + ChatColor.GREEN;
        }

        return ChatColor.GRAY + label + ": " + ChatColor.RED + capDay;
    }

    private String makeDayLine(String label, int currentDay) {
        return ChatColor.GRAY + label + ": " + ChatColor.YELLOW + currentDay;
    }
}