package org.example.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealCommand implements CommandExecutor {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage(PREFIX + ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player target;
        if (args.length == 0) {
            target = player;
        } else if (args.length == 1) {
            if ("all".equalsIgnoreCase(args[0])) {
                int count = 0;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online == null || !online.isOnline()) {
                        continue;
                    }
                    online.setHealth(online.getMaxHealth());
                    online.sendMessage(PREFIX + ChatColor.GREEN + "You have been healed.");
                    count++;
                }
                player.sendMessage(PREFIX + ChatColor.GREEN + "Healed " + count + " players.");
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Player not found.");
                return true;
            }
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /heal [player|all]");
            return true;
        }

        target.setHealth(target.getMaxHealth());
        target.sendMessage(PREFIX + ChatColor.GREEN + "You have been healed.");
        if (target != player) {
            player.sendMessage(PREFIX + ChatColor.GREEN + "Healed " + target.getName() + ".");
        }
        return true;
    }
}