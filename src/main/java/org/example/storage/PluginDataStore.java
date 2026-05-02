package org.example.storage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.game.GameStateManager;
import org.example.koth.KothManager;
import org.example.listener.GameplayCapListener;
import org.example.settings.SettingsManager;
import org.example.stats.PlayerStatsManager;
import org.example.team.TeamData;
import org.example.team.TeamManager;

public class PluginDataStore {

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final SettingsManager settingsManager;
    private final GameStateManager gameStateManager;
    private final GameplayCapListener gameplayCapListener;
    private final PlayerStatsManager playerStatsManager;
    private final KothManager kothManager;
    private final File defaultDataFile;
    private final File mapProfilesFolder;
    private File dataFile;
    private String activeMapProfileName = "default";
    private boolean loading;
    private YamlConfiguration configuration;
    private static final String ACTIVE_PROFILE_FILE_NAME = "active-profile.yml";


    private void saveActiveProfile() {
        File file = new File(plugin.getDataFolder(), ACTIVE_PROFILE_FILE_NAME);
        YamlConfiguration config = new YamlConfiguration();
        config.set("activeMapProfile", activeMapProfileName);
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save active profile: " + exception.getMessage());
        }
    }

    private String loadActiveProfile() {
        File file = new File(plugin.getDataFolder(), ACTIVE_PROFILE_FILE_NAME);
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String name = config.getString("activeMapProfile", "default");
            return name == null || name.trim().isEmpty() ? "default" : name.trim();
        }

        if (defaultDataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(defaultDataFile);
            String name = config.getString("meta.activeMapProfileName", "default");
            if (name != null && !name.trim().isEmpty() && !"default".equalsIgnoreCase(name.trim())) {
                plugin.getLogger().info("[FK] Migrating active profile from data.yml: " + name.trim());
                return name.trim();
            }
        }

        return "default";
    }
    public PluginDataStore(JavaPlugin plugin, TeamManager teamManager, SettingsManager settingsManager,
                           GameStateManager gameStateManager, GameplayCapListener gameplayCapListener, PlayerStatsManager playerStatsManager, KothManager kothManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.settingsManager = settingsManager;
        this.gameStateManager = gameStateManager;
        this.gameplayCapListener = gameplayCapListener;
        this.playerStatsManager = playerStatsManager;
        this.kothManager = kothManager;
        this.defaultDataFile = new File(plugin.getDataFolder(), "data.yml");
        this.mapProfilesFolder = new File(plugin.getDataFolder(), "map-profiles");

        this.activeMapProfileName = loadActiveProfile();
        if ("default".equalsIgnoreCase(activeMapProfileName)) {
            this.dataFile = defaultDataFile;
        } else {
            if (!mapProfilesFolder.exists()) {
                mapProfilesFolder.mkdirs();
            }
            this.dataFile = new File(mapProfilesFolder, toProfileFileName(activeMapProfileName));
            if (!this.dataFile.exists()) {
                plugin.getLogger().warning("[FK] Profile file not found for '" + activeMapProfileName + "': " + this.dataFile.getAbsolutePath() + " — falling back to default.");
                this.activeMapProfileName = "default";
                this.dataFile = defaultDataFile;
            }
        }

        saveActiveProfile();
        plugin.getLogger().info("[FK][Debug] PluginDataStore initialized with profile: '" + activeMapProfileName + "' -> " + dataFile.getAbsolutePath());
    }

    public synchronized boolean switchToMapProfile(String mapName) {
        if (mapName == null || mapName.trim().isEmpty()) {
            return false;
        }

        save();

        String trimmed = mapName.trim();
        if ("default".equalsIgnoreCase(trimmed)) {
            dataFile = defaultDataFile;
            activeMapProfileName = "default";
        } else {
            if (!mapProfilesFolder.exists()) {
                mapProfilesFolder.mkdirs();
            }
            dataFile = new File(mapProfilesFolder, toProfileFileName(trimmed));
            activeMapProfileName = trimmed;
        }

        saveActiveProfile();
        configuration = null;
        load();
        return true;
    }

    public synchronized String getActiveMapProfileName() {
        return activeMapProfileName;
    }

    public synchronized void setGameStartLocation(Location location) {
        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        if (location == null || location.getWorld() == null) {
            configuration.set("game.startLocation.world", null);
            configuration.set("game.startLocation.x", null);
            configuration.set("game.startLocation.y", null);
            configuration.set("game.startLocation.z", null);
            configuration.set("game.startLocation.yaw", null);
            configuration.set("game.startLocation.pitch", null);
            save();
            return;
        }

        configuration.set("game.startLocation.world", location.getWorld().getName());
        configuration.set("game.startLocation.x", location.getX());
        configuration.set("game.startLocation.y", location.getY());
        configuration.set("game.startLocation.z", location.getZ());
        configuration.set("game.startLocation.yaw", Float.valueOf(location.getYaw()));
        configuration.set("game.startLocation.pitch", Float.valueOf(location.getPitch()));
        save();

        try {
            location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        } catch (Exception ignored) {
        }
    }

    public synchronized Location getGameStartLocation() {
        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        String worldName = configuration.getString("game.startLocation.world", null);
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = new WorldCreator(worldName).createWorld();
            if (world == null) {
                return null;
            }
        }

        double x = configuration.getDouble("game.startLocation.x", world.getSpawnLocation().getX());
        double y = configuration.getDouble("game.startLocation.y", world.getSpawnLocation().getY());
        double z = configuration.getDouble("game.startLocation.z", world.getSpawnLocation().getZ());
        float yaw = (float) configuration.getDouble("game.startLocation.yaw", 0.0D);
        float pitch = (float) configuration.getDouble("game.startLocation.pitch", 0.0D);
        Location location = new Location(world, x, y, z, yaw, pitch);
        return location;
    }

    public synchronized void setMapSwitchLocation(Location location) {
        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        if (location == null || location.getWorld() == null) {
            configuration.set("map.switchLocation.world", null);
            configuration.set("map.switchLocation.x", null);
            configuration.set("map.switchLocation.y", null);
            configuration.set("map.switchLocation.z", null);
            configuration.set("map.switchLocation.yaw", null);
            configuration.set("map.switchLocation.pitch", null);
            save();
            return;
        }

        configuration.set("map.switchLocation.world", location.getWorld().getName());
        configuration.set("map.switchLocation.x", location.getX());
        configuration.set("map.switchLocation.y", location.getY());
        configuration.set("map.switchLocation.z", location.getZ());
        configuration.set("map.switchLocation.yaw", Float.valueOf(location.getYaw()));
        configuration.set("map.switchLocation.pitch", Float.valueOf(location.getPitch()));
        save();

        plugin.getLogger().info("[FK][Debug] Saved map switch point for profile '" + activeMapProfileName + "' -> "
            + location.getWorld().getName() + " @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
    }

    public synchronized Location getMapSwitchLocation() {
        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        String worldName = configuration.getString("map.switchLocation.world", null);
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = new WorldCreator(worldName).createWorld();
            if (world == null) {
                return null;
            }
        }

        double x = configuration.getDouble("map.switchLocation.x", world.getSpawnLocation().getX());
        double y = configuration.getDouble("map.switchLocation.y", world.getSpawnLocation().getY());
        double z = configuration.getDouble("map.switchLocation.z", world.getSpawnLocation().getZ());
        float yaw = (float) configuration.getDouble("map.switchLocation.yaw", 0.0D);
        float pitch = (float) configuration.getDouble("map.switchLocation.pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public synchronized void setWorldDifficulty(World world, Difficulty difficulty) {
        if (world == null || difficulty == null) {
            return;
        }

        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        configuration.set("worlds." + world.getName() + ".difficulty", difficulty.name());
        save();
        try {
            world.setDifficulty(difficulty);
        } catch (Exception ignored) {
        }
    }

    public synchronized Difficulty getWorldDifficulty(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }

        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        String raw = configuration.getString("worlds." + worldName + ".difficulty", null);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public synchronized void applyStoredWorldDifficulties() {
        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        ConfigurationSection worldsSection = configuration.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return;
        }

        for (String worldName : worldsSection.getKeys(false)) {
            Difficulty difficulty = getWorldDifficulty(worldName);
            if (difficulty == null) {
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                try {
                    world.setDifficulty(difficulty);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public synchronized void applyGameStartLocationToWorld() {
        Location location = getGameStartLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        try {
            location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        } catch (Exception ignored) {
        }
    }

    public void load() {
        loading = true;
        plugin.getLogger().info("[FK][Debug] Loading profile: '" + activeMapProfileName + "' from file: " + dataFile.getAbsolutePath());
        try {
            ensureFileExists();
            configuration = YamlConfiguration.loadConfiguration(dataFile);

            settingsManager.setBaseMessagesEnabled(configuration.getBoolean("settings.baseMessagesEnabled", false));
            if (configuration.contains("settings.chestRoomMessagesEnabled")) {
                settingsManager.setChestRoomMessagesEnabled(configuration.getBoolean("settings.chestRoomMessagesEnabled", true));
            } else {
                settingsManager.setChestRoomMessagesEnabled(configuration.getBoolean("settings.baseMessagesSelfOnly", true));
            }
            settingsManager.setCurrentDay(configuration.getInt("settings.currentDay", 0));
            settingsManager.setNetherCapDay(readActivationDay("settings.netherCapDay", "settings.netherCapEnabled", 0));
            settingsManager.setEndCapDay(readActivationDay("settings.endCapDay", "settings.endCapEnabled", 0));
            settingsManager.setPvpCapDay(readActivationDay("settings.pvpCapDay", "settings.pvpCapEnabled", 0));
            settingsManager.setTntCapDay(readActivationDay("settings.tntCapDay", "settings.tntCapEnabled", 0));
            settingsManager.setFriendlyFireEnabled(readBinarySetting("settings.friendlyFireEnabled", "settings.friendlyFireDay", false));
            settingsManager.setHealthBelowNameEnabled(readBinarySetting("settings.healthBelowNameEnabled", "settings.healthBelowNameDay", false));
            settingsManager.setDayHudEnabled(configuration.getBoolean("settings.dayHudEnabled", true));
            settingsManager.setNightVisionEnabled(configuration.getBoolean("settings.nightVisionEnabled", false));
            settingsManager.setCavePlaceYLimit(configuration.getInt("settings.cavePlaceYLimit", 50));
            settingsManager.setEnderPearlEnabled(configuration.getBoolean("settings.enderPearlEnabled", true));
            settingsManager.setChargedCreeperDropTntEnabled(configuration.getBoolean("settings.chargedCreeperDropTntEnabled", false));
            settingsManager.setChargedCreeperSpawnChance(configuration.getInt("settings.chargedCreeperSpawnChance", 0));
            settingsManager.setTntClimbEnabled(configuration.getBoolean("settings.tntClimbEnabled", true));
            settingsManager.setCompassEnabled(configuration.getBoolean("settings.compassEnabled", false));
            settingsManager.setAutoPauseOnQuitEnabled(configuration.getBoolean("settings.autoPauseOnQuitEnabled", false));
            settingsManager.setBucketAssaultEnabled(configuration.getBoolean("settings.bucketAssaultEnabled", true));
            settingsManager.setNetherAssaultEnabled(configuration.getBoolean("settings.netherAssaultEnabled", true));
            settingsManager.setPotionsEnabled(configuration.getBoolean("settings.potionsEnabled", true));
            settingsManager.setColoredNamesEnabled(configuration.getBoolean("settings.coloredNamesEnabled", true));
            settingsManager.setBaseCaptureTimeSeconds(configuration.getInt("settings.baseCaptureTimeSeconds", 30));
            settingsManager.setBaseCaptureRequiredPlayers(configuration.getInt("settings.baseCaptureRequiredPlayers", 1));
            settingsManager.setHungerConsumptionRate(configuration.getDouble("settings.hungerConsumptionRate", 1.0D));
            if (configuration.contains("settings.deathLimit")) {
                settingsManager.setDeathLimit(configuration.isInt("settings.deathLimit")
                        ? Integer.valueOf(configuration.getInt("settings.deathLimit"))
                        : null);
            } else {
                settingsManager.setDeathLimit(null);
            }

            Set<String> allowedBlocks = new java.util.HashSet<String>();
            for (String materialName : configuration.getStringList("settings.allowedPlaceBlocks")) {
                if (materialName.contains(":")) {
                    org.bukkit.inventory.ItemStack stack = settingsManager.fromBlockKey(materialName);
                    if (stack != null) {
                        allowedBlocks.add(materialName);
                    }
                    continue;
                }
                Material material = Material.getMaterial(materialName);
                if (material != null && material.isBlock()) {
                    allowedBlocks.add(settingsManager.toBlockKey(material, (byte) 0));
                }
            }
            settingsManager.setGloballyAllowedPlaceBlocks(allowedBlocks);

            List<?> serializedLoadout = configuration.getList("settings.baseLoadout");
            java.util.List<org.bukkit.inventory.ItemStack> loadoutItems = new java.util.ArrayList<org.bukkit.inventory.ItemStack>();
            if (serializedLoadout != null) {
                for (Object raw : serializedLoadout) {
                    if (raw instanceof org.bukkit.inventory.ItemStack) {
                        loadoutItems.add(((org.bukkit.inventory.ItemStack) raw).clone());
                    }
                }
            }
            settingsManager.setBaseLoadoutItems(loadoutItems);

            teamManager.clearTeams();
            int nextTeamNumber = configuration.getInt("teams.nextId", 1);

            ConfigurationSection teamsSection = configuration.getConfigurationSection("teams.entries");
            if (teamsSection != null) {
                for (String key : teamsSection.getKeys(false)) {
                    ConfigurationSection teamSection = teamsSection.getConfigurationSection(key);
                    if (teamSection == null) {
                        continue;
                    }

                    int id = Integer.parseInt(key);
                    TeamData team = new TeamData(id, teamSection.getString("name", "Team" + id));
                    team.color = org.bukkit.DyeColor.valueOf(teamSection.getString("color", "WHITE"));
                    team.baseRadius = teamSection.getInt("base.radius", 0);

                    String worldName = teamSection.getString("base.world", null);
                    if (worldName != null) {
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            double x = teamSection.getDouble("base.x", 0.0D);
                            double y = teamSection.getDouble("base.y", 0.0D);
                            double z = teamSection.getDouble("base.z", 0.0D);
                            team.baseCenter = new Location(world, x, y, z);
                        }
                    }

                    for (String memberString : teamSection.getStringList("members")) {
                        try {
                            team.members.add(UUID.fromString(memberString));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    teamManager.getTeams().put(id, team);
                }
            }

            teamManager.setNextTeamNumber(nextTeamNumber);

            if (gameStateManager != null) {
                gameStateManager.setPersistedPlayedMillis(Math.max(0L, configuration.getLong("game.elapsedMillis", 0L)));
                restoreGameState();
            }

            if (gameplayCapListener != null) {
                Map<UUID, Integer> loadedDeaths = new java.util.HashMap<UUID, Integer>();
                ConfigurationSection deathsSection = configuration.getConfigurationSection("deaths.used");
                if (deathsSection != null) {
                    for (String key : deathsSection.getKeys(false)) {
                        try {
                            UUID playerId = UUID.fromString(key);
                            int usedDeaths = Math.max(0, deathsSection.getInt(key, 0));
                            loadedDeaths.put(playerId, Integer.valueOf(usedDeaths));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                gameplayCapListener.setDeathCounts(loadedDeaths);
            }

            if (playerStatsManager != null) {
                Map<UUID, PlayerStatsManager.PlayerStats> loadedStats = new java.util.HashMap<UUID, PlayerStatsManager.PlayerStats>();
                ConfigurationSection statsSection = configuration.getConfigurationSection("playerStats.global");
                if (statsSection == null) {
                    statsSection = configuration.getConfigurationSection("playerStats");
                }
                if (statsSection != null) {
                    for (String key : statsSection.getKeys(false)) {
                        try {
                            UUID playerId = UUID.fromString(key);
                            ConfigurationSection playerSection = statsSection.getConfigurationSection(key);
                            if (playerSection == null) {
                                continue;
                            }
                            PlayerStatsManager.PlayerStats stats = new PlayerStatsManager.PlayerStats();
                            stats.setKills(playerSection.getInt("kills", 0));
                            stats.setDeaths(playerSection.getInt("deaths", 0));
                            stats.setOresMined(playerSection.getInt("oresMined", 0));
                            stats.setAnimalsKilled(playerSection.getInt("animalsKilled", 0));
                            stats.setBasesCaptured(playerSection.getInt("basesCaptured", 0));
                            stats.setGamesWon(playerSection.getInt("gamesWon", 0));
                            loadedStats.put(playerId, stats);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                playerStatsManager.setAllGlobal(loadedStats);
                playerStatsManager.resetAll();
            }

            if (kothManager != null) {
                kothManager.setEnabled(configuration.getBoolean("koth.enabled", false));
                kothManager.setRewardCount(configuration.getInt("koth.rewardCount", 1));
                kothManager.setStartDay(configuration.getInt("koth.startDay", 1));
                kothManager.setEndDay(configuration.getInt("koth.endDay", 10));
                kothManager.setZoneRadius(configuration.getInt("koth.zoneRadius", 0));

                String zoneWorldName = configuration.getString("koth.zoneWorld", null);
                if (zoneWorldName != null) {
                    World world = Bukkit.getWorld(zoneWorldName);
                    if (world != null) {
                        double x = configuration.getDouble("koth.zoneX", 0.0D);
                        double y = configuration.getDouble("koth.zoneY", 0.0D);
                        double z = configuration.getDouble("koth.zoneZ", 0.0D);
                        kothManager.setZoneCenter(new Location(world, x, y, z));
                    }
                }

                List<?> serializedRewards = configuration.getList("koth.rewards");
                java.util.List<org.bukkit.inventory.ItemStack> rewardItems = new java.util.ArrayList<org.bukkit.inventory.ItemStack>();
                if (serializedRewards != null) {
                    for (Object raw : serializedRewards) {
                        if (raw instanceof org.bukkit.inventory.ItemStack) {
                            rewardItems.add(((org.bukkit.inventory.ItemStack) raw).clone());
                        }
                    }
                }
                kothManager.setRewardItems(rewardItems);
            }

            applyGameStartLocationToWorld();
            applyStoredWorldDifficulties();
        } finally {
            loading = false;
        }
    }

    public void save() {
        if (loading) {
            return;
        }

        ensureFileExists();
        if (configuration == null) {
            configuration = new YamlConfiguration();
        }

        configuration.set("settings.baseMessagesEnabled", settingsManager.isBaseMessagesEnabled());
        configuration.set("settings.chestRoomMessagesEnabled", settingsManager.isChestRoomMessagesEnabled());
        configuration.set("settings.netherCapDay", settingsManager.getNetherCapDay());
        configuration.set("settings.endCapDay", settingsManager.getEndCapDay());
        configuration.set("settings.pvpCapDay", settingsManager.getPvpCapDay());
        configuration.set("settings.tntCapDay", settingsManager.getTntCapDay());
        configuration.set("settings.friendlyFireEnabled", settingsManager.isFriendlyFireEnabled());
        configuration.set("settings.healthBelowNameEnabled", settingsManager.isHealthBelowNameEnabled());
        configuration.set("settings.dayHudEnabled", settingsManager.isDayHudEnabled());
        configuration.set("settings.nightVisionEnabled", settingsManager.isNightVisionEnabled());
        configuration.set("settings.cavePlaceYLimit", settingsManager.getCavePlaceYLimit());
        configuration.set("settings.enderPearlEnabled", settingsManager.isEnderPearlEnabled());
        configuration.set("settings.chargedCreeperDropTntEnabled", settingsManager.isChargedCreeperDropTntEnabled());
        configuration.set("settings.chargedCreeperSpawnChance", settingsManager.getChargedCreeperSpawnChance());
        configuration.set("settings.tntClimbEnabled", settingsManager.isTntClimbEnabled());
        configuration.set("settings.compassEnabled", settingsManager.isCompassEnabled());
        configuration.set("settings.autoPauseOnQuitEnabled", settingsManager.isAutoPauseOnQuitEnabled());
        configuration.set("settings.bucketAssaultEnabled", settingsManager.isBucketAssaultEnabled());
        configuration.set("settings.netherAssaultEnabled", settingsManager.isNetherAssaultEnabled());
        configuration.set("settings.potionsEnabled", settingsManager.isPotionsEnabled());
        configuration.set("settings.coloredNamesEnabled", settingsManager.isColoredNamesEnabled());
        configuration.set("settings.baseCaptureTimeSeconds", settingsManager.getBaseCaptureTimeSeconds());
        configuration.set("settings.baseCaptureRequiredPlayers", settingsManager.getBaseCaptureRequiredPlayers());
        configuration.set("settings.hungerConsumptionRate", settingsManager.getHungerConsumptionRate());
        configuration.set("settings.deathLimit", settingsManager.getDeathLimit());
        java.util.List<String> allowedBlocks = new java.util.ArrayList<String>();
        for (String blockKey : settingsManager.getGloballyAllowedPlaceBlocks()) {
            if (blockKey != null && !blockKey.trim().isEmpty()) {
                allowedBlocks.add(blockKey);
            }
        }
        configuration.set("settings.allowedPlaceBlocks", allowedBlocks);

        java.util.List<org.bukkit.inventory.ItemStack> baseLoadoutItems = new java.util.ArrayList<org.bukkit.inventory.ItemStack>();
        for (org.bukkit.inventory.ItemStack item : settingsManager.getBaseLoadoutItems()) {
            if (item != null && item.getType() != null && item.getType() != Material.AIR) {
                baseLoadoutItems.add(item);
            }
        }
        configuration.set("settings.baseLoadout", baseLoadoutItems);
        configuration.set("settings.currentDay", settingsManager.getCurrentDay());
        String persistedGameState = gameStateManager == null ? "stopped" : gameStateManager.getPersistedState();
        configuration.set("game.state", persistedGameState);
        configuration.set("game.running", gameStateManager != null && gameStateManager.isRunning());
        configuration.set("game.paused", gameStateManager != null && gameStateManager.isPaused());
        configuration.set("game.elapsedMillis", gameStateManager == null ? 0L : gameStateManager.getPersistedPlayedMillis());

        configuration.set("deaths.used", null);
        if (gameplayCapListener != null) {
            for (Map.Entry<UUID, Integer> deathEntry : gameplayCapListener.getDeathCountsSnapshot().entrySet()) {
                configuration.set("deaths.used." + deathEntry.getKey().toString(), Math.max(0, deathEntry.getValue().intValue()));
            }
        }

        configuration.set("playerStats", null);
        configuration.set("playerStats.global", null);
        if (playerStatsManager != null) {
            for (Map.Entry<UUID, PlayerStatsManager.PlayerStats> entry : playerStatsManager.getGlobalSnapshot().entrySet()) {
                String path = "playerStats.global." + entry.getKey().toString();
                PlayerStatsManager.PlayerStats stats = entry.getValue();
                configuration.set(path + ".kills", stats.getKills());
                configuration.set(path + ".deaths", stats.getDeaths());
                configuration.set(path + ".oresMined", stats.getOresMined());
                configuration.set(path + ".animalsKilled", stats.getAnimalsKilled());
                configuration.set(path + ".basesCaptured", stats.getBasesCaptured());
                configuration.set(path + ".gamesWon", stats.getGamesWon());
            }
        }

        if (kothManager != null) {
            configuration.set("koth.enabled", kothManager.isEnabled());
            configuration.set("koth.rewardCount", kothManager.getRewardCount());
            configuration.set("koth.startDay", kothManager.getStartDay());
            configuration.set("koth.endDay", kothManager.getEndDay());
            configuration.set("koth.zoneRadius", kothManager.getZoneRadius());

            if (kothManager.getZoneCenter() != null && kothManager.getZoneCenter().getWorld() != null) {
                configuration.set("koth.zoneWorld", kothManager.getZoneCenter().getWorld().getName());
                configuration.set("koth.zoneX", kothManager.getZoneCenter().getX());
                configuration.set("koth.zoneY", kothManager.getZoneCenter().getY());
                configuration.set("koth.zoneZ", kothManager.getZoneCenter().getZ());
            } else {
                configuration.set("koth.zoneWorld", null);
                configuration.set("koth.zoneX", null);
                configuration.set("koth.zoneY", null);
                configuration.set("koth.zoneZ", null);
            }

            java.util.List<org.bukkit.inventory.ItemStack> rewardItems = new java.util.ArrayList<org.bukkit.inventory.ItemStack>();
            for (org.bukkit.inventory.ItemStack item : kothManager.getRewardItems()) {
                if (item != null && item.getType() != null && item.getType() != Material.AIR) {
                    rewardItems.add(item);
                }
            }
            configuration.set("koth.rewards", rewardItems);
        }

        configuration.set("teams.nextId", teamManager.getNextTeamNumber());
        configuration.set("teams.entries", null);

        for (Map.Entry<Integer, TeamData> entry : teamManager.getTeams().entrySet()) {
            TeamData team = entry.getValue();
            String path = "teams.entries." + entry.getKey();
            configuration.set(path + ".name", team.name);
            configuration.set(path + ".color", team.color.name());
            configuration.set(path + ".base.radius", team.baseRadius);
            if (team.baseCenter != null && team.baseCenter.getWorld() != null) {
                configuration.set(path + ".base.world", team.baseCenter.getWorld().getName());
                configuration.set(path + ".base.x", team.baseCenter.getX());
                configuration.set(path + ".base.y", team.baseCenter.getY());
                configuration.set(path + ".base.z", team.baseCenter.getZ());
            } else {
                configuration.set(path + ".base.world", null);
                configuration.set(path + ".base.x", null);
                configuration.set(path + ".base.y", null);
                configuration.set(path + ".base.z", null);
            }
            configuration.set(path + ".members", team.members.stream().map(UUID::toString).toArray(String[]::new));
        }

        try {
            configuration.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save data.yml: " + exception.getMessage());
        }
    }

    private void ensureFileExists() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create data.yml: " + exception.getMessage());
            }
        }
    }

    private String toProfileFileName(String mapName) {
        String normalized = mapName.replaceAll("[^A-Za-z0-9._-]", "_");
        return normalized + ".yml";
    }

    private int readActivationDay(String dayKey, String legacyBoolKey, int defaultDay) {
        if (configuration.contains(dayKey)) {
            return Math.max(0, configuration.getInt(dayKey, defaultDay));
        }

        if (configuration.contains(legacyBoolKey)) {
            return configuration.getBoolean(legacyBoolKey, defaultDay == 0) ? 0 : Integer.MAX_VALUE;
        }

        return defaultDay;
    }

    private boolean readBinarySetting(String boolKey, String legacyDayKey, boolean defaultValue) {
        if (configuration.contains(boolKey)) {
            return configuration.getBoolean(boolKey, defaultValue);
        }

        if (configuration.contains(legacyDayKey)) {
            int day = Math.max(0, configuration.getInt(legacyDayKey, defaultValue ? 0 : Integer.MAX_VALUE));
            return settingsManager.getCurrentDay() >= day;
        }

        return defaultValue;
    }

    private void restoreGameState() {
        if (configuration.contains("game.running") || configuration.contains("game.paused")) {
            boolean running = configuration.getBoolean("game.running", false);
            boolean paused = configuration.getBoolean("game.paused", false);
            if (!running) {
                gameStateManager.restorePersistedState("stopped");
            } else if (paused) {
                gameStateManager.restorePersistedState("paused");
            } else {
                gameStateManager.restorePersistedState("running");
            }
            return;
        }

        gameStateManager.restorePersistedState(configuration.getString("game.state", "stopped"));
    }

    public synchronized void setPlayerLastLocation(UUID playerId, Location location) {
        if (playerId == null || location == null || location.getWorld() == null) {
            return;
        }

        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        String path = "players.lastLocation." + playerId.toString();
        configuration.set(path + ".world", location.getWorld().getName());
        configuration.set(path + ".x", location.getX());
        configuration.set(path + ".y", location.getY());
        configuration.set(path + ".z", location.getZ());
        configuration.set(path + ".yaw", Float.valueOf(location.getYaw()));
        configuration.set(path + ".pitch", Float.valueOf(location.getPitch()));

        try {
            configuration.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save player last location: " + exception.getMessage());
        }
    }

    public synchronized Location getPlayerLastLocation(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        ensureFileExists();
        if (configuration == null) {
            configuration = YamlConfiguration.loadConfiguration(dataFile);
        }

        String path = "players.lastLocation." + playerId.toString();
        String worldName = configuration.getString(path + ".world", null);
        if (worldName == null || worldName.trim().isEmpty()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = new WorldCreator(worldName).createWorld();
            if (world == null) {
                return null;
            }
        }

        double x = configuration.getDouble(path + ".x", world.getSpawnLocation().getX());
        double y = configuration.getDouble(path + ".y", world.getSpawnLocation().getY());
        double z = configuration.getDouble(path + ".z", world.getSpawnLocation().getZ());
        float yaw = (float) configuration.getDouble(path + ".yaw", 0.0D);
        float pitch = (float) configuration.getDouble(path + ".pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
