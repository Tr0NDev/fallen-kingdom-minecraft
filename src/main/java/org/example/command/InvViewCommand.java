package org.example.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvViewCommand implements Listener {

    private static final String PREFIX           = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final String INV_TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] &b");

    private static final int INFO_SLOT       = 13;
    private static final int BOOTS_SLOT      = 45;
    private static final int LEGGINGS_SLOT   = 46;
    private static final int CHESTPLATE_SLOT = 47;
    private static final int HELMET_SLOT     = 48;
    private static final int MAINHAND_SLOT   = 49;

    private final JavaPlugin plugin;
    private final Map<UUID, UUID>       viewerToTarget = new HashMap<UUID, UUID>();
    private final Map<UUID, BukkitTask> refreshTasks   = new HashMap<UUID, BukkitTask>();

    public InvViewCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public boolean handle(Player viewer, String[] args) {
        if (!viewer.isOp()) {
            viewer.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
            return true;
        }
        if (args.length < 2) {
            viewer.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk inv <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            viewer.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }
        if (target.getUniqueId().equals(viewer.getUniqueId())) {
            viewer.sendMessage(PREFIX + ChatColor.RED + "You cannot view your own inventory this way.");
            return true;
        }
        openInvView(viewer, target);
        return true;
    }

    private void openInvView(final Player viewer, final Player target) {
        stopRefreshTask(viewer.getUniqueId());

        final String title = INV_TITLE_PREFIX + target.getName();
        Inventory gui = Bukkit.createInventory(null, 54, title);

        renderInventory(gui, target);
        viewerToTarget.put(viewer.getUniqueId(), target.getUniqueId());
        viewer.openInventory(gui);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!viewer.isOnline()) {
                    stopRefreshTask(viewer.getUniqueId());
                    return;
                }
                if (viewer.getOpenInventory() == null
                        || viewer.getOpenInventory().getTopInventory() == null
                        || !title.equals(viewer.getOpenInventory().getTopInventory().getTitle())) {
                    stopRefreshTask(viewer.getUniqueId());
                    return;
                }
                if (!target.isOnline()) {
                    viewer.closeInventory();
                    viewer.sendMessage(PREFIX + ChatColor.RED + target.getName() + " disconnected.");
                    stopRefreshTask(viewer.getUniqueId());
                    return;
                }
                renderInventory(viewer.getOpenInventory().getTopInventory(), target);
                viewer.updateInventory();
            }
        }, 10L, 10L);

        refreshTasks.put(viewer.getUniqueId(), task);
    }

    private void renderInventory(Inventory gui, Player target) {
        gui.clear();

        ItemStack[] contents = target.getInventory().getContents();
        ItemStack[] armor = target.getInventory().getArmorContents();
        ItemStack mainHand = target.getItemInHand();

        ItemStack sep = makeSeparator();

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, cloneOrNull(contents[i]));
        }

        for (int i = 9; i <= 17; i++) {
            gui.setItem(i, sep);
        }
        gui.setItem(INFO_SLOT, makeInfoItem(target));

        for (int i = 9; i < 36; i++) {
            gui.setItem(i + 9, cloneOrNull(contents[i]));
        }

        for (int i = 45; i <= 53; i++) {
            gui.setItem(i, sep);
        }
        gui.setItem(BOOTS_SLOT,      makeEquipDisplay(armor[0], "Boots"));
        gui.setItem(LEGGINGS_SLOT,   makeEquipDisplay(armor[1], "Leggings"));
        gui.setItem(CHESTPLATE_SLOT, makeEquipDisplay(armor[2], "Chestplate"));
        gui.setItem(HELMET_SLOT,     makeEquipDisplay(armor[3], "Helmet"));
        gui.setItem(MAINHAND_SLOT,   makeEquipDisplay(mainHand, "Main hand"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player viewer = (Player) event.getWhoClicked();
        if (!viewerToTarget.containsKey(viewer.getUniqueId())) return;

        Inventory top = event.getInventory();
        if (top == null || top.getTitle() == null) return;
        if (!top.getTitle().startsWith(INV_TITLE_PREFIX)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= top.getSize()) return;

        if (isFixedSlot(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        scheduleSync(viewer, top);
    }

    private void scheduleSync(final Player viewer, final Inventory gui) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                UUID targetId = viewerToTarget.get(viewer.getUniqueId());
                if (targetId == null) return;
                Player target = Bukkit.getPlayer(targetId);
                if (target == null || !target.isOnline()) return;

                for (int i = 0; i < 9; i++) {
                    target.getInventory().setItem(i, cloneOrNull(gui.getItem(i)));
                }
                for (int guiSlot = 18; guiSlot <= 44; guiSlot++) {
                    target.getInventory().setItem(guiSlot - 9, cloneOrNull(gui.getItem(guiSlot)));
                }
                target.updateInventory();
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player viewer = (Player) event.getPlayer();
        UUID viewerId = viewer.getUniqueId();
        if (!viewerToTarget.containsKey(viewerId)) return;

        Inventory inv = event.getInventory();
        if (inv == null || inv.getTitle() == null) return;
        if (!inv.getTitle().startsWith(INV_TITLE_PREFIX)) return;

        stopRefreshTask(viewerId);
        viewerToTarget.remove(viewerId);
    }

    private boolean isFixedSlot(int slot) {
        return (slot >= 9 && slot <= 17) || (slot >= 45 && slot <= 53);
    }

    private ItemStack makeSeparator() {
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta m = sep.getItemMeta();
        m.setDisplayName(" ");
        sep.setItemMeta(m);
        return sep;
    }

    private ItemStack makeBarrier(String label) {
        ItemStack b = new ItemStack(Material.BARRIER, 1);
        ItemMeta m = b.getItemMeta();
        m.setDisplayName(ChatColor.DARK_GRAY + label + ": " + ChatColor.RED + "Empty");
        b.setItemMeta(m);
        return b;
    }

    private ItemStack makeEquipDisplay(ItemStack item, String slotName) {
        if (item == null || item.getType() == Material.AIR) {
            return makeBarrier(slotName);
        }

        ItemStack display = item.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        if (!meta.hasDisplayName()) {
            meta.setDisplayName(ChatColor.GOLD + formatItemName(item));
        }

        List<String> lore = meta.hasLore()
                ? new ArrayList<String>(meta.getLore())
                : new ArrayList<String>();

        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack makeInfoItem(Player target) {
        ItemStack info = new ItemStack(Material.PAPER, 1);
        ItemMeta m = info.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + target.getName() + "'s Inventory");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Health: " + ChatColor.RED
                + String.format("%.1f", target.getHealth()) + "/20");
        lore.add(ChatColor.GRAY + "Food:   " + ChatColor.GREEN
                + target.getFoodLevel() + "/20");
        lore.add(ChatColor.GRAY + "XP:     " + ChatColor.YELLOW
                + "Lvl " + target.getLevel()
                + ChatColor.GRAY + " (" + Math.round(target.getExp() * 100) + "% to next)");
        lore.add(ChatColor.GRAY + "Mode:   " + ChatColor.AQUA
                + target.getGameMode().name());
        lore.add(ChatColor.DARK_GRAY + "Items can be taken or moved.");
        m.setLore(lore);
        info.setItemMeta(m);
        return info;
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return (item != null && item.getType() != Material.AIR) ? item.clone() : null;
    }

    private String formatItemName(ItemStack item) {
        if (item == null) return "Air";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String raw = item.getType().name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String vanillaName(Enchantment e) {
        String n = e.getName();
        if (n.equals("PROTECTION_ENVIRONMENTAL"))  return "Protection";
        if (n.equals("PROTECTION_FIRE"))           return "Fire Protection";
        if (n.equals("PROTECTION_FALL"))           return "Feather Falling";
        if (n.equals("PROTECTION_EXPLOSIONS"))     return "Blast Protection";
        if (n.equals("PROTECTION_PROJECTILE"))     return "Projectile Protection";
        if (n.equals("OXYGEN"))                    return "Respiration";
        if (n.equals("WATER_WORKER"))              return "Aqua Affinity";
        if (n.equals("THORNS"))                    return "Thorns";
        if (n.equals("DAMAGE_ALL"))                return "Sharpness";
        if (n.equals("DAMAGE_UNDEAD"))             return "Smite";
        if (n.equals("DAMAGE_ARTHROPODS"))         return "Bane of Arthropods";
        if (n.equals("KNOCKBACK"))                 return "Knockback";
        if (n.equals("FIRE_ASPECT"))               return "Fire Aspect";
        if (n.equals("LOOT_BONUS_MOBS"))           return "Looting";
        if (n.equals("DIG_SPEED"))                 return "Efficiency";
        if (n.equals("SILK_TOUCH"))                return "Silk Touch";
        if (n.equals("DURABILITY"))                return "Unbreaking";
        if (n.equals("LOOT_BONUS_BLOCKS"))         return "Fortune";
        if (n.equals("ARROW_DAMAGE"))              return "Power";
        if (n.equals("ARROW_KNOCKBACK"))           return "Punch";
        if (n.equals("ARROW_FIRE"))                return "Flame";
        if (n.equals("ARROW_INFINITE"))            return "Infinity";
        if (n.equals("LUCK"))                      return "Luck of the Sea";
        if (n.equals("LURE"))                      return "Lure";
        return n.replace('_', ' ').toLowerCase();
    }

    private String toRoman(int level) {
        switch (level) {
            case 1:  return "I";
            case 2:  return "II";
            case 3:  return "III";
            case 4:  return "IV";
            case 5:  return "V";
            default: return String.valueOf(level);
        }
    }

    private void stopRefreshTask(UUID viewerId) {
        BukkitTask task = refreshTasks.remove(viewerId);
        if (task != null) task.cancel();
    }

    public void cleanup() {
        Iterator<Map.Entry<UUID, BukkitTask>> it = refreshTasks.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().cancel();
            it.remove();
        }
        viewerToTarget.clear();
    }
}