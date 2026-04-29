package org.example.command;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameModeShortcutCommand implements CommandExecutor {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private final GameMode targetGameMode;
    private final String modeLabel;

    public GameModeShortcutCommand(GameMode targetGameMode, String modeLabel) {
        this.targetGameMode = targetGameMode;
        this.modeLabel = modeLabel;
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

        if (args.length > 1) {
            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /" + label.toLowerCase() + " [player|all]");
            return true;
        }

        if (args.length == 0) {
            player.setGameMode(targetGameMode);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Your game mode is now " + ChatColor.YELLOW + modeLabel + ChatColor.GREEN + ".");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online != null && online.isOnline()) {
                    online.setGameMode(targetGameMode);
                    online.sendMessage(PREFIX + ChatColor.GREEN + "Your game mode is now " + ChatColor.YELLOW + modeLabel + ChatColor.GREEN + ".");
                }
            }
            player.sendMessage(PREFIX + ChatColor.GREEN + "Applied " + ChatColor.YELLOW + modeLabel + ChatColor.GREEN + " to all online players.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }

        target.setGameMode(targetGameMode);
        target.sendMessage(PREFIX + ChatColor.GREEN + "Your game mode is now " + ChatColor.YELLOW + modeLabel + ChatColor.GREEN + ".");
        player.sendMessage(PREFIX + ChatColor.GREEN + "Set " + ChatColor.YELLOW + target.getName() + ChatColor.GREEN + " to " + ChatColor.YELLOW + modeLabel + ChatColor.GREEN + ".");
        return true;
    }
}
