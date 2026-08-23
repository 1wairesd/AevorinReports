package dev.aevorinstudios.aevorinReports.discord;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.ConfigManager;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DiscordManager {
    private final BukkitPlugin plugin;
    private JDA jda;
    private String channelId;
    private String logChannelId;
    private boolean enabled;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> presenceTask;
    private ScheduledFuture<?> pollTask;
    private long lastReportId = -1;
    private boolean invalidChannelIdWarned;
    private boolean invalidLogChannelIdWarned;

    public DiscordManager(BukkitPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.channelId = plugin.getConfig().getString("discord.channel-id");
        this.logChannelId = plugin.getConfig().getString("discord.log-channel-id");
    }

    public void start() {
        if (!enabled)
            return;

        String token = plugin.getConfig().getString("discord.bot-token");
        if (token == null || token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord bot token is not set!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .disableCache(CacheFlag.FORUM_TAGS) // Workaround: JDA 5.4.0 crashes on forum channels with null
                                                        // available_tags
                    .addEventListeners(new DiscordListener(plugin))
                    .build();
            jda.awaitReady();

            // Set initial Presence
            updatePresence();

            // Folia compatible scheduler (Java Executor)
            scheduler = Executors.newSingleThreadScheduledExecutor();
            presenceTask = scheduler.scheduleAtFixedRate(this::updatePresence, 5, 5, TimeUnit.MINUTES);

            // Start network mode polling if enabled
            if (plugin.getConfigManager().getConfig().getDiscord().getNetworkMode().isEnabled()) {
                startNetworkPolling();
            }

            // Register Slash Commands
            jda.updateCommands().addCommands(
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("resolve",
                            lang("discord.commands.resolve.description", "Resolve a report"))
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "id",
                                    lang("discord.commands.options.id", "The Report ID"), true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("reject",
                            lang("discord.commands.reject.description", "Reject a report"))
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "id",
                                    lang("discord.commands.options.id", "The Report ID"), true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands
                            .slash("pending",
                                    lang("discord.commands.pending.description", "Set report status back to pending"))
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "id",
                                    lang("discord.commands.options.id", "The Report ID"), true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands
                            .slash("lookup",
                                    lang("discord.commands.lookup.description",
                                            "Lookup detailed information about a report"))
                            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.INTEGER, "id",
                                    lang("discord.commands.options.id", "The Report ID"), true),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("reports",
                            lang("discord.commands.reports.description", "List all active reports")),
                    net.dv8tion.jda.api.interactions.commands.build.Commands.slash("help",
                            lang("discord.commands.help.description", "Show the help menu")))
                    .queue();

            plugin.getLogger().info("Discord bot successfully started and commands registered!");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("intents")) {
                plugin.getLogger().severe("--------------------------------------------------");
                plugin.getLogger().severe("DISCORD BOT ERROR: DISALLOWED INTENTS");
                plugin.getLogger().severe("Please enable 'MESSAGE_CONTENT' and 'SERVER MEMBERS'");
                plugin.getLogger().severe("intents in the Discord Developer Portal under the 'Bot' tab!");
                plugin.getLogger().severe("Link: https://discord.com/developers/applications");
                plugin.getLogger().severe("--------------------------------------------------");
            } else {
                plugin.getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("--------------------------------------------------");
            plugin.getLogger().severe("UNEXPECTED ERROR STARTING DISCORD BOT");
            plugin.getLogger().severe("Error: " + e.getMessage());

            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().log(Level.SEVERE, "Stack trace for support:", e);
            } else {
                plugin.getLogger().severe("Enable 'debug.enabled' in config.yml for the full stack trace.");
            }
            plugin.getLogger().severe("--------------------------------------------------");
        }
    }

    private void updatePresence() {
        if (jda == null)
            return;

        ConfigManager.Config.DiscordConfig.DiscordBotSettingsConfig settings = plugin.getConfigManager().getConfig()
                .getDiscord().getBotSettings();

        // Status
        OnlineStatus status = OnlineStatus.fromKey(settings.getStatus().toLowerCase());
        if (status == OnlineStatus.UNKNOWN)
            status = OnlineStatus.ONLINE;

        // Activity
        String message = settings.getActivity().getMessage()
                .replace("%online_players%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        String typeStr = settings.getActivity().getType().toUpperCase();
        Activity activity = switch (typeStr) {
            case "PLAYING" -> Activity.playing(message);
            case "LISTENING" -> Activity.listening(message);
            case "COMPETING" -> Activity.competing(message);
            default -> Activity.watching(message);
        };

        jda.getPresence().setPresence(status, activity);
    }

    public void stop() {
        if (presenceTask != null) {
            presenceTask.cancel(false);
        }
        if (pollTask != null) {
            pollTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (jda != null) {
            jda.shutdown();

            // Block until JDA fully shuts down, otherwise its background threads may
            // try to load classes from the closed plugin jar after an unload/reload
            // ("IllegalStateException: zip file closed"). Recommended pattern from
            // the official JDA troubleshooting guide:
            // https://jda.wiki/using-jda/troubleshooting/
            try {
                if (!jda.awaitShutdown(java.time.Duration.ofSeconds(10))) {
                    jda.shutdownNow(); // Cancel queued requests and force shutdown
                    jda.awaitShutdown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
        }
    }

    private void startNetworkPolling() {
        dev.aevorinstudios.aevorinReports.database.DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            throw new IllegalStateException(
                    "Database manager is not initialized. Discord network mode requires a working database connection.");
        }

        // Initialize with high current ID so we only poll for NEW reports
        lastReportId = db.getMaxReportId();
        int pollInterval = plugin.getConfigManager().getConfig().getDiscord().getNetworkMode().getPollInterval();

        plugin.getLogger().info("[Discord] Network mode enabled - polling database every " + pollInterval
                + " seconds (Starting from ID: " + lastReportId + ")");

        pollTask = scheduler.scheduleAtFixedRate(this::pollForNewReports, pollInterval, pollInterval, TimeUnit.SECONDS);
    }

    private void pollForNewReports() {
        if (jda == null || jda.getStatus() != JDA.Status.CONNECTED) {
            return;
        }

        try {
            List<dev.aevorinstudios.aevorinReports.reports.Report> newReports = plugin.getDatabaseManager()
                    .getReportsAfterId(lastReportId);

            for (dev.aevorinstudios.aevorinReports.reports.Report report : newReports) {
                // Update tracker immediately to the highest ID processed
                if (report.getId() > lastReportId) {
                    lastReportId = report.getId();
                }

                // Resolve player names before sending
                String reporterName = dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver
                        .resolvePlayerName(report.getReporterUuid());
                String reportedName = dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver
                        .resolvePlayerName(report.getReportedUuid());

                report.setReporterName(reporterName);
                report.setReportedPlayerName(reportedName);

                sendReportEmbed(report);
            }

            if (!newReports.isEmpty()) {
                plugin.getLogger().info("[Discord] Polled and sent " + newReports.size() + " new report(s) to Discord");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Discord] Error polling for new reports: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                e.printStackTrace();
            }
        }
    }

    public void sendReportNotification(Report report) {
        // In network mode, we rely purely on the polling task to avoid duplicates
        // and to centralized notifications in multi-server networks.
        if (plugin.getConfigManager().getConfig().getDiscord().getNetworkMode().isEnabled()) {
            return;
        }

        sendReportEmbed(report);
    }

    private void sendReportEmbed(Report report) {
        if (!enabled || jda == null || channelId == null || channelId.isEmpty())
            return;

        if (!isDiscordId(channelId)) {
            warnInvalidChannelIdOnce();
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId.trim());
        if (channel == null) {
            plugin.getLogger().warning("Discord channel with ID " + channelId + " not found!");
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(report.getId()));

        String title = lang(
                "discord.notifications.new-report.title",
                plugin.getConfig().getString("discord.notifications.title", "New Report (#{id})"),
                placeholders);

        String colorHex = plugin.getConfig().getString("discord.notifications.color", "#ff5555");
        Color color = Color.RED;
        try {
            color = Color.decode(colorHex);
        } catch (NumberFormatException ignored) {
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .addField(lang("discord.fields.reporter", "Reporter"),
                        report.getReporterName() != null ? report.getReporterName() : unknown(), true)
                .addField(lang("discord.fields.reported", "Reported"),
                        report.getReportedPlayerName() != null ? report.getReportedPlayerName() : unknown(), true)
                .addField(lang("discord.fields.id", "ID"), "#" + report.getId(), true)
                .addField(lang("discord.fields.reason", "Reason"), "```" + report.getReason() + "```", false);

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            embed.addField(lang("discord.fields.server", "Server"), "`" + report.getServerName() + "`", true);
        }

        embed.addField(lang("discord.fields.location", "Location"),
                "`" + (report.getWorld() != null ? report.getWorld() : unknown()) + "` ("
                        + (report.getCoordinates() != null ? report.getCoordinates() : unknown()) + ")",
                true);

        String footer = plugin.getConfig().getString("discord.notifications.footer", "AevorinReports • %date%")
                .replace("%date%", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        placeholders.put("date", report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        footer = lang("discord.notifications.new-report.footer", footer, placeholders);
        embed.setFooter(footer);

        try {
            if (channel.getGuild().getSelfMember() == null) {
                plugin.getLogger().warning("[Discord] Cannot send report: Self member not cached for guild "
                        + channel.getGuild().getName());
                return;
            }

            channel.sendMessageEmbeds(embed.build()).queue(
                    success -> {
                    },
                    error -> plugin.getLogger()
                            .warning("[Discord] Failed to send report embed to #" + channel.getName() + ": "
                                    + error.getMessage()));
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[Discord] Could not send report notification to channel " + channelId + " - " + e.getMessage());
        }
    }

    public void sendLogUpdate(Report report, String adminName) {
        if (!enabled || jda == null || logChannelId == null || logChannelId.isEmpty())
            return;

        if (!isDiscordId(logChannelId)) {
            warnInvalidLogChannelIdOnce();
            return;
        }

        TextChannel channel = jda.getTextChannelById(logChannelId.trim());
        if (channel == null) {
            plugin.getLogger().warning("Discord log channel with ID " + logChannelId + " not found!");
            return;
        }

        Report.ReportStatus status = report.getStatus();
        Color color = switch (status) {
            case RESOLVED -> Color.GREEN;
            case REJECTED -> Color.RED;
            case PENDING -> Color.ORANGE;
        };

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(lang("discord.notifications.status-update.title", "Report Updated"))
                .setDescription(lang("discord.notifications.status-update.description",
                        "Report **#{id}** has been **{status}** by {admin}.",
                        Map.of(
                                "id", String.valueOf(report.getId()),
                                "status", localizedStatus(status),
                                "admin", adminName)))
                .setColor(color);

        try {
            if (channel.getGuild().getSelfMember() == null) {
                plugin.getLogger().warning("[Discord] Cannot send log update: Self member not cached for guild "
                        + channel.getGuild().getName());
                return;
            }

            channel.sendMessageEmbeds(embed.build()).queue(
                    success -> {
                    },
                    error -> plugin.getLogger()
                            .warning("[Discord] Failed to send log update embed to #" + channel.getName() + ": "
                                    + error.getMessage()));
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[Discord] Could not send log update to channel " + logChannelId + " - " + e.getMessage());
        }
    }

    private boolean isDiscordId(String configuredValue) {
        return configuredValue != null && configuredValue.trim().matches("\\d+");
    }

    private void warnInvalidChannelIdOnce() {
        if (invalidChannelIdWarned) {
            return;
        }
        invalidChannelIdWarned = true;
        plugin.getLogger().warning("[Discord] Invalid discord.channel-id in config.yml. Use only the numeric channel ID, not a Discord URL.");
    }

    private void warnInvalidLogChannelIdOnce() {
        if (invalidLogChannelIdWarned) {
            return;
        }
        invalidLogChannelIdWarned = true;
        plugin.getLogger().warning("[Discord] Invalid discord.log-channel-id in config.yml. Use only the numeric channel ID, not a Discord URL.");
    }

    private String localizedStatus(Report.ReportStatus status) {
        return lang("common.status." + status.name().toLowerCase(Locale.ROOT), status.name().toLowerCase(Locale.ROOT));
    }

    private String unknown() {
        return lang("common.unknown", "Unknown");
    }

    private String lang(String path, String fallback) {
        String value = LanguageManager.get(plugin).getRawMessage(path);
        return value.startsWith("Missing lang: ") ? fallback : value;
    }

    private String lang(String path, String fallback, Map<String, String> placeholders) {
        String value = lang(path, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
            value = value.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return value;
    }
}
