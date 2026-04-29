package org.example.map;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.example.scoreboard.TeamSidebarManager;
import org.example.storage.PluginDataStore;
import org.example.team.TeamManager;
import org.example.game.GameStateManager;

public class MapVoteManager implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final String VOTE_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bMap Vote");
    private static final String MAP_LIST_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bMap List");
    private static final int DEFAULT_VOTE_DURATION_SECONDS = 60;
    private static final int MIN_VOTE_DURATION_SECONDS = 5;
    private static final int MAX_VOTE_DURATION_SECONDS = 600;
    private static final int DESCRIPTION_LORE_LINE_LENGTH = 34;

    private final JavaPlugin plugin;
    private final PluginDataStore dataStore;
    private final TeamManager teamManager;
    private final TeamSidebarManager teamSidebarManager;
    private final GameStateManager gameStateManager;
    private final Random random = new Random();
    private final Map<UUID, String> playerVotes = new HashMap<UUID, String>();
    private final Map<Integer, String> slotToWorldName = new LinkedHashMap<Integer, String>();
    private final Map<String, MapDescription> descriptionCache = new HashMap<String, MapDescription>();

    private boolean voteActive;
    private int secondsLeft;
    private int voteDurationSeconds = DEFAULT_VOTE_DURATION_SECONDS;
    private boolean autoStartOnMapSwitch;
    private BukkitTask voteTask;
    private List<String> candidateMapNames = new ArrayList<String>();

    private static class MapDescription {
        private final String creator;
        private final String description;
        private final Integer teams;

        private MapDescription(String creator, String description, Integer teams) {
            this.creator = creator;
            this.description = description;
            this.teams = teams;
        }
    }

    public MapVoteManager(JavaPlugin plugin, PluginDataStore dataStore, TeamManager teamManager, TeamSidebarManager teamSidebarManager, GameStateManager gameStateManager) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.teamManager = teamManager;
        this.teamSidebarManager = teamSidebarManager;
        this.gameStateManager = gameStateManager;
    }

    public boolean isVoteActive() {
        return voteActive;
    }

    public int getVoteDurationSeconds() {
        if (voteDurationSeconds < MIN_VOTE_DURATION_SECONDS) {
            voteDurationSeconds = readVoteDurationSeconds();
        }
        return voteDurationSeconds;
    }

    public int setVoteDurationSeconds(int seconds) {
        int clamped = Math.max(MIN_VOTE_DURATION_SECONDS, Math.min(MAX_VOTE_DURATION_SECONDS, seconds));
        voteDurationSeconds = clamped;

        FileConfiguration configuration = loadMapsConfiguration();
        configuration.set("voteTimerSeconds", Integer.valueOf(clamped));
        saveMapsConfiguration(configuration);

        if (voteActive) {
            secondsLeft = clamped;
            refreshOpenVoteMenus();
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Map vote timer updated to " + clamped + " seconds.");
        }

        return clamped;
    }

    public boolean isAutoStartOnMapSwitch() {
        return autoStartOnMapSwitch;
    }

    public void setAutoStartOnMapSwitch(boolean enabled) {
        autoStartOnMapSwitch = enabled;
        FileConfiguration configuration = loadMapsConfiguration();
        configuration.set("autoStartOnMapSwitch", Boolean.valueOf(enabled));
        saveMapsConfiguration(configuration);
    }

    public boolean startVote(Player starter) {
        if (gameStateManager != null && gameStateManager.isRunning()) {
            if (starter != null) {
                starter.sendMessage(PREFIX + ChatColor.RED + "You cannot start a map vote while a game is running.");
            }
            return false;
        }

        if (voteActive) {
            if (starter != null) {
                starter.sendMessage(PREFIX + ChatColor.RED + "A map vote is already running.");
                openVoteMenu(starter);
            }
            return false;
        }

        List<String> maps = getConfiguredMapNames();
        if (maps.size() < 2) {
            if (starter != null) {
                starter.sendMessage(PREFIX + ChatColor.RED + "At least 2 valid maps are required in plugins/Tr0NFK/maps.yml.");
            }
            return false;
        }

        candidateMapNames = maps;
        playerVotes.clear();
        descriptionCache.clear();
        for (String mapName : candidateMapNames) {
            descriptionCache.put(mapName, readMapDescription(mapName));
        }
        voteDurationSeconds = readVoteDurationSeconds();
        autoStartOnMapSwitch = readAutoStartOnMapSwitch();
        voteActive = true;
        secondsLeft = voteDurationSeconds;

        Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Map vote started for " + secondsLeft + " seconds. Choose in the GUI.");
        for (Player online : Bukkit.getOnlinePlayers()) {
            openVoteMenu(online);
        }

        voteTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!voteActive) {
                    stopVoteTask();
                    return;
                }

                secondsLeft--;
                refreshOpenVoteMenus();
                if (secondsLeft == 30 || secondsLeft == 10 || secondsLeft == 5) {
                    Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Map vote ends in " + secondsLeft + " seconds.");
                }

                if (secondsLeft <= 0) {
                    finishVote();
                }
            }
        }, 20L, 20L);

        return true;
    }

    public void openVoteMenu(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!voteActive) {
            player.sendMessage(PREFIX + ChatColor.RED + "No map vote is running.");
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 54, VOTE_TITLE);
        renderVoteMenu(menu, player.getUniqueId());

        player.openInventory(menu);
    }

    public void openMapListMenu(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        List<String> maps = getConfiguredMapNames();
        descriptionCache.clear();
        for (String mapName : maps) {
            descriptionCache.put(mapName, readMapDescription(mapName));
        }

        Inventory menu = Bukkit.createInventory(null, 54, MAP_LIST_TITLE);
        renderMapListMenu(menu, maps);
        player.openInventory(menu);
    }

    @EventHandler
    public void onVoteClose(InventoryCloseEvent event) {
        if (!voteActive || event == null || event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        if (!VOTE_TITLE.equals(event.getInventory().getTitle())) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (voteActive && player.isOnline()) {
                    openVoteMenu(player);
                }
            }
        });
    }

    public List<String> getConfiguredMapNames() {
        return new ArrayList<String>(getConfiguredMapNamesInternal());
    }

    public boolean switchMapForPlayer(String requestedMapName, Player target, Player executor) {
        List<Player> targets = new ArrayList<Player>();
        if (target != null) {
            targets.add(target);
        }
        return switchMapForPlayers(requestedMapName, targets, executor, false);
    }

    public boolean switchMapForPlayers(String requestedMapName, List<Player> targets, Player executor) {
        return switchMapForPlayers(requestedMapName, targets, executor, false);
    }

    private boolean switchMapForPlayers(String requestedMapName, List<Player> targets, Player executor, boolean allowAutoStart) {
        if (gameStateManager != null && gameStateManager.isRunning()) {
            if (executor != null) {
                executor.sendMessage(PREFIX + ChatColor.RED + "You cannot switch maps while a game is running.");
            }
            return false;
        }

        String mapName = normalizeConfiguredMapName(requestedMapName);
        if (mapName == null) {
            if (executor != null) {
                executor.sendMessage(PREFIX + ChatColor.RED + "Unknown map: " + requestedMapName + ".");
            }
            return false;
        }

        World world = loadOrGetWorld(mapName);
        if (world == null) {
            if (executor != null) {
                executor.sendMessage(PREFIX + ChatColor.RED + "Failed to load map: " + mapName + ".");
            }
            return false;
        }

        if (dataStore != null && !dataStore.switchToMapProfile(mapName)) {
            if (executor != null) {
                executor.sendMessage(PREFIX + ChatColor.RED + "Failed to switch map configuration profile.");
            }
            return false;
        }

        if (dataStore != null) {
            autoStartOnMapSwitch = readAutoStartOnMapSwitch();
            dataStore.applyStoredWorldDifficulties();
        }

        if (teamSidebarManager != null) {
            teamSidebarManager.refreshTeams();
        }
        if (teamManager != null) {
            teamManager.updatePlayerDisplayNames();
        }

        List<Player> onlineTargets = new ArrayList<Player>();
        if (targets != null) {
            for (Player target : targets) {
                if (target != null && target.isOnline()) {
                    onlineTargets.add(target);
                }
            }
        }

        if (onlineTargets.isEmpty()) {
            if (executor != null) {
                executor.sendMessage(PREFIX + ChatColor.RED + "Target player is not online.");
            }
            return false;
        }

        Location targetLocation = world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
        if (dataStore != null) {
            Location configuredSwitchPoint = dataStore.getMapSwitchLocation();
            if (configuredSwitchPoint != null && configuredSwitchPoint.getWorld() != null
                    && configuredSwitchPoint.getWorld().getName().equalsIgnoreCase(world.getName())) {
                targetLocation = configuredSwitchPoint.clone().add(0.5D, 0.0D, 0.5D);
            }
        }
        for (Player target : onlineTargets) {
            target.teleport(targetLocation);
            target.setBedSpawnLocation(targetLocation, true);
        }

        if (executor != null) {
            if (onlineTargets.size() == 1) {
                executor.sendMessage(PREFIX + ChatColor.GREEN + "Switched to map " + ChatColor.YELLOW + mapName
                        + ChatColor.GREEN + " and teleported " + ChatColor.YELLOW + onlineTargets.get(0).getName() + ChatColor.GREEN + ".");
            } else {
                executor.sendMessage(PREFIX + ChatColor.GREEN + "Switched to map " + ChatColor.YELLOW + mapName
                        + ChatColor.GREEN + " and teleported " + ChatColor.YELLOW + onlineTargets.size() + ChatColor.GREEN + " players.");
            }
        }

        for (Player target : onlineTargets) {
            if (executor == null || !executor.getUniqueId().equals(target.getUniqueId())) {
                target.sendMessage(PREFIX + ChatColor.GREEN + "You were teleported to map " + ChatColor.YELLOW + mapName + ChatColor.GREEN + ".");
            }
        }

        if (allowAutoStart && autoStartOnMapSwitch && gameStateManager != null && !gameStateManager.isRunning() && isAllOnlinePlayersTargeted(onlineTargets)) {
            if (teamManager != null) {
                boolean assigned = teamManager.assignOnlinePlayersRandomlyBalanced();
                teamManager.updatePlayerDisplayNames();
                if (teamSidebarManager != null) {
                    teamSidebarManager.refreshTeams();
                }
                if (assigned) {
                    Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Teams have been randomly assigned for this match.");
                }
            }
            gameStateManager.start(executor);
        }

        return true;
    }

    private boolean isAllOnlinePlayersTargeted(List<Player> targets) {
        if (targets == null) {
            return false;
        }

        List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        if (online.isEmpty() || targets.size() != online.size()) {
            return false;
        }

        java.util.Set<UUID> targetIds = new java.util.HashSet<UUID>();
        for (Player target : targets) {
            if (target != null) {
                targetIds.add(target.getUniqueId());
            }
        }

        for (Player onlinePlayer : online) {
            if (onlinePlayer == null || !targetIds.contains(onlinePlayer.getUniqueId())) {
                return false;
            }
        }

        return true;
    }

    @EventHandler
    public void onVoteClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        String title = event.getInventory().getTitle();
        if (!VOTE_TITLE.equals(title) && !MAP_LIST_TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (MAP_LIST_TITLE.equals(title)) {
            if (!player.isOp()) {
                return;
            }

            String mapName = slotToWorldName.get(Integer.valueOf(event.getRawSlot()));
            if (mapName == null) {
                return;
            }

            List<Player> targets = new ArrayList<Player>();
            targets.add(player);
            switchMapForPlayers(mapName, targets, player);
            return;
        }

        if (!voteActive) {
            player.closeInventory();
            player.sendMessage(PREFIX + ChatColor.RED + "This vote has already ended.");
            return;
        }

        if (event.getRawSlot() == 48) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Only operators can force start the selected map.");
                return;
            }

            secondsLeft = 0;
            finishVote();
            return;
        }

        if (event.getRawSlot() == 47) {
            if (!player.isOp()) {
                return;
            }

            cancelVote(player);
            return;
        }

        if (event.getRawSlot() == 50) {
            if (!player.isOp()) {
                return;
            }

            setAutoStartOnMapSwitch(!autoStartOnMapSwitch);
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Auto-start on map switch: "
                    + (autoStartOnMapSwitch ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.YELLOW + ".");
            refreshOpenVoteMenus();
            return;
        }

        String worldName = slotToWorldName.get(Integer.valueOf(event.getRawSlot()));
        if (worldName == null) {
            return;
        }

        playerVotes.put(player.getUniqueId(), worldName);
        player.sendMessage(PREFIX + ChatColor.GREEN + "You voted for " + ChatColor.YELLOW + worldName + ChatColor.GREEN + ".");
        refreshOpenVoteMenus();
    }

    private void finishVote() {
        voteActive = false;
        stopVoteTask();

        if (gameStateManager != null && gameStateManager.isRunning()) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Map vote cancelled: a game is already running.");
            return;
        }

        String winnerName = resolveWinnerName();
        if (winnerName == null) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Map vote failed: no valid map selected.");
            return;
        }

        List<Player> targets = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        if (!switchMapForPlayers(winnerName, targets, null, true)) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Failed to switch to selected map: " + winnerName + ".");
            return;
        }

        if (!autoStartOnMapSwitch) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.GREEN + "Map selected: " + ChatColor.YELLOW + winnerName
                    + ChatColor.GREEN + ". Use /fk start when ready.");
        }
    }

    private void cancelVote(Player cancelledBy) {
        voteActive = false;
        stopVoteTask();
        playerVotes.clear();

        if (cancelledBy != null) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Map vote cancelled by " + ChatColor.YELLOW + cancelledBy.getName() + ChatColor.RED + ".");
        } else {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Map vote cancelled.");
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != null && online.isOnline() && online.getOpenInventory() != null
                    && online.getOpenInventory().getTopInventory() != null
                    && VOTE_TITLE.equals(online.getOpenInventory().getTopInventory().getTitle())) {
                online.closeInventory();
            }
        }
    }

    private void stopVoteTask() {
        if (voteTask != null) {
            voteTask.cancel();
            voteTask = null;
        }
    }

    private String resolveWinnerName() {
        if (candidateMapNames.isEmpty()) {
            return null;
        }

        int bestVotes = -1;
        List<String> bestMapNames = new ArrayList<String>();

        for (String mapName : candidateMapNames) {
            int votes = getVoteCountForMap(mapName);
            if (votes > bestVotes) {
                bestVotes = votes;
                bestMapNames.clear();
                bestMapNames.add(mapName);
            } else if (votes == bestVotes) {
                bestMapNames.add(mapName);
            }
        }

        if (bestMapNames.isEmpty()) {
            return null;
        }

        return bestMapNames.get(random.nextInt(bestMapNames.size()));
    }

    private int getVoteCountForMap(String mapName) {
        int count = 0;
        for (String vote : playerVotes.values()) {
            if (mapName.equalsIgnoreCase(vote)) {
                count++;
            }
        }
        return count;
    }

    private List<String> getConfiguredMapNamesInternal() {
        FileConfiguration configuration = loadMapsConfiguration();
        List<String> raw = configuration.getStringList("maps");
        List<String> result = new ArrayList<String>();
        for (String name : raw) {
            if (name == null) {
                continue;
            }
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!result.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private int readVoteDurationSeconds() {
        FileConfiguration configuration = loadMapsConfiguration();
        int raw = configuration.getInt("voteTimerSeconds", DEFAULT_VOTE_DURATION_SECONDS);
        int clamped = Math.max(MIN_VOTE_DURATION_SECONDS, Math.min(MAX_VOTE_DURATION_SECONDS, raw));
        if (raw != clamped) {
            configuration.set("voteTimerSeconds", Integer.valueOf(clamped));
            saveMapsConfiguration(configuration);
        }
        return clamped;
    }

    private boolean readAutoStartOnMapSwitch() {
        FileConfiguration configuration = loadMapsConfiguration();
        return configuration.getBoolean("autoStartOnMapSwitch", false);
    }

    private File getMapsFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return new File(dataFolder, "maps.yml");
    }

    private FileConfiguration loadMapsConfiguration() {
        File mapsFile = getMapsFile();
        if (!mapsFile.exists()) {
            FileConfiguration created = new YamlConfiguration();
            List<String> example = new ArrayList<String>();
            example.add("world");
            example.add("map2");
            created.set("maps", example);
            created.set("voteTimerSeconds", Integer.valueOf(DEFAULT_VOTE_DURATION_SECONDS));
            created.set("autoStartOnMapSwitch", Boolean.FALSE);
            saveMapsConfiguration(created);
            return created;
        }

        FileConfiguration loaded = YamlConfiguration.loadConfiguration(mapsFile);
        if (!loaded.contains("voteTimerSeconds")) {
            loaded.set("voteTimerSeconds", Integer.valueOf(DEFAULT_VOTE_DURATION_SECONDS));
            saveMapsConfiguration(loaded);
        }
        if (!loaded.contains("autoStartOnMapSwitch")) {
            loaded.set("autoStartOnMapSwitch", Boolean.FALSE);
            saveMapsConfiguration(loaded);
        }
        return loaded;
    }

    private void saveMapsConfiguration(FileConfiguration configuration) {
        if (configuration == null) {
            return;
        }

        try {
            configuration.save(getMapsFile());
        } catch (Exception ignored) {
            //pipi
        }
    }

    private String normalizeConfiguredMapName(String requestedName) {
        if (requestedName == null) {
            return null;
        }

        String trimmed = requestedName.trim();
        for (String configured : getConfiguredMapNamesInternal()) {
            if (configured.equalsIgnoreCase(trimmed)) {
                return configured;
            }
        }
        return null;
    }

    private void refreshOpenVoteMenus() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline() || online.getOpenInventory() == null
                    || online.getOpenInventory().getTopInventory() == null) {
                continue;
            }

            Inventory top = online.getOpenInventory().getTopInventory();
            if (!VOTE_TITLE.equals(top.getTitle())) {
                continue;
            }

            renderVoteMenu(top, online.getUniqueId());
            online.updateInventory();
        }
    }

    private void renderVoteMenu(Inventory menu, UUID viewerId) {
        slotToWorldName.clear();
        clearVoteArea(menu);

        int[] slots = new int[] {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int index = 0;
        for (String mapName : candidateMapNames) {
            if (index >= slots.length) {
                break;
            }

            int slot = slots[index];
            slotToWorldName.put(Integer.valueOf(slot), mapName);

            int votes = getVoteCountForMap(mapName);
            ItemStack item = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + mapName);
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Votes: " + ChatColor.YELLOW + votes);

            MapDescription mapDescription = descriptionCache.get(mapName);
            if (mapDescription != null) {
                if (mapDescription.teams != null) {
                    lore.add(ChatColor.GRAY + "Teams: " + ChatColor.YELLOW + mapDescription.teams);
                }
                if (mapDescription.creator != null && !mapDescription.creator.trim().isEmpty()) {
                    lore.add(ChatColor.GRAY + "Creator: " + ChatColor.YELLOW + mapDescription.creator);
                }
                if (mapDescription.description != null && !mapDescription.description.trim().isEmpty()) {
                    addWrappedDescriptionLore(lore, mapDescription.description);
                }
            }

            String currentVote = playerVotes.get(viewerId);
            if (mapName.equalsIgnoreCase(currentVote)) {
                lore.add(ChatColor.AQUA + "You voted for this map.");
            } else {
                lore.add(ChatColor.YELLOW + "Click to vote.");
            }

            if (!isMapLoadable(mapName)) {
                lore.add(ChatColor.RED + "World folder not found in server root.");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
            menu.setItem(slot, item);
            index++;
        }

        ItemStack info = new ItemStack(Material.WATCH, 1);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Vote timer");
        List<String> infoLore = new ArrayList<String>();
        infoLore.add(ChatColor.GRAY + "Time left: " + ChatColor.YELLOW + Math.max(0, secondsLeft) + "s");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        menu.setItem(49, info);

        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer != null && viewer.isOnline() && viewer.isOp()) {
            ItemStack cancel = new ItemStack(Material.BARRIER, 1);
            ItemMeta cancelMeta = cancel.getItemMeta();
            cancelMeta.setDisplayName(ChatColor.RED + "Cancel vote");
            List<String> cancelLore = new ArrayList<String>();
            cancelLore.add(ChatColor.GRAY + "Stop the current vote immediately.");
            cancelLore.add(ChatColor.YELLOW + "Click to cancel.");
            cancelMeta.setLore(cancelLore);
            cancel.setItemMeta(cancelMeta);
            menu.setItem(47, cancel);

            ItemStack force = new ItemStack(Material.BLAZE_POWDER, 1);
            ItemMeta forceMeta = force.getItemMeta();
            forceMeta.setDisplayName(ChatColor.RED + "Force start now");
            List<String> forceLore = new ArrayList<String>();
            forceLore.add(ChatColor.GRAY + "Set timer to " + ChatColor.YELLOW + "0" + ChatColor.GRAY + " and end vote.");
            forceLore.add(ChatColor.YELLOW + "Click to force start.");
            forceMeta.setLore(forceLore);
            force.setItemMeta(forceMeta);
            menu.setItem(48, force);

            ItemStack autoStart = new ItemStack(Material.WOOL, 1, (short) (autoStartOnMapSwitch ? 5 : 14));
            ItemMeta autoStartMeta = autoStart.getItemMeta();
            autoStartMeta.setDisplayName((autoStartOnMapSwitch ? ChatColor.GREEN : ChatColor.RED) + "Auto-start on map switch");
            List<String> autoStartLore = new ArrayList<String>();
            autoStartLore.add(ChatColor.GRAY + "Status: " + (autoStartOnMapSwitch ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            autoStartLore.add(ChatColor.YELLOW + "If enabled:");
            autoStartLore.add(ChatColor.GRAY + "- Switch map for all players");
            autoStartLore.add(ChatColor.GRAY + "- Randomly distribute teams");
            autoStartLore.add(ChatColor.GRAY + "- Start the game immediately");
            autoStartLore.add(ChatColor.YELLOW + "Click to toggle.");
            autoStartMeta.setLore(autoStartLore);
            autoStart.setItemMeta(autoStartMeta);
            menu.setItem(50, autoStart);
        }
    }

    private void renderMapListMenu(Inventory menu, List<String> maps) {
        slotToWorldName.clear();
        clearVoteArea(menu);
        String activeMapName = dataStore == null ? null : dataStore.getActiveMapProfileName();

        int[] slots = new int[] {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int index = 0;
        for (String mapName : maps) {
            if (index >= slots.length) {
                break;
            }

            ItemStack item = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + mapName);
            List<String> lore = new ArrayList<String>();
            slotToWorldName.put(Integer.valueOf(slots[index]), mapName);

            MapDescription mapDescription = descriptionCache.get(mapName);
            if (mapDescription != null) {
                if (mapDescription.teams != null) {
                    lore.add(ChatColor.GRAY + "Teams: " + ChatColor.YELLOW + mapDescription.teams);
                }
                if (mapDescription.creator != null && !mapDescription.creator.trim().isEmpty()) {
                    lore.add(ChatColor.GRAY + "Creator: " + ChatColor.YELLOW + mapDescription.creator);
                }
                if (mapDescription.description != null && !mapDescription.description.trim().isEmpty()) {
                    addWrappedDescriptionLore(lore, mapDescription.description);
                }
            }

            lore.add(ChatColor.GRAY + "Active map: " + ChatColor.YELLOW + (activeMapName == null ? "Unknown" : activeMapName));
            if (activeMapName != null && activeMapName.equalsIgnoreCase(mapName)) {
                lore.add(ChatColor.GREEN + "You are currently on this map.");
            }

            lore.add(ChatColor.GRAY + "Loadable: " + (isMapLoadable(mapName) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            menu.setItem(slots[index], item);
            index++;
        }
    }

    private void clearVoteArea(Inventory menu) {
        int[] slots = new int[] {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                47,
                48,
                49,
                50
        };
        for (int slot : slots) {
            menu.setItem(slot, null);
        }
    }

    private boolean isMapLoadable(String mapName) {
        if (mapName == null || mapName.trim().isEmpty()) {
            return false;
        }
        File worldFolder = new File(Bukkit.getWorldContainer(), mapName);
        File levelDat = new File(worldFolder, "level.dat");
        return worldFolder.exists() && worldFolder.isDirectory() && levelDat.exists();
    }

    private World loadOrGetWorld(String mapName) {
        World alreadyLoaded = Bukkit.getWorld(mapName);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }

        if (!isMapLoadable(mapName)) {
            return null;
        }

        WorldCreator creator = new WorldCreator(mapName);
        return creator.createWorld();
    }

    private MapDescription readMapDescription(String mapName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), mapName);
        File descriptionFile = new File(worldFolder, "description.json");
        if (!descriptionFile.exists() || !descriptionFile.isFile()) {
            return new MapDescription(null, null, null);
        }

        try {
            String json = new String(Files.readAllBytes(descriptionFile.toPath()), StandardCharsets.UTF_8);
            String creator = extractStringField(json, "creator");
            String description = extractStringField(json, "description");
            Integer teams = extractIntField(json, "teams");
            if (teams == null) {
                teams = extractIntField(json, "teamCount");
            }
            return new MapDescription(creator, description, teams);
        } catch (Exception ignored) {
            return new MapDescription(null, null, null);
        }
    }

    private String extractStringField(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:[^\\\"\\\\]|\\\\.)*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }

        String raw = matcher.group(1);
        return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private void addWrappedDescriptionLore(List<String> lore, String description) {
        if (lore == null || description == null) {
            return;
        }

        String[] paragraphs = description.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph == null ? "" : paragraph.trim();
            if (trimmed.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + " ");
                continue;
            }

            for (String line : wrapText(trimmed, DESCRIPTION_LORE_LINE_LENGTH)) {
                lore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + line);
            }
        }
    }

    private List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<String>();
        if (text == null) {
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }

            while (word.length() > maxLineLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
                lines.add(word.substring(0, maxLineLength));
                word = word.substring(maxLineLength);
            }

            int projectedLength = currentLine.length() == 0
                    ? word.length()
                    : currentLine.length() + 1 + word.length();
            if (projectedLength > maxLineLength) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            if (currentLine.length() > 0) {
                currentLine.append(' ');
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private Integer extractIntField(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.valueOf(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
