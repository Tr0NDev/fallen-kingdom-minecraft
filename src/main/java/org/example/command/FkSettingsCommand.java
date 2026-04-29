package org.example.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.example.game.GameStateManager;
import org.example.scoreboard.TeamSidebarManager;
import org.example.settings.SettingsManager;
import org.example.storage.PluginDataStore;
import org.example.stats.PlayerStatsManager;
import org.example.team.TeamData;
import org.example.team.TeamManager;

public class FkSettingsCommand {

    public static final String SETTINGS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bSettings");
    public static final String GLOBAL_STATS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bGlobal Stats");
    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    public static final int SLOT_NETHER_CAP = 10;
    public static final int SLOT_END_CAP = 11;
    public static final int SLOT_PVP_CAP = 12;
    public static final int SLOT_TNT_CAP = 13;
    public static final int SLOT_MAP_SWITCH_POINT = 15;
    public static final int SLOT_BASE_MESSAGES = 19;
    public static final int SLOT_BASE_MESSAGES_SELF_ONLY = 34;
    public static final int SLOT_FRIENDLY_FIRE = 20;
    public static final int SLOT_HEALTH_BELOW_NAME = 21;
    public static final int SLOT_TNT_CLIMB = 22;
    public static final int SLOT_ENDER_PEARL = 23;
    public static final int SLOT_CHARGED_CREEPER_TNT = 24;
    public static final int SLOT_BUCKET_ASSAULT = 25;
    public static final int SLOT_NETHER_ASSAULT = 14;
    public static final int SLOT_AUTO_PAUSE_ON_QUIT = 28;
    public static final int SLOT_POTIONS = 29;
    public static final int SLOT_COMPASS = 30;
    public static final int SLOT_DAY_HUD = 31;
    public static final int SLOT_NIGHT_VISION = 32;
    public static final int SLOT_PLACE_BLOCK_CAVE = 37;
    public static final int SLOT_DEATH_LIMIT = 38;
    public static final int SLOT_HUNGER_RATE = 39;
    public static final int SLOT_BASE_CAPTURE_TIME = 42;
    public static final int SLOT_BASE_CAPTURE_REQUIRED_PLAYERS = 43;
    public static final int SLOT_COLORED_NAMES = 33;
    public static final int SLOT_GAME_START_POINT = 44;
    public static final int SLOT_ALLOWED_BLOCKS = 40;
    public static final int SLOT_BASE_LOADOUT = 41;
    public static final int SLOT_ALLOWED_BLOCKS_BACK = 49;
    public static final int SLOT_BASE_LOADOUT_BACK = 49;
    public static final String ALLOWED_BLOCKS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bAllowed Blocks");
    public static final String BASE_LOADOUT_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bBase Inventory");
        private static final List<Integer> READ_ONLY_LAYOUT_SLOTS = Arrays.asList(
            18, 19, 20, 21,
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 
            45, 46, 47, 48, 49, 50, 51, 52);
        private static final List<Integer> READ_ONLY_SOURCE_ORDER = Arrays.asList(
            SLOT_NETHER_CAP, SLOT_END_CAP, SLOT_PVP_CAP, SLOT_TNT_CAP,
            SLOT_NETHER_ASSAULT,
            SLOT_BASE_MESSAGES, SLOT_BASE_MESSAGES_SELF_ONLY, SLOT_FRIENDLY_FIRE, SLOT_HEALTH_BELOW_NAME,
            SLOT_TNT_CLIMB,
            SLOT_ENDER_PEARL, SLOT_CHARGED_CREEPER_TNT, SLOT_BUCKET_ASSAULT, SLOT_POTIONS,
            SLOT_AUTO_PAUSE_ON_QUIT, SLOT_COMPASS, SLOT_DAY_HUD, SLOT_NIGHT_VISION, SLOT_COLORED_NAMES,
            SLOT_PLACE_BLOCK_CAVE, SLOT_DEATH_LIMIT, SLOT_HUNGER_RATE, SLOT_BASE_CAPTURE_TIME,
            SLOT_BASE_CAPTURE_REQUIRED_PLAYERS,
            SLOT_ALLOWED_BLOCKS, SLOT_BASE_LOADOUT);

    private final SettingsManager settingsManager;
    private final TeamManager teamManager;
    private GameStateManager gameStateManager;
    private PlayerStatsManager playerStatsManager;
    private TeamSidebarManager teamSidebarManager;
    private PluginDataStore dataStore;
    private JavaPlugin plugin;
    private final Set<UUID> readOnlyViewers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> readOnlyAllowedBlocksViewers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> readOnlyBaseLoadoutViewers = java.util.Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Map<UUID, BukkitTask> readOnlyMenuTasks = new ConcurrentHashMap<UUID, BukkitTask>();
        private static final List<Integer> ALLOWED_BLOCK_MENU_SLOTS = Arrays.asList(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43);

    public FkSettingsCommand(SettingsManager settingsManager, TeamManager teamManager) {
        this.settingsManager = settingsManager;
        this.teamManager = teamManager;
    }

    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    public void setPlayerStatsManager(PlayerStatsManager playerStatsManager) {
        this.playerStatsManager = playerStatsManager;
    }

    public void setTeamSidebarManager(TeamSidebarManager teamSidebarManager) {
        this.teamSidebarManager = teamSidebarManager;
    }

    public void setPluginDataStore(PluginDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void setPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void openSettingsMenu(Player player) {
        openSettingsMenu(player, false);
    }

    public void openReadOnlySettingsMenu(Player player) {
        openSettingsMenu(player, true);
    }

    public boolean isReadOnlyViewer(UUID playerId) {
        return readOnlyViewers.contains(playerId);
    }

    private void openSettingsMenu(Player player, boolean readOnly) {
        if (readOnly) {
            readOnlyViewers.add(player.getUniqueId());
        } else {
            readOnlyViewers.remove(player.getUniqueId());
        }

        Inventory menu = Bukkit.createInventory(null, 54, SETTINGS_TITLE);
        if (!readOnly) {
            fillBorder(menu);
            stopReadOnlyMenuLiveUpdates(player.getUniqueId());
        }

        if (readOnly) {
            fillReadOnlySecondRow(menu);
            menu.setItem(0, createReadOnlyClockItem());
            menu.setItem(4, createReadOnlyPlayerHeadItem(player));
            startReadOnlyMenuLiveUpdates(player);
        }

        menu.setItem(getDisplaySlot(SLOT_NETHER_CAP, readOnly), createDayCapItem(Material.OBSIDIAN, "Nether cap", settingsManager.getNetherCapDay(), settingsManager.isNetherCapEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_END_CAP, readOnly), createDayCapItem(Material.ENDER_PORTAL_FRAME, "End cap", settingsManager.getEndCapDay(), settingsManager.isEndCapEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_PVP_CAP, readOnly), createDayCapItem(Material.DIAMOND_SWORD, "PvP cap", settingsManager.getPvpCapDay(), settingsManager.isPvpCapEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_TNT_CAP, readOnly), createDayCapItem(Material.TNT, "Assault cap", settingsManager.getTntCapDay(), settingsManager.isTntCapEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_NETHER_ASSAULT, readOnly), createToggleItem("Nether assault", settingsManager.isNetherAssaultEnabled(), !readOnly));
        if (!readOnly) {
            menu.setItem(SLOT_MAP_SWITCH_POINT, createInfoItem(Material.BEACON, "Map switch point", getMapSwitchPointInfo(), true));
        }

        menu.setItem(getDisplaySlot(SLOT_BASE_MESSAGES, readOnly), createToggleItem("Base message", settingsManager.isBaseMessagesEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_BASE_MESSAGES_SELF_ONLY, readOnly), createToggleItem("Chest room message", settingsManager.isChestRoomMessagesEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_FRIENDLY_FIRE, readOnly), createToggleItem("Friendly fire", settingsManager.isFriendlyFireEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_HEALTH_BELOW_NAME, readOnly), createToggleItem("Health below name", settingsManager.isHealthBelowNameEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_TNT_CLIMB, readOnly), createToggleItem("TNT climb", settingsManager.isTntClimbEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_ENDER_PEARL, readOnly), createToggleItem("Ender pearl", settingsManager.isEnderPearlEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_CHARGED_CREEPER_TNT, readOnly), createToggleItem("Charged creeper TNT drop", settingsManager.isChargedCreeperDropTntEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_BUCKET_ASSAULT, readOnly), createToggleItem("Bucket assault", settingsManager.isBucketAssaultEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_POTIONS, readOnly), createToggleItem("Potions", settingsManager.isPotionsEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_AUTO_PAUSE_ON_QUIT, readOnly), createToggleItem("Auto pause on quit", settingsManager.isAutoPauseOnQuitEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_COMPASS, readOnly), createToggleItem("Compass HUD", settingsManager.isCompassEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_DAY_HUD, readOnly), createToggleItem("Day in hotbar", settingsManager.isDayHudEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_NIGHT_VISION, readOnly), createToggleItem("Night vision", settingsManager.isNightVisionEnabled(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_COLORED_NAMES, readOnly), createToggleItem("Colored names", settingsManager.isColoredNamesEnabled(), !readOnly));
        if (!readOnly) {
            menu.setItem(getDisplaySlot(SLOT_GAME_START_POINT, false), createInfoItem(Material.ENDER_PORTAL_FRAME, "Game start point", getGameStartPointInfo(), true));
        }

        menu.setItem(getDisplaySlot(SLOT_PLACE_BLOCK_CAVE, readOnly), createValueItem("Cave place Y limit", String.valueOf(settingsManager.getCavePlaceYLimit()), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_DEATH_LIMIT, readOnly), createValueItem("Death limit", settingsManager.getDeathLimit() == null ? "NULL" : settingsManager.getDeathLimit().toString(), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_HUNGER_RATE, readOnly), createValueItem("Hunger rate", String.valueOf(settingsManager.getHungerConsumptionRate()), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_BASE_CAPTURE_TIME, readOnly), createValueItem("Base capture time (sec)", String.valueOf(settingsManager.getBaseCaptureTimeSeconds()), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_BASE_CAPTURE_REQUIRED_PLAYERS, readOnly), createValueItem("Base capture players", String.valueOf(settingsManager.getBaseCaptureRequiredPlayers()), !readOnly));
        menu.setItem(getDisplaySlot(SLOT_ALLOWED_BLOCKS, readOnly), createInfoItem(Material.CHEST, "Allowed blocks", settingsManager.getGloballyAllowedPlaceBlocks().size() + " blocks", !readOnly));
        menu.setItem(getDisplaySlot(SLOT_BASE_LOADOUT, readOnly), createInfoItem(Material.CHEST, "Base inventory", settingsManager.getBaseLoadoutItems().size() + " items", !readOnly));

        player.openInventory(menu);
    }

    private void fillReadOnlySecondRow(Inventory menu) {
        ItemStack pane = createBorderItem();
        for (int slot = 9; slot <= 17; slot++) {
            menu.setItem(slot, pane);
        }
    }

    public int setChargedCreeperSpawnChance(int chance) {
        settingsManager.setChargedCreeperSpawnChance(chance);
        return settingsManager.getChargedCreeperSpawnChance();
    }
    private void startReadOnlyMenuLiveUpdates(final Player player) {
        if (plugin == null || player == null) {
            return;
        }

        stopReadOnlyMenuLiveUpdates(player.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                Player online = Bukkit.getPlayer(player.getUniqueId());
                if (online == null || !online.isOnline()) {
                    stopReadOnlyMenuLiveUpdates(player.getUniqueId());
                    return;
                }

                Inventory top = online.getOpenInventory() == null ? null : online.getOpenInventory().getTopInventory();
                if (top == null || !SETTINGS_TITLE.equals(top.getTitle()) || !isReadOnlyViewer(player.getUniqueId())) {
                    stopReadOnlyMenuLiveUpdates(player.getUniqueId());
                    return;
                }

                top.setItem(0, createReadOnlyClockItem());
                top.setItem(4, createReadOnlyPlayerHeadItem(online));
            }
        }, 20L, 20L);
        readOnlyMenuTasks.put(player.getUniqueId(), task);
    }

    public void stopReadOnlyMenuLiveUpdates(UUID playerId) {
        BukkitTask task = readOnlyMenuTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void clearReadOnlyViewer(UUID playerId) {
        readOnlyViewers.remove(playerId);
        stopReadOnlyMenuLiveUpdates(playerId);
    }

    private ItemStack createToggleItem(String name, boolean enabled, boolean showClickHint) {
        ItemStack item = new ItemStack(Material.WOOL, 1, (short) (enabled ? 5 : 14));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Status: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        if (showClickHint) {
            lore.add(ChatColor.YELLOW + "Click to toggle.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDayCapItem(Material material, String name, int activationDay, boolean enabled, boolean showClickHint) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Activates on day: " + ChatColor.YELLOW + activationDay);
        if (showClickHint) {
            lore.add(ChatColor.YELLOW + "Click then type activation day in chat.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createValueItem(String name, String value, boolean showClickHint) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Value: " + ChatColor.YELLOW + value);
        if (showClickHint) {
            lore.add(ChatColor.YELLOW + "Click then type a value in chat.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(Material material, String name, String info, boolean showClickHint) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + name);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + info);
        if (showClickHint) {
            lore.add(ChatColor.YELLOW + "Click to configure.");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReadOnlyPlayerHeadItem(Player player) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player.getName());
        meta.setDisplayName(ChatColor.GOLD + "Stats: " + ChatColor.YELLOW + player.getName());
        List<String> lore = new ArrayList<String>();
        PlayerStatsManager.PlayerStats stats = playerStatsManager == null
                ? new PlayerStatsManager.PlayerStats()
                : playerStatsManager.getPlayerStats(player.getUniqueId());
        lore.add(ChatColor.GRAY + "Kills: " + ChatColor.YELLOW + stats.getKills());
        lore.add(ChatColor.GRAY + "Deaths: " + ChatColor.YELLOW + stats.getDeaths());
        lore.add(ChatColor.GRAY + "Ores mined: " + ChatColor.YELLOW + stats.getOresMined());
        lore.add(ChatColor.GRAY + "Animals killed: " + ChatColor.YELLOW + stats.getAnimalsKilled());
        lore.add(ChatColor.GRAY + "Bases captured: " + ChatColor.YELLOW + stats.getBasesCaptured());
        lore.add(ChatColor.GRAY + "Games won: " + ChatColor.YELLOW + stats.getGamesWon());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openGlobalStatsMenu(Player viewer) {
        if (viewer == null) {
            return;
        }

        if (readOnlyViewers != null) {
            readOnlyViewers.add(viewer.getUniqueId());
        }

        Inventory menu = Bukkit.createInventory(null, 54, GLOBAL_STATS_TITLE);
        ItemStack border = createBorderItem();
        for (int slot = 45; slot < 54; slot++) {
            menu.setItem(slot, border);
        }

        if (playerStatsManager == null) {
            viewer.openInventory(menu);
            viewer.sendMessage(PREFIX + ChatColor.RED + "Stats manager is not available.");
            return;
        }

        Map<String, Map.Entry<UUID, PlayerStatsManager.PlayerStats>> nameToEntry = new java.util.LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerStatsManager.PlayerStats> entry : playerStatsManager.getGlobalSnapshot().entrySet()) {
            String name = resolvePlayerName(entry.getKey());
            if (name == null) name = "Unknown";
            String lower = name.toLowerCase();
            if (!nameToEntry.containsKey(lower)) {
                nameToEntry.put(lower, entry);
            }
        }

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> entries = new ArrayList<>(nameToEntry.values());
        Collections.sort(entries, new Comparator<Map.Entry<UUID, PlayerStatsManager.PlayerStats>>() {
            @Override
            public int compare(Map.Entry<UUID, PlayerStatsManager.PlayerStats> first,
                    Map.Entry<UUID, PlayerStatsManager.PlayerStats> second) {
                String firstName = resolvePlayerName(first.getKey());
                String secondName = resolvePlayerName(second.getKey());
                return firstName.compareToIgnoreCase(secondName);
            }
        });

        int slot = 0;
        for (Map.Entry<UUID, PlayerStatsManager.PlayerStats> entry : entries) {
            if (slot >= 45) {
                break;
            }
            menu.setItem(slot, createGlobalStatsHeadItem(entry.getKey(), entry.getValue()));
            slot++;
        }

        viewer.openInventory(menu);
    }

    private ItemStack createGlobalStatsHeadItem(UUID playerId, PlayerStatsManager.PlayerStats stats) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        String playerName = resolvePlayerName(playerId);
        if (meta != null) {
            if (playerName != null && !playerName.isEmpty()) {
                meta.setOwner(playerName);
            }

            meta.setDisplayName(ChatColor.GOLD + "Stats: " + ChatColor.YELLOW + playerName);
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Kills: " + ChatColor.YELLOW + stats.getKills());
            lore.add(ChatColor.GRAY + "Deaths: " + ChatColor.YELLOW + stats.getDeaths());
            lore.add(ChatColor.GRAY + "Ores mined: " + ChatColor.YELLOW + stats.getOresMined());
            lore.add(ChatColor.GRAY + "Animals killed: " + ChatColor.YELLOW + stats.getAnimalsKilled());
            lore.add(ChatColor.GRAY + "Bases captured: " + ChatColor.YELLOW + stats.getBasesCaptured());
            lore.add(ChatColor.GRAY + "Games won: " + ChatColor.YELLOW + stats.getGamesWon());
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String resolvePlayerName(UUID playerId) {
        if (playerId == null) {
            return "Unknown";
        }

        Player online = Bukkit.getPlayer(playerId);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        if (offline != null && offline.getName() != null && !offline.getName().trim().isEmpty()) {
            return offline.getName();
        }

        String raw = playerId.toString();
        return raw.length() >= 8 ? raw.substring(0, 8) : raw;
    }

    private ItemStack createReadOnlyClockItem() {
        ItemStack item = new ItemStack(Material.WATCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Match timer");
        List<String> lore = new ArrayList<String>();
        String total = gameStateManager == null ? "00:00:00" : gameStateManager.getFormattedTotalMatchTime();
        String nextDay = gameStateManager == null ? "00:20:00" : gameStateManager.getFormattedTimeUntilNextDay();
        lore.add(ChatColor.GRAY + "Total time: " + ChatColor.YELLOW + total);
        lore.add(ChatColor.GRAY + "Before next day: " + ChatColor.YELLOW + nextDay);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public int getDisplaySlot(int baseSlot, boolean readOnly) {
        if (!readOnly) {
            return baseSlot;
        }

        int index = READ_ONLY_SOURCE_ORDER.indexOf(baseSlot);
        if (index < 0 || index >= READ_ONLY_LAYOUT_SLOTS.size()) {
            return baseSlot;
        }

        return READ_ONLY_LAYOUT_SLOTS.get(index);
    }

    public boolean handleToggleClick(Player player, int slot) {
        if (slot == SLOT_BASE_MESSAGES) {
            settingsManager.toggleBaseMessages();
            broadcastSettingChange("Chest Room message: " + (settingsManager.isBaseMessagesEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_FRIENDLY_FIRE) {
            settingsManager.toggleFriendlyFire();
            broadcastSettingChange("Friendly fire: " + (settingsManager.isFriendlyFireEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_BASE_MESSAGES_SELF_ONLY) {
            settingsManager.toggleBaseMessagesSelfOnly();
            broadcastSettingChange("Base message self-only: " + (settingsManager.isBaseMessagesSelfOnly() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_PLACE_BLOCK_CAVE) {
            return false;
        }

        if (slot == SLOT_HEALTH_BELOW_NAME) {
            settingsManager.toggleHealthBelowName();
            broadcastSettingChange("Health below name: " + (settingsManager.isHealthBelowNameEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_ENDER_PEARL) {
            settingsManager.toggleEnderPearl();
            broadcastSettingChange("Ender pearl: " + (settingsManager.isEnderPearlEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_CHARGED_CREEPER_TNT) {
            settingsManager.toggleChargedCreeperDropTnt();
            broadcastSettingChange("Charged creeper TNT drop: " + (settingsManager.isChargedCreeperDropTntEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_TNT_CLIMB) {
            settingsManager.toggleTntClimb();
            broadcastSettingChange("TNT climb: " + (settingsManager.isTntClimbEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_BUCKET_ASSAULT) {
            settingsManager.toggleBucketAssault();
            broadcastSettingChange("Bucket assault: " + (settingsManager.isBucketAssaultEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_NETHER_ASSAULT) {
            settingsManager.toggleNetherAssault();
            broadcastSettingChange("Nether assault: " + (settingsManager.isNetherAssaultEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_POTIONS) {
            settingsManager.togglePotions();
            broadcastSettingChange("Potions: " + (settingsManager.isPotionsEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_AUTO_PAUSE_ON_QUIT) {
            settingsManager.toggleAutoPauseOnQuit();
            broadcastSettingChange("Auto pause on quit: " + (settingsManager.isAutoPauseOnQuitEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_COMPASS) {
            settingsManager.toggleCompass();
            broadcastSettingChange("Compass HUD: " + (settingsManager.isCompassEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_DAY_HUD) {
            settingsManager.toggleDayHud();
            broadcastSettingChange("Day in hotbar: " + (settingsManager.isDayHudEnabled() ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_NIGHT_VISION) {
            settingsManager.toggleNightVision();
            boolean enabled = settingsManager.isNightVisionEnabled();
            if (enabled) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1000000, 0, true, false), true);
                }
            } else {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.removePotionEffect(PotionEffectType.NIGHT_VISION);
                }
            }
            broadcastSettingChange("Night vision: " + (enabled ? "enabled" : "disabled"));
            openSettingsMenu(player);
            return true;
        }

        if (slot == SLOT_COLORED_NAMES) {
            settingsManager.toggleColoredNames();
            broadcastSettingChange("Colored names: " + (settingsManager.isColoredNamesEnabled() ? "enabled" : "disabled"));
            teamManager.updatePlayerDisplayNames();
            if (teamSidebarManager != null) {
                teamSidebarManager.refreshTeams();
            }
            openSettingsMenu(player);
            return true;
        }

        return false;
    }

    public boolean handleGameStartPointClick(Player player, int slot) {
        if (slot != SLOT_GAME_START_POINT) {
            return false;
        }

        if (dataStore == null) {
            return true;
        }

        org.bukkit.Location location = player.getLocation().clone();

        dataStore.setGameStartLocation(location);
        broadcastSettingChange("Game start point set to your current location.");
        openSettingsMenu(player);
        return true;
    }

    public boolean handleMapSwitchPointClick(Player player, int slot) {
        if (slot != SLOT_MAP_SWITCH_POINT) {
            return false;
        }

        if (dataStore == null) {
            return true;
        }

        org.bukkit.Location location = player.getLocation().clone();
        dataStore.setMapSwitchLocation(location);
        broadcastSettingChange("Map switch point set to your current location.");
        openSettingsMenu(player);
        return true;
    }

    private String getGameStartPointInfo() {
        if (dataStore == null) {
            return "Not set";
        }

        org.bukkit.Location location = dataStore.getGameStartLocation();
        if (location == null || location.getWorld() == null) {
            return "Not set";
        }

        return location.getWorld().getName() + " @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private String getMapSwitchPointInfo() {
        if (dataStore == null) {
            return "Not set";
        }

        org.bukkit.Location location = dataStore.getMapSwitchLocation();
        if (location == null || location.getWorld() == null) {
            return "Not set";
        }

        return location.getWorld().getName() + " @ " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public void applyDayToWorlds() {
        long worldTime = (settingsManager.getCurrentDay() * 24000L) + 1000L;
        for (World world : Bukkit.getWorlds()) {
            world.setFullTime(worldTime);
        }
    }

    public void setDayAndRefresh(Player player, int day) {
        setDayInternal(player, day, true);
    }

    public void setDayFromCommand(Player player, int day) {
        setDayInternal(player, day, false);
    }

    private void setDayInternal(Player player, int day, boolean reopenSettingsMenu) {
        int previousDay = settingsManager.getCurrentDay();
        settingsManager.setCurrentDay(day);
        applyDayToWorlds();
        announceActivatedCaps(previousDay, settingsManager.getCurrentDay());
        broadcastSettingChange("Current day is now " + settingsManager.getCurrentDay());
        if (reopenSettingsMenu) {
            openSettingsMenu(player);
        }
    }

    public boolean isDayCapSlot(int slot) {
        return slot == SLOT_NETHER_CAP
                || slot == SLOT_END_CAP
                || slot == SLOT_PVP_CAP
            || slot == SLOT_TNT_CAP;
    }

    public String getCapName(int slot) {
        if (slot == SLOT_NETHER_CAP) {
            return "Nether cap";
        }
        if (slot == SLOT_END_CAP) {
            return "End cap";
        }
        if (slot == SLOT_PVP_CAP) {
            return "PvP cap";
        }
        if (slot == SLOT_TNT_CAP) {
            return "Assault cap";
        }
        return "Unknown cap";
    }

    public void setCapDayAndRefresh(Player player, int slot, int activationDay) {
        if (slot == SLOT_NETHER_CAP) {
            settingsManager.setNetherCapDay(activationDay);
        } else if (slot == SLOT_END_CAP) {
            settingsManager.setEndCapDay(activationDay);
        } else if (slot == SLOT_PVP_CAP) {
            settingsManager.setPvpCapDay(activationDay);
        } else if (slot == SLOT_TNT_CAP) {
            settingsManager.setTntCapDay(activationDay);
        } else {
            return;
        }

        broadcastSettingChange(getCapName(slot) + " activation day: " + Math.max(0, activationDay));
        openSettingsMenu(player);
    }

    public void setCaveLimitAndRefresh(Player player, int yLimit) {
        settingsManager.setCavePlaceYLimit(yLimit);
        broadcastSettingChange("Cave place Y limit: " + settingsManager.getCavePlaceYLimit());
        openSettingsMenu(player);
    }

    public void setDeathLimitAndRefresh(Player player, Integer limit) {
        settingsManager.setDeathLimit(limit);
        broadcastSettingChange("Death limit: " + (limit == null ? "NULL" : limit.toString()));
        openSettingsMenu(player);
    }

    public void setHungerRateAndRefresh(Player player, double rate) {
        settingsManager.setHungerConsumptionRate(rate);
        broadcastSettingChange("Hunger rate: " + settingsManager.getHungerConsumptionRate());
        openSettingsMenu(player);
    }

    public void setBaseCaptureTimeAndRefresh(Player player, int seconds) {
        settingsManager.setBaseCaptureTimeSeconds(seconds);
        broadcastSettingChange("Base capture time: " + settingsManager.getBaseCaptureTimeSeconds() + " seconds");
        openSettingsMenu(player);
    }

    public void setBaseCaptureRequiredPlayersAndRefresh(Player player, int requiredPlayers) {
        settingsManager.setBaseCaptureRequiredPlayers(requiredPlayers);
        broadcastSettingChange("Base capture required players: " + settingsManager.getBaseCaptureRequiredPlayers());
        openSettingsMenu(player);
    }

    public void openAllowedBlocksMenu(Player player) {
        openAllowedBlocksMenu(player, false);
    }

    public void openAllowedBlocksMenu(Player player, boolean readOnly) {
        if (readOnly) {
            readOnlyAllowedBlocksViewers.add(player.getUniqueId());
        } else {
            readOnlyAllowedBlocksViewers.remove(player.getUniqueId());
        }

        Inventory menu = Bukkit.createInventory(null, 54, ALLOWED_BLOCKS_TITLE);
        fillBorder(menu);
        int slotIndex = 0;
        for (String blockKey : settingsManager.getGloballyAllowedPlaceBlocks()) {
            if (slotIndex >= ALLOWED_BLOCK_MENU_SLOTS.size()) {
                break;
            }
            ItemStack stack = settingsManager.fromBlockKey(blockKey);
            if (stack == null) {
                continue;
            }
            menu.setItem(ALLOWED_BLOCK_MENU_SLOTS.get(slotIndex), stack);
            slotIndex++;
        }
        menu.setItem(SLOT_ALLOWED_BLOCKS_BACK, createBackItem());
        player.openInventory(menu);
    }

    public void openBaseLoadoutMenu(Player player) {
        openBaseLoadoutMenu(player, false);
    }

    public void openBaseLoadoutMenu(Player player, boolean readOnly) {
        if (readOnly) {
            readOnlyBaseLoadoutViewers.add(player.getUniqueId());
        } else {
            readOnlyBaseLoadoutViewers.remove(player.getUniqueId());
        }

        Inventory menu = Bukkit.createInventory(null, 54, BASE_LOADOUT_TITLE);
        fillBorder(menu);

        int slotIndex = 0;
        for (ItemStack item : settingsManager.getBaseLoadoutItems()) {
            if (slotIndex >= ALLOWED_BLOCK_MENU_SLOTS.size()) {
                break;
            }
            if (item == null || item.getType() == null || item.getType() == Material.AIR) {
                continue;
            }
            menu.setItem(ALLOWED_BLOCK_MENU_SLOTS.get(slotIndex), item.clone());
            slotIndex++;
        }

        menu.setItem(SLOT_BASE_LOADOUT_BACK, createBackItem());

        player.openInventory(menu);
    }

    public void saveAllowedBlocksFromMenu(Inventory inventory) {
        Set<String> materials = new HashSet<String>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.STAINED_GLASS_PANE) {
                continue;
            }
            if (item.getType() != null && item.getType().isBlock()) {
                materials.add(settingsManager.toBlockKey(item.getType(), item.getData().getData()));
            }
        }
        settingsManager.setGloballyAllowedPlaceBlocks(materials);
    }

    public void saveBaseLoadoutFromMenu(Inventory inventory) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == SLOT_BASE_LOADOUT_BACK) {
                continue;
            }
            ItemStack item = inventory.getItem(slot);
            if (item == null) {
                continue;
            }
            if (item.getType() == Material.STAINED_GLASS_PANE || item.getType() == Material.AIR) {
                continue;
            }
            items.add(item.clone());
        }
        settingsManager.setBaseLoadoutItems(items);
    }

    public boolean isReadOnlyAllowedBlocksViewer(UUID playerId) {
        return readOnlyAllowedBlocksViewers.contains(playerId);
    }

    public boolean isReadOnlyBaseLoadoutViewer(UUID playerId) {
        return readOnlyBaseLoadoutViewers.contains(playerId);
    }

    public void clearReadOnlyInventoryViews(UUID playerId) {
        readOnlyAllowedBlocksViewers.remove(playerId);
        readOnlyBaseLoadoutViewers.remove(playerId);
    }

    public void broadcastSettingChange(String message) {
        Bukkit.broadcastMessage(PREFIX + ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Settings Update: "
                + ChatColor.YELLOW + message);
    }

    public void announceActivatedCaps(int previousDay, int currentDay) {
        if (currentDay <= previousDay) {
            return;
        }

        announceCapIfActivated(previousDay, currentDay, settingsManager.getNetherCapDay(), "Nether cap is now enabled.");
        announceCapIfActivated(previousDay, currentDay, settingsManager.getEndCapDay(), "End cap is now enabled.");
        announceCapIfActivated(previousDay, currentDay, settingsManager.getPvpCapDay(), "PvP is now enabled.");
        if (announceCapIfActivated(previousDay, currentDay, settingsManager.getTntCapDay(), "Assault cap is now enabled.")) {
            eliminateTeamsWithoutChests();
        }
        announceWarningIfNeeded(previousDay, currentDay, settingsManager.getTntCapDay());
    }

    private boolean announceCapIfActivated(int previousDay, int currentDay, int activationDay, String message) {
        if (previousDay < activationDay && currentDay >= activationDay) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.RED + ChatColor.BOLD.toString() + message);
            return true;
        }
        return false;
    }

    private void announceWarningIfNeeded(int previousDay, int currentDay, int assaultCapDay) {
        int warningDay = assaultCapDay - 1;
        if (previousDay < warningDay && currentDay >= warningDay) {
            Bukkit.broadcastMessage(PREFIX + ChatColor.YELLOW + "Warning: If your base does not have a chest before the assault cap, you will be eliminated!");
        }
    }

    private void eliminateTeamsWithoutChests() {
        for (TeamData team : teamManager.getTeams().values()) {
            if (team.baseRadius > 0 && teamManager.getCaptureBounds(team) == null) {
                team.baseCenter = null;
                team.baseRadius = 0;
                teamManager.invalidateCaptureBounds(team.id);
                Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Team "
                        + teamManager.getTeamChatColor(team.color) + team.name
                        + ChatColor.RED + " has been eliminated (no chest in base after assault cap).");
            }
        }
    }

    private void fillBorder(Inventory menu) {
        ItemStack border = createBorderItem();
        int size = menu.getSize();
        int rows = size / 9;

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            boolean isBorder = row == 0 || row == rows - 1 || column == 0 || column == 8;
            if (isBorder) {
                menu.setItem(slot, border);
            }
        }
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GRAY + " ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Back");
        item.setItemMeta(meta);
        return item;
    }

    public void announceAllowedBlocksUpdated(Player player) {
    }
}
