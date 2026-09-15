package com.fistpractice;

import com.fistpractice.arena.ArenaManager;
import com.fistpractice.commands.*;
import com.fistpractice.configuration.GameModeManager;
import com.fistpractice.database.DatabaseManager;
import com.fistpractice.duel.DuelManager;
import com.fistpractice.gui.MenuManager;
import com.fistpractice.history.HistoryManager;
import com.fistpractice.hooks.DiscordWebhook;
import com.fistpractice.hooks.PlaceholderHook;
import com.fistpractice.kit.KitManager;
import com.fistpractice.listeners.CombatListener;
import com.fistpractice.listeners.GuiListener;
import com.fistpractice.listeners.MatchListener;
import com.fistpractice.listeners.PlayerConnectionListener;
import com.fistpractice.match.MatchManager;
import com.fistpractice.party.PartyManager;
import com.fistpractice.profile.ProfileManager;
import com.fistpractice.queue.QueueManager;
import com.fistpractice.ranking.RatingManager;
import com.fistpractice.spectator.SpectatorManager;
import com.fistpractice.tournament.TournamentManager;
import com.fistpractice.utilities.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class FistPractice extends JavaPlugin {

    private static FistPractice instance;

    private DatabaseManager databaseManager;
    private GameModeManager gameModeManager;
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private ProfileManager profileManager;
    private RatingManager ratingManager;
    private HistoryManager historyManager;
    private MatchManager matchManager;
    private DuelManager duelManager;
    private PartyManager partyManager;
    private QueueManager queueManager;
    private SpectatorManager spectatorManager;
    private TournamentManager tournamentManager;
    private MenuManager menuManager;
    private PlaceholderHook placeholderHook;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Core / data layer first
        MessageUtil.init(this);
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        gameModeManager = new GameModeManager(this);
        gameModeManager.loadModes();

        kitManager = new KitManager(this);
        kitManager.loadKits();

        arenaManager = new ArenaManager(this);
        arenaManager.loadArenas();

        profileManager = new ProfileManager(this);
        ratingManager = new RatingManager(this);
        historyManager = new HistoryManager(this);

        // Gameplay layer
        matchManager = new MatchManager(this);
        duelManager = new DuelManager(this);
        partyManager = new PartyManager(this);
        queueManager = new QueueManager(this);
        queueManager.start();
        spectatorManager = new SpectatorManager(this);
        tournamentManager = new TournamentManager(this);

        // GUI
        menuManager = new MenuManager(this);

        // Hooks
        if (getConfig().getBoolean("hooks.placeholderapi", true)
                && getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderHook = new PlaceholderHook(this);
            placeholderHook.register();
        }
        discordWebhook = new DiscordWebhook(this);

        registerListeners();
        registerCommands();

        getLogger().info("FistPractice enabled - " + gameModeManager.getAll().size() + " modes, "
                + kitManager.getAll().size() + " kits, " + arenaManager.getAll().size() + " arenas loaded.");
    }

    @Override
    public void onDisable() {
        if (matchManager != null) {
            // Best-effort: forfeit / restore everyone still mid-match on shutdown.
            matchManager.getActiveMatches().forEach(m -> m.getAllPlayers().forEach(uuid -> {
                var player = getServer().getPlayer(uuid);
                if (player != null && m.getSavedStates().containsKey(uuid)) {
                    m.getSavedStates().get(uuid).restore(player);
                }
            }));
        }
        if (profileManager != null) {
            getServer().getOnlinePlayers().forEach(p -> profileManager.unload(p.getUniqueId()));
        }
        if (databaseManager != null) databaseManager.shutdown();
        MessageUtil.shutdown();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MatchListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    }

    private void registerCommands() {
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("queue").setExecutor(new QueueCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("profile").setExecutor(new ProfileCommand(this));
        getCommand("stats").setExecutor(new ProfileCommand(this));
        getCommand("matchhistory").setExecutor(new MatchHistoryCommand(this));
        getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("tournament").setExecutor(new TournamentCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("fistpractice").setExecutor(new AdminCommand(this));
    }

    private org.bukkit.configuration.file.YamlConfiguration menusConfig;

    private org.bukkit.configuration.file.YamlConfiguration menus() {
        if (menusConfig == null) {
            java.io.File file = new java.io.File(getDataFolder(), "menus.yml");
            if (!file.exists()) saveResource("menus.yml", false);
            menusConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        }
        return menusConfig;
    }

    public int getMenusConfigInt(String path, int def) { return menus().getInt(path, def); }
    public String getMenusConfigString(String path, String def) { return menus().getString(path, def); }

    public void reload() {
        reloadConfig();
        MessageUtil.reload();
        gameModeManager.loadModes();
        kitManager.loadKits();
        arenaManager.loadArenas();
        ratingManager.reload();
        menusConfig = null;
    }

    public static FistPractice getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public GameModeManager getGameModeManager() { return gameModeManager; }
    public KitManager getKitManager() { return kitManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public RatingManager getRatingManager() { return ratingManager; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public SpectatorManager getSpectatorManager() { return spectatorManager; }
    public TournamentManager getTournamentManager() { return tournamentManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public DiscordWebhook getDiscordWebhook() { return discordWebhook; }
}
