package org.example.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.team.TeamManager;

public class NickCommand implements CommandExecutor {

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

        if (args.length != 1) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /nick <name>");
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

        teamManager.setPlayerNickname(player.getUniqueId(), requested);
        teamManager.updatePlayerDisplayNames();
        teamManager.refreshPlayerAppearance(player);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Nickname updated to " + ChatColor.YELLOW + requested + ChatColor.GREEN + ".");
        return true;
    }
}
