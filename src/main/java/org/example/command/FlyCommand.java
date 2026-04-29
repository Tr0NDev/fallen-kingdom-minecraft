package org.example.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class FlyCommand implements CommandExecutor {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private static final float SPEED_INCREMENT = 0.1f;
    private static final float MAX_SPEED = 1.0f;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            openFlySpeedGui(player);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("speed")) {
            try {
                int speed = Integer.parseInt(args[1]);
                if (speed < 1 || speed > 5) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Speed must be between 1 and 5.");
                    return true;
                }

                Player target = player;
                if (args.length >= 3) {
                    target = Bukkit.getPlayerExact(args[2]);
                    if (target == null || !target.isOnline()) {
                        player.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[2]);
                        return true;
                    }
                }

                setFlySpeed(target, speed);
                player.sendMessage(PREFIX + ChatColor.GREEN + "Fly speed set to " + ChatColor.YELLOW + speed + "x" + ChatColor.GREEN + " for " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + ".");
                if (!target.equals(player)) {
                    target.sendMessage(PREFIX + ChatColor.GREEN + "Your fly speed has been set to " + ChatColor.YELLOW + speed + "x" + ChatColor.GREEN + ".");
                }
                return true;
            } catch (NumberFormatException exception) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fly speed <1-5> [player]");
                return true;
            }
        }

        player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fly [speed <1-5> [player]]");
        return true;
    }

    private void openFlySpeedGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.translateAlternateColorCodes('&', "&8Fly Speed"));

        gui.setItem(2, createBootsItem(1, Material.LEATHER_BOOTS, ChatColor.GRAY + "Wood Boots", "x1 Speed"));
        gui.setItem(4, createBootsItem(2, Material.CHAINMAIL_BOOTS, ChatColor.YELLOW + "Stone Boots", "x2 Speed"));
        gui.setItem(6, createBootsItem(3, Material.IRON_BOOTS, ChatColor.AQUA + "Iron Boots", "x3 Speed"));
        gui.setItem(8, createBootsItem(5, Material.DIAMOND_BOOTS, ChatColor.LIGHT_PURPLE + "Diamond Boots", "x5 Speed"));

        player.openInventory(gui);
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Click on boots to set fly speed.");
    }

    private ItemStack createBootsItem(int speedMultiplier, Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + lore);
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private void setFlySpeed(Player player, int speedMultiplier) {
        float speed = Math.min(SPEED_INCREMENT * speedMultiplier, MAX_SPEED);
        player.setFlySpeed(speed);
        player.setAllowFlight(true);
        player.setFlying(true);
    }
}
