package org.example.koth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class KothCommand implements Listener {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final String KOTH_MENU_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bKOTH Settings");
    private static final String REWARD_MENU_TITLE = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &bKOTH Rewards");

    private static final int SLOT_ENABLE_DISABLE = 10;
    private static final int SLOT_SET_ZONE = 12;
    private static final int SLOT_REWARDS = 14;
    private static final int SLOT_REWARD_COUNT = 16;
    private static final int SLOT_CAPTURE_TIME = 32;
    private static final int SLOT_START_KOTH = 34;
    private static final int SLOT_START_DAY = 28;
    private static final int SLOT_END_DAY = 30;
    private static final int SLOT_BACK = 40;

    private final JavaPlugin plugin;
    private final KothManager kothManager;

    private final Map<Player, Boolean> settingZone = new HashMap<>();
    private final Map<Player, Location> zoneCenters = new HashMap<>();

    public KothCommand(JavaPlugin plugin, KothManager kothManager) {
        this.plugin = plugin;
        this.kothManager = kothManager;
    }

    public void openKothMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, KOTH_MENU_TITLE);

        menu.setItem(SLOT_ENABLE_DISABLE, createEnableDisableItem());

        menu.setItem(SLOT_SET_ZONE, createSetZoneItem());

        menu.setItem(SLOT_REWARDS, createRewardsItem());

        menu.setItem(SLOT_REWARD_COUNT, createRewardCountItem());

        menu.setItem(SLOT_CAPTURE_TIME, createCaptureTimeItem());

        menu.setItem(SLOT_START_KOTH, createStartKothItem());

        menu.setItem(SLOT_START_DAY, createStartDayItem());

        menu.setItem(SLOT_END_DAY, createEndDayItem());

        menu.setItem(SLOT_BACK, createBackItem());

        fillBorder(menu);

        player.openInventory(menu);
    }

    private ItemStack createEnableDisableItem() {
        Material material = kothManager.isEnabled() ? Material.WOOL : Material.WOOL;
        short data = kothManager.isEnabled() ? DyeColor.LIME.getWoolData() : DyeColor.RED.getWoolData();
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Enable/Disable KOTH");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Currently: " + (kothManager.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        lore.add(ChatColor.YELLOW + "Click to toggle");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSetZoneItem() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Set KOTH Zone");
        List<String> lore = new ArrayList<>();
        if (kothManager.getZoneCenter() != null && kothManager.getZoneRadius() > 0) {
            lore.add(ChatColor.GRAY + "Center: " + kothManager.getZoneCenter().getBlockX() + ", " +
                    kothManager.getZoneCenter().getBlockY() + ", " + kothManager.getZoneCenter().getBlockZ());
            lore.add(ChatColor.GRAY + "Radius: " + kothManager.getZoneRadius() + " blocks");
        } else {
            lore.add(ChatColor.GRAY + "No zone set");
        }
        lore.add(ChatColor.YELLOW + "Click, then type the radius in chat.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardsItem() {
        ItemStack item = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "KOTH Rewards");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Items: " + kothManager.getRewardItems().size());
        lore.add(ChatColor.YELLOW + "Click to manage rewards");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardCountItem() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Reward Count");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Count: " + kothManager.getRewardCount());
        lore.add(ChatColor.YELLOW + "Click to set reward count");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCaptureTimeItem() {
        ItemStack item = new ItemStack(Material.WATCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Capture Time");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Seconds to capture: " + kothManager.getCaptureTimeSeconds());
        lore.add(ChatColor.YELLOW + "Click to set capture duration in chat");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStartDayItem() {
        ItemStack item = new ItemStack(Material.WATCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Start Day");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Day: " + kothManager.getStartDay());
        int scheduledDay = kothManager.getScheduledKothDay();
        lore.add(ChatColor.GRAY + "Next KOTH day: " + (scheduledDay > 0 ? ChatColor.YELLOW + String.valueOf(scheduledDay) : ChatColor.RED + "none"));
        lore.add(ChatColor.YELLOW + "Click to set start day");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEndDayItem() {
        ItemStack item = new ItemStack(Material.WATCH, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "End Day");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Day: " + kothManager.getEndDay());
        lore.add(ChatColor.YELLOW + "Click to set end day");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStartKothItem() {
        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Start KOTH Now");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to start the active KOTH immediately");
        int scheduledDay = kothManager.getScheduledKothDay();
        lore.add(ChatColor.GRAY + "Auto-start day: " + (scheduledDay > 0 ? ChatColor.YELLOW + String.valueOf(scheduledDay) : ChatColor.RED + "none"));
        lore.add(ChatColor.YELLOW + "Requires KOTH configured and enabled");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Back");
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory menu) {
        ItemStack borderItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName(ChatColor.DARK_GRAY + " ");
        borderItem.setItemMeta(borderMeta);

        int rows = menu.getSize() / 9;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                    int slot = row * 9 + col;
                    if (menu.getItem(slot) == null) {
                        menu.setItem(slot, borderItem);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(KOTH_MENU_TITLE)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }

            int slot = event.getSlot();

            if (slot == SLOT_ENABLE_DISABLE) {
                kothManager.setEnabled(!kothManager.isEnabled());
                openKothMenu(player);
            } else if (slot == SLOT_SET_ZONE) {
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type the radius for the KOTH zone in chat:");
                settingZone.put(player, true);
                zoneCenters.put(player, player.getLocation());
            } else if (slot == SLOT_REWARDS) {
                kothManager.openRewardInventory(player);
            } else if (slot == SLOT_REWARD_COUNT) {
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type the number of rewards to give to the winner in chat:");
                kothManager.setPendingRewardCount(player, 0);
            } else if (slot == SLOT_CAPTURE_TIME) {
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type how many seconds a player must stay in the KOTH zone to win:");
                kothManager.setPendingCaptureTime(player, 0);
            } else if (slot == SLOT_START_KOTH) {
                player.closeInventory();
                if (!kothManager.isEnabled()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "KOTH is not enabled!");
                } else if (kothManager.isActive()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "KOTH is already active!");
                } else if (kothManager.getZoneCenter() == null || kothManager.getZoneRadius() <= 0) {
                    player.sendMessage(PREFIX + ChatColor.RED + "KOTH zone is not set!");
                } else if (kothManager.getRewardItems().isEmpty()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "No KOTH rewards configured!");
                } else {
                    kothManager.forceStartKoth();
                    player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH started manually!");
                }
            } else if (slot == SLOT_START_DAY) {
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type the start day for KOTH in chat:");
                kothManager.setPendingStartDay(player, 0);
            } else if (slot == SLOT_END_DAY) {
                player.closeInventory();
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Type the end day for KOTH in chat:");
                kothManager.setPendingEndDay(player, 0);
            } else if (slot == SLOT_BACK) {
                player.closeInventory();
            }
        } else if (title.equals(REWARD_MENU_TITLE)) {
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        if (title.equals(REWARD_MENU_TITLE)) {
            kothManager.saveRewardsFromInventory(event.getInventory());
            player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH rewards saved!");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        Integer pendingRewardCount = kothManager.getPendingRewardCount(player);
        if (pendingRewardCount != null) {
            event.setCancelled(true);
            event.getRecipients().clear();
            try {
                int count = Integer.parseInt(event.getMessage());
                if (count > 0) {
                    kothManager.setRewardCount(count);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Reward count set to " + count);
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "Reward count must be positive!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Invalid number!");
            }
            return;
        }

        Integer pendingStartDay = kothManager.getPendingStartDay(player);
        if (pendingStartDay != null) {
            event.setCancelled(true);
            event.getRecipients().clear();
            try {
                int day = Integer.parseInt(event.getMessage());
                if (day > 0) {
                    kothManager.setStartDay(day);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Start day set to " + day);
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "Start day must be positive!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Invalid number!");
            }
            return;
        }

        Integer pendingEndDay = kothManager.getPendingEndDay(player);
        if (pendingEndDay != null) {
            event.setCancelled(true);
            event.getRecipients().clear();
            try {
                int day = Integer.parseInt(event.getMessage());
                if (day > 0 && day >= kothManager.getStartDay()) {
                    kothManager.setEndDay(day);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "End day set to " + day);
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "End day must be positive and >= start day!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Invalid number!");
            }
            return;
        }

        Integer pendingCaptureTime = kothManager.getPendingCaptureTime(player);
        if (pendingCaptureTime != null) {
            event.setCancelled(true);
            event.getRecipients().clear();
            try {
                int seconds = Integer.parseInt(event.getMessage());
                if (seconds > 0) {
                    kothManager.setCaptureTimeSeconds(seconds);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH capture time set to " + seconds + " seconds.");
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "Capture time must be positive!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Invalid number!");
            }
            return;
        }

        if (settingZone.containsKey(player)) {
            event.setCancelled(true);
            event.getRecipients().clear();
            try {
                int radius = Integer.parseInt(event.getMessage());
                if (radius > 0) {
                    Location center = zoneCenters.get(player);
                    kothManager.setZone(center, radius);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH zone set! Center: " +
                            center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ() +
                            " | Radius: " + radius + " blocks");
                } else {
                    player.sendMessage(PREFIX + ChatColor.RED + "Radius must be positive!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Invalid number!");
            }
            settingZone.remove(player);
            zoneCenters.remove(player);
            return;
        }
    }
}