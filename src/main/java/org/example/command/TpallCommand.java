package org.example.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpallCommand implements CommandExecutor {

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

        if (args.length != 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /tpall");
            return true;
        }

        Location location = player.getLocation();
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.teleport(location);
                online.sendMessage(PREFIX + ChatColor.GREEN + "Teleported to " + player.getName() + ".");
                count++;
            }
        }
        player.sendMessage(PREFIX + ChatColor.GREEN + "Teleported " + count + " players to you.");
        return true;
    }
}