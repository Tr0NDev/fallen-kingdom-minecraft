package org.example.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.example.game.GameStateManager;
import org.example.koth.KothCommand;
import org.example.koth.KothManager;
import org.example.map.MapVoteManager;
import org.example.scoreboard.CapSidebarManager;
import org.example.team.TeamManager;

public class FkCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
        private static final String[] ENCHANT_COMPLETIONS = new String[] {
            "ARROW_DAMAGE", "ARROW_FIRE", "ARROW_INFINITE", "ARROW_KNOCKBACK",
            "DAMAGE_ALL", "DAMAGE_ARTHROPODS", "DAMAGE_UNDEAD", "DIG_SPEED", "DURABILITY",
            "FIRE_ASPECT", "KNOCKBACK", "LOOT_BONUS_BLOCKS", "LOOT_BONUS_MOBS",
            "OXYGEN", "PROTECTION_ENVIRONMENTAL", "PROTECTION_EXPLOSIONS", "PROTECTION_FALL",
            "PROTECTION_FIRE", "PROTECTION_PROJECTILE", "SILK_TOUCH", "THORNS", "WATER_WORKER"
        };
    private final TeamManager teamManager;
    private final FkSettingsCommand settingsCommand;
    private final GameStateManager gameStateManager;
    private final KothCommand kothCommand;
    private final KothManager kothManager;
    private final MapVoteManager mapVoteManager;
    private final CapSidebarManager capSidebarManager;

    public FkCommand(TeamManager teamManager, FkSettingsCommand settingsCommand, GameStateManager gameStateManager,
            KothCommand kothCommand, KothManager kothManager, MapVoteManager mapVoteManager,
            CapSidebarManager capSidebarManager) {
        this.teamManager = teamManager;
        this.settingsCommand = settingsCommand;
        this.gameStateManager = gameStateManager;
        this.kothCommand = kothCommand;
        this.kothManager = kothManager;
        this.mapVoteManager = mapVoteManager;
        this.capSidebarManager = capSidebarManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "This command is only for players.");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("fk") && args.length == 0) {
            settingsCommand.openReadOnlySettingsMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 1 && args[0].equalsIgnoreCase("help")) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Available commands:");
            player.sendMessage(ChatColor.AQUA + "/fk" + ChatColor.GRAY + " - show settings menu");
            player.sendMessage(ChatColor.AQUA + "/fk help" + ChatColor.GRAY + " - show this help");
            player.sendMessage(ChatColor.AQUA + "/fk show" + ChatColor.GRAY + " - show base limits and capture zones");
            player.sendMessage(ChatColor.AQUA + "/fk stats" + ChatColor.GRAY + " - show global player stats");
            player.sendMessage(ChatColor.AQUA + "/fk map list" + ChatColor.GRAY + " - show all maps with info");
            player.sendMessage(ChatColor.AQUA + "/fk scoreboard" + ChatColor.GRAY + " - toggle cap scoreboard");
            if (player.isOp()) {
                player.sendMessage(ChatColor.AQUA + "/fk team" + ChatColor.GRAY + " - open team menu");
                player.sendMessage(ChatColor.AQUA + "/fk koth" + ChatColor.GRAY + " - open KOTH menu");
                player.sendMessage(ChatColor.AQUA + "/fk map" + ChatColor.GRAY + " - start/open map vote GUI");
                player.sendMessage(ChatColor.AQUA + "/fk settings" + ChatColor.GRAY + " - open game settings");
                player.sendMessage(ChatColor.AQUA + "/fk koth start" + ChatColor.GRAY + " - force start KOTH");
                player.sendMessage(ChatColor.AQUA + "/fk koth stop" + ChatColor.GRAY + " - stop KOTH");
                player.sendMessage(ChatColor.AQUA + "/fk setday <number>" + ChatColor.GRAY + " - set the current day");
                player.sendMessage(ChatColor.AQUA + "/fk start" + ChatColor.GRAY + " - start the game");
                player.sendMessage(ChatColor.AQUA + "/fk pause" + ChatColor.GRAY + " - pause the game");
                player.sendMessage(ChatColor.AQUA + "/fk resume" + ChatColor.GRAY + " - resume the game");
                player.sendMessage(ChatColor.AQUA + "/fk stop" + ChatColor.GRAY + " - stop the game");
                player.sendMessage(ChatColor.AQUA + "/fk map switch <map> [player|all]" + ChatColor.GRAY + " - switch map and teleport targets");
                player.sendMessage(ChatColor.AQUA + "/fk map timer <seconds>" + ChatColor.GRAY + " - set map vote timer");
                player.sendMessage(ChatColor.AQUA + "/fk creeper <0-100>" + ChatColor.GRAY + " - set charged creeper spawn chance");
                player.sendMessage(ChatColor.AQUA + "/fk item rename <name>" + ChatColor.GRAY + " - rename held item");
                player.sendMessage(ChatColor.AQUA + "/fk item enchant <enchant> <level>" + ChatColor.GRAY + " - enchant held item");
                player.sendMessage(ChatColor.DARK_GRAY + "------------------------------");
                player.sendMessage(ChatColor.AQUA + "/heal [player|all]" + ChatColor.GRAY + " - heal targets");
                player.sendMessage(ChatColor.AQUA + "/feed [player|all]" + ChatColor.GRAY + " - feed targets");
                player.sendMessage(ChatColor.AQUA + "/tpall" + ChatColor.GRAY + " - teleport all players to you");
                player.sendMessage(ChatColor.AQUA + "/gmc, /gms, /gmsp, /gma" + ChatColor.GRAY + " - gamemode shortcuts");
                player.sendMessage(ChatColor.AQUA + "/nick <name>" + ChatColor.GRAY + " - rename yourself");
                player.sendMessage(ChatColor.AQUA + "/skin <name>" + ChatColor.GRAY + " - copy another player's skin");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 1 && args[0].equalsIgnoreCase("show")) {
            teamManager.showBaseBoundsWithParticles(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 1 && args[0].equalsIgnoreCase("stats")) {
            settingsCommand.openGlobalStatsMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 1 && args[0].equalsIgnoreCase("scoreboard")) {
            if (capSidebarManager == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Scoreboard system is not available.");
                return true;
            }

            boolean enabled;
            if (args.length == 1) {
                enabled = capSidebarManager.toggle(player);
            } else if (args[1].equalsIgnoreCase("on")) {
                capSidebarManager.setEnabled(player, true);
                enabled = true;
            } else if (args[1].equalsIgnoreCase("off")) {
                capSidebarManager.setEnabled(player, false);
                enabled = false;
            } else {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk scoreboard [on|off]");
                return true;
            }

            player.sendMessage(PREFIX + ChatColor.GREEN + "Cap scoreboard is now "
                    + (enabled ? ChatColor.YELLOW + "enabled" : ChatColor.YELLOW + "disabled") + ChatColor.GREEN + ".");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 1 && args[0].equalsIgnoreCase("creeper")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }

            if (args.length != 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk creeper <0-100>");
                return true;
            }

            try {
                int chance = Integer.parseInt(args[1]);
                int appliedChance = settingsCommand.setChargedCreeperSpawnChance(chance);
                player.sendMessage(PREFIX + ChatColor.GREEN + "Charged creeper spawn chance set to "
                        + ChatColor.YELLOW + appliedChance + ChatColor.GREEN + "%.");
            } catch (NumberFormatException exception) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk creeper <0-100>");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 2 && args[0].equalsIgnoreCase("item")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }

            ItemStack item = player.getItemInHand();
            if (item == null || item.getType() == null || item.getType() == Material.AIR) {
                player.sendMessage(PREFIX + ChatColor.RED + "Hold an item in your hand first.");
                return true;
            }

            if (args[1].equalsIgnoreCase("rename")) {
                if (args.length < 3) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk item rename <name>");
                    return true;
                }

                StringBuilder nameBuilder = new StringBuilder();
                for (int index = 2; index < args.length; index++) {
                    if (index > 2) {
                        nameBuilder.append(' ');
                    }
                    nameBuilder.append(args[index]);
                }

                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "This item cannot be renamed.");
                    return true;
                }

                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nameBuilder.toString()));
                item.setItemMeta(meta);
                player.setItemInHand(item);
                player.sendMessage(PREFIX + ChatColor.GREEN + "Item renamed.");
                return true;
            }

            if (args[1].equalsIgnoreCase("enchant")) {
                if (args.length != 4) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk item enchant <enchant> <level>");
                    return true;
                }

                Enchantment enchantment = resolveEnchantment(args[2]);
                if (enchantment == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Unknown enchantment: " + args[2]);
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[3]);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) {
                        player.sendMessage(PREFIX + ChatColor.RED + "This item cannot be enchanted.");
                        return true;
                    }

                    meta.addEnchant(enchantment, level, true);
                    item.setItemMeta(meta);
                    player.setItemInHand(item);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Item enchanted with " + ChatColor.YELLOW
                            + enchantment.getName() + " " + level + ChatColor.GREEN + ".");
                } catch (NumberFormatException exception) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk item enchant <enchant> <level>");
                }
                return true;
            }

            player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk item rename <name> | /fk item enchant <enchant> <level>");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 1 && args[0].equalsIgnoreCase("map")) {
            if (mapVoteManager.isVoteActive()) {
                mapVoteManager.openVoteMenu(player);
                return true;
            }

            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }

            mapVoteManager.startVote(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 2 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("list")) {
            mapVoteManager.openMapListMenu(player);
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 1) {
            if (args[0].equalsIgnoreCase("team")) {
                teamManager.openTeamListMenu(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("settings")) {
                settingsCommand.openSettingsMenu(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("koth")) {
                kothCommand.openKothMenu(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("start")) {
                gameStateManager.start(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("pause")) {
                gameStateManager.pause();
                return true;
            }

            if (args[0].equalsIgnoreCase("resume")) {
                gameStateManager.resume();
                return true;
            }

            if (args[0].equalsIgnoreCase("stop")) {
                gameStateManager.stop();
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 2 && args[0].equalsIgnoreCase("koth") && args[1].equalsIgnoreCase("start")) {
            if (!kothManager.isEnabled()) {
                player.sendMessage(PREFIX + ChatColor.RED + "KOTH is not enabled!");
                return true;
            }
            if (kothManager.isActive()) {
                player.sendMessage(PREFIX + ChatColor.RED + "KOTH is already active!");
                return true;
            }
            if (kothManager.getZoneCenter() == null || kothManager.getZoneRadius() <= 0) {
                player.sendMessage(PREFIX + ChatColor.RED + "KOTH zone is not set!");
                return true;
            }
            if (kothManager.getRewardItems().isEmpty()) {
                player.sendMessage(PREFIX + ChatColor.RED + "No KOTH rewards configured!");
                return true;
            }
            kothManager.forceStartKoth();
            player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH started manually!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 2 && args[0].equalsIgnoreCase("koth") && args[1].equalsIgnoreCase("stop")) {
            if (!kothManager.isActive()) {
                player.sendMessage(PREFIX + ChatColor.RED + "KOTH is not active!");
                return true;
            }
            kothManager.stopKoth();
            player.sendMessage(PREFIX + ChatColor.GREEN + "KOTH stopped!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 2 && args[0].equalsIgnoreCase("setday")) {
            try {
                int day = Integer.parseInt(args[1]);
                settingsCommand.setDayFromCommand(player, day);
                kothManager.startKothIfApplicable(day);
            } catch (NumberFormatException exception) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk setday <number>");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("switch")) {
            if (args.length > 4) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk map switch <map> [player|all]");
                return true;
            }

            String requestedMap = args[2];
            List<Player> targets = new ArrayList<Player>();

            if (args.length == 3) {
                targets.add(player);
            } else if ("all".equalsIgnoreCase(args[3])) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online != null && online.isOnline()) {
                        targets.add(online);
                    }
                }
            } else {
                Player target = Bukkit.getPlayerExact(args[3]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[3]);
                    return true;
                }
                targets.add(target);
            }

            mapVoteManager.switchMapForPlayers(requestedMap, targets, player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("timer")) {
            try {
                int seconds = Integer.parseInt(args[2]);
                int applied = mapVoteManager.setVoteDurationSeconds(seconds);
                player.sendMessage(PREFIX + ChatColor.GREEN + "Map vote timer set to " + ChatColor.YELLOW + applied + ChatColor.GREEN + " seconds.");
            } catch (NumberFormatException exception) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk map timer <seconds>");
            }
            return true;
        }

        player.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: /fk, /fk help, /fk show, /fk stats, /fk team, /fk settings, /fk koth, /fk koth start, /fk koth stop, /fk setday <number>, /fk start, /fk pause, /fk resume, /fk stop, /fk map, /fk map list, /fk map switch <map> [player|all], /fk map timer <seconds>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("fk")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<String>();
            String input = args[0].toLowerCase();
            if ("team".startsWith(input)) {
                completions.add("team");
            }
            if ("settings".startsWith(input)) {
                completions.add("settings");
            }
            if ("koth".startsWith(input)) {
                completions.add("koth");
            }
            if ("start".startsWith(input)) {
                completions.add("start");
            }
            if ("pause".startsWith(input)) {
                completions.add("pause");
            }
            if ("resume".startsWith(input)) {
                completions.add("resume");
            }
            if ("stop".startsWith(input)) {
                completions.add("stop");
            }
            if ("show".startsWith(input)) {
                completions.add("show");
            }
            if ("stats".startsWith(input)) {
                completions.add("stats");
            }
            if ("setday".startsWith(input)) {
                completions.add("setday");
            }
            if ("map".startsWith(input)) {
                completions.add("map");
            }
            if ("item".startsWith(input)) {
                completions.add("item");
            }
            if ("scoreboard".startsWith(input)) {
                completions.add("scoreboard");
            }
            if ("creeper".startsWith(input)) {
                completions.add("creeper");
            }
            if ("help".startsWith(input)) {
                completions.add("help");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("rename".startsWith(input)) {
                completions.add("rename");
            }
            if ("enchant".startsWith(input)) {
                completions.add("enchant");
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("enchant")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            for (String enchantName : ENCHANT_COMPLETIONS) {
                if (enchantName.toLowerCase().startsWith(input)) {
                    completions.add(enchantName);
                }
            }
            for (String enchantAlias : getEnchantAliases()) {
                if (enchantAlias.toLowerCase().startsWith(input) && !completions.contains(enchantAlias)) {
                    completions.add(enchantAlias);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("scoreboard")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("on".startsWith(input)) {
                completions.add("on");
            }
            if ("off".startsWith(input)) {
                completions.add("off");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("creeper")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("0".startsWith(input)) {
                completions.add("0");
            }
            if ("25".startsWith(input)) {
                completions.add("25");
            }
            if ("50".startsWith(input)) {
                completions.add("50");
            }
            if ("75".startsWith(input)) {
                completions.add("75");
            }
            if ("100".startsWith(input)) {
                completions.add("100");
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("map")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("list".startsWith(input)) {
                completions.add("list");
            }
            if ("switch".startsWith(input)) {
                completions.add("switch");
            }
            if ("timer".startsWith(input)) {
                completions.add("timer");
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("switch")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            for (String mapName : mapVoteManager.getConfiguredMapNames()) {
                if (mapName.toLowerCase().startsWith(input)) {
                    completions.add(mapName);
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("timer")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            String current = String.valueOf(mapVoteManager.getVoteDurationSeconds());
            if (current.startsWith(input)) {
                completions.add(current);
            }
            if ("60".startsWith(input)) {
                completions.add("60");
            }
            if ("90".startsWith(input)) {
                completions.add("90");
            }
            if ("120".startsWith(input)) {
                completions.add("120");
            }
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("switch")) {
            List<String> completions = new ArrayList<String>();
            String input = args[3].toLowerCase();
            if ("all".startsWith(input)) {
                completions.add("all");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(input)) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("koth")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("start".startsWith(input)) {
                completions.add("start");
            }
            if ("stop".startsWith(input)) {
                completions.add("stop");
            }
            return completions;
        }

        return Collections.emptyList();
    }

    private Enchantment resolveEnchantment(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.equals("SHARPNESS")) {
            normalized = "DAMAGE_ALL";
        } else if (normalized.equals("SMITE")) {
            normalized = "DAMAGE_UNDEAD";
        } else if (normalized.equals("BANE_OF_ARTHROPODS") || normalized.equals("BANE")) {
            normalized = "DAMAGE_ARTHROPODS";
        } else if (normalized.equals("EFFICIENCY")) {
            normalized = "DIG_SPEED";
        } else if (normalized.equals("UNBREAKING")) {
            normalized = "DURABILITY";
        } else if (normalized.equals("FORTUNE")) {
            normalized = "LOOT_BONUS_BLOCKS";
        } else if (normalized.equals("LOOTING")) {
            normalized = "LOOT_BONUS_MOBS";
        } else if (normalized.equals("RESPIRATION")) {
            normalized = "OXYGEN";
        } else if (normalized.equals("PROTECTION")) {
            normalized = "PROTECTION_ENVIRONMENTAL";
        } else if (normalized.equals("POWER")) {
            normalized = "ARROW_DAMAGE";
        } else if (normalized.equals("PUNCH")) {
            normalized = "ARROW_KNOCKBACK";
        } else if (normalized.equals("INFINITY")) {
            normalized = "ARROW_INFINITE";
        } else if (normalized.equals("FLAME")) {
            normalized = "ARROW_FIRE";
        } else if (normalized.equals("FIRE_PROTECTION")) {
            normalized = "PROTECTION_FIRE";
        } else if (normalized.equals("BLAST_PROTECTION")) {
            normalized = "PROTECTION_EXPLOSIONS";
        } else if (normalized.equals("PROJECTILE_PROTECTION")) {
            normalized = "PROTECTION_PROJECTILE";
        } else if (normalized.equals("AQUA_AFFINITY")) {
            normalized = "WATER_WORKER";
        }

        Enchantment enchantment = Enchantment.getByName(normalized);
        if (enchantment != null) {
            return enchantment;
        }

        for (Enchantment candidate : Enchantment.values()) {
            if (candidate != null && candidate.getName() != null && candidate.getName().equalsIgnoreCase(input)) {
                return candidate;
            }
        }

        return null;
    }

    private List<String> getEnchantAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("sharpness");
        aliases.add("smite");
        aliases.add("bane");
        aliases.add("efficiency");
        aliases.add("unbreaking");
        aliases.add("fortune");
        aliases.add("looting");
        aliases.add("protection");
        aliases.add("fire_protection");
        aliases.add("blast_protection");
        aliases.add("projectile_protection");
        aliases.add("power");
        aliases.add("punch");
        aliases.add("flame");
        aliases.add("infinity");
        aliases.add("respiration");
        aliases.add("aqua_affinity");
        aliases.add("depth_strider");
        aliases.add("thorns");
        aliases.add("silk_touch");
        return aliases;
    }
}
