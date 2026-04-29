package org.example;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.example.command.FkCommand;
import org.example.command.FkSettingsCommand;
import org.example.command.HealCommand;
import org.example.command.FeedCommand;
import org.example.command.GameModeShortcutCommand;
import org.example.command.NickCommand;
import org.example.command.SkinCommand;
import org.example.command.TpallCommand;
import org.example.command.FlyCommand;
import org.example.game.GameStateManager;
import org.example.koth.KothCommand;
import org.example.koth.KothManager;
import org.example.listener.GameplayCapListener;
import org.example.listener.GameStateListener;
import org.example.listener.PauseProtectionListener;
import org.example.listener.PlayerStateListener;
import org.example.listener.PlayerStatsListener;
import org.example.listener.WorldDifficultyListener;
import org.example.listener.FlyGuiListener;
import org.example.map.MapVoteManager;
import org.example.settings.SettingsManager;
import org.example.settings.SettingsMenuListener;
import org.example.stats.PlayerStatsManager;
import org.example.storage.PluginDataStore;
import org.example.scoreboard.TeamSidebarManager;
import org.example.scoreboard.CapSidebarManager;
import org.example.team.TeamListener;
import org.example.team.TeamManager;

public class Tr0NFKPlugin extends JavaPlugin {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&2[&aFK&2] ");
    private SettingsManager settingsManager;
    private TeamManager teamManager;
    private TeamSidebarManager teamSidebarManager;
    private CapSidebarManager capSidebarManager;
    private FkSettingsCommand settingsCommand;
    private KothManager kothManager;
    private KothCommand kothCommand;
    private PluginDataStore dataStore;
    private GameStateManager gameStateManager;
    private GameplayCapListener gameplayCapListener;
    private PlayerStatsManager playerStatsManager;
    private MapVoteManager mapVoteManager;

    @Override
    public void onEnable() {
        settingsManager = new SettingsManager();
        playerStatsManager = new PlayerStatsManager();
        teamManager = new TeamManager(this, settingsManager);
        teamSidebarManager = new TeamSidebarManager(this, teamManager, settingsManager);
        capSidebarManager = new CapSidebarManager(this, settingsManager, teamManager, teamSidebarManager);
        settingsCommand = new FkSettingsCommand(settingsManager, teamManager);
        kothManager = new KothManager(this);
        kothCommand = new KothCommand(this, kothManager);
        settingsCommand.setPlugin(this);
        gameStateManager = new GameStateManager(this, settingsManager, settingsCommand, teamManager, kothManager);
        settingsCommand.setGameStateManager(gameStateManager);
        settingsCommand.setPlayerStatsManager(playerStatsManager);
        settingsCommand.setTeamSidebarManager(teamSidebarManager);
        gameStateManager.setTeamSidebarManager(teamSidebarManager);
        gameStateManager.setPlayerStatsManager(playerStatsManager);
        gameplayCapListener = new GameplayCapListener(this, teamManager, settingsManager, gameStateManager);
        teamManager.setGameStateManager(gameStateManager);
        dataStore = new PluginDataStore(this, teamManager, settingsManager, gameStateManager, gameplayCapListener, playerStatsManager, kothManager);
        dataStore.load();
        mapVoteManager = new MapVoteManager(this, dataStore, teamManager, teamSidebarManager, gameStateManager);
        settingsCommand.setPluginDataStore(dataStore);
        gameStateManager.setDataStore(dataStore);
        gameStateManager.enforceAdventureModeWhenStopped();

        teamManager.setSaveAction(new Runnable() {
            @Override
            public void run() {
                dataStore.save();
            }
        });
        settingsManager.setSaveAction(new Runnable() {
            @Override
            public void run() {
                dataStore.save();
            }
        });
        gameStateManager.setSaveAction(new Runnable() {
            @Override
            public void run() {
                dataStore.save();
            }
        });
        gameplayCapListener.setSaveAction(new Runnable() {
            @Override
            public void run() {
                dataStore.save();
            }
        });
        playerStatsManager.setSaveAction(new Runnable() {
            @Override
            public void run() {
                dataStore.save();
            }
        });

        getServer().getPluginManager().registerEvents(new TeamListener(teamManager), this);
        getServer().getPluginManager().registerEvents(gameplayCapListener, this);
        getServer().getPluginManager().registerEvents(new GameStateListener(gameStateManager), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(this, teamManager, settingsManager, gameStateManager, dataStore), this);
        getServer().getPluginManager().registerEvents(new PlayerStatsListener(playerStatsManager), this);
        getServer().getPluginManager().registerEvents(new SettingsMenuListener(this, settingsManager, settingsCommand), this);
        getServer().getPluginManager().registerEvents(new PauseProtectionListener(gameStateManager), this);
        getServer().getPluginManager().registerEvents(new WorldDifficultyListener(dataStore), this);
        getServer().getPluginManager().registerEvents(new FlyGuiListener(), this);
        getServer().getPluginManager().registerEvents(kothCommand, this);
        getServer().getPluginManager().registerEvents(mapVoteManager, this);

        new org.example.listener.StatsMenuListener(settingsCommand, this);
        settingsCommand.applyDayToWorlds();
        gameStateManager.startHud();
        teamSidebarManager.start();
        capSidebarManager.start();

        PluginCommand fkCommand = getCommand("fk");
        if (fkCommand != null) {
            FkCommand fkExecutor = new FkCommand(teamManager, settingsCommand, gameStateManager, kothCommand, kothManager, mapVoteManager, capSidebarManager);
            fkCommand.setExecutor(fkExecutor);
            fkCommand.setTabCompleter(fkExecutor);
        }

        PluginCommand healCommand = getCommand("heal");
        if (healCommand != null) {
            healCommand.setExecutor(new HealCommand());
        }

        PluginCommand feedCommand = getCommand("feed");
        if (feedCommand != null) {
            feedCommand.setExecutor(new FeedCommand());
        }

        PluginCommand tpallCommand = getCommand("tpall");
        if (tpallCommand != null) {
            tpallCommand.setExecutor(new TpallCommand());
        }

        PluginCommand gmcCommand = getCommand("gmc");
        if (gmcCommand != null) {
            gmcCommand.setExecutor(new GameModeShortcutCommand(GameMode.CREATIVE, "Creative"));
        }

        PluginCommand gmsCommand = getCommand("gms");
        if (gmsCommand != null) {
            gmsCommand.setExecutor(new GameModeShortcutCommand(GameMode.SURVIVAL, "Survival"));
        }

        PluginCommand gmspCommand = getCommand("gmsp");
        if (gmspCommand != null) {
            gmspCommand.setExecutor(new GameModeShortcutCommand(GameMode.SPECTATOR, "Spectator"));
        }

        PluginCommand gmaCommand = getCommand("gma");
        if (gmaCommand != null) {
            gmaCommand.setExecutor(new GameModeShortcutCommand(GameMode.ADVENTURE, "Adventure"));
        }

        PluginCommand nickCommand = getCommand("nick");
        if (nickCommand != null) {
            nickCommand.setExecutor(new NickCommand(teamManager));
        }

        PluginCommand skinCommand = getCommand("skin");
        if (skinCommand != null) {
            skinCommand.setExecutor(new SkinCommand());
        }

        PluginCommand flyCommand = getCommand("fly");
        if (flyCommand != null) {
            flyCommand.setExecutor(new FlyCommand());
        }

        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + "Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (teamSidebarManager != null) {
            teamSidebarManager.stop();
        }
        if (capSidebarManager != null) {
            capSidebarManager.stop();
        }
        if (gameStateManager != null) {
            gameStateManager.shutdown();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }
}
