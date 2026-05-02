package org.example.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
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
import org.example.command.InvViewCommand;

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
    private final InvViewCommand invViewCommand;

    public FkCommand(TeamManager teamManager, FkSettingsCommand settingsCommand, GameStateManager gameStateManager,
                     KothCommand kothCommand, KothManager kothManager, MapVoteManager mapVoteManager,
                     CapSidebarManager capSidebarManager, InvViewCommand invViewCommand) {
        this.teamManager = teamManager;
        this.settingsCommand = settingsCommand;
        this.gameStateManager = gameStateManager;
        this.kothCommand = kothCommand;
        this.kothManager = kothManager;
        this.mapVoteManager = mapVoteManager;
        this.capSidebarManager = capSidebarManager;
        this.invViewCommand = invViewCommand;
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
                player.sendMessage(ChatColor.AQUA + "/fk map clone <map> [newname]" + ChatColor.GRAY + " - clone a map config");
                player.sendMessage(ChatColor.AQUA + "/fk map load <map>" + ChatColor.GRAY + " - load a world into memory");
                player.sendMessage(ChatColor.AQUA + "/fk map load list" + ChatColor.GRAY + " - list all currently loaded worlds");
                player.sendMessage(ChatColor.AQUA + "/fk map unload <map>" + ChatColor.GRAY + " - unload a world from memory");
                player.sendMessage(ChatColor.AQUA + "/fk map delete <map>" + ChatColor.GRAY + " - delete a map permanently");
                player.sendMessage(ChatColor.AQUA + "/fk inv <player>" + ChatColor.GRAY + " - view and manage a player inventory live");
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

        if (command.getName().equalsIgnoreCase("fk") && args.length == 3
                && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("delete")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }
            deleteMap(player, args[2]);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 3
                && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("unload")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }
            unloadMap(player, args[2]);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 2
                && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("load")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }

            if (args.length == 2) {
                player.sendMessage(PREFIX + ChatColor.RED + "Usage: /fk map load <map> | /fk map load list");
                return true;
            }

            if (args[2].equalsIgnoreCase("list")) {
                listLoadedWorlds(player);
                return true;
            }

            loadMap(player, args[2]);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length >= 3
                && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("clone")) {
            if (!player.isOp()) {
                player.sendMessage(PREFIX + ChatColor.RED + "You do not have the required permissions.");
                return true;
            }

            String sourceMap = args[2];
            String destMap = (args.length >= 4) ? args[3] : null;

            player.sendMessage(PREFIX + ChatColor.YELLOW + "Loading, cloning map...");
            cloneMap(player, sourceMap, destMap);
            return true;
        }

        if (command.getName().equalsIgnoreCase("fk") && args.length == 2 && args[0].equalsIgnoreCase("inv")) {
            return invViewCommand.handle(player, args);
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

        player.sendMessage(PREFIX + ChatColor.YELLOW + "Usage: /fk, /fk help, /fk show, /fk stats, /fk team, /fk settings, /fk koth, /fk koth start, /fk koth stop, /fk setday <number>, /fk start, /fk pause, /fk resume, /fk stop, /fk map, /fk map list, /fk map switch <map> [player|all], /fk map timer <seconds>, /fk map clone <map> [newname], /fk map load <map>, /fk map load list, /fk map unload <map>, /fk map delete <map>, /fk inv <player>");
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
            if ("team".startsWith(input)) completions.add("team");
            if ("settings".startsWith(input)) completions.add("settings");
            if ("koth".startsWith(input)) completions.add("koth");
            if ("start".startsWith(input)) completions.add("start");
            if ("pause".startsWith(input)) completions.add("pause");
            if ("resume".startsWith(input)) completions.add("resume");
            if ("stop".startsWith(input)) completions.add("stop");
            if ("show".startsWith(input)) completions.add("show");
            if ("stats".startsWith(input)) completions.add("stats");
            if ("setday".startsWith(input)) completions.add("setday");
            if ("map".startsWith(input)) completions.add("map");
            if ("item".startsWith(input)) completions.add("item");
            if ("scoreboard".startsWith(input)) completions.add("scoreboard");
            if ("creeper".startsWith(input)) completions.add("creeper");
            if ("help".startsWith(input)) completions.add("help");
            if ("inv".startsWith(input)) completions.add("inv");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("inv")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(input)) {
                    completions.add(online.getName());
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("rename".startsWith(input)) completions.add("rename");
            if ("enchant".startsWith(input)) completions.add("enchant");
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
            if ("on".startsWith(input)) completions.add("on");
            if ("off".startsWith(input)) completions.add("off");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("creeper")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("0".startsWith(input)) completions.add("0");
            if ("25".startsWith(input)) completions.add("25");
            if ("50".startsWith(input)) completions.add("50");
            if ("75".startsWith(input)) completions.add("75");
            if ("100".startsWith(input)) completions.add("100");
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("map")) {
            List<String> completions = new ArrayList<String>();
            String input = args[1].toLowerCase();
            if ("list".startsWith(input)) completions.add("list");
            if ("switch".startsWith(input)) completions.add("switch");
            if ("timer".startsWith(input)) completions.add("timer");
            if ("clone".startsWith(input)) completions.add("clone");
            if ("delete".startsWith(input)) completions.add("delete");
            if ("unload".startsWith(input)) completions.add("unload");
            if ("load".startsWith(input)) completions.add("load");
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("load")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            if ("list".startsWith(input)) completions.add("list");
            java.io.File worldContainer = Bukkit.getWorldContainer();
            java.io.File[] worldFolders = worldContainer.listFiles();
            if (worldFolders != null) {
                for (java.io.File folder : worldFolders) {
                    if (!folder.isDirectory()) continue;
                    if (!new java.io.File(folder, "level.dat").exists()) continue;
                    String folderName = folder.getName();
                    if (Bukkit.getWorld(folderName) != null) continue;
                    if (folderName.toLowerCase().startsWith(input)) {
                        completions.add(folderName);
                    }
                }
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

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("delete")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            for (String mapName : mapVoteManager.getConfiguredMapNames()) {
                if (mapName.toLowerCase().startsWith(input)) {
                    completions.add(mapName);
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("unload")) {
            List<String> completions = new ArrayList<String>();
            String input = args[2].toLowerCase();
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().startsWith(input)) {
                    completions.add(w.getName());
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("clone")) {
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
            if (current.startsWith(input)) completions.add(current);
            if ("60".startsWith(input)) completions.add("60");
            if ("90".startsWith(input)) completions.add("90");
            if ("120".startsWith(input)) completions.add("120");
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("map") && args[1].equalsIgnoreCase("switch")) {
            List<String> completions = new ArrayList<String>();
            String input = args[3].toLowerCase();
            if ("all".startsWith(input)) completions.add("all");
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
            if ("start".startsWith(input)) completions.add("start");
            if ("stop".startsWith(input)) completions.add("stop");
            return completions;
        }

        return Collections.emptyList();
    }

    private void loadMap(Player player, String mapName) {
        if (Bukkit.getWorld(mapName) != null) {
            player.sendMessage(PREFIX + ChatColor.RED + "World \"" + mapName + "\" is already loaded.");
            return;
        }

        java.io.File worldContainer = Bukkit.getWorldContainer();
        java.io.File worldFolder = new java.io.File(worldContainer, mapName);

        if (!worldFolder.exists() || !worldFolder.isDirectory()) {
            player.sendMessage(PREFIX + ChatColor.RED + "No world folder found for \"" + mapName + "\".");
            return;
        }

        if (!new java.io.File(worldFolder, "level.dat").exists()) {
            player.sendMessage(PREFIX + ChatColor.RED + "The folder \"" + mapName + "\" does not appear to be a valid world (missing level.dat).");
            return;
        }

        java.io.File sessionLock = new java.io.File(worldFolder, "session.lock");
        if (sessionLock.exists()) {
            sessionLock.delete();
        }

        player.sendMessage(PREFIX + ChatColor.YELLOW + "Loading world \"" + mapName + "\"...");
        org.bukkit.World loaded = Bukkit.createWorld(new WorldCreator(mapName));

        if (loaded != null) {
            player.sendMessage(PREFIX + ChatColor.GREEN + "World \"" + mapName + "\" loaded successfully.");
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Failed to load world \"" + mapName + "\".");
        }
    }

    private void listLoadedWorlds(Player player) {
        List<org.bukkit.World> worlds = Bukkit.getWorlds();
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Loaded worlds (" + worlds.size() + "):");
        for (org.bukkit.World world : worlds) {
            int players = world.getPlayers().size();
            player.sendMessage(ChatColor.GRAY + "  - " + ChatColor.AQUA + world.getName()
                    + ChatColor.GRAY + " | players: " + ChatColor.WHITE + players
                    + ChatColor.GRAY + " | env: " + ChatColor.WHITE + world.getEnvironment().name());
        }
    }

    private void unloadMap(Player player, String mapName) {
        org.bukkit.World world = Bukkit.getWorld(mapName);
        if (world == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "World \"" + mapName + "\" is not loaded.");
            return;
        }

        if (Bukkit.getWorlds().get(0).equals(world)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Cannot unload the default world.");
            return;
        }

        org.bukkit.World fallback = Bukkit.getWorlds().get(0);
        org.bukkit.Location fallbackLoc = fallback.getSpawnLocation();

        for (org.bukkit.entity.Player online : new java.util.ArrayList<>(world.getPlayers())) {
            online.teleport(fallbackLoc);
            online.sendMessage(PREFIX + ChatColor.YELLOW + "The map \"" + mapName + "\" is being unloaded, you have been teleported.");
        }

        world.save();
        boolean success = Bukkit.unloadWorld(world, true);

        if (success) {
            player.sendMessage(PREFIX + ChatColor.GREEN + "World \"" + mapName + "\" unloaded successfully.");
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Failed to unload world \"" + mapName + "\". It may still have players or be protected.");
        }
    }

    private void cloneMap(Player player, String sourceMap, String destName) {
        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass());
        java.io.File pluginFolder = plugin.getDataFolder();

        java.io.File mapYml = new java.io.File(pluginFolder, "maps.yml");
        if (!mapYml.exists()) {
            player.sendMessage(PREFIX + ChatColor.RED + "maps.yml not found in plugin folder.");
            return;
        }

        org.bukkit.configuration.file.YamlConfiguration mapConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(mapYml);
        java.util.List<String> mapList = mapConfig.getStringList("maps");

        boolean sourceFound = false;
        for (String m : mapList) {
            if (m.equalsIgnoreCase(sourceMap)) {
                sourceFound = true;
                break;
            }
        }
        if (!sourceFound) {
            player.sendMessage(PREFIX + ChatColor.RED + "Source map \"" + sourceMap + "\" not found in maps.yml.");
            return;
        }

        if (destName == null || destName.trim().isEmpty()) {
            destName = resolveCloneName(sourceMap, mapList, pluginFolder);
        }
        final String finalDest = destName;

        for (String m : mapList) {
            if (m.equalsIgnoreCase(finalDest)) {
                player.sendMessage(PREFIX + ChatColor.RED + "A map named \"" + finalDest + "\" already exists in maps.yml.");
                return;
            }
        }

        mapList.add(finalDest);
        mapConfig.set("maps", mapList);
        try {
            mapConfig.save(mapYml);
        } catch (java.io.IOException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Error while saving maps.yml: " + e.getMessage());
            return;
        }

        for (String suffix : new String[]{"", "_nether", "_the_end"}) {
            org.bukkit.World loadedWorld = org.bukkit.Bukkit.getWorld(sourceMap + suffix);
            if (loadedWorld != null) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Saving and unloading \"" + sourceMap + suffix + "\"...");
                org.bukkit.World fallback = org.bukkit.Bukkit.getWorlds().get(0);
                org.bukkit.Location fallbackLoc = fallback.getSpawnLocation();
                for (org.bukkit.entity.Player online : new java.util.ArrayList<org.bukkit.entity.Player>(loadedWorld.getPlayers())) {
                    online.teleport(fallbackLoc);
                    online.sendMessage(PREFIX + ChatColor.YELLOW + "Temporarily teleported for map cloning.");
                }
                loadedWorld.save();
                org.bukkit.Bukkit.unloadWorld(loadedWorld, true);
            }
        }

        java.io.File worldContainer = org.bukkit.Bukkit.getWorldContainer();

        java.io.File sourceWorld = new java.io.File(worldContainer, sourceMap);
        java.io.File destWorld   = new java.io.File(worldContainer, finalDest);
        boolean overworldCopied = false;
        if (sourceWorld.exists() && sourceWorld.isDirectory()) {
            if (destWorld.exists()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Folder \"" + finalDest + "\" already exists, skipped.");
            } else {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Copying world folder...");
                try {
                    copyDirectory(sourceWorld, destWorld);
                    java.io.File sessionLock = new java.io.File(destWorld, "session.lock");
                    if (sessionLock.exists()) { sessionLock.delete(); }
                    overworldCopied = true;
                } catch (java.io.IOException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Error copying world folder: " + e.getMessage());
                    return;
                }
            }
        } else {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "No folder \"" + sourceMap + "\" found at server root, skipped.");
        }

        java.io.File sourceNether = new java.io.File(worldContainer, sourceMap + "_nether");
        java.io.File destNether   = new java.io.File(worldContainer, finalDest + "_nether");
        boolean netherCopied = false;
        if (sourceNether.exists() && sourceNether.isDirectory()) {
            if (destNether.exists()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Nether folder already exists, skipped.");
            } else {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Copying nether folder...");
                try {
                    copyDirectory(sourceNether, destNether);
                    java.io.File sessionLock = new java.io.File(destNether, "session.lock");
                    if (sessionLock.exists()) { sessionLock.delete(); }
                    netherCopied = true;
                } catch (java.io.IOException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Error copying nether folder: " + e.getMessage());
                }
            }
        }

        java.io.File sourceEnd = new java.io.File(worldContainer, sourceMap + "_the_end");
        java.io.File destEnd   = new java.io.File(worldContainer, finalDest + "_the_end");
        boolean endCopied = false;
        if (sourceEnd.exists() && sourceEnd.isDirectory()) {
            if (destEnd.exists()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "End folder already exists, skipped.");
            } else {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Copying end folder...");
                try {
                    copyDirectory(sourceEnd, destEnd);
                    java.io.File sessionLock = new java.io.File(destEnd, "session.lock");
                    if (sessionLock.exists()) { sessionLock.delete(); }
                    endCopied = true;
                } catch (java.io.IOException e) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Error copying end folder: " + e.getMessage());
                }
            }
        }

        java.io.File profilesDir   = new java.io.File(pluginFolder, "map-profiles");
        java.io.File sourceProfile = new java.io.File(profilesDir, sourceMap + ".yml");
        java.io.File destProfile   = new java.io.File(profilesDir, finalDest + ".yml");
        boolean profileCopied = false;

        if (sourceProfile.exists()) {
            try {
                profilesDir.mkdirs();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(new java.io.FileInputStream(sourceProfile), "UTF-8"));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                String text = content.toString();
                text = text.replaceAll(
                        "(?m)(:\\s+)" + java.util.regex.Pattern.quote(sourceMap) + "(?=\\s*$)",
                        "$1" + finalDest
                );
                java.io.BufferedWriter writer = new java.io.BufferedWriter(
                        new java.io.OutputStreamWriter(new java.io.FileOutputStream(destProfile), "UTF-8"));
                writer.write(text);
                writer.close();
                profileCopied = true;
            } catch (java.io.IOException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Error while copying profile: " + e.getMessage());
                return;
            }
        } else {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "No profile found for \"" + sourceMap + "\", map-profiles skipped.");
        }

        player.sendMessage(PREFIX + ChatColor.GREEN + "Cloning completed!");
        player.sendMessage(PREFIX + ChatColor.YELLOW + sourceMap + ChatColor.GREEN + " -> " + ChatColor.YELLOW + finalDest + ChatColor.GREEN + " :");
        player.sendMessage(ChatColor.GRAY + "  ✔ maps.yml updated");
        if (profileCopied)   player.sendMessage(ChatColor.GRAY + "  ✔ map-profiles/" + finalDest + ".yml copied and adapted");
        if (overworldCopied) player.sendMessage(ChatColor.GRAY + "  ✔ world folder " + finalDest + "/ copied");
        if (netherCopied)    player.sendMessage(ChatColor.GRAY + "  ✔ nether folder " + finalDest + "_nether/ copied");
        if (endCopied)       player.sendMessage(ChatColor.GRAY + "  ✔ end folder " + finalDest + "_the_end/ copied");
        player.sendMessage(PREFIX + ChatColor.GRAY + "Reload config to apply.");
    }

    private void replaceWorldNameInConfig(org.bukkit.configuration.file.YamlConfiguration config, String sourceWorld, String destWorld) {
        replaceInSection(config, config, sourceWorld, destWorld);
    }

    private void replaceInSection(org.bukkit.configuration.ConfigurationSection root, org.bukkit.configuration.ConfigurationSection section, String sourceWorld, String destWorld) {
        for (String key : new java.util.ArrayList<>(section.getKeys(false))) {
            Object value = section.get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                org.bukkit.configuration.ConfigurationSection child = (org.bukkit.configuration.ConfigurationSection) value;
                if (key.equals(sourceWorld)) {
                    String parentPath = section.getCurrentPath();
                    String newPath = (parentPath == null || parentPath.isEmpty()) ? destWorld : parentPath + "." + destWorld;
                    java.util.Map<String, Object> values = child.getValues(true);
                    section.set(key, null);
                    org.bukkit.configuration.ConfigurationSection newSection = root.createSection(newPath);
                    for (java.util.Map.Entry<String, Object> entry : values.entrySet()) {
                        Object val = entry.getValue();
                        if (val instanceof String) {
                            val = replaceWorldInString((String) val, sourceWorld, destWorld);
                        }
                        newSection.set(entry.getKey(), val);
                    }
                } else {
                    replaceInSection(root, child, sourceWorld, destWorld);
                }
            } else if (value instanceof String) {
                String replaced = replaceWorldInString((String) value, sourceWorld, destWorld);
                if (!replaced.equals(value)) {
                    section.set(key, replaced);
                }
            } else if (value instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) value;
                java.util.List<Object> newList = new java.util.ArrayList<>();
                boolean changed = false;
                for (Object item : list) {
                    if (item instanceof String) {
                        String replaced = replaceWorldInString((String) item, sourceWorld, destWorld);
                        newList.add(replaced);
                        if (!replaced.equals(item)) changed = true;
                    } else {
                        newList.add(item);
                    }
                }
                if (changed) {
                    section.set(key, newList);
                }
            }
        }
    }

    private String replaceWorldInString(String value, String sourceWorld, String destWorld) {
        if (value == null) return null;
        if (value.equalsIgnoreCase(sourceWorld)) return destWorld;
        return value;
    }

    private void deleteMap(Player player, String mapName) {
        org.bukkit.plugin.java.JavaPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass());
        java.io.File pluginFolder = plugin.getDataFolder();

        java.io.File mapYml = new java.io.File(pluginFolder, "maps.yml");
        if (!mapYml.exists()) {
            player.sendMessage(PREFIX + ChatColor.RED + "maps.yml not found.");
            return;
        }
        org.bukkit.configuration.file.YamlConfiguration mapConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(mapYml);
        java.util.List<String> mapList = mapConfig.getStringList("maps");
        String exactName = null;
        for (String m : mapList) {
            if (m.equalsIgnoreCase(mapName)) { exactName = m; break; }
        }
        if (exactName == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Map \"" + mapName + "\" not found in maps.yml.");
            return;
        }

        final String finalExactName = exactName;

        for (String suffix : new String[]{"", "_nether", "_the_end"}) {
            org.bukkit.World loaded = org.bukkit.Bukkit.getWorld(finalExactName + suffix);
            if (loaded != null) {
                org.bukkit.World fallback = org.bukkit.Bukkit.getWorlds().get(0);
                org.bukkit.Location fallbackLoc = fallback.getSpawnLocation();
                for (org.bukkit.entity.Player online : new java.util.ArrayList<>(loaded.getPlayers())) {
                    online.teleport(fallbackLoc);
                    online.sendMessage(PREFIX + ChatColor.RED + "The map \"" + finalExactName + "\" is being deleted, you have been teleported.");
                }
                loaded.save();
                org.bukkit.Bukkit.unloadWorld(loaded, false);
            }
        }

        mapList.remove(exactName);
        mapConfig.set("maps", mapList);
        try {
            mapConfig.save(mapYml);
        } catch (java.io.IOException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Error saving maps.yml: " + e.getMessage());
            return;
        }

        java.io.File profile = new java.io.File(new java.io.File(pluginFolder, "map-profiles"), exactName + ".yml");
        boolean profileDeleted = profile.exists() && profile.delete();

        player.sendMessage(PREFIX + ChatColor.YELLOW + "Waiting for server to flush chunks before deleting...");

        final java.io.File worldContainer = org.bukkit.Bukkit.getWorldContainer();
        final String playerName = player.getName();
        final boolean profileDeletedFinal = profileDeleted;

        org.bukkit.plugin.java.JavaPlugin pluginRef = plugin;
        pluginRef.getServer().getScheduler().runTaskLater(pluginRef, () -> {
            Player p = org.bukkit.Bukkit.getPlayerExact(playerName);
            boolean overworldDeleted = false;
            boolean netherDeleted    = false;
            boolean endDeleted       = false;

            for (String suffix : new String[]{"", "_nether", "_the_end"}) {
                java.io.File dir = new java.io.File(worldContainer, finalExactName + suffix);
                if (!dir.exists()) continue;
                java.io.File sessionLock = new java.io.File(dir, "session.lock");
                if (sessionLock.exists()) sessionLock.delete();
                boolean deleted = deleteDirectoryWithRetry(dir, 5, 300);
                if (suffix.isEmpty())              overworldDeleted = deleted;
                else if (suffix.equals("_nether")) netherDeleted    = deleted;
                else                               endDeleted       = deleted;
            }

            if (p != null && p.isOnline()) {
                p.sendMessage(PREFIX + ChatColor.RED + "Map \"" + finalExactName + "\" deleted:");
                p.sendMessage(ChatColor.GRAY + "  ✔ maps.yml updated");
                if (profileDeletedFinal) p.sendMessage(ChatColor.GRAY + "  ✔ map-profiles/" + finalExactName + ".yml deleted");
                if (overworldDeleted)    p.sendMessage(ChatColor.GRAY + "  ✔ folder " + finalExactName + "/ deleted");
                else                     p.sendMessage(ChatColor.YELLOW + "  ⚠ folder " + finalExactName + "/ not found or partially deleted");
                if (netherDeleted)       p.sendMessage(ChatColor.GRAY + "  ✔ folder " + finalExactName + "_nether/ deleted");
                if (endDeleted)          p.sendMessage(ChatColor.GRAY + "  ✔ folder " + finalExactName + "_the_end/ deleted");
            }
        }, 60L);
    }

    private boolean deleteDirectoryWithRetry(java.io.File dir, int maxRetries, long delayMs) {
        if (dir == null || !dir.exists()) return false;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            if (deleteDirectory(dir)) return true;
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
        }
        return !dir.exists();
    }

    private boolean deleteDirectory(java.io.File dir) {
        if (dir == null || !dir.exists()) return false;
        if (dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        return dir.delete();
    }

    private void copyDirectory(java.io.File src, java.io.File dst) throws java.io.IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            java.io.File[] children = src.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    if (child.getName().equalsIgnoreCase("uid.dat")) continue;
                    copyDirectory(child, new java.io.File(dst, child.getName()));
                }
            }
        } else {
            java.io.InputStream  in  = new java.io.FileInputStream(src);
            java.io.OutputStream out = new java.io.FileOutputStream(dst);
            byte[] buffer = new byte[8192];
            int length;
            try {
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            } finally {
                in.close();
                out.close();
            }
        }
    }

    private String resolveCloneName(String sourceMap, java.util.List<String> existingMaps, java.io.File pluginFolder) {
        String base = sourceMap.replaceAll("\\d+$", "");
        if (base.isEmpty()) base = sourceMap;

        java.io.File profilesDir = new java.io.File(pluginFolder, "map-profiles");
        int index = 1;
        while (true) {
            String candidate = base + index;
            boolean takenInList = false;
            for (String m : existingMaps) {
                if (m.equalsIgnoreCase(candidate)) { takenInList = true; break; }
            }
            java.io.File candidateFile = new java.io.File(profilesDir, candidate + ".yml");
            if (!takenInList && !candidateFile.exists()) return candidate;
            index++;
        }
    }

    private Enchantment resolveEnchantment(String input) {
        if (input == null) return null;
        String normalized = input.trim().toUpperCase();
        if (normalized.isEmpty()) return null;
        if (normalized.equals("SHARPNESS"))                                             normalized = "DAMAGE_ALL";
        else if (normalized.equals("SMITE"))                                            normalized = "DAMAGE_UNDEAD";
        else if (normalized.equals("BANE_OF_ARTHROPODS") || normalized.equals("BANE")) normalized = "DAMAGE_ARTHROPODS";
        else if (normalized.equals("EFFICIENCY"))                                       normalized = "DIG_SPEED";
        else if (normalized.equals("UNBREAKING"))                                       normalized = "DURABILITY";
        else if (normalized.equals("FORTUNE"))                                          normalized = "LOOT_BONUS_BLOCKS";
        else if (normalized.equals("LOOTING"))                                          normalized = "LOOT_BONUS_MOBS";
        else if (normalized.equals("RESPIRATION"))                                      normalized = "OXYGEN";
        else if (normalized.equals("PROTECTION"))                                       normalized = "PROTECTION_ENVIRONMENTAL";
        else if (normalized.equals("POWER"))                                            normalized = "ARROW_DAMAGE";
        else if (normalized.equals("PUNCH"))                                            normalized = "ARROW_KNOCKBACK";
        else if (normalized.equals("INFINITY"))                                         normalized = "ARROW_INFINITE";
        else if (normalized.equals("FLAME"))                                            normalized = "ARROW_FIRE";
        else if (normalized.equals("FIRE_PROTECTION"))                                  normalized = "PROTECTION_FIRE";
        else if (normalized.equals("BLAST_PROTECTION"))                                 normalized = "PROTECTION_EXPLOSIONS";
        else if (normalized.equals("PROJECTILE_PROTECTION"))                            normalized = "PROTECTION_PROJECTILE";
        else if (normalized.equals("AQUA_AFFINITY"))                                    normalized = "WATER_WORKER";
        Enchantment enchantment = Enchantment.getByName(normalized);
        if (enchantment != null) return enchantment;
        for (Enchantment candidate : Enchantment.values()) {
            if (candidate != null && candidate.getName() != null && candidate.getName().equalsIgnoreCase(input)) {
                return candidate;
            }
        }
        return null;
    }

    private List<String> getEnchantAliases() {
        List<String> aliases = new ArrayList<String>();
        aliases.add("sharpness"); aliases.add("smite"); aliases.add("bane");
        aliases.add("efficiency"); aliases.add("unbreaking"); aliases.add("fortune");
        aliases.add("looting"); aliases.add("protection"); aliases.add("fire_protection");
        aliases.add("blast_protection"); aliases.add("projectile_protection");
        aliases.add("power"); aliases.add("punch"); aliases.add("flame");
        aliases.add("infinity"); aliases.add("respiration"); aliases.add("aqua_affinity");
        aliases.add("depth_strider"); aliases.add("thorns"); aliases.add("silk_touch");
        return aliases;
    }
}