package org.example.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.example.team.TeamManager;

public class NickCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private final TeamManager teamManager;

    public NickCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

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

        if (args.length < 1 || args.length > 2) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /nick <name> [player]");
            return true;
        }

        String requested = ChatColor.translateAlternateColorCodes('&', args[0]).trim();
        String plain = ChatColor.stripColor(requested);
        if (plain == null || plain.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Nickname cannot be empty.");
            return true;
        }

        if (plain.length() > 16) {
            player.sendMessage(PREFIX + ChatColor.RED + "Nickname must be 16 characters max.");
            return true;
        }

        Player target;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else {
            target = player;
        }

        teamManager.setPlayerNickname(target.getUniqueId(), requested);
        teamManager.updatePlayerDisplayNames();
        teamManager.refreshPlayerAppearance(target);

        if (target == player) {
            player.sendMessage(PREFIX + ChatColor.GREEN + "Nickname updated to " + ChatColor.YELLOW + requested + ChatColor.GREEN + ".");
        } else {
            player.sendMessage(PREFIX + ChatColor.GREEN + "Nickname of " + ChatColor.YELLOW + target.getName()
                    + ChatColor.GREEN + " updated to " + ChatColor.YELLOW + requested + ChatColor.GREEN + ".");
            target.sendMessage(PREFIX + ChatColor.GREEN + "Your nickname was changed to " + ChatColor.YELLOW + requested
                    + ChatColor.GREEN + " by " + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + ".");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(input)) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}