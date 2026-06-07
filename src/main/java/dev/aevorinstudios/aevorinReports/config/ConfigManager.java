package dev.aevorinstudios.aevorinReports.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile ConfigManager instance;
    private static final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock();

    private final Path configPath;
    private Config config;
    private final Plugin plugin;

    private ConfigManager(Plugin plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.configPath = dataDirectory.resolve("config.yml");
        loadConfig();
    }

    public static ConfigManager initialize(Plugin plugin, Path dataDirectory) {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager(plugin, dataDirectory);
                }
            }
        }
        return instance;
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager has not been initialized. Call initialize() first.");
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private Config convertYamlToConfig(Map<String, Object> yamlConfig) {
        Config config = new Config();

        if (yamlConfig == null)
            return config;

        // Server Configuration
        if (yamlConfig.containsKey("server-name")) {
            config.setServerName(asString(yamlConfig.get("server-name"), "survival"));
        } else {
            config.setServerName("survival"); // Default if missing
        }

        // Configuration Version
        Object versionObj = yamlConfig.get("config-version");
        if (versionObj != null) {
            try {
                if (versionObj instanceof Number) {
                    config.setConfigVersion(((Number) versionObj).intValue());
                } else {
                    config.setConfigVersion(Integer.parseInt(String.valueOf(versionObj)));
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid 'config-version' value: '" + versionObj + "'. It must be an integer.");
            }
        } else {
            config.setConfigVersion(0);
        }

        // Database Configuration
        if (yamlConfig.containsKey("database")) {
            Map<String, Object> db = (Map<String, Object>) yamlConfig.get("database");
            if (db != null) {
                config.getDatabase().setType(asString(db.get("type"), "file"));

                // MySQL Config
                if (db.containsKey("mysql")) {
                    Map<String, Object> mysql = (Map<String, Object>) db.get("mysql");
                    if (mysql != null) {
                        config.getDatabase().getMysql().setHost(asString(mysql.get("host"), "localhost"));
                        config.getDatabase().getMysql()
                                .setPort(asInt(mysql.get("port"), 3306));
                        config.getDatabase().getMysql()
                                .setDatabase(asString(mysql.get("database"), "aevorinreports"));
                        config.getDatabase().getMysql().setUsername(asString(mysql.get("username"), "root"));
                        config.getDatabase().getMysql().setPassword(asString(mysql.get("password"), ""));
                    }
                }

                // File Storage Config
                if (db.containsKey("file")) {
                    Map<String, Object> file = (Map<String, Object>) db.get("file");
                    if (file != null) {
                        config.getDatabase().getFile()
                                .setPath(asString(file.get("path"), "database/reports.db"));
                    }
                }
            }
        }

        // Reports Configuration
        if (yamlConfig.containsKey("reports")) {
            Map<String, Object> reports = (Map<String, Object>) yamlConfig.get("reports");
            if (reports != null) {
                config.getReports().setCooldownSeconds(asInt(reports.get("cooldown"), 300));
                config.getReports().setAllowAnonymousReports(asBoolean(reports.get("allow-anonymous"), true));
                config.getReports()
                        .setMaxActiveReportsPerPlayer(asInt(reports.get("max-active-reports"), 3));
                config.getReports().setChatHistoryLines(asInt(reports.get("chat-history-lines"), 50));
                config.getReports().setLogInventory(asBoolean(reports.get("logInventory"), true));
                config.getReports().setLogLocation(asBoolean(reports.get("logLocation"), true));
                config.getReports().setAllowSelfReporting(asBoolean(reports.get("allow-self-reporting"), false));
                config.getReports().setAllowOfflinePlayerReporting(asBoolean(reports.get("allow-offline-player-reporting"), true));

                if (reports.containsKey("gui")) {
                    Map<String, Object> gui = (Map<String, Object>) reports.get("gui");
                    if (gui != null) {
                        config.getReports().getGui().setType(asString(gui.get("type"), "book"));
                    }
                }

                if (reports.containsKey("categories")) {
                    List<?> categories = (List<?>) reports.get("categories");
                    if (categories != null) {
                        List<String> categoryStrings = new ArrayList<>();
                        for (Object obj : categories) {
                            categoryStrings.add(String.valueOf(obj));
                        }
                        config.getReports().setCategories(categoryStrings);
                    }
                }
            }
        }

        // Notifications Configuration
        if (yamlConfig.containsKey("notifications")) {
            Map<String, Object> notifications = (Map<String, Object>) yamlConfig.get("notifications");
            if (notifications != null) {
                config.getNotifications().setEnableChatNotifications(
                        asBoolean(notifications.get("enableChatNotifications"), true));
                config.getNotifications().setEnableTitleNotifications(
                        asBoolean(notifications.get("enableTitleNotifications"), true));
                config.getNotifications().setEnableSoundNotifications(
                        asBoolean(notifications.get("enableSoundNotifications"), true));
                config.getNotifications().setNotificationSound(
                        asString(notifications.get("notificationSound"), "BLOCK_NOTE_BLOCK_PLING"));
            }
        }

        // Update Checker Configuration
        if (yamlConfig.containsKey("update-checker")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> updateChecker = (Map<String, Object>) yamlConfig.get("update-checker");
            if (updateChecker != null) {
                config.getUpdateChecker().setCheckInterval(asInt(updateChecker.get("check-interval"), 60));
                config.getUpdateChecker().setNotifyOnJoin(asBoolean(updateChecker.get("notify-on-join"), true));
                config.getUpdateChecker().setUpdateChannel(asString(updateChecker.get("update-channel"), "release"));
            }
        }

        // Custom Reasons
        if (yamlConfig.containsKey("customReasons")) {
            Map<?, ?> customReasonsRaw = (Map<?, ?>) yamlConfig.get("customReasons");
            Map<String, String> customReasons = new HashMap<>();
            if (customReasonsRaw != null) {
                for (Map.Entry<?, ?> entry : customReasonsRaw.entrySet()) {
                    customReasons.put(
                            String.valueOf(entry.getKey()),
                            String.valueOf(entry.getValue()));
                }
                config.setCustomReasons(new HashMap<>(customReasons));
            }
        }

        // Discord Configuration
        if (yamlConfig.containsKey("discord")) {
            Map<String, Object> discordRaw = (Map<String, Object>) yamlConfig.get("discord");
            if (discordRaw != null) {
                config.getDiscord().setEnabled(asBoolean(discordRaw.get("enabled"), false));
                config.getDiscord().setBotToken(asString(discordRaw.get("bot-token"), ""));
                config.getDiscord().setChannelId(asString(discordRaw.get("channel-id"), ""));
                config.getDiscord().setLogChannelId(asString(discordRaw.get("log-channel-id"), ""));
                config.getDiscord().setStaffRoleId(asString(discordRaw.get("staff-role-id"), ""));
                config.getDiscord().setLookupColor(asString(discordRaw.get("lookup-color"), "#00ffff"));

                if (discordRaw.containsKey("bot-settings")) {
                    Map<String, Object> botRaw = (Map<String, Object>) discordRaw.get("bot-settings");
                    if (botRaw != null) {
                        config.getDiscord().getBotSettings()
                                .setStatus(asString(botRaw.get("status"), "ONLINE"));
                        if (botRaw.containsKey("activity")) {
                            Map<String, Object> activityRaw = (Map<String, Object>) botRaw.get("activity");
                            if (activityRaw != null) {
                                config.getDiscord().getBotSettings().getActivity()
                                        .setType(asString(activityRaw.get("type"), "WATCHING"));
                                config.getDiscord().getBotSettings().getActivity()
                                        .setMessage(asString(activityRaw.get("message"), "Reports"));
                            }
                        }
                    }
                }

                if (discordRaw.containsKey("notifications")) {
                    Map<String, Object> notifyRaw = (Map<String, Object>) discordRaw.get("notifications");
                    if (notifyRaw != null) {
                        config.getDiscord().getNotifications()
                                .setTitle(asString(notifyRaw.get("title"), "New Report (#%id%)"));
                        config.getDiscord().getNotifications()
                                .setColor(asString(notifyRaw.get("color"), "#ff5555"));
                        config.getDiscord().getNotifications()
                                .setFooter(asString(notifyRaw.get("footer"), "AevorinReports • %date%"));
                    }
                }

                if (discordRaw.containsKey("network-mode")) {
                    Map<String, Object> networkRaw = (Map<String, Object>) discordRaw.get("network-mode");
                    if (networkRaw != null) {
                        config.getDiscord().getNetworkMode()
                                .setEnabled(asBoolean(networkRaw.get("enabled"), false));
                        config.getDiscord().getNetworkMode().setPollInterval(
                                asInt(networkRaw.get("poll-interval"), 10));
                    }
                }
            }
        }

        return config;
    }

    private void checkForUpdates() {
        if (!Files.exists(configPath))
            return;

        try {
            // Load bundled default config from JAR
            Map<String, Object> defaultConfigMap;
            try (InputStream inputStream = getClass().getResourceAsStream("/config.yml")) {
                if (inputStream == null)
                    return;
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                defaultConfigMap = yaml.load(inputStream);
            }

            // Load user config
            Map<String, Object> userConfigMap;
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                userConfigMap = yaml.load(inputStream);
            }

            if (userConfigMap == null || defaultConfigMap == null)
                return;

            Object userVersionObj = userConfigMap.getOrDefault("config-version", 0);
            int userVersion;
            try {
                if (userVersionObj instanceof Number) {
                    userVersion = ((Number) userVersionObj).intValue();
                } else {
                    userVersion = Integer.parseInt(String.valueOf(userVersionObj));
                }
            } catch (NumberFormatException e) {
                logger.error(
                        "Invalid 'config-version' in config.yml: '{}'. Skipping update check to prevent corruption.",
                        userVersionObj);
                return;
            }

            Object jarVersionObj = defaultConfigMap.getOrDefault("config-version", 0);
            int jarVersion = (jarVersionObj instanceof Number) ? ((Number) jarVersionObj).intValue()
                    : Integer.parseInt(String.valueOf(jarVersionObj));

            if (userVersion < jarVersion) {
                logger.info("Configuration update detected (v{} -> v{}). Creating backup...", userVersion, jarVersion);

                // 1. Create a backup
                Path backupPath = configPath.resolveSibling("config.yml.old");
                Files.copy(configPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 2. Perform Migration steps if needed before merging (structural changes)
                applyMigrations(userConfigMap, userVersion);

                // 3. Update using our comment-preserving ConfigUpdater
                ConfigUpdater.update(plugin, "config.yml", configPath.toFile());
                logger.info("Configuration successfully updated while preserving comments.");
            }
        } catch (Exception e) {
            logger.error("Failed to update configuration: {}. Please check your config.yml manually.", e.getMessage());
        }
    }

    private void applyMigrations(Map<String, Object> config, int version) {
        if (version < 2) {
            // New update-checker settings added in v2
            if (!config.containsKey("update-checker")) {
                Map<String, Object> updateChecker = new HashMap<>();
                updateChecker.put("check-interval", 60);
                updateChecker.put("notify-on-join", true);
                updateChecker.put("update-channel", "release");
                config.put("update-checker", updateChecker);
            }
        }
        if (version < 5) {
            // Version 5: Prefix moved to language files
            if (config.containsKey("notifications")) {
                Object rawNotifications = config.get("notifications");
                if (rawNotifications instanceof Map) {
                    ((Map<String, Object>) rawNotifications).remove("prefix");
                }
            }
        }
    }

    private String asString(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int asInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return (int) Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public void loadConfig() {
        configLock.writeLock().lock();
        try {
            if (!Files.exists(configPath)) {
                logger.info("Configuration file not found, creating default config.yml from template");
                plugin.saveResource("config.yml", false);
            }

            // Check for updates and merge before loading into memory
            checkForUpdates();

            logger.info("Loading configuration from {}", configPath);
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setAllowDuplicateKeys(false);
                loaderOptions.setMaxAliasesForCollections(50);

                Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
                Object loadedYaml = yaml.load(inputStream);

                if (!(loadedYaml instanceof Map)) {
                    throw new IllegalStateException("Invalid configuration format: Root element must be a mapping");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> yamlConfig = (Map<String, Object>) loadedYaml;

                try {
                    config = convertYamlToConfig(yamlConfig);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to convert configuration: " + e.getMessage(), e);
                }

                try {
                    validateConfiguration();
                } catch (Exception e) {
                    throw new IllegalStateException("Configuration validation failed: " + e.getMessage(), e);
                }

                logger.info("Configuration loaded successfully");

            } catch (IOException e) {
                throw new IllegalStateException("Failed to read configuration file: " + e.getMessage(), e);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (Files.exists(configPath)) {
                // If the file exists but is invalid, DO NOT overwrite it.
                // Log a clean error and let the plugin shutdown.
                logger.error("CRITICAL CONFIGURATION ERROR: {}", e.getMessage());
                logger.error("The plugin will NOT start to prevent overwriting your existing configuration.");
                logger.error("Please fix the error in your config.yml and restart the server.");
                throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: " + e.getMessage(), e);
            } else {
                // Only load defaults if the file is missing
                logger.warn("Configuration file missing or empty. Loading default configuration.");
                config = createDefaultConfig();
                try {
                    saveConfig();
                } catch (Exception ex) {
                    logger.error("Failed to save default configuration: {}", ex.getMessage(), ex);
                }
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }

    public Config getConfig() {
        configLock.readLock().lock();
        try {
            return config;
        } finally {
            configLock.readLock().unlock();
        }
    }

    private Config createDefaultConfig() {
        Config defaultConfig = new Config();
        return defaultConfig;
    }

    private void validateConfiguration() throws IllegalStateException {
        if (config == null) {
            throw new IllegalStateException("Configuration not loaded");
        }

        if (config.getDatabase().getType().equals("mysql") &&
                (config.getDatabase().getMysql().getPassword().isEmpty() ||
                        config.getDatabase().getMysql().getUsername().isEmpty())) {
            throw new IllegalStateException("MySQL credentials must be provided");
        }

        validateDatabaseConfig();
        validatePerformanceConfig();
        validateReportsConfig();
        validateUpdateCheckerConfig();
    }

    private void validateUpdateCheckerConfig() {
        Config.UpdateCheckerConfig updateChecker = config.getUpdateChecker();
        if (updateChecker.getCheckInterval() < 1) {
            logger.warn("Invalid update check-interval {}, defaulting to 60", updateChecker.getCheckInterval());
            updateChecker.setCheckInterval(60);
        }
        String channel = updateChecker.getUpdateChannel().toLowerCase();
        if (!channel.equals("all") && !channel.equals("alpha") && !channel.equals("beta")
                && !channel.equals("release")) {
            logger.warn("Invalid update channel '{}', defaulting to 'release'", updateChecker.getUpdateChannel());
            updateChecker.setUpdateChannel("release");
        }
    }

    private void validateDatabaseConfig() {
        Config.DatabaseConfig db = config.getDatabase();
        if (!"mysql".equals(db.getType()) && !"file".equals(db.getType())) {
            logger.warn("Invalid database type '{}', defaulting to 'file'", db.getType());
            db.setType("file");
        }

        if ("mysql".equals(db.getType())) {
            // Correct class path: MySQLConfig is nested inside DatabaseConfig
            Config.DatabaseConfig.MySQLConfig mysql = db.getMysql();
            if (mysql.getPort() <= 0 || mysql.getPort() > 65535) {
                logger.warn("Invalid MySQL port {}, defaulting to 3306", mysql.getPort());
                mysql.setPort(3306);
            }
        }
    }

    private void validatePerformanceConfig() {
        Config.PerformanceConfig performance = config.getPerformance();
        if (performance.getCacheDuration() < 0) {
            logger.warn("Invalid cacheDuration value, setting to default");
            performance.setCacheDuration(15);
        }
        if (performance.getMaxCacheSize() < 0) {
            logger.warn("Invalid maxCacheSize value, setting to default");
            performance.setMaxCacheSize(1000);
        }
        if (performance.getBatchSize() < 1) {
            logger.warn("Invalid batchSize value, setting to default");
            performance.setBatchSize(50);
        }
        if (performance.getBackgroundTaskInterval() < 0) {
            logger.warn("Invalid backgroundTaskInterval value, setting to default");
            performance.setBackgroundTaskInterval(300);
        }
        if (performance.getCacheCleanupInterval() < 0) {
            logger.warn("Invalid cacheCleanupInterval value, setting to default");
            performance.setCacheCleanupInterval(30);
        }
    }

    private void validateReportsConfig() {
        Config.ReportsConfig reports = config.getReports();
        if (reports.getCooldownSeconds() < 0) {
            logger.warn("Invalid cooldownSeconds value, setting to default");
            reports.setCooldownSeconds(300);
        }
        if (reports.getMaxActiveReportsPerPlayer() < 1) {
            logger.warn("Invalid maxActiveReportsPerPlayer value, setting to default");
            reports.setMaxActiveReportsPerPlayer(3);
        }
        if (reports.getChatHistoryLines() < 0) {
            logger.warn("Invalid chatHistoryLines value, setting to default");
            reports.setChatHistoryLines(50);
        }
    }

    public void saveConfig() {
        configLock.writeLock().lock();
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            Map<String, Object> configMap = convertConfigToYaml();

            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, yaml.dump(configMap));

            logger.info("Configuration saved successfully to {}", configPath);
        } catch (IOException e) {
            logger.error("Failed to save configuration: {}", e.getMessage(), e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertConfigToYaml() {
        Map<String, Object> result = new HashMap<>();

        // Server Configuration
        result.put("server-name", config.getServerName());
        result.put("config-version", config.getConfigVersion());

        // Database Configuration
        Map<String, Object> database = new HashMap<>();
        database.put("type", config.getDatabase().getType());

        // MySQL Config
        Map<String, Object> mysql = new HashMap<>();
        mysql.put("host", config.getDatabase().getMysql().getHost());
        mysql.put("port", config.getDatabase().getMysql().getPort());
        mysql.put("database", config.getDatabase().getMysql().getDatabase());
        mysql.put("username", config.getDatabase().getMysql().getUsername());
        mysql.put("password", config.getDatabase().getMysql().getPassword());
        database.put("mysql", mysql);

        // File Storage Config
        Map<String, Object> file = new HashMap<>();
        file.put("path", config.getDatabase().getFile().getPath());
        database.put("file", file);

        result.put("database", database);

        // Reports Configuration
        Map<String, Object> reports = new HashMap<>();
        reports.put("cooldown", config.getReports().getCooldownSeconds());
        reports.put("allow-anonymous", config.getReports().isAllowAnonymousReports());
        reports.put("max-active-reports", config.getReports().getMaxActiveReportsPerPlayer());
        reports.put("chat-history-lines", config.getReports().getChatHistoryLines());
        reports.put("logInventory", config.getReports().isLogInventory());
        reports.put("logLocation", config.getReports().isLogLocation());
        reports.put("allow-offline-player-reporting", config.getReports().isAllowOfflinePlayerReporting());
        result.put("reports", reports);

        // Notifications Configuration
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("enableChatNotifications", config.getNotifications().isEnableChatNotifications());
        notifications.put("enableTitleNotifications", config.getNotifications().isEnableTitleNotifications());
        notifications.put("enableSoundNotifications", config.getNotifications().isEnableSoundNotifications());
        notifications.put("notificationSound", config.getNotifications().getNotificationSound());
        result.put("notifications", notifications);

        // Performance Configuration
        Map<String, Object> performance = new HashMap<>();
        performance.put("enableCaching", config.getPerformance().isEnableCaching());
        performance.put("cacheDuration", config.getPerformance().getCacheDuration());
        performance.put("maxCacheSize", config.getPerformance().getMaxCacheSize());
        performance.put("asyncProcessing", config.getPerformance().isAsyncProcessing());
        performance.put("batchSize", config.getPerformance().getBatchSize());
        performance.put("backgroundTaskInterval", config.getPerformance().getBackgroundTaskInterval());
        performance.put("cacheCleanupInterval", config.getPerformance().getCacheCleanupInterval());
        result.put("performance", performance);

        // Custom Reasons
        if (config.getCustomReasons() != null) {
            result.put("customReasons", new HashMap<>(config.getCustomReasons()));
        }

        // Discord Configuration
        Map<String, Object> discord = new HashMap<>();
        discord.put("enabled", config.getDiscord().isEnabled());
        discord.put("bot-token", config.getDiscord().getBotToken());
        discord.put("channel-id", config.getDiscord().getChannelId());
        discord.put("log-channel-id", config.getDiscord().getLogChannelId());
        discord.put("staff-role-id", config.getDiscord().getStaffRoleId());
        discord.put("lookup-color", config.getDiscord().getLookupColor());

        Map<String, Object> botSettings = new HashMap<>();
        botSettings.put("status", config.getDiscord().getBotSettings().getStatus());
        Map<String, Object> activity = new HashMap<>();
        activity.put("type", config.getDiscord().getBotSettings().getActivity().getType());
        activity.put("message", config.getDiscord().getBotSettings().getActivity().getMessage());
        botSettings.put("activity", activity);
        discord.put("bot-settings", botSettings);

        Map<String, Object> discordNotify = new HashMap<>();
        discordNotify.put("title", config.getDiscord().getNotifications().getTitle());
        discordNotify.put("color", config.getDiscord().getNotifications().getColor());
        discordNotify.put("footer", config.getDiscord().getNotifications().getFooter());
        discord.put("notifications", discordNotify);

        Map<String, Object> networkMode = new HashMap<>();
        networkMode.put("enabled", config.getDiscord().getNetworkMode().isEnabled());
        networkMode.put("poll-interval", config.getDiscord().getNetworkMode().getPollInterval());
        discord.put("network-mode", networkMode);

        // Update Checker Configuration
        Map<String, Object> updateChecker = new HashMap<>();
        updateChecker.put("check-interval", config.getUpdateChecker().getCheckInterval());
        updateChecker.put("notify-on-join", config.getUpdateChecker().isNotifyOnJoin());
        updateChecker.put("update-channel", config.getUpdateChecker().getUpdateChannel());
        result.put("update-checker", updateChecker);

        result.put("discord", discord);


        return result;
    }


    public @Nullable String getMessage(String key) {
        // Return a proper message based on the key
        // This is a placeholder implementation - in a real scenario, this would
        // retrieve from a messages file
        if (key == null) {
            return "";
        }

        // Default messages for common keys
        Map<String, String> defaultMessages = new HashMap<>();
        defaultMessages.put("messages.book.main-title", "Report Management");
        defaultMessages.put("messages.book.group-title", "Reports: {status}");
        defaultMessages.put("messages.book.view-details", "Click to view details");
        defaultMessages.put("messages.book.view-group", "View {count} {status} reports");
        defaultMessages.put("messages.errors.no-reports", "No {status} reports found.");
        defaultMessages.put("messages.errors.book-meta-null", "Error creating book.");
        defaultMessages.put("messages.errors.player-only", "This command is for players only.");
        defaultMessages.put("messages.errors.no-permission", "You don't have permission to use this command.");
        defaultMessages.put("messages.errors.invalid-group", "Invalid report status group.");

        return defaultMessages.getOrDefault(key, "");
    }

    public String getSymbol(String symbolKey) {
        // Return appropriate symbols based on the key
        Map<String, String> symbols = new HashMap<>();
        symbols.put("group_bullet", "• ");

        return symbols.getOrDefault(symbolKey, "");
    }

    public String getSeparator() {
        // Return a separator string for the book GUI
        return "------------------------";
    }

    public @NotNull String getFormattedMessage(String messageKey, String placeholder1, String value1,
            String placeholder2, String value2) {
        // Get the base message
        String message = getMessage(messageKey);
        if (message == null || message.isEmpty()) {
            return "Message not found: " + messageKey;
        }

        // Replace placeholders
        if (placeholder1 != null && value1 != null) {
            message = message.replace(placeholder1, value1);
        }

        if (placeholder2 != null && value2 != null) {
            message = message.replace(placeholder2, value2);
        }

        return message;
    }

    public int getReportsPerPage() {
        // Return the configured number of reports per page
        return 5; // Default value
    }

    public @NotNull Style getStatusColor(String statusName) {
        // Return appropriate color style based on report status
        if (statusName == null) {
            return Style.empty();
        }

        switch (statusName.toUpperCase()) {
            case "PENDING":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GOLD);
            case "IN_PROGRESS":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.YELLOW);
            case "RESOLVED":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GREEN);
            case "REJECTED":
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.RED);
            default:
                return Style.style(net.kyori.adventure.text.format.NamedTextColor.GRAY);
        }
    }

    public String getText(String key) {
        // Return text content based on the key
        Map<String, String> textMap = new HashMap<>();
        textMap.put("messages.report-format.number-prefix", "#");
        textMap.put("gui.spacing.after_number", ": ");

        return textMap.getOrDefault(key, "");
    }

    @Data
    public static class Config {
        private String serverName = "survival";
        private int configVersion = 6;
        private DatabaseConfig database = new DatabaseConfig();
        private ReportsConfig reports = new ReportsConfig();
        private NotificationsConfig notifications = new NotificationsConfig();
        private PerformanceConfig performance = new PerformanceConfig();
        private DiscordConfig discord = new DiscordConfig();
        private UpdateCheckerConfig updateChecker = new UpdateCheckerConfig();
        private Map<String, String> customReasons = new HashMap<>();

        @Data
        public static class DiscordConfig {
            private boolean enabled = false;
            private String botToken = "";
            private String channelId = "";
            private String logChannelId = "";
            private String staffRoleId = "";
            private String lookupColor = "#00ffff";
            private NetworkModeConfig networkMode = new NetworkModeConfig();
            private DiscordBotSettingsConfig botSettings = new DiscordBotSettingsConfig();
            private DiscordNotificationsConfig notifications = new DiscordNotificationsConfig();

            @Data
            public static class NetworkModeConfig {
                private boolean enabled = false;
                private int pollInterval = 10;
            }

            @Data
            public static class DiscordBotSettingsConfig {
                private String status = "ONLINE";
                private ActivityConfig activity = new ActivityConfig();

                @Data
                public static class ActivityConfig {
                    private String type = "WATCHING";
                    private String message = "Reports";
                }
            }

            @Data
            public static class DiscordNotificationsConfig {
                private String title = "New Report (#%id%)";
                private String color = "#ff5555";
                private String footer = "AevorinReports • %date%";
            }
        }

        @Data
        public static class DatabaseConfig {
            private String type = "file";
            private MySQLConfig mysql = new MySQLConfig();
            private FileStorageConfig file = new FileStorageConfig();

            @Data
            public static class MySQLConfig {
                private String host = "localhost";
                private int port = 3306;
                private String database = "aevorinreports";
                private String username = "root";
                private String password = "";
            }

            @Data
            public static class FileStorageConfig {
                private String path = "database/reports.db";
            }
        }

        @Data
        public static class ReportsConfig {
            private int cooldownSeconds = 300;
            private boolean allowAnonymousReports = true;
            private int maxActiveReportsPerPlayer = 3;
            private int chatHistoryLines = 50;
            private boolean logInventory = true;
            private boolean logLocation = true;
            private boolean allowSelfReporting = false;
            private boolean allowOfflinePlayerReporting = true;
            private GUIConfig gui = new GUIConfig();
            private List<String> categories = new ArrayList<>(Arrays.asList("Hacking/Cheating", "Harassment/Bullying", "Spam/Advertisement", "Griefing/Vandalism", "Bug Exploit", "Other"));

            @Data
            public static class GUIConfig {
                private String type = "book";
            }
        }

        @Data
        public static class NotificationsConfig {
            private boolean enableChatNotifications = true;
            private boolean enableTitleNotifications = true;
            private boolean enableSoundNotifications = true;
            private String notificationSound = "BLOCK_NOTE_BLOCK_PLING";
        }

        @Data
        public static class PerformanceConfig {
            private boolean enableCaching = true;
            private int cacheDuration = 15;
            private int maxCacheSize = 1000;
            private boolean asyncProcessing = true;
            private int batchSize = 50;
            private int backgroundTaskInterval = 300;
            private int cacheCleanupInterval = 30;
        }

        @Data
        public static class UpdateCheckerConfig {
            private int checkInterval = 60;
            private boolean notifyOnJoin = true;
            private String updateChannel = "release";
        }
    }

}