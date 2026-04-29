package org.example.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand implements CommandExecutor {

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
                    online.setFoodLevel(20);
                    online.setSaturation(20.0F);
                    online.sendMessage(PREFIX + ChatColor.GREEN + "You have been fed.");
                    count++;
                }
                player.sendMessage(PREFIX + ChatColor.GREEN + "Fed " + count + " players.");
                return true;
            }

            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Player not found.");
                return true;
            }
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /feed [player|all]");
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0F);
        target.sendMessage(PREFIX + ChatColor.GREEN + "You have been fed.");
        if (target != player) {
            player.sendMessage(PREFIX + ChatColor.GREEN + "Fed " + target.getName() + ".");
        }
        return true;
    }
}