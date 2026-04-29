package org.example.settings;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.command.FkSettingsCommand;

public class SettingsMenuListener implements Listener {

    private final SettingsManager settingsManager;
    private final FkSettingsCommand settingsCommand;
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> valueInputPending = new ConcurrentHashMap<UUID, Integer>();

    public SettingsMenuListener(JavaPlugin plugin, SettingsManager settingsManager, FkSettingsCommand settingsCommand) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.settingsCommand = settingsCommand;
    }

    @EventHandler
    public void onSettingsClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        String inventoryTitle = event.getInventory().getTitle();
        if (FkSettingsCommand.ALLOWED_BLOCKS_TITLE.equals(inventoryTitle)
                || FkSettingsCommand.BASE_LOADOUT_TITLE.equals(inventoryTitle)) {
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            if (FkSettingsCommand.ALLOWED_BLOCKS_TITLE.equals(inventoryTitle)
                    && event.getRawSlot() == FkSettingsCommand.SLOT_ALLOWED_BLOCKS_BACK) {
                event.setCancelled(true);
                if (settingsCommand.isReadOnlyAllowedBlocksViewer(player.getUniqueId())) {
                    settingsCommand.openReadOnlySettingsMenu(player);
                } else {
                    settingsCommand.openSettingsMenu(player);
                }
                return;
            }

            if (FkSettingsCommand.BASE_LOADOUT_TITLE.equals(inventoryTitle)
                    && event.getRawSlot() == FkSettingsCommand.SLOT_BASE_LOADOUT_BACK) {
                event.setCancelled(true);
                if (settingsCommand.isReadOnlyBaseLoadoutViewer(player.getUniqueId())) {
                    settingsCommand.openReadOnlySettingsMenu(player);
                } else {
                    settingsCommand.openSettingsMenu(player);
                }
                return;
            }

            boolean readOnlyAllowed = FkSettingsCommand.ALLOWED_BLOCKS_TITLE.equals(inventoryTitle)
                    && settingsCommand.isReadOnlyAllowedBlocksViewer(player.getUniqueId());
            boolean readOnlyLoadout = FkSettingsCommand.BASE_LOADOUT_TITLE.equals(inventoryTitle)
                    && settingsCommand.isReadOnlyBaseLoadoutViewer(player.getUniqueId());

            if (readOnlyAllowed || readOnlyLoadout) {
                event.setCancelled(true);
            }
            return;
        }

        if (!FkSettingsCommand.SETTINGS_TITLE.equals(inventoryTitle)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (settingsCommand.isReadOnlyViewer(player.getUniqueId())) {
            int readOnlyAllowedBlocksSlot = settingsCommand.getDisplaySlot(FkSettingsCommand.SLOT_ALLOWED_BLOCKS, true);
            int readOnlyBaseLoadoutSlot = settingsCommand.getDisplaySlot(FkSettingsCommand.SLOT_BASE_LOADOUT, true);

            if (event.getRawSlot() == readOnlyAllowedBlocksSlot) {
                settingsCommand.openAllowedBlocksMenu(player, true);
                return;
            }

            if (event.getRawSlot() == readOnlyBaseLoadoutSlot) {
                settingsCommand.openBaseLoadoutMenu(player, true);
                return;
            }

            return;
        }

        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &cYou do not have the required permissions."));
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) {
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_PLACE_BLOCK_CAVE) {
            valueInputPending.put(player.getUniqueId(), FkSettingsCommand.SLOT_PLACE_BLOCK_CAVE);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType cave Y limit in chat."));
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_DEATH_LIMIT) {
            valueInputPending.put(player.getUniqueId(), FkSettingsCommand.SLOT_DEATH_LIMIT);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType death limit or NULL in chat."));
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_HUNGER_RATE) {
            valueInputPending.put(player.getUniqueId(), FkSettingsCommand.SLOT_HUNGER_RATE);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType hunger rate in chat (example: 2 or 0.2)."));
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_BASE_CAPTURE_TIME) {
            valueInputPending.put(player.getUniqueId(), FkSettingsCommand.SLOT_BASE_CAPTURE_TIME);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType base capture time in seconds."));
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_BASE_CAPTURE_REQUIRED_PLAYERS) {
            valueInputPending.put(player.getUniqueId(), FkSettingsCommand.SLOT_BASE_CAPTURE_REQUIRED_PLAYERS);
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType the required number of players for base capture."));
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_ALLOWED_BLOCKS) {
            settingsCommand.openAllowedBlocksMenu(player);
            return;
        }

        if (event.getRawSlot() == FkSettingsCommand.SLOT_BASE_LOADOUT) {
            settingsCommand.openBaseLoadoutMenu(player);
            return;
        }

        if (settingsCommand.handleGameStartPointClick(player, event.getRawSlot())) {
            return;
        }

        if (settingsCommand.handleMapSwitchPointClick(player, event.getRawSlot())) {
            return;
        }

        if (settingsCommand.isDayCapSlot(event.getRawSlot())) {
                valueInputPending.put(player.getUniqueId(), event.getRawSlot());
            player.closeInventory();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &eType activation day for "
                    + settingsCommand.getCapName(event.getRawSlot()) + " in chat."));
            return;
        }

        if (currentItem.getType() == Material.WOOL || currentItem.getType() == Material.WATCH) {
            settingsCommand.handleToggleClick(player, event.getRawSlot());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSettingsChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer pendingSlot = valueInputPending.get(player.getUniqueId());
        if (pendingSlot == null) {
            return;
        }

        event.setCancelled(true);
        event.getRecipients().clear();
        valueInputPending.remove(player.getUniqueId());

        final String text = event.getMessage().trim();
        try {
            final Integer parsedNullable;
            final int parsedNumber;
            final double parsedDecimal;

            if (pendingSlot.intValue() == FkSettingsCommand.SLOT_HUNGER_RATE) {
                parsedDecimal = Double.parseDouble(text);
                parsedNumber = 0;
                parsedNullable = null;
            } else if (pendingSlot.intValue() == FkSettingsCommand.SLOT_DEATH_LIMIT) {
                if ("NULL".equalsIgnoreCase(text)) {
                    parsedNullable = null;
                    parsedNumber = 0;
                } else {
                    parsedNumber = Integer.parseInt(text);
                    parsedNullable = Integer.valueOf(parsedNumber);
                }
                parsedDecimal = 0.0D;
            } else {
                parsedNumber = Integer.parseInt(text);
                parsedNullable = null;
                parsedDecimal = 0.0D;
            }

            player.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (pendingSlot.intValue() == FkSettingsCommand.SLOT_PLACE_BLOCK_CAVE) {
                        settingsCommand.setCaveLimitAndRefresh(player, parsedNumber);
                    } else if (pendingSlot.intValue() == FkSettingsCommand.SLOT_DEATH_LIMIT) {
                        settingsCommand.setDeathLimitAndRefresh(player, parsedNullable);
                    } else if (pendingSlot.intValue() == FkSettingsCommand.SLOT_HUNGER_RATE) {
                        settingsCommand.setHungerRateAndRefresh(player, parsedDecimal);
                    } else if (pendingSlot.intValue() == FkSettingsCommand.SLOT_BASE_CAPTURE_TIME) {
                        settingsCommand.setBaseCaptureTimeAndRefresh(player, parsedNumber);
                    } else if (pendingSlot.intValue() == FkSettingsCommand.SLOT_BASE_CAPTURE_REQUIRED_PLAYERS) {
                        settingsCommand.setBaseCaptureRequiredPlayersAndRefresh(player, parsedNumber);
                    } else {
                        settingsCommand.setCapDayAndRefresh(player, pendingSlot.intValue(), parsedNumber);
                    }
                }
            });
        } catch (NumberFormatException exception) {
            player.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &cPlease type a valid value."));
                    settingsCommand.openSettingsMenu(player);
                }
            });
        }
    }

    @EventHandler
    public void onSettingsClose(InventoryCloseEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        if (!FkSettingsCommand.SETTINGS_TITLE.equals(event.getInventory().getTitle())) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        settingsCommand.clearReadOnlyViewer(player.getUniqueId());
    }

    @EventHandler
    public void onAllowedBlocksClose(InventoryCloseEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        if (!FkSettingsCommand.ALLOWED_BLOCKS_TITLE.equals(event.getInventory().getTitle())) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (settingsCommand.isReadOnlyAllowedBlocksViewer(player.getUniqueId())) {
            settingsCommand.clearReadOnlyInventoryViews(player.getUniqueId());
            return;
        }

        if (!player.isOp()) {
            return;
        }

        settingsCommand.saveAllowedBlocksFromMenu(event.getInventory());
        settingsCommand.announceAllowedBlocksUpdated(player);
    }

    @EventHandler
    public void onBaseLoadoutClose(InventoryCloseEvent event) {
        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            return;
        }

        if (!FkSettingsCommand.BASE_LOADOUT_TITLE.equals(event.getInventory().getTitle())) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (settingsCommand.isReadOnlyBaseLoadoutViewer(player.getUniqueId())) {
            settingsCommand.clearReadOnlyInventoryViews(player.getUniqueId());
            return;
        }

        if (!player.isOp()) {
            return;
        }

        settingsCommand.saveBaseLoadoutFromMenu(event.getInventory());
        settingsCommand.announceBaseLoadoutUpdated(player);
    }
}
