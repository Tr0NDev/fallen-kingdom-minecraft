package org.example.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.example.command.FkSettingsCommand;
import org.example.koth.KothManager;
import org.example.scoreboard.TeamSidebarManager;
import org.example.settings.SettingsManager;
import org.example.storage.PluginDataStore;
import org.example.stats.PlayerStatsManager;
import org.example.team.TeamData;
import org.example.team.TeamManager;
import org.example.util.LegacyBossBarUtil;

public class GameStateManager {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final long DAY_DURATION_MILLIS = 20L * 60L * 1000L;
    private static final Runnable NO_OP = () -> { };

    private final JavaPlugin plugin;
    private final SettingsManager settingsManager;
    private final FkSettingsCommand settingsCommand;
    private final TeamManager teamManager;
    private final KothManager kothManager;
    private PluginDataStore dataStore;
    private TeamSidebarManager teamSidebarManager;
    private BukkitTask dayCycleTask;
    private BukkitTask hudTask;
    private boolean running;
    private boolean paused;
    private long matchStartMillis;
    private long pausedAccumulatedMillis;
    private long pauseStartedMillis;
    private long nextDayAtMillis;
    private long persistedPlayedMillis;
    private final Map<Integer, BaseCaptureProgress> baseCaptureProgress = new ConcurrentHashMap<Integer, BaseCaptureProgress>();
    private PlayerStatsManager playerStatsManager;
    private Runnable saveAction = NO_OP;

    private static class BaseCaptureProgress {
        private final int attackingTeamId;
        private final long startedAtMillis;

        private BaseCaptureProgress(int attackingTeamId, long startedAtMillis) {
            this.attackingTeamId = attackingTeamId;
            this.startedAtMillis = startedAtMillis;
        }
    }

    public GameStateManager(JavaPlugin plugin, SettingsManager settingsManager, FkSettingsCommand settingsCommand,
            TeamManager teamManager, KothManager kothManager) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.settingsCommand = settingsCommand;
        this.teamManager = teamManager;
        this.kothManager = kothManager;
    }

    public void setDataStore(PluginDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getPersistedState() {
        if (!running) {
            return "stopped";
        }
        return paused ? "paused" : "running";
    }

    public void restorePersistedState(String state) {
        if ("running".equalsIgnoreCase(state)) {
            running = true;
            paused = false;
            resetMatchTiming(persistedPlayedMillis);
            removePauseEffects();
            ensureDayCycleTask();
            return;
        }

        if ("paused".equalsIgnoreCase(state)) {
            running = true;
            paused = true;
            resetMatchTiming(persistedPlayedMillis);
            pauseStartedMillis = System.currentTimeMillis();
            ensureDayCycleTask();
            applyPauseEffects();
            return;
        }

        running = false;
        paused = false;
        clearMatchTiming();
        cancelDayCycleTask();
        removePauseEffects();
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction == null ? NO_OP : saveAction;
    }

    public void setPlayerStatsManager(PlayerStatsManager playerStatsManager) {
        this.playerStatsManager = playerStatsManager;
    }

    public void setTeamSidebarManager(TeamSidebarManager teamSidebarManager) {
        this.teamSidebarManager = teamSidebarManager;
    }

    public void setPersistedPlayedMillis(long playedMillis) {
        this.persistedPlayedMillis = Math.max(0L, playedMillis);
    }

    public long getPersistedPlayedMillis() {
        return getEffectivePlayedMillis();
    }

    public void start() {
        start(null);
    }

    public void start(Player preferredStartPlayer) {
        if (!running) {
            running = true;
            paused = false;
            settingsManager.setCurrentDay(0);
            settingsCommand.applyDayToWorlds();
            kothManager.resetKothSchedule();
            resetMatchTiming(0L);
            removePauseEffects();
            ensureDayCycleTask();
            clearAllPlayersInventories();
            setSurvivalModeForAllPlayers();
            teleportPlayersToGameStartPoint(preferredStartPlayer);
            if (playerStatsManager != null) {
                playerStatsManager.resetAll();
            }
            giveBaseLoadoutToTeamPlayers();
            saveAction.run();
            Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + ChatColor.BOLD + "The game has started.");
            return;
        }

        if (paused) {
            paused = false;
            resumeMatchTiming();
            removePauseEffects();
            setSurvivalModeForAllPlayers();
            saveAction.run();
            Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + ChatColor.BOLD + "The game has resumed.");
            return;
        }

        Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "The game is already running.");
    }

    public void stop() {
        if (!running && !paused) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "The game is already stopped.");
            return;
        }

        freezeMatchTiming();
        running = false;
        paused = false;
        cancelDayCycleTask();
        baseCaptureProgress.clear();
        LegacyBossBarUtil.hideAll();
        removePauseEffects();
        saveAction.run();
        setAdventureModeForAllPlayers();
        Bukkit.broadcastMessage(PREFIX + ChatColor.RED + ChatColor.BOLD + "The game has been stopped.");
    }

    public void pause() {
        if (!running) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Start the game first with /fk start.");
            return;
        }

        if (paused) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "The game is already paused.");
            return;
        }

        paused = true;
        freezeMatchTiming();
        LegacyBossBarUtil.hideAll();
        applyPauseEffects();
        saveAction.run();
        Bukkit.broadcastMessage(PREFIX + ChatColor.GOLD + ChatColor.BOLD + "The game is now paused.");
    }

    public void resume() {
        if (!running) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Start the game first with /fk start.");
            return;
        }

        if (!paused) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "The game is not paused.");
            return;
        }

        paused = false;
        resumeMatchTiming();
        removePauseEffects();
        saveAction.run();
        Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + ChatColor.BOLD + "The game has resumed.");
    }

    public String getFormattedTotalMatchTime() {
        return formatDuration(getEffectivePlayedMillis());
    }

    public String getFormattedTimeUntilNextDay() {
        long remaining;
        if (!running) {
            remaining = DAY_DURATION_MILLIS;
        } else if (paused) {
            remaining = Math.max(0L, nextDayAtMillis - pauseStartedMillis);
        } else {
            remaining = Math.max(0L, nextDayAtMillis - System.currentTimeMillis());
        }
        return formatDuration(remaining);
    }

    public void shutdown() {
        cancelDayCycleTask();
        cancelHudTask();
        LegacyBossBarUtil.hideAll();
        removePauseEffects();
        saveAction.run();
    }

    public void startHud() {
        cancelHudTask();
        hudTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!running) {
                    sendActionBar(player, " ");
                    continue;
                }

                if (kothManager != null && kothManager.isActive() && kothManager.getCurrentKing() != null) {
                    continue;
                }

                StringBuilder message = new StringBuilder();

                if (settingsManager.isDayHudEnabled()) {
                    message.append(ChatColor.GOLD).append("Day: ").append(ChatColor.YELLOW).append(settingsManager.getCurrentDay());
                }

                if (settingsManager.isCompassEnabled()) {
                    String baseIndicator = getBaseIndicator(player);
                    String kothIndicator = getKothIndicator(player);
                    String indicator = null;

                    if (baseIndicator != null && kothIndicator != null) {
                        indicator = ChatColor.AQUA + baseIndicator + ChatColor.DARK_GRAY + " | " + ChatColor.RED + kothIndicator;
                    } else if (kothIndicator != null) {
                        indicator = ChatColor.RED + kothIndicator;
                    } else if (baseIndicator != null) {
                        indicator = ChatColor.AQUA + baseIndicator;
                    }

                    if (indicator != null) {
                        if (message.length() > 0) {
                            message.append(ChatColor.DARK_GRAY).append(" | ");
                        }
                        message.append(indicator);
                    }
                }

                sendActionBar(player, message.length() == 0 ? " " : message.toString());
            }
        }, 0L, 20L);
    }

    private String getBaseIndicator(Player player) {
        TeamData team = teamManager == null ? null : teamManager.getTeamByPlayer(player.getUniqueId());
        if (team == null || team.baseCenter == null || team.baseCenter.getWorld() == null) {
            return null;
        }

        Location playerLocation = player.getLocation();
        if (playerLocation.getWorld() == null || !playerLocation.getWorld().equals(team.baseCenter.getWorld())) {
            return null;
        }

        player.setCompassTarget(team.baseCenter);

        double deltaX = team.baseCenter.getX() - playerLocation.getX();
        double deltaZ = team.baseCenter.getZ() - playerLocation.getZ();

        double distance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        int roundedDistance = (int) Math.round(distance);
        if (distance < 1.0D) {
            return "Base: ↑ 0m";
        }

        double yawRad = Math.toRadians(playerLocation.getYaw());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = forwardZ;
        double rightZ = -forwardX;

        double invLength = 1.0D / distance;
        double targetNormX = deltaX * invLength;
        double targetNormZ = deltaZ * invLength;

        double forwardDot = (targetNormX * forwardX) + (targetNormZ * forwardZ);
        double rightDot = (targetNormX * rightX) + (targetNormZ * rightZ);
        double relativeAngle = Math.toDegrees(Math.atan2(rightDot, forwardDot));

        String arrow = getRelativeArrow(relativeAngle);
        return "Base: " + arrow + " " + roundedDistance + "m";
    }

    private String getRelativeArrow(double relativeAngle) {
        if (relativeAngle >= -22.5D && relativeAngle < 22.5D) {
            return "↑";
        }
        if (relativeAngle >= 22.5D && relativeAngle < 67.5D) {
            return "↖";
        }
        if (relativeAngle >= 67.5D && relativeAngle < 112.5D) {
            return "←";
        }
        if (relativeAngle >= 112.5D && relativeAngle < 157.5D) {
            return "↙";
        }
        if (relativeAngle >= 157.5D || relativeAngle < -157.5D) {
            return "↓";
        }
        if (relativeAngle >= -157.5D && relativeAngle < -112.5D) {
            return "↘";
        }
        if (relativeAngle >= -112.5D && relativeAngle < -67.5D) {
            return "→";
        }
        return "↗";
    }

    private String getKothIndicator(Player player) {
        if (!kothManager.isActive() || kothManager.getZoneCenter() == null || kothManager.getZoneCenter().getWorld() == null) {
            return null;
        }

        Location playerLocation = player.getLocation();
        if (playerLocation.getWorld() == null || !playerLocation.getWorld().equals(kothManager.getZoneCenter().getWorld())) {
            return null;
        }

        player.setCompassTarget(kothManager.getZoneCenter());

        double deltaX = kothManager.getZoneCenter().getX() - playerLocation.getX();
        double deltaZ = kothManager.getZoneCenter().getZ() - playerLocation.getZ();
        double distance = Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
        int roundedDistance = (int) Math.round(distance);

        if (distance < 1.0D) {
            return "KOTH: ↑ 0m";
        }

        double yawRad = Math.toRadians(playerLocation.getYaw());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = forwardZ;
        double rightZ = -forwardX;

        double invLength = 1.0D / distance;
        double targetNormX = deltaX * invLength;
        double targetNormZ = deltaZ * invLength;

        double forwardDot = (targetNormX * forwardX) + (targetNormZ * forwardZ);
        double rightDot = (targetNormX * rightX) + (targetNormZ * rightZ);
        double relativeAngle = Math.toDegrees(Math.atan2(rightDot, forwardDot));

        String arrow = getRelativeArrow(relativeAngle);
        return "KOTH: " + arrow + " " + roundedDistance + "m";
    }

    private void ensureDayCycleTask() {
        cancelDayCycleTask();
        if (nextDayAtMillis <= 0L) {
            nextDayAtMillis = System.currentTimeMillis() + DAY_DURATION_MILLIS;
        }
        dayCycleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running || paused) {
                return;
            }

            long now = System.currentTimeMillis();
            while (now >= nextDayAtMillis) {
                if (kothManager != null && kothManager.isActive()) {
                    kothManager.stopKoth();
                }
                
                int previousDay = settingsManager.getCurrentDay();
                settingsManager.addDay(1);
                saveAction.run();
                settingsCommand.applyDayToWorlds();
                settingsCommand.announceActivatedCaps(previousDay, settingsManager.getCurrentDay());
                kothManager.startKothIfApplicable(settingsManager.getCurrentDay());
                Bukkit.broadcastMessage(PREFIX + ChatColor.AQUA + "Day " + settingsManager.getCurrentDay() + " has begun.");
                nextDayAtMillis += DAY_DURATION_MILLIS;
            }

            tickBaseCaptures(now);
        }, 20L, 20L);
    }

    private void sendActionBar(Player player, String message) {
        if (player == null) {
            return;
        }

        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            String version = parts.length > 3 ? parts[3] : "";

            Class<?> serializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Method serializeMethod = serializerClass.getMethod("a", String.class);
            Object component = serializeMethod.invoke(null, "{\"text\":\"" + escapeJson(message) + "\"}");

            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Constructor<?> packetCtor = packetClass.getConstructor(
                    Class.forName("net.minecraft.server." + version + ".IChatBaseComponent"), byte.class);
            Object packet = packetCtor.newInstance(component, (byte) 2);

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Object handle = getHandle.invoke(craftPlayer);

            Field connectionField = handle.getClass().getField("playerConnection");
            Object connection = connectionField.get(handle);

            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacket.invoke(connection, packet);
        } catch (Throwable ignored) {
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void tickBaseCaptures(long now) {
        if (teamManager == null) {
            LegacyBossBarUtil.hideAll();
            return;
        }

        if (!settingsManager.isTntCapEnabled()) {
            baseCaptureProgress.clear();
            LegacyBossBarUtil.hideAll();
            return;
        }

        long captureDurationMillis = Math.max(1000L, settingsManager.getBaseCaptureTimeSeconds() * 1000L);
        int requiredPlayers = Math.max(1, settingsManager.getBaseCaptureRequiredPlayers());
        Set<UUID> activeBars = new HashSet<UUID>();

        for (TeamData defendingTeam : teamManager.getTeams().values()) {
            if (defendingTeam == null || defendingTeam.baseCenter == null || defendingTeam.baseCenter.getWorld() == null
                    || defendingTeam.baseRadius <= 0) {
                baseCaptureProgress.remove(Integer.valueOf(defendingTeam == null ? -1 : defendingTeam.id));
                continue;
            }

            TeamManager.CaptureBounds bounds = teamManager.getCaptureBounds(defendingTeam);
            if (bounds == null) {
                baseCaptureProgress.remove(Integer.valueOf(defendingTeam.id));
                continue;
            }

            Integer teamKey = Integer.valueOf(defendingTeam.id);
            BaseCaptureProgress progress = baseCaptureProgress.get(teamKey);
            Map<Integer, List<Player>> attackersByTeam = findEnemyPlayersInCaptureZoneByTeam(defendingTeam, bounds);
            TeamData attackerTeam = null;

            if (progress != null) {
                attackerTeam = teamManager.getTeams().get(progress.attackingTeamId);
                if (attackerTeam != null) {
                    List<Player> activeAttackers = attackersByTeam.get(attackerTeam.id);
                    if (activeAttackers == null || activeAttackers.size() < requiredPlayers) {
                        attackerTeam = null;
                    }
                }
                if (attackerTeam == null) {
                    attackerTeam = selectEligibleAttackerTeam(attackersByTeam, requiredPlayers);
                    if (attackerTeam != null) {
                        progress = new BaseCaptureProgress(attackerTeam.id, now);
                        baseCaptureProgress.put(teamKey, progress);
                    } else {
                        baseCaptureProgress.remove(teamKey);
                        continue;
                    }
                }
            } else {
                attackerTeam = selectEligibleAttackerTeam(attackersByTeam, requiredPlayers);
                if (attackerTeam == null) {
                    baseCaptureProgress.remove(teamKey);
                    continue;
                }
                progress = new BaseCaptureProgress(attackerTeam.id, now);
                baseCaptureProgress.put(teamKey, progress);
            }

            List<Player> activeAttackers = attackersByTeam.get(attackerTeam.id);
            if (activeAttackers == null || activeAttackers.isEmpty()) {
                baseCaptureProgress.remove(teamKey);
                continue;
            }

            double ratio = Math.min(1.0D, (double) (now - progress.startedAtMillis) / (double) captureDurationMillis);
            int percent = (int) Math.round(ratio * 100.0D);
            String title = ChatColor.RED + "Capturing " + teamManager.getTeamChatColor(defendingTeam.color)
                    + defendingTeam.name + ChatColor.RED + " " + percent + "%";
            for (Player attacker : activeAttackers) {
                activeBars.add(attacker.getUniqueId());
                LegacyBossBarUtil.show(attacker, title, ratio);
            }

            if ((now - progress.startedAtMillis) < captureDurationMillis) {
                continue;
            }

            defendingTeam.baseCenter = null;
            defendingTeam.baseRadius = 0;
            teamManager.invalidateCaptureBounds(defendingTeam.id);
            baseCaptureProgress.remove(teamKey);
            saveAction.run();

            Player captureRepresentative = activeAttackers.get(0);
            String capturerName = captureRepresentative.getName();

            if (playerStatsManager != null) {
                playerStatsManager.addBaseCaptured(captureRepresentative.getUniqueId());
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != null && online.isOnline()) {
                    online.playSound(online.getLocation(), Sound.WITHER_SPAWN, 10.0F, 1.0F);
                }
            }

            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Team "
                    + teamManager.getTeamChatColor(defendingTeam.color) + defendingTeam.name
                    + ChatColor.RED + " has been eliminated by "
                    + teamManager.getTeamChatColor(attackerTeam.color) + attackerTeam.name
                    + ChatColor.RED + " (" + capturerName + ").");

                TeamData winnerTeam = findLastTeamWithBase();
                if (winnerTeam != null) {
                awardGameWin(winnerTeam);
                Bukkit.broadcastMessage(PREFIX + teamManager.getTeamChatColor(winnerTeam.color) + winnerTeam.name
                    + ChatColor.GREEN + " has won the game!");
                stop();
                return;
                }
        }

        LegacyBossBarUtil.hideAllExcept(activeBars);
    }

    private Map<Integer, List<Player>> findEnemyPlayersInCaptureZoneByTeam(TeamData defendingTeam, TeamManager.CaptureBounds bounds) {
        Map<Integer, List<Player>> attackersByTeam = new HashMap<Integer, List<Player>>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline() || online.isDead()) {
                continue;
            }

            TeamData playerTeam = teamManager.getTeamByPlayer(online.getUniqueId());
            if (playerTeam == null || playerTeam.id == defendingTeam.id) {
                continue;
            }

            if (teamManager.isInsideCaptureBounds(online.getLocation(), bounds)) {
                List<Player> attackers = attackersByTeam.get(playerTeam.id);
                if (attackers == null) {
                    attackers = new ArrayList<Player>();
                    attackersByTeam.put(playerTeam.id, attackers);
                }
                attackers.add(online);
            }
        }
        return attackersByTeam;
    }

    private TeamData selectEligibleAttackerTeam(Map<Integer, List<Player>> attackersByTeam, int minPlayers) {
        for (Map.Entry<Integer, List<Player>> entry : attackersByTeam.entrySet()) {
            List<Player> attackers = entry.getValue();
            if (attackers != null && attackers.size() >= minPlayers) {
                return teamManager.getTeams().get(entry.getKey());
            }
        }
        return null;
    }

    private TeamData findLastTeamWithBase() {
        TeamData candidate = null;
        int aliveBases = 0;
        for (TeamData team : teamManager.getTeams().values()) {
            if (team == null || team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
                continue;
            }

            aliveBases++;
            candidate = team;
            if (aliveBases > 1) {
                return null;
            }
        }

        return aliveBases == 1 ? candidate : null;
    }

    private void awardGameWin(TeamData winnerTeam) {
        if (playerStatsManager == null || winnerTeam == null) {
            return;
        }

        for (UUID memberId : winnerTeam.members) {
            if (memberId != null) {
                playerStatsManager.addGameWon(memberId);
            }
        }
    }

    private void cancelDayCycleTask() {
        if (dayCycleTask != null) {
            dayCycleTask.cancel();
            dayCycleTask = null;
        }
    }

    private void cancelHudTask() {
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
    }

    private void applyPauseEffects() {
        PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                continue;
            }
            player.addPotionEffect(blind, true);
        }
    }

    private void removePauseEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void clearAllPlayersInventories() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.getInventory().clear();
            onlinePlayer.getInventory().setArmorContents(null);
            onlinePlayer.updateInventory();
        }
    }

    private void setGameModeForAllPlayers(GameMode gameMode) {
        if (gameMode == null) {
            return;
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.setGameMode(gameMode);
            }
        }
    }

    private void setAdventureModeForAllPlayers() {
        setGameModeForAllPlayers(GameMode.ADVENTURE);
    }

    private void setSurvivalModeForAllPlayers() {
        setGameModeForAllPlayers(GameMode.SURVIVAL);
    }

    public void enforceAdventureModeWhenStopped() {
        if (!running) {
            setAdventureModeForAllPlayers();
        }
    }

    private void teleportPlayersToGameStartPoint(Player preferredStartPlayer) {
        Location target = null;
        if (dataStore != null) {
            target = dataStore.getGameStartLocation();
        }


        if (target == null || target.getWorld() == null) {
            if (preferredStartPlayer != null && preferredStartPlayer.getWorld() != null) {
                target = preferredStartPlayer.getLocation().clone();
            } else if (!Bukkit.getWorlds().isEmpty()) {
                target = Bukkit.getWorlds().get(0).getSpawnLocation().clone();
            } else {
                return;
            }
        }

        target = target.clone().add(0.5D, 0.0D, 0.5D);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.teleport(target);
            onlinePlayer.setBedSpawnLocation(target, true);
        }


        try {
            target.getWorld().setSpawnLocation(target.getBlockX(), target.getBlockY(), target.getBlockZ());
        } catch (Exception ignored) {
        }
    }

    private void resetMatchTiming(long initialPlayedMillis) {
        long now = System.currentTimeMillis();
        long safeInitialPlayedMillis = Math.max(0L, initialPlayedMillis);
        matchStartMillis = now - safeInitialPlayedMillis;
        pausedAccumulatedMillis = 0L;
        pauseStartedMillis = 0L;
        long remainder = safeInitialPlayedMillis % DAY_DURATION_MILLIS;
        long nextDayDelay = remainder == 0L ? DAY_DURATION_MILLIS : (DAY_DURATION_MILLIS - remainder);
        nextDayAtMillis = now + nextDayDelay;
        persistedPlayedMillis = safeInitialPlayedMillis;
    }

    private void clearMatchTiming() {
        matchStartMillis = 0L;
        pausedAccumulatedMillis = 0L;
        pauseStartedMillis = 0L;
        nextDayAtMillis = 0L;
    }

    private void freezeMatchTiming() {
        if (pauseStartedMillis > 0L || !running) {
            return;
        }
        pauseStartedMillis = System.currentTimeMillis();
    }

    private void resumeMatchTiming() {
        if (pauseStartedMillis <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        long pausedDuration = Math.max(0L, now - pauseStartedMillis);
        pausedAccumulatedMillis += pausedDuration;
        nextDayAtMillis += pausedDuration;
        pauseStartedMillis = 0L;
    }

    private long getEffectivePlayedMillis() {
        if (matchStartMillis <= 0L) {
            return 0L;
        }

        long now = System.currentTimeMillis();
        long referenceTime = pauseStartedMillis > 0L ? pauseStartedMillis : now;
        long elapsed = referenceTime - matchStartMillis - pausedAccumulatedMillis;
        return Math.max(0L, elapsed);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void giveBaseLoadoutToTeamPlayers() {
        java.util.List<ItemStack> loadoutItems = settingsManager.getBaseLoadoutItems();
        if (loadoutItems.isEmpty()) {
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (teamManager.getTeamByPlayer(onlinePlayer.getUniqueId()) == null) {
                continue;
            }

            for (ItemStack item : loadoutItems) {
                if (item == null || item.getType() == null) {
                    continue;
                }

                java.util.Map<Integer, ItemStack> overflow = onlinePlayer.getInventory().addItem(item.clone());
                for (ItemStack left : overflow.values()) {
                    onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), left);
                }
            }
        }
    }
}
