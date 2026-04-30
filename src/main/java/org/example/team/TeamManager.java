package org.example.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Effect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.example.game.GameStateManager;
import org.example.settings.SettingsManager;

public class TeamManager implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final String TEAM_LIST_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bTeams");
    private static final String TEAM_EDITOR_TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bTeam Editor");
    private static final String TEAM_COLORS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bTeam Colors");
    private static final String TEAM_MEMBERS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bTeam Members");
    private static final String TEAM_ADD_MEMBERS_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bAdd Players");
    private static final String TEAM_TRANSFER_CONFIRM_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &cConfirm Transfer");
    private static final String TEAM_DELETE_CONFIRM_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &cConfirm Delete");
    private static final String TEAM_REMOVE_CONFIRM_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &cConfirm Remove");
    private static final String TEAM_BASE_RADIUS_PROMPT = ChatColor.translateAlternateColorCodes('&', "&eType the base radius in chat.");
    private static final int TEAM_MENU_BACK_SLOT = 40;
    private static final int TEAM_LIST_CREATE_SLOT = 40;
    private static final long CAPTURE_BOUNDS_CACHE_MILLIS = 5000L;
    private static final int SHOW_PARTICLE_SECONDS = 10;

    public static class CaptureBounds {
        public final World world;
        public final int minX;
        public final int maxX;
        public final int minY;
        public final int maxY;
        public final int minZ;
        public final int maxZ;

        public CaptureBounds(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    private static class CachedCaptureBounds {
        private final long computedAtMillis;
        private final CaptureBounds bounds;

        private CachedCaptureBounds(long computedAtMillis, CaptureBounds bounds) {
            this.computedAtMillis = computedAtMillis;
            this.bounds = bounds;
        }
    }

    private final JavaPlugin plugin;
    private final SettingsManager settingsManager;
    private final Map<Integer, TeamData> teams = new LinkedHashMap<Integer, TeamData>();
    private final Map<UUID, Integer> renamePending = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> baseRadiusPending = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Location> baseCenterPending = new ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, Integer> editorContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> colorContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> membersContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> addMembersContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> deleteConfirmContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, UUID> transferTargetContext = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Integer> transferOldTeamContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> removeMemberTeamContext = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, UUID> removeMemberTargetContext = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, String> playerNicknames = new ConcurrentHashMap<UUID, String>();
    private final Map<Integer, CachedCaptureBounds> captureBoundsCache = new ConcurrentHashMap<Integer, CachedCaptureBounds>();
    private final Random random = new Random();
    private GameStateManager gameStateManager;
    private Runnable saveAction = new Runnable() {
        @Override
        public void run() {
        }
    };
    private int nextTeamNumber = 1;

    public TeamManager(JavaPlugin plugin, SettingsManager settingsManager) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction == null ? new Runnable() {
            @Override
            public void run() {
            }
        } : saveAction;
    }

    public void setGameStateManager(GameStateManager gameStateManager) {
        this.gameStateManager = gameStateManager;
    }

    public Map<Integer, TeamData> getTeams() {
        return teams;
    }

    public void clearTeams() {
        teams.clear();
        captureBoundsCache.clear();
    }

    public int getNextTeamNumber() {
        return nextTeamNumber;
    }

    public void setNextTeamNumber(int nextTeamNumber) {
        this.nextTeamNumber = nextTeamNumber;
    }

    private void requestSave() {
        saveAction.run();
    }

    private void clearPlayerContexts(UUID playerId) {
        editorContext.remove(playerId);
        colorContext.remove(playerId);
        membersContext.remove(playerId);
        addMembersContext.remove(playerId);
        deleteConfirmContext.remove(playerId);
        baseRadiusPending.remove(playerId);
        baseCenterPending.remove(playerId);
        transferTargetContext.remove(playerId);
        transferOldTeamContext.remove(playerId);
        removeMemberTeamContext.remove(playerId);
        removeMemberTargetContext.remove(playerId);
    }

    public void openTeamListMenu(Player player) {
        clearPlayerContexts(player.getUniqueId());
        Inventory menu = Bukkit.createInventory(null, 54, TEAM_LIST_TITLE);
        fillBorder(menu);

        List<Integer> contentSlots = getInnerSlots54(TEAM_LIST_CREATE_SLOT);

        int slotIndex = 0;
        for (TeamData team : teams.values()) {
            if (slotIndex >= contentSlots.size()) {
                break;
            }

            ItemStack teamItem = new ItemStack(Material.WOOL, 1, team.color.getWoolData());
            ItemMeta teamMeta = teamItem.getItemMeta();
            teamMeta.setDisplayName(getTeamColor(team.color) + team.name);
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Members: " + team.members.size());
            if (team.baseRadius > 0 && team.baseCenter != null) {
                lore.add(ChatColor.GRAY + "Base: " + team.baseRadius + " blocks");
                lore.add(ChatColor.GRAY + "Center: " + team.baseCenter.getBlockX() + ", " + team.baseCenter.getBlockY() + ", " + team.baseCenter.getBlockZ());
            } else {
                lore.add(ChatColor.GRAY + "Base: Not set");
            }
            lore.add(ChatColor.YELLOW + "Click to edit this team.");
            teamMeta.setLore(lore);
            teamItem.setItemMeta(teamMeta);
            menu.setItem(contentSlots.get(slotIndex), teamItem);
            slotIndex++;
        }

        ItemStack createTeamItem = new ItemStack(Material.PAPER, 1);
        ItemMeta createTeamMeta = createTeamItem.getItemMeta();
        createTeamMeta.setDisplayName(ChatColor.GREEN + "Create a team");
        createTeamItem.setItemMeta(createTeamMeta);
        menu.setItem(TEAM_LIST_CREATE_SLOT, createTeamItem);

        ItemStack randomizeItem = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta randomizeMeta = randomizeItem.getItemMeta();
        randomizeMeta.setDisplayName(ChatColor.AQUA + "Randomize this world");
        List<String> randomizeLore = new ArrayList<String>();
        randomizeLore.add(ChatColor.GRAY + "Assign players from your current world");
        randomizeLore.add(ChatColor.GRAY + "into the available teams.");
        randomizeLore.add(ChatColor.YELLOW + "Click to randomize.");
        randomizeMeta.setLore(randomizeLore);
        randomizeItem.setItemMeta(randomizeMeta);
        menu.setItem(42, randomizeItem);

        player.openInventory(menu);
    }

    private void openTeamEditor(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        editorContext.put(player.getUniqueId(), teamId);

        Inventory menu = Bukkit.createInventory(null, 54, getTeamEditorTitle(team));
        fillBorder(menu);

        ItemStack renameItem = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta renameMeta = renameItem.getItemMeta();
        renameMeta.setDisplayName(ChatColor.YELLOW + "Rename team");
        List<String> renameLore = new ArrayList<String>();
        renameLore.add(ChatColor.GRAY + "Current name: " + getTeamColor(team.color) + team.name);
        renameLore.add(ChatColor.YELLOW + "Click to type a new name in chat.");
        renameMeta.setLore(renameLore);
        renameItem.setItemMeta(renameMeta);
        menu.setItem(11, renameItem);

        ItemStack colorItem = new ItemStack(Material.WOOL, 1, team.color.getWoolData());
        ItemMeta colorMeta = colorItem.getItemMeta();
        colorMeta.setDisplayName(ChatColor.YELLOW + "Team color");
        List<String> colorLore = new ArrayList<String>();
        colorLore.add(ChatColor.GRAY + "Current: " + getTeamColor(team.color) + capitalize(team.color.name()));
        colorLore.add(ChatColor.YELLOW + "Click to change.");
        colorMeta.setLore(colorLore);
        colorItem.setItemMeta(colorMeta);
        menu.setItem(13, colorItem);

        ItemStack membersItem = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta membersMeta = membersItem.getItemMeta();
        membersMeta.setDisplayName(ChatColor.YELLOW + "Members");
        List<String> membersLore = new ArrayList<String>();
        membersLore.add(ChatColor.GRAY + "View team members.");
        membersMeta.setLore(membersLore);
        membersItem.setItemMeta(membersMeta);
        menu.setItem(15, membersItem);

        ItemStack flagItem = new ItemStack(Material.BANNER, 1);
        ItemMeta flagMeta = flagItem.getItemMeta();
        flagMeta.setDisplayName(ChatColor.YELLOW + "Set base");
        List<String> flagLore = new ArrayList<String>();
        if (team.baseRadius > 0 && team.baseCenter != null) {
            flagLore.add(ChatColor.GRAY + "Radius: " + team.baseRadius);
            flagLore.add(ChatColor.GRAY + "Center: " + team.baseCenter.getBlockX() + ", " + team.baseCenter.getBlockY() + ", " + team.baseCenter.getBlockZ());
        } else {
            flagLore.add(ChatColor.GRAY + "Radius: None");
        }
        flagLore.add(ChatColor.YELLOW + "Click, then type the radius in chat.");
        flagMeta.setLore(flagLore);
        flagItem.setItemMeta(flagMeta);
        menu.setItem(22, flagItem);

        ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL, 1);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        teleportMeta.setDisplayName(ChatColor.YELLOW + "Teleport to base");
        List<String> teleportLore = new ArrayList<String>();
        if (team.baseCenter != null && team.baseCenter.getWorld() != null && team.baseRadius > 0) {
            teleportLore.add(ChatColor.GRAY + "World: " + team.baseCenter.getWorld().getName());
            teleportLore.add(ChatColor.GRAY + "Center: " + team.baseCenter.getBlockX() + ", "
                    + team.baseCenter.getBlockY() + ", " + team.baseCenter.getBlockZ());
            teleportLore.add(ChatColor.YELLOW + "Click to teleport.");
        } else {
            teleportLore.add(ChatColor.RED + "Base is not set for this team.");
        }
        teleportMeta.setLore(teleportLore);
        teleportItem.setItemMeta(teleportMeta);
        menu.setItem(20, teleportItem);

        ItemStack addPlayerItem = new ItemStack(Material.PAPER, 1);
        ItemMeta addPlayerMeta = addPlayerItem.getItemMeta();
        addPlayerMeta.setDisplayName(ChatColor.YELLOW + "Add player");
        List<String> addPlayerLore = new ArrayList<String>();
        addPlayerLore.add(ChatColor.GRAY + "Open player list to add members.");
        addPlayerMeta.setLore(addPlayerLore);
        addPlayerItem.setItemMeta(addPlayerMeta);
        menu.setItem(24, addPlayerItem);

        ItemStack deleteItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.RED + "Delete team");
        deleteItem.setItemMeta(deleteMeta);
        menu.setItem(31, deleteItem);

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(TEAM_MENU_BACK_SLOT, backItem);

        player.openInventory(menu);
    }

    private String getTeamEditorTitle(TeamData team) {
        return TEAM_EDITOR_TITLE_PREFIX + ChatColor.GRAY + " - " + getTeamColor(team.color) + team.name;
    }

    private void promptBaseRadius(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        baseRadiusPending.put(player.getUniqueId(), teamId);
        baseCenterPending.put(player.getUniqueId(), player.getLocation().clone());
        player.closeInventory();
        player.sendMessage(PREFIX + ChatColor.YELLOW + TEAM_BASE_RADIUS_PROMPT);
    }

    private void openTeamColorsMenu(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        colorContext.put(player.getUniqueId(), teamId);

        Inventory menu = Bukkit.createInventory(null, 54, TEAM_COLORS_TITLE);
        fillBorder(menu);
        List<Integer> contentSlots = getInnerSlots54(TEAM_MENU_BACK_SLOT);
        DyeColor[] allColors = DyeColor.values();

        int slotIndex = 0;
        for (DyeColor color : allColors) {
            if (slotIndex >= contentSlots.size()) {
                break;
            }

            ItemStack wool = new ItemStack(Material.WOOL, 1, color.getWoolData());
            ItemMeta meta = wool.getItemMeta();
            meta.setDisplayName(getTeamColor(color) + capitalize(color.name()));
            wool.setItemMeta(meta);
            menu.setItem(contentSlots.get(slotIndex), wool);
            slotIndex++;
        }

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(TEAM_MENU_BACK_SLOT, backItem);

        player.openInventory(menu);
    }

    private void openMembersMenu(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        membersContext.put(player.getUniqueId(), teamId);

        Inventory menu = Bukkit.createInventory(null, 54, TEAM_MEMBERS_TITLE);
        fillBorder(menu);
        List<Integer> contentSlots = getInnerSlots54(TEAM_MENU_BACK_SLOT);

        int slotIndex = 0;
        for (UUID memberId : team.members) {
            if (slotIndex >= contentSlots.size()) {
                break;
            }

            Player member = Bukkit.getPlayer(memberId);
            String memberName = member != null ? member.getName() : Bukkit.getOfflinePlayer(memberId).getName();
            if (memberName == null) {
                memberName = memberId.toString();
            }

            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwner(memberName);
            meta.setDisplayName(ChatColor.AQUA + memberName);
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.RED + "Click to remove from team");
            meta.setLore(lore);
            skull.setItemMeta(meta);
            menu.setItem(contentSlots.get(slotIndex), skull);
            slotIndex++;
        }

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(TEAM_MENU_BACK_SLOT, backItem);

        player.openInventory(menu);
    }

    private void openAddMembersMenu(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        addMembersContext.put(player.getUniqueId(), teamId);

        Inventory menu = Bukkit.createInventory(null, 54, TEAM_ADD_MEMBERS_TITLE);
        fillBorder(menu);
        List<Integer> contentSlots = getInnerSlots54(TEAM_MENU_BACK_SLOT);

        int slotIndex = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slotIndex >= contentSlots.size()) {
                break;
            }

            if (team.members.contains(onlinePlayer.getUniqueId())) {
                continue;
            }

            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwner(onlinePlayer.getName());
            meta.setDisplayName(ChatColor.GREEN + onlinePlayer.getName());

            TeamData currentTeam = getTeamByPlayer(onlinePlayer.getUniqueId());
            List<String> lore = new ArrayList<String>();
            if (currentTeam == null) {
                lore.add(ChatColor.GRAY + "Current team: None");
                lore.add(ChatColor.YELLOW + "Click to add to this team.");
            } else {
                lore.add(ChatColor.GRAY + "Current team: " + getTeamColor(currentTeam.color) + currentTeam.name);
                lore.add(ChatColor.RED + "Click to transfer this player.");
            }
            meta.setLore(lore);

            skull.setItemMeta(meta);
            menu.setItem(contentSlots.get(slotIndex), skull);
            slotIndex++;
        }

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(TEAM_MENU_BACK_SLOT, backItem);

        player.openInventory(menu);
    }

    private void openTransferConfirmMenu(Player player, int destinationTeamId, UUID targetId, int oldTeamId) {
        TeamData destination = teams.get(destinationTeamId);
        TeamData oldTeam = teams.get(oldTeamId);
        Player target = Bukkit.getPlayer(targetId);

        if (destination == null || oldTeam == null || target == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Transfer target is no longer available.");
            openAddMembersMenu(player, destinationTeamId);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        addMembersContext.put(player.getUniqueId(), destinationTeamId);
        transferTargetContext.put(player.getUniqueId(), targetId);
        transferOldTeamContext.put(player.getUniqueId(), oldTeamId);

        Inventory menu = Bukkit.createInventory(null, 27, TEAM_TRANSFER_CONFIRM_TITLE);
        fillBorder(menu);

        ItemStack yesItem = new ItemStack(Material.WOOL, 1, DyeColor.LIME.getWoolData());
        ItemMeta yesMeta = yesItem.getItemMeta();
        yesMeta.setDisplayName(ChatColor.GREEN + "Yes, transfer player");
        List<String> yesLore = new ArrayList<String>();
        yesLore.add(ChatColor.GRAY + target.getName() + " -> " + getTeamColor(destination.color) + destination.name);
        yesMeta.setLore(yesLore);
        yesItem.setItemMeta(yesMeta);
        menu.setItem(11, yesItem);

        ItemStack noItem = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
        ItemMeta noMeta = noItem.getItemMeta();
        noMeta.setDisplayName(ChatColor.RED + "No, cancel");
        noItem.setItemMeta(noMeta);
        menu.setItem(15, noItem);

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(13, backItem);

        player.openInventory(menu);
    }

    private void openRemoveConfirmMenu(Player player, int teamId, UUID targetId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String targetName = target.getName() == null ? targetId.toString() : target.getName();

        clearPlayerContexts(player.getUniqueId());
        removeMemberTeamContext.put(player.getUniqueId(), teamId);
        removeMemberTargetContext.put(player.getUniqueId(), targetId);

        Inventory menu = Bukkit.createInventory(null, 27, TEAM_REMOVE_CONFIRM_TITLE);
        fillBorder(menu);

        ItemStack yesItem = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
        ItemMeta yesMeta = yesItem.getItemMeta();
        yesMeta.setDisplayName(ChatColor.RED + "Yes, remove player");
        List<String> yesLore = new ArrayList<String>();
        yesLore.add(ChatColor.GRAY + "Remove " + targetName + " from " + getTeamColor(team.color) + team.name);
        yesMeta.setLore(yesLore);
        yesItem.setItemMeta(yesMeta);
        menu.setItem(11, yesItem);

        ItemStack noItem = new ItemStack(Material.WOOL, 1, DyeColor.LIME.getWoolData());
        ItemMeta noMeta = noItem.getItemMeta();
        noMeta.setDisplayName(ChatColor.GREEN + "No, keep player");
        noItem.setItemMeta(noMeta);
        menu.setItem(15, noItem);

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(13, backItem);

        player.openInventory(menu);
    }

    private void openDeleteConfirmMenu(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        clearPlayerContexts(player.getUniqueId());
        deleteConfirmContext.put(player.getUniqueId(), teamId);

        Inventory menu = Bukkit.createInventory(null, 27, TEAM_DELETE_CONFIRM_TITLE);
        fillBorder(menu);

        ItemStack yesItem = new ItemStack(Material.WOOL, 1, DyeColor.RED.getWoolData());
        ItemMeta yesMeta = yesItem.getItemMeta();
        yesMeta.setDisplayName(ChatColor.RED + "Yes, delete team");
        List<String> yesLore = new ArrayList<String>();
        yesLore.add(ChatColor.GRAY + "This will permanently delete " + getTeamColor(team.color) + team.name + ChatColor.GRAY + ".");
        yesMeta.setLore(yesLore);
        yesItem.setItemMeta(yesMeta);
        menu.setItem(11, yesItem);

        ItemStack noItem = new ItemStack(Material.WOOL, 1, DyeColor.LIME.getWoolData());
        ItemMeta noMeta = noItem.getItemMeta();
        noMeta.setDisplayName(ChatColor.GREEN + "No, keep team");
        noItem.setItemMeta(noMeta);
        menu.setItem(15, noItem);

        ItemStack backItem = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "Back");
        backItem.setItemMeta(backMeta);
        menu.setItem(13, backItem);

        player.openInventory(menu);
    }

    private ChatColor getTeamColor(DyeColor color) {
        switch (color) {
            case BLACK:
                return ChatColor.DARK_GRAY;
            case BLUE:
                return ChatColor.BLUE;
            case BROWN:
                return ChatColor.GOLD;
            case CYAN:
                return ChatColor.DARK_AQUA;
            case GRAY:
                return ChatColor.GRAY;
            case GREEN:
                return ChatColor.DARK_GREEN;
            case LIGHT_BLUE:
                return ChatColor.AQUA;
            case LIME:
                return ChatColor.GREEN;
            case MAGENTA:
                return ChatColor.LIGHT_PURPLE;
            case ORANGE:
                return ChatColor.GOLD;
            case PINK:
                return ChatColor.LIGHT_PURPLE;
            case PURPLE:
                return ChatColor.DARK_PURPLE;
            case RED:
                return ChatColor.RED;
            case SILVER:
                return ChatColor.WHITE;
            case WHITE:
                return ChatColor.WHITE;
            case YELLOW:
                return ChatColor.YELLOW;
            default:
                return ChatColor.WHITE;
        }
    }

    public ChatColor getTeamChatColor(DyeColor color) {
        return getTeamColor(color);
    }

    private String capitalize(String text) {
        String lower = text.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private DyeColor getRandomTeamColor() {
        DyeColor[] colors = DyeColor.values();
        return colors[random.nextInt(colors.length)];
    }

    public TeamData getTeamByPlayer(UUID playerId) {
        for (TeamData team : teams.values()) {
            if (team.members.contains(playerId)) {
                return team;
            }
        }
        return null;
    }

    public boolean isInsideTeamBase(Player player, Location location) {
        TeamData team = getTeamByPlayer(player.getUniqueId());
        return isInsideBaseSquare(location, team, 0.0D);
    }

    private boolean isInsideBaseSquare(Location location, TeamData team, double extraDistance) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        if (team == null || team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
            return false;
        }

        if (!location.getWorld().equals(team.baseCenter.getWorld())) {
            return false;
        }

        double halfSide = team.baseRadius + Math.max(0.0D, extraDistance);
        double deltaX = Math.abs(location.getBlockX() - team.baseCenter.getBlockX());
        double deltaZ = Math.abs(location.getBlockZ() - team.baseCenter.getBlockZ());
        return deltaX <= halfSide && deltaZ <= halfSide;
    }

    private void addPlayerToTeam(TeamData selectedTeam, UUID playerId) {
        TeamData oldTeam = getTeamByPlayer(playerId);
        if (oldTeam != null) {
            oldTeam.members.remove(playerId);
        }
        selectedTeam.members.add(playerId);
        requestSave();
    }

    private void removePlayerFromTeam(TeamData team, UUID playerId) {
        team.members.remove(playerId);
        requestSave();
    }

    public boolean assignOnlinePlayersRandomlyBalanced() {
        return assignOnlinePlayersRandomlyBalanced(null);
    }

    public boolean assignOnlinePlayersRandomlyBalanced(World world) {
        if (teams.isEmpty()) {
            return false;
        }

        List<Player> onlinePlayers = new ArrayList<Player>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            return false;
        }

        if (world != null) {
            List<Player> worldPlayers = new ArrayList<Player>();
            for (Player onlinePlayer : onlinePlayers) {
                if (onlinePlayer != null && onlinePlayer.getWorld() != null && onlinePlayer.getWorld().equals(world)) {
                    worldPlayers.add(onlinePlayer);
                }
            }
            onlinePlayers = worldPlayers;
        }

        if (onlinePlayers.isEmpty()) {
            return false;
        }

        java.util.Set<UUID> onlineIds = new java.util.HashSet<UUID>();
        for (Player onlinePlayer : onlinePlayers) {
            if (onlinePlayer != null) {
                onlineIds.add(onlinePlayer.getUniqueId());
            }
        }

        for (TeamData team : teams.values()) {
            team.members.removeIf(onlineIds::contains);
        }

        List<TeamData> availableTeams = new ArrayList<TeamData>(teams.values());
        Collections.shuffle(onlinePlayers, random);
        Collections.shuffle(availableTeams, random);

        int teamCount = availableTeams.size();
        for (int index = 0; index < onlinePlayers.size(); index++) {
            Player onlinePlayer = onlinePlayers.get(index);
            if (onlinePlayer == null) {
                continue;
            }

            TeamData targetTeam = availableTeams.get(index % teamCount);
            targetTeam.members.add(onlinePlayer.getUniqueId());
        }

        requestSave();
        return true;
    }

    private UUID findTeamMemberIdByName(TeamData team, String targetName) {
        if (team == null || targetName == null) {
            return null;
        }

        String normalized = targetName.trim();
        for (UUID memberId : team.members) {
            Player online = Bukkit.getPlayer(memberId);
            String memberName = online != null ? online.getName() : Bukkit.getOfflinePlayer(memberId).getName();
            if (memberName != null && memberName.equalsIgnoreCase(normalized)) {
                return memberId;
            }
            if (memberName == null && memberId.toString().equalsIgnoreCase(normalized)) {
                return memberId;
            }
        }

        return null;
    }

    private TeamData getTeamBySlot(int slot) {
        int index = 0;
        for (TeamData team : teams.values()) {
            if (index == slot) {
                return team;
            }
            index++;
        }
        return null;
    }

    public void handleChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final Integer teamId = renamePending.remove(player.getUniqueId());
        final Integer baseTeamId = baseRadiusPending.remove(player.getUniqueId());
        final Location baseCenter = baseCenterPending.remove(player.getUniqueId());

        if (teamId == null && baseTeamId == null) {
            return;
        }

        event.setCancelled(true);
        event.getRecipients().clear();
        final String newName = event.getMessage().trim();

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (baseTeamId != null) {
                    TeamData team = teams.get(baseTeamId);
                    if (team == null) {
                        player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
                        openTeamListMenu(player);
                        return;
                    }

                    try {
                        int radius = Integer.parseInt(newName);
                        if (radius <= 0) {
                            player.sendMessage(PREFIX + ChatColor.RED + "Radius must be greater than 0.");
                            openTeamEditor(player, baseTeamId);
                            return;
                        }

                        team.baseCenter = baseCenter == null ? null : baseCenter.clone();
                        team.baseRadius = radius;
                        invalidateCaptureBounds(team.id);
                        requestSave();
                        player.sendMessage(PREFIX + ChatColor.GREEN + "Base radius set to " + radius + " blocks.");
                        openTeamEditor(player, baseTeamId);
                    } catch (NumberFormatException exception) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Please type a valid number.");
                        openTeamEditor(player, baseTeamId);
                    }
                    return;
                }

                TeamData team = teams.get(teamId);
                if (team == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
                    openTeamListMenu(player);
                    return;
                }

                if (newName.isEmpty()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Team name cannot be empty.");
                    openTeamEditor(player, teamId);
                    return;
                }

                team.name = ChatColor.stripColor(newName);
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Team renamed to " + team.name + ".");
                openTeamEditor(player, teamId);
            }
        });

    }

    public void updatePlayerDisplayNames() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String baseName = getPlayerBaseName(player);
            TeamData team = getTeamByPlayer(player.getUniqueId());
            if (team != null && settingsManager.isColoredNamesEnabled()) {
                ChatColor color = getTeamChatColor(team.color);
                String coloredName = color + baseName;
                player.setDisplayName(coloredName);
                player.setPlayerListName(coloredName);
                player.setCustomName(coloredName);
                player.setCustomNameVisible(true);
                applyNmsCustomName(player, coloredName);
            } else {
                player.setDisplayName(baseName);
                player.setPlayerListName(baseName);
                player.setCustomName(baseName);
                player.setCustomNameVisible(true);
                applyNmsCustomName(player, baseName);
            }
        }
    }

    public void setPlayerNickname(UUID playerId, String nickname) {
        if (playerId == null) {
            return;
        }

        if (nickname == null || nickname.trim().isEmpty()) {
            playerNicknames.remove(playerId);
            return;
        }

        playerNicknames.put(playerId, nickname);
    }

    public String getPlayerNickname(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return playerNicknames.get(playerId);
    }

    public String getPlayerBaseName(Player player) {
        if (player == null) {
            return "";
        }
        String nickname = getPlayerNickname(player.getUniqueId());
        if (nickname == null || nickname.trim().isEmpty()) {
            return player.getName();
        }
        return nickname;
    }

    public void refreshPlayerAppearance(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }

            online.hidePlayer(player);
            online.showPlayer(player);
        }
    }

    private void applyNmsCustomName(Player player, String customName) {
        try {
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            Object handle = getHandleMethod.invoke(player);
            if (handle == null) {
                return;
            }

            Method setCustomName = handle.getClass().getMethod("setCustomName", String.class);
            Method setCustomNameVisible = handle.getClass().getMethod("setCustomNameVisible", boolean.class);
            setCustomName.invoke(handle, customName);
            setCustomNameVisible.invoke(handle, Boolean.TRUE.booleanValue());
        } catch (Exception exception) {
            //prout
        }
    }

    public void formatChat(AsyncPlayerChatEvent event) {
        TeamData team = getTeamByPlayer(event.getPlayer().getUniqueId());
        if (team == null) {
            return;
        }

        String message = event.getMessage();
        String teamPrefix = getTeamColor(team.color) + "[" + team.name + "] " + ChatColor.RESET;

        event.setCancelled(true);

        if (message.startsWith("!")) {
            String globalMessage = message.length() > 1 ? message.substring(1).trim() : "";
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Global] " + teamPrefix + getPlayerBaseName(event.getPlayer())
                    + ChatColor.GRAY + ": " + ChatColor.WHITE + globalMessage);
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            TeamData recipientTeam = getTeamByPlayer(onlinePlayer.getUniqueId());
            if (recipientTeam != null && recipientTeam.id == team.id) {
                onlinePlayer.sendMessage(ChatColor.GREEN + "[Team] " + teamPrefix + getPlayerBaseName(event.getPlayer())
                        + ChatColor.GRAY + ": " + ChatColor.WHITE + message);
            }
        }
    }

    public void handleMenuClick(InventoryClickEvent event) {
        if (event.getWhoClicked() == null || event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getInventory().getTitle();

        if (!TEAM_LIST_TITLE.equals(title)
            && !title.startsWith(TEAM_EDITOR_TITLE_PREFIX)
            && !TEAM_COLORS_TITLE.equals(title)
            && !TEAM_MEMBERS_TITLE.equals(title)
            && !TEAM_ADD_MEMBERS_TITLE.equals(title)
            && !TEAM_TRANSFER_CONFIRM_TITLE.equals(title)
            && !TEAM_DELETE_CONFIRM_TITLE.equals(title)
            && !TEAM_REMOVE_CONFIRM_TITLE.equals(title)) {
            return;
        }

        event.setCancelled(true);

        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
            return;
        }

        if (TEAM_LIST_TITLE.equals(title)) {
            if (event.getRawSlot() == TEAM_LIST_CREATE_SLOT) {
                int teamId = nextTeamNumber;
                TeamData team = new TeamData(teamId, "Team" + teamId);
                team.color = getRandomTeamColor();
                teams.put(teamId, team);
                nextTeamNumber++;
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Created " + team.name + ".");
                openTeamEditor(player, teamId);
            } else if (event.getRawSlot() == 42) {
                boolean assigned = assignOnlinePlayersRandomlyBalanced(player.getWorld());
                if (assigned) {
                    updatePlayerDisplayNames();
                    requestSave();
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Players from this world were randomized into teams.");
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "No players in this world could be randomized.");
                }
            } else {
                List<Integer> contentSlots = getInnerSlots54(TEAM_LIST_CREATE_SLOT);
                int teamIndex = contentSlots.indexOf(event.getRawSlot());
                TeamData team = teamIndex < 0 ? null : getTeamBySlot(teamIndex);
                if (team != null) {
                    openTeamEditor(player, team.id);
                }
            }
            return;
        }

        if (title.startsWith(TEAM_EDITOR_TITLE_PREFIX)) {
            Integer teamId = editorContext.get(player.getUniqueId());
            if (teamId == null || !teams.containsKey(teamId)) {
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == 11) {
                renamePending.put(player.getUniqueId(), teamId);
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type the team name in chat.");
                return;
            }

            if (event.getRawSlot() == 13) {
                openTeamColorsMenu(player, teamId);
                return;
            }

            if (event.getRawSlot() == 15) {
                openMembersMenu(player, teamId);
                return;
            }

            if (event.getRawSlot() == 22) {
                promptBaseRadius(player, teamId);
                return;
            }

            if (event.getRawSlot() == 20) {
                teleportToTeamBase(player, teamId);
                return;
            }

            if (event.getRawSlot() == 24) {
                openAddMembersMenu(player, teamId);
                return;
            }

            if (event.getRawSlot() == 31) {
                openDeleteConfirmMenu(player, teamId);
                return;
            }

            if (event.getRawSlot() == TEAM_MENU_BACK_SLOT) {
                openTeamListMenu(player);
            }
            return;
        }

        if (TEAM_COLORS_TITLE.equals(title)) {
            Integer teamId = colorContext.get(player.getUniqueId());
            TeamData team = teamId == null ? null : teams.get(teamId);
            if (team == null) {
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == TEAM_MENU_BACK_SLOT) {
                openTeamEditor(player, teamId);
                return;
            }

            if (event.getRawSlot() >= 0 && event.getRawSlot() < 54 && event.getCurrentItem() != null
                    && event.getCurrentItem().getType() == Material.WOOL) {
                team.color = DyeColor.getByWoolData(event.getCurrentItem().getData().getData());
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Team color updated.");
                openTeamEditor(player, teamId);
            }
            return;
        }

        if (TEAM_MEMBERS_TITLE.equals(title)) {
            Integer teamId = membersContext.get(player.getUniqueId());
            if (teamId == null || !teams.containsKey(teamId)) {
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == TEAM_MENU_BACK_SLOT) {
                openTeamEditor(player, teamId);
                return;
            }

            if (event.getRawSlot() >= 0 && event.getRawSlot() < 54 && event.getCurrentItem() != null
                    && event.getCurrentItem().getType() == Material.SKULL_ITEM) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta == null || meta.getDisplayName() == null) {
                    return;
                }

                String targetName = ChatColor.stripColor(meta.getDisplayName());
                TeamData team = teams.get(teamId);
                UUID targetId = findTeamMemberIdByName(team, targetName);
                if (targetId == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "This player is no longer in the team.");
                    openMembersMenu(player, teamId);
                    return;
                }

                openRemoveConfirmMenu(player, teamId, targetId);
            }
            return;
        }

        if (TEAM_ADD_MEMBERS_TITLE.equals(title)) {
            Integer teamId = addMembersContext.get(player.getUniqueId());
            TeamData team = teamId == null ? null : teams.get(teamId);
            if (team == null) {
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == TEAM_MENU_BACK_SLOT) {
                openTeamEditor(player, teamId);
                return;
            }

            if (event.getRawSlot() >= 0 && event.getRawSlot() < 54 && event.getCurrentItem() != null
                    && event.getCurrentItem().getType() == Material.SKULL_ITEM) {
                ItemMeta meta = event.getCurrentItem().getItemMeta();
                if (meta == null || meta.getDisplayName() == null) {
                    return;
                }

                String targetName = ChatColor.stripColor(meta.getDisplayName());
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "This player is no longer online.");
                    openAddMembersMenu(player, teamId);
                    return;
                }

                TeamData oldTeam = getTeamByPlayer(target.getUniqueId());
                if (oldTeam != null && oldTeam.id != team.id) {
                    openTransferConfirmMenu(player, teamId, target.getUniqueId(), oldTeam.id);
                    return;
                }

                addPlayerToTeam(team, target.getUniqueId());
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Added " + target.getName() + " to " + team.name + ".");
                openAddMembersMenu(player, teamId);
            }
            return;
        }

        if (TEAM_TRANSFER_CONFIRM_TITLE.equals(title)) {
            Integer destinationTeamId = addMembersContext.get(player.getUniqueId());
            Integer oldTeamId = transferOldTeamContext.get(player.getUniqueId());
            UUID targetId = transferTargetContext.get(player.getUniqueId());

            if (destinationTeamId == null || oldTeamId == null || targetId == null) {
                openTeamListMenu(player);
                return;
            }

            TeamData destinationTeam = teams.get(destinationTeamId);
            TeamData oldTeam = teams.get(oldTeamId);
            Player target = Bukkit.getPlayer(targetId);

            if (destinationTeam == null || oldTeam == null || target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Transfer target is no longer available.");
                openAddMembersMenu(player, destinationTeamId);
                return;
            }

            if (event.getRawSlot() == 11) {
                addPlayerToTeam(destinationTeam, targetId);
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Transferred " + target.getName() + " from "
                        + oldTeam.name + " to " + destinationTeam.name + ".");
                openAddMembersMenu(player, destinationTeamId);
                return;
            }

            if (event.getRawSlot() == 15 || event.getRawSlot() == 13) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Transfer cancelled.");
                openAddMembersMenu(player, destinationTeamId);
            }
            return;
        }

        if (TEAM_REMOVE_CONFIRM_TITLE.equals(title)) {
            Integer teamId = removeMemberTeamContext.get(player.getUniqueId());
            UUID targetId = removeMemberTargetContext.get(player.getUniqueId());

            if (teamId == null || targetId == null) {
                openTeamListMenu(player);
                return;
            }

            TeamData team = teams.get(teamId);
            if (team == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Remove target is no longer available.");
                openMembersMenu(player, teamId);
                return;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
            String targetName = target.getName() == null ? targetId.toString() : target.getName();

            if (event.getRawSlot() == 11) {
                removePlayerFromTeam(team, targetId);
                requestSave();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Removed " + targetName + " from " + team.name + ".");
                openMembersMenu(player, teamId);
                return;
            }

            if (event.getRawSlot() == 15 || event.getRawSlot() == 13) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Remove cancelled.");
                openMembersMenu(player, teamId);
            }
            return;
        }

        if (TEAM_DELETE_CONFIRM_TITLE.equals(title)) {
            Integer teamId = deleteConfirmContext.get(player.getUniqueId());
            TeamData team = teamId == null ? null : teams.get(teamId);
            if (team == null) {
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == 11) {
                TeamData removed = teams.remove(teamId);
                if (removed != null) {
                    invalidateCaptureBounds(removed.id);
                    requestSave();
                    player.sendMessage(PREFIX + ChatColor.RED + "Deleted " + removed.name + ".");
                }
                openTeamListMenu(player);
                return;
            }

            if (event.getRawSlot() == 15 || event.getRawSlot() == 13) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Delete cancelled.");
                openTeamEditor(player, teamId);
            }
        }
    }

    private List<Integer> getInnerSlots54(int reservedSlot) {
        List<Integer> slots = new ArrayList<Integer>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = (row * 9) + col;
                if (slot != reservedSlot) {
                    slots.add(slot);
                }
            }
        }
        return slots;
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

    public void handleBlockPlace(BlockPlaceEvent event) {
        if (isChestBlock(event.getBlockPlaced() == null ? null : event.getBlockPlaced().getType())) {
            TeamData team = getTeamByPlayer(event.getPlayer().getUniqueId());
            if (team != null) {
                invalidateCaptureBounds(team.id);
            }
        }

        if (!canPlaceBlock(event.getPlayer(), event.getBlockPlaced())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "You cannot place blocks here.");
        }
    }

    public void handleBlockBreak(BlockBreakEvent event) {
        if (isChestBlock(event.getBlock() == null ? null : event.getBlock().getType())) {
            TeamData team = getTeamByLocation(event.getBlock() == null ? null : event.getBlock().getLocation());
            if (team != null) {
                invalidateCaptureBounds(team.id);
                if (settingsManager.isTntCapEnabled() && team.baseRadius > 0
                        && !hasAnyChestInBaseExcluding(team, event.getBlock().getLocation())) {
                    eliminateTeamWithoutChest(team);
                }
            }
        }

        if (!canBreakBlock(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PREFIX + ChatColor.RED + "You cannot break blocks inside an enemy base.");
        }
    }

    private boolean hasAnyChestInBaseExcluding(TeamData team, Location excludedLocation) {
        if (team == null || team.baseCenter == null || team.baseRadius <= 0 || excludedLocation == null) {
            return false;
        }

        World world = team.baseCenter.getWorld();
        if (world == null) {
            return false;
        }

        int centerX = team.baseCenter.getBlockX();
        int centerY = team.baseCenter.getBlockY();
        int centerZ = team.baseCenter.getBlockZ();

        int minSearchX = centerX - team.baseRadius;
        int maxSearchX = centerX + team.baseRadius;
        int minSearchZ = centerZ - team.baseRadius;
        int maxSearchZ = centerZ + team.baseRadius;

        int minSearchY = 0;
        int maxSearchY = Math.min(world.getMaxHeight() - 1, 255);

        for (int x = minSearchX; x <= maxSearchX; x++) {
            for (int z = minSearchZ; z <= maxSearchZ; z++) {
                for (int y = minSearchY; y <= maxSearchY; y++) {
                    if (x == excludedLocation.getBlockX() && y == excludedLocation.getBlockY()
                            && z == excludedLocation.getBlockZ()
                            && world.equals(excludedLocation.getWorld())) {
                        continue;
                    }
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (isChestBlock(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void eliminateTeamWithoutChest(TeamData team) {
        if (team == null) {
            return;
        }
        team.baseCenter = null;
        team.baseRadius = 0;
        invalidateCaptureBounds(team.id);
        requestSave();
        Bukkit.broadcastMessage(PREFIX + ChatColor.RED + "Team "
                + getTeamChatColor(team.color) + team.name
                + ChatColor.RED + " has been eliminated (no chest in base after assault cap).");
    }

    private TeamData getTeamByLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        for (TeamData team : teams.values()) {
            if (isInsideBaseSquare(location, team, 0.0D)) {
                return team;
            }
        }
        return null;
    }

    private boolean isChestBlock(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    public void invalidateCaptureBounds(int teamId) {
        captureBoundsCache.remove(Integer.valueOf(teamId));
    }

    public CaptureBounds getCaptureBounds(TeamData team) {
        if (team == null || team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
            return null;
        }

        long now = System.currentTimeMillis();
        CachedCaptureBounds cached = captureBoundsCache.get(Integer.valueOf(team.id));
        if (cached != null && (now - cached.computedAtMillis) <= CAPTURE_BOUNDS_CACHE_MILLIS) {
            return cached.bounds;
        }

        CaptureBounds computed = computeCaptureBounds(team);
        captureBoundsCache.put(Integer.valueOf(team.id), new CachedCaptureBounds(now, computed));
        return computed;
    }

    private CaptureBounds computeCaptureBounds(TeamData team) {
        World world = team.baseCenter.getWorld();
        if (world == null) {
            return null;
        }

        int centerX = team.baseCenter.getBlockX();
        int centerY = team.baseCenter.getBlockY();
        int centerZ = team.baseCenter.getBlockZ();

        int minSearchX = centerX - team.baseRadius;
        int maxSearchX = centerX + team.baseRadius;
        int minSearchZ = centerZ - team.baseRadius;
        int maxSearchZ = centerZ + team.baseRadius;

        int minSearchY = 0;
        int maxSearchY = Math.min(world.getMaxHeight() - 1, 255);

        boolean foundChest = false;
        int chestMinX = 0;
        int chestMaxX = 0;
        int chestMinY = 0;
        int chestMaxY = 0;
        int chestMinZ = 0;
        int chestMaxZ = 0;

        for (int x = minSearchX; x <= maxSearchX; x++) {
            for (int z = minSearchZ; z <= maxSearchZ; z++) {
                for (int y = minSearchY; y <= maxSearchY; y++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (!isChestBlock(type)) {
                        continue;
                    }

                    if (!foundChest) {
                        foundChest = true;
                        chestMinX = x;
                        chestMaxX = x;
                        chestMinY = y;
                        chestMaxY = y;
                        chestMinZ = z;
                        chestMaxZ = z;
                    } else {
                        chestMinX = Math.min(chestMinX, x);
                        chestMaxX = Math.max(chestMaxX, x);
                        chestMinY = Math.min(chestMinY, y);
                        chestMaxY = Math.max(chestMaxY, y);
                        chestMinZ = Math.min(chestMinZ, z);
                        chestMaxZ = Math.max(chestMaxZ, z);
                    }
                }
            }
        }

        if (!foundChest) {
            return null;
        }

        int minX = chestMinX - 1;
        int maxX = chestMaxX + 1;
        int minY = chestMinY;
        int maxY = Math.min(world.getMaxHeight() - 1, chestMaxY + 1);
        int minZ = chestMinZ - 1;
        int maxZ = chestMaxZ + 1;

        return new CaptureBounds(world, minX, maxX, minY, maxY, minZ, maxZ);
    }

    public boolean isInsideCaptureBounds(Location location, CaptureBounds bounds) {
        if (location == null || location.getWorld() == null || bounds == null || bounds.world == null) {
            return false;
        }

        if (!location.getWorld().equals(bounds.world)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= bounds.minX && x <= bounds.maxX
                && y >= bounds.minY && y <= bounds.maxY
                && z >= bounds.minZ && z <= bounds.maxZ;
    }

    public void showBaseBounds(Player player) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Base limits:");

        TeamData playerTeam = getTeamByPlayer(player.getUniqueId());
        boolean showOnlyOwnTeam = playerTeam != null;
        boolean foundAny = false;

        for (TeamData team : teams.values()) {
            if (team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
                continue;
            }
            if (showOnlyOwnTeam && team != playerTeam) {
                continue;
            }

            foundAny = true;
            int minX = team.baseCenter.getBlockX() - team.baseRadius;
            int maxX = team.baseCenter.getBlockX() + team.baseRadius;
            int minZ = team.baseCenter.getBlockZ() - team.baseRadius;
            int maxZ = team.baseCenter.getBlockZ() + team.baseRadius;

            player.sendMessage(getTeamColor(team.color) + team.name + ChatColor.GRAY + " [" + team.baseCenter.getWorld().getName() + "]");
            player.sendMessage(ChatColor.GRAY + "Base square X:" + minX + " -> " + maxX
                    + " | Z:" + minZ + " -> " + maxZ
                    + " | Y(center):" + team.baseCenter.getBlockY());

            CaptureBounds bounds = getCaptureBounds(team);
            if (bounds == null) {
                player.sendMessage(ChatColor.RED + "Capture zone: no chest found in base.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Capture zone X:" + bounds.minX + " -> " + bounds.maxX
                        + " | Y:" + bounds.minY + " -> " + bounds.maxY
                        + " | Z:" + bounds.minZ + " -> " + bounds.maxZ);
            }
        }

        if (!foundAny) {
            if (showOnlyOwnTeam) {
                player.sendMessage(ChatColor.RED + "Your team does not have a base configured.");
            } else {
                player.sendMessage(ChatColor.RED + "No team base configured.");
            }
        }
    }

    public void showBaseBoundsWithParticles(final Player player) {
        showBaseBounds(player);

        if (player == null || !player.isOnline()) {
            return;
        }

        TeamData playerTeam = getTeamByPlayer(player.getUniqueId());
        final boolean showOnlyOwnTeam = playerTeam != null;
        final TeamData teamToShow = playerTeam;
        final int maxTicks = SHOW_PARTICLE_SECONDS * 20;
        new BukkitRunnable() {
            private int elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsedTicks >= maxTicks) {
                    cancel();
                    return;
                }

                for (TeamData team : teams.values()) {
                    if (team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
                        continue;
                    }
                    if (showOnlyOwnTeam && team != teamToShow) {
                        continue;
                    }

                    CaptureBounds bounds = getCaptureBounds(team);
                    if (bounds != null) {
                        renderCaptureBounds(player, bounds);
                    }
                }

                elapsedTicks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void renderCaptureBounds(Player player, CaptureBounds bounds) {
        World world = bounds.world;
        if (world == null) {
            return;
        }

        double bottomY = bounds.minY + 0.15D;
        double topY = bounds.maxY + 0.15D;

        for (int x = bounds.minX; x <= bounds.maxX; x++) {
            playMarker(player, new Location(world, x + 0.5D, bottomY, bounds.minZ + 0.5D));
            playMarker(player, new Location(world, x + 0.5D, bottomY, bounds.maxZ + 0.5D));
            playMarker(player, new Location(world, x + 0.5D, topY, bounds.minZ + 0.5D));
            playMarker(player, new Location(world, x + 0.5D, topY, bounds.maxZ + 0.5D));
        }

        for (int z = bounds.minZ + 1; z <= bounds.maxZ - 1; z++) {
            playMarker(player, new Location(world, bounds.minX + 0.5D, bottomY, z + 0.5D));
            playMarker(player, new Location(world, bounds.maxX + 0.5D, bottomY, z + 0.5D));
            playMarker(player, new Location(world, bounds.minX + 0.5D, topY, z + 0.5D));
            playMarker(player, new Location(world, bounds.maxX + 0.5D, topY, z + 0.5D));
        }

        for (int y = bounds.minY; y <= bounds.maxY; y++) {
            double markerY = y + 0.15D;
            playMarker(player, new Location(world, bounds.minX + 0.5D, markerY, bounds.minZ + 0.5D));
            playMarker(player, new Location(world, bounds.minX + 0.5D, markerY, bounds.maxZ + 0.5D));
            playMarker(player, new Location(world, bounds.maxX + 0.5D, markerY, bounds.minZ + 0.5D));
            playMarker(player, new Location(world, bounds.maxX + 0.5D, markerY, bounds.maxZ + 0.5D));
        }
    }

    private void teleportToTeamBase(Player player, int teamId) {
        TeamData team = teams.get(teamId);
        if (team == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team no longer exists.");
            openTeamListMenu(player);
            return;
        }

        if (team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "This team has no base configured.");
            openTeamEditor(player, teamId);
            return;
        }

        Location target = team.baseCenter.clone().add(0.5D, 1.0D, 0.5D);
        player.teleport(target);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Teleported to " + getTeamColor(team.color) + team.name
                + ChatColor.GREEN + " base.");
        openTeamEditor(player, teamId);
    }

    private void playMarker(Player player, Location location) {
        player.playEffect(location, Effect.MOBSPAWNER_FLAMES, 0);
    }

    private boolean canPlaceBlock(Player player, Block block) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (block == null) {
            return true;
        }

        World world = block.getWorld();
        if (world != null && (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END)) {
            return true;
        }

        if (isInsideTeamBase(player, block.getLocation())) {
            return true;
        }

        if (block.getType() == Material.TNT && settingsManager.isTntCapEnabled()) {
            return true;
        }

        if (isLocationInsideEnemyBase(player, block.getLocation())) {
            return false;
        }

        if (gameStateManager == null || !gameStateManager.isRunning()) {
            return true;
        }

        if (settingsManager.isGloballyAllowedPlaceBlock(block.getType(), block.getData())) {
            return true;
        }

        if (block.getY() <= settingsManager.getCavePlaceYLimit()) {
            return true;
        }

        return false;
    }

    public boolean isLocationInsideEnemyBase(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        TeamData playerTeam = getTeamByPlayer(player.getUniqueId());
        for (TeamData team : teams.values()) {
            if (team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
                continue;
            }

            if (!location.getWorld().equals(team.baseCenter.getWorld())) {
                continue;
            }

            if (!isInsideBaseSquare(location, team, 0.0D)) {
                continue;
            }

            if (playerTeam == null || playerTeam.id != team.id) {
                return true;
            }
        }

        return false;
    }

    public boolean isLocationWithinEnemyBaseDistance(Player player, Location location, int extraDistance) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        TeamData playerTeam = getTeamByPlayer(player.getUniqueId());
        double safeExtraDistance = Math.max(0, extraDistance);

        for (TeamData team : teams.values()) {
            if (team.baseCenter == null || team.baseCenter.getWorld() == null || team.baseRadius <= 0) {
                continue;
            }

            if (!location.getWorld().equals(team.baseCenter.getWorld())) {
                continue;
            }

            if (playerTeam != null && playerTeam.id == team.id) {
                continue;
            }

            if (isInsideBaseSquare(location, team, safeExtraDistance)) {
                return true;
            }
        }

        return false;
    }

    private boolean canBreakBlock(Player player, Block block) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        if (isLocationInsideEnemyBase(player, block == null ? null : block.getLocation())) {
            if (block != null && block.getType() == Material.TNT && settingsManager.isTntCapEnabled()) {
                return true;
            }
            return false;
        }

        TeamData playerTeam = getTeamByPlayer(player.getUniqueId());
        if (block == null || block.getWorld() == null) {
            return false;
        }

        for (TeamData team : teams.values()) {
            if (team.baseCenter == null || team.baseRadius <= 0 || team.baseCenter.getWorld() == null) {
                continue;
            }

            if (!block.getWorld().equals(team.baseCenter.getWorld())) {
                continue;
            }

            if (!isInsideBaseSquare(block.getLocation(), team, 0.0D)) {
                continue;
            }

            if (playerTeam != null && team.id == playerTeam.id) {
                return true;
            }

            return false;
        }

        return true;
    }
}
