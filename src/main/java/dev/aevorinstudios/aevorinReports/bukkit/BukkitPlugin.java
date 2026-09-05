package dev.aevorinstudios.aevorinReports.bukkit;

import dev.aevorinstudios.aevorinReports.commands.BukkitReportCommand;
import dev.aevorinstudios.aevorinReports.commands.BukkitReportsCommand;
import dev.aevorinstudios.aevorinReports.commands.ViewReportCommand;
import dev.aevorinstudios.aevorinReports.commands.SetReportStatusCommand;
import dev.aevorinstudios.aevorinReports.placeholders.AevorinReportsExpansion;

import dev.aevorinstudios.aevorinReports.config.ConfigManager;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.discord.DiscordManager;
import dev.aevorinstudios.aevorinReports.handlers.CustomReasonHandler;
import dev.aevorinstudios.aevorinReports.utils.ExceptionHandler;
import dev.aevorinstudios.aevorinReports.utils.ModrinthUpdateChecker;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.ErrorTracker;
import dev.faststats.data.Metric;
import dev.aevorinstudios.aevorinReports.reports.Report;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The Main Bukkit plugin class for AevorinReports
 * Handles initialization, configuration, and lifecycle management
 */
public class BukkitPlugin extends JavaPlugin implements org.bukkit.command.CommandExecutor {

    @Getter
    private static BukkitPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private ModrinthUpdateChecker updateChecker;
    @Getter
    private CustomReasonHandler customReasonHandler;
    @Getter
    private BukkitReportCommand bukkitReportCommand;
    @Getter
    private DiscordManager discordManager;

    // FastStats Metrics
    public static final ErrorTracker FAST_STATS_ERROR_TRACKER = ErrorTracker.contextAware();
    private BukkitContext fastStats;

    // Plugin state tracking
    private boolean databaseInitialized = false;
    private boolean configInitialized = false;

    @Override
    public void onEnable() {
        if (dev.aevorinstudios.aevorinReports.utils.RegionGuard.isRestricted()) {
            getLogger().severe("====================================================");
            getLogger().severe("AevorinReports is restricted in your region.");
            getLogger().severe("Plugin will not be enabled.");
            getLogger().severe("#FreePalestine");
            getLogger().severe("====================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            instance = this;
            getLogger().info("Initializing AevorinReports");

            // Initialize exception handler first with enhanced configuration
            initializeExceptionHandler();

            // Initialize configuration with validation
            if (!initializeConfig()) {
                getLogger().severe("Shutting down AevorinReports due to a critical configuration error.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // Initialize database connection
            if (!initializeDatabase()) {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // Start cross-server notification polling if using MySQL
            if ("mysql".equalsIgnoreCase(configManager.getConfig().getDatabase().getType())) {
                new dev.aevorinstudios.aevorinReports.tasks.NotificationPollingTask(this)
                    .runTaskTimerAsynchronously(this, 100L, 100L); // Poll every 5 seconds (100 ticks)
                getLogger().info("Started cross-server notification polling task.");
            }

            // Initialize CustomReasonHandler
            customReasonHandler = new CustomReasonHandler(this);

            // Initialize Discord integration
            discordManager = new DiscordManager(this);
            discordManager.start();

            // Register commands with error handling
            registerCommands();

            // Register listeners with validation
            registerListeners();

            // Register PlaceholderAPI expansion
            registerPlaceholderExpansion();

            // Register reload commands
            getCommand("ar").setExecutor(this);
            getCommand("aevorinreports").setExecutor(this);

            // Initialize and start the Modrinth update checker
            String modrinthProjectId = "OwqSnlXx"; // Hardcoded Project ID
            updateChecker = new ModrinthUpdateChecker(this, modrinthProjectId);
            updateChecker.startUpdateChecker();
            getLogger().info("Modrinth update checker initialized with project ID: " + modrinthProjectId);

            // Initialize bStats Metrics
            int pluginId = 28310;
            new org.bstats.bukkit.Metrics(this, pluginId);
            getLogger().info("bStats Metrics initialized properly.");

            // Initialize FastStats Metrics
            initializeFastStats();

            getLogger().info("AevorinReports has been enabled!");
        } catch (Exception e) {
            // Use our custom exception handler for startup errors with detailed context
            Map<String, Object> context = new HashMap<>();
            context.put("plugin_version", getDescription().getVersion());
            context.put("server_version", Bukkit.getVersion());
            context.put("config_initialized", configInitialized);
            context.put("database_initialized", databaseInitialized);

            ExceptionHandler.getInstance().handleException(e, "Plugin Startup", context);
            getLogger().severe("Failed to enable AevorinReports due to a critical error. Check the logs for details.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label,
            String[] args) {
        if ((command.getName().equalsIgnoreCase("ar") || command.getName().equalsIgnoreCase("aevorinreports"))
                && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            LanguageManager lang = LanguageManager.get(this);
            if (!sender.hasPermission("aevorinreports.reload")) {
                dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender,
                        lang.getMessage("messages.error.no-permission"));
                return true;
            }
            try {
                this.reloadConfig();
                if (configManager != null) {
                    configManager.loadConfig();
                }
                LanguageManager.reloadAll(this);
                if (customReasonHandler != null) {
                    // Refresh categories or other state if needed
                }
                dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender,
                        lang.getMessage("messages.admin.reload-success"));
            } catch (Exception e) {
                dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender,
                        lang.getMessage("messages.admin.reload-failure"));
                getLogger().warning("Error reloading configuration: " + e.getMessage());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AevorinReports");

        // Gracefully close database connections
        if (databaseManager != null) {
            try {
                getLogger().info("Closing database connections...");
                databaseManager.close();
                getLogger().info("Database connections closed successfully.");
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "Database Shutdown");
                getLogger().warning("Error while closing database connections.");
            }
        }

        // Stop Discord bot
        if (discordManager != null) {
            discordManager.stop();
        }

        // Shutdown FastStats
        if (fastStats != null) {
            fastStats.shutdown();
        }

        getLogger().info("AevorinReports has been disabled!");
    }

    /**
     * Initializes and validates the plugin configuration
     * 
     * @return true if configuration was successfully initialized, false otherwise
     */
    private boolean initializeConfig() {
        try {
            getLogger().info("Loading configuration files...");
            Path dataFolder = getDataFolder().toPath();
            configManager = ConfigManager.initialize(this, dataFolder);

            // Validate critical configuration sections
            boolean valid = validateConfiguration();

            LanguageManager.reloadAll(this);
            configInitialized = true;
            getLogger().info("Configuration loaded successfully" + (valid ? "." : " with warnings."));
            return valid;
        } catch (Exception e) {
            // Check if it's our critical config error
            if (e.getMessage() != null && e.getMessage().contains("CRITICAL CONFIGURATION ERROR")) {
                getLogger().severe(e.getMessage());
            } else {
                ExceptionHandler.getInstance().handleException(e, "Config Initialization");
            }
            return false;
        }
    }

    /**
     * Validates critical configuration sections
     * 
     * @return true if all critical configurations are valid, false otherwise
     */
    private boolean validateConfiguration() {
        boolean valid = true;

        // Validate database configuration
        String dbType = configManager.getConfig().getDatabase().getType();
        if (dbType == null || dbType.isEmpty()) {
            configManager.getConfig().getDatabase().setType("file"); // Default to file
            getLogger().warning("Database type not specified in config. Defaulting to SQLite.");
            // We validly fell back, so don't return false unless critical
        } else if ("mysql".equalsIgnoreCase(dbType)) {
            // Validate MySQL configuration
            if (configManager.getConfig().getDatabase().getMysql().getHost().isEmpty()) {
                getLogger().warning("MySQL host not specified in config.");
                valid = false;
            }
            if (configManager.getConfig().getDatabase().getMysql().getDatabase().isEmpty()) {
                getLogger().warning("MySQL database name not specified in config.");
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Initializes the database connection with a retry mechanism
     * 
     * @return true if a database was successfully initialized, false otherwise
     */
    private boolean initializeDatabase() {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                ConfigManager.Config.DatabaseConfig dbConfig = configManager.getConfig().getDatabase();
                if ("mysql".equalsIgnoreCase(dbConfig.getType())) {
                    ConfigManager.Config.DatabaseConfig.MySQLConfig mysqlConfig = dbConfig.getMysql();
                    databaseManager = new DatabaseManager(
                            mysqlConfig.getHost(),
                            mysqlConfig.getPort(),
                            mysqlConfig.getDatabase(),
                            mysqlConfig.getUsername(),
                            mysqlConfig.getPassword());
                } else {
                    ConfigManager.Config.DatabaseConfig.FileStorageConfig fileConfig = dbConfig.getFile();
                    databaseManager = new DatabaseManager(fileConfig.getPath());
                }

                if (databaseManager.testConnection()) {
                    databaseInitialized = true;
                    dev.aevorinstudios.aevorinReports.utils.ServerIdentity identity = new dev.aevorinstudios.aevorinReports.utils.ServerIdentity(
                            getLogger(), getDataFolder());
                    databaseManager.syncServerIdentity(identity.getIdentityToken(), configManager.getConfig().getServerName());
                    return true;
                } else {
                    throw new Exception("Connection test failed");
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    getLogger().severe("AevorinReports: Database connection failed. Please check your credentials in config.yml. Error: " + e.getMessage());
                    return false;
                } else {
                    try { Thread.sleep(2000 * retryCount); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return false;
    }

    /**
     * Registers plugin commands with error handling
     */
    private void registerCommands() {
        getLogger().info("Registering commands...");
        try {
            bukkitReportCommand = new BukkitReportCommand(this);
            getCommand("report").setExecutor(bukkitReportCommand);
            getCommand("report").setTabCompleter(bukkitReportCommand);
            getLogger().info("Registered 'report' command and tab completer");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration",
                    Map.of("command", "report", "class", "BukkitReportCommand"));
        }

        try {
            getCommand("reports").setExecutor(new BukkitReportsCommand(this));
            getLogger().info("Registered 'reports' command");
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration",
                    Map.of("command", "reports", "class", "BukkitReportsCommand"));
        }

        try {
            getCommand("viewreport").setExecutor(new ViewReportCommand(this));
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration",
                    Map.of("command", "viewreport", "class", "ViewReportCommand"));
        }

        try {
            SetReportStatusCommand setReportStatusCommand = new SetReportStatusCommand(this);
            getCommand("setreportstatus").setExecutor(setReportStatusCommand);
            getCommand("setreportstatus").setTabCompleter(setReportStatusCommand);
        } catch (Exception e) {
            ExceptionHandler.getInstance().handleException(e, "Command Registration",
                    Map.of("command", "setreportstatus", "class", "SetReportStatusCommand"));
        }

        getLogger().info("Commands registered successfully!");
    }

    /**
     * Registers event listeners
     */
    private void registerListeners() {
        getLogger().info("Registering event listeners...");

        // Register central container GUI listener
        getServer().getPluginManager()
                .registerEvents(new dev.aevorinstudios.aevorinReports.listeners.ReportsContainerListener(this), this);

        getLogger().info("Event listeners registered successfully!");
    }

    /**
     * Registers PlaceholderAPI expansion if PlaceholderAPI is available
     */
    private void registerPlaceholderExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found! Registering placeholders...");
            try {
                AevorinReportsExpansion expansion = new AevorinReportsExpansion(this);
                if (expansion.register()) {
                    getLogger().info("PlaceholderAPI expansion registered successfully!");
                } else {
                    getLogger().warning("Failed to register PlaceholderAPI expansion.");
                }
            } catch (Exception e) {
                ExceptionHandler.getInstance().handleException(e, "PlaceholderAPI Registration");
                getLogger().warning("Could not register PlaceholderAPI expansion: " + e.getMessage());
            }
        } else {
            getLogger().info("PlaceholderAPI not found. Skipping placeholder registration.");
        }
    }

    /**
     * Gets the configuration manager instance
     * 
     * @return The configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the database manager instance
     * 
     * @return The database manager
     * @throws IllegalStateException if a database is not initialized
     */
    public DatabaseManager getDatabaseManager() {
        if (!databaseInitialized) {
            getLogger().warning("Attempting to access database manager before initialization");
        }
        return databaseManager;
    }

    /**
     * Initialize the exception handler for better error reporting with enhanced
     * configuration
     */
    private void initializeExceptionHandler() {
        getLogger().info("Initializing enhanced exception handler...");

        // Get the exception handler instance and configure it
        ExceptionHandler handler = ExceptionHandler.getInstance();

        // Read configuration from config if available, otherwise use enhanced defaults
        int maxRepetitions = getConfig().getInt("error_handler.max_repetitions", 5);
        long suppressionMinutes = getConfig().getLong("error_handler.suppression_minutes", 15);
        boolean detailedLogging = getConfig().getBoolean("error_handler.detailed_logging", true);
        boolean logStackTraces = getConfig().getBoolean("error_handler.log_stack_traces", true);
        boolean groupSimilarErrors = getConfig().getBoolean("error_handler.group_similar_errors", true);

        // Configure with enhanced settings
        handler.configure(maxRepetitions, suppressionMinutes, detailedLogging, logStackTraces);
        handler.setGroupSimilarErrors(groupSimilarErrors);
    }

    /**
     * Initializes FastStats Metrics with custom metrics and error tracking
     */
    private void initializeFastStats() {
        try {
            // Note: The token should ideally be provided by the developer
            // Replace "YOUR_FASTSTATS_TOKEN" with your actual project token from
            // faststats.dev
            fastStats = new BukkitContext.Factory(this, "cdaa0f2024f6fc7c8e32992f30799c43")
                    .errorTrackerService(FAST_STATS_ERROR_TRACKER)
                    .metrics(f -> f
                            .addMetric(Metric.number("pending_reports",
                                    () -> {
                                        DatabaseManager db = getDatabaseManager();
                                        return db != null ? db.getReportCountByStatus(Report.ReportStatus.PENDING) : 0;
                                    }))
                            .addMetric(Metric.number("total_reports", () -> {
                                DatabaseManager db = getDatabaseManager();
                                return db != null ? db.getTotalReportsCount() : 0;
                            }))
                            .addMetric(Metric.string("gui_provider",
                                    () -> configManager != null && configManager.getConfig() != null
                                            && configManager.getConfig().getReports() != null
                                            && configManager.getConfig().getReports().getGui() != null
                                                    ? configManager.getConfig().getReports().getGui().getType()
                                                    : "unknown"))
                            .addMetric(Metric.string("db_backend",
                                    () -> configManager != null && configManager.getConfig() != null
                                            && configManager.getConfig().getDatabase() != null
                                                    ? configManager.getConfig().getDatabase().getType()
                                                    : "unknown"))
                            .addMetric(Metric.number("configured_categories",
                                    () -> configManager != null && configManager.getConfig() != null
                                            && configManager.getConfig().getReports() != null
                                            && configManager.getConfig().getReports().getCategories() != null
                                                    ? configManager.getConfig().getReports().getCategories().size()
                                                    : 0))
                            .addMetric(Metric.string("discord_integration",
                                    () -> configManager != null && configManager.getConfig() != null
                                            && configManager.getConfig().getDiscord() != null
                                                    ? String.valueOf(configManager.getConfig().getDiscord().isEnabled())
                                                    : "false"))
                            .create())
                    .create();

            fastStats.ready();
            getLogger().info("FastStats Metrics initialized and ready.");
        } catch (Exception e) {
            getLogger().warning("Could not initialize FastStats Metrics: " + e.getMessage());
        }
    }
}