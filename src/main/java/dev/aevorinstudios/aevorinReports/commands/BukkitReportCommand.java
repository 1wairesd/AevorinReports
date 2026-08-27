package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportReasonContainerGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import dev.aevorinstudios.aevorinReports.utils.SchedulerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BukkitReportCommand implements CommandExecutor, TabCompleter {
    private static final long OFFLINE_NAMES_CACHE_TTL_MS = 5 * 60 * 1000L;

    private final BukkitPlugin plugin;
    private final Map<java.util.UUID, Long> cooldowns = new HashMap<>();
    private final Map<java.util.UUID, Integer> pendingReservations = new ConcurrentHashMap<>();
    private volatile List<String> cachedOfflineNames = List.of();
    private volatile long offlineNamesCacheTime = 0L;
    private final java.util.concurrent.atomic.AtomicBoolean refreshingOfflineNames =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    public BukkitReportCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = LanguageManager.get(plugin);
        
        if (!(sender instanceof Player player)) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(sender, lang.getMessage("messages.error.player-only"));
            return true;
        }

        if (!player.hasPermission("aevorinreports.report")) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.no-permission"));
            return true;
        }

        if (args.length == 0) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.usage-report"));
            return true;
        }

        String targetPlayer = args[0];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetPlayer);
        boolean allowOfflinePlayerReporting = plugin.getConfig().getBoolean("reports.allow-offline-player-reporting", true);

        if ((!allowOfflinePlayerReporting && !target.isOnline()) || (!target.isOnline() && !target.hasPlayedBefore())) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.invalid-player"));
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : targetPlayer;

        // Self-reporting check
        boolean allowSelfReporting = plugin.getConfig().getBoolean("reports.allow-self-reporting", false);
        if (!allowSelfReporting && player.getUniqueId().equals(target.getUniqueId())) {
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.cannot-report-self"));
            return true;
        }

        // Check report cooldown
        if (!player.hasPermission("aevorinreports.bypass.cooldown")) {
            long lastReport = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            int cooldownSeconds = plugin.getConfigManager().getConfig().getReports().getCooldownSeconds();
            long currentTime = System.currentTimeMillis();

            if (currentTime < lastReport + (cooldownSeconds * 1000L)) {
                long timeLeft = (lastReport + (cooldownSeconds * 1000L) - currentTime) / 1000L;
                String message = lang.getMessage("messages.report.cooldown", Map.of("time", formatTime(timeLeft)));
                MessageUtils.sendMessage(player, message);
                return true;
            }
        }

        // Check active reports limit
        if (!player.hasPermission("aevorinreports.bypass.limit")) {
            int maxActive = plugin.getConfigManager().getConfig().getReports().getMaxActiveReportsPerPlayer();
            long activeCount = plugin.getDatabaseManager().getReportsCountByReporterAndStatus(
                    player.getUniqueId(), Report.ReportStatus.PENDING);
            int pending = pendingReservations.getOrDefault(player.getUniqueId(), 0);

            if (activeCount + pending >= maxActive) {
                MessageUtils.sendMessage(player, lang.getMessage("messages.report.limit-reached"));
                return true;
            }
        }

        // If reason is provided, create report directly
        if (args.length > 1) {
            String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // Handle custom reason
            if (reason.equalsIgnoreCase("custom")) {
                if (!plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                    MessageUtils.sendMessage(player, lang.getMessage("messages.error.custom-reason-disabled"));
                    return true;
                }

                plugin.getCustomReasonHandler().startCustomReason(player, targetName);
                MessageUtils.sendMessage(player, lang.getMessage("messages.report.custom-reason-prompt"));
                return true;
            }

            // Check if reason is a valid category
            java.util.List<String> validCategories = lang.getReasonList();
            boolean isValidCategory = false;
            for (String cat : validCategories) {
                if (cat.equalsIgnoreCase(reason)) {
                    isValidCategory = true;
                    reason = cat; // Use exact case
                    break;
                }
            }

            if (!isValidCategory) {
                if (!plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                    dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.category-invalid", java.util.Map.of("categories", String.join(", ", validCategories))));
                    return true;
                }

                int minLength = plugin.getConfig().getInt("reports.custom-reason-min-length", 10);
                int maxLength = plugin.getConfig().getInt("reports.custom-reason-max-length", 100);

                if (reason.length() < minLength) {
                    dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.custom-reason-too-short", java.util.Map.of("min", String.valueOf(minLength))));
                    return true;
                }

                if (reason.length() > maxLength) {
                    dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.error.custom-reason-too-long", java.util.Map.of("max", String.valueOf(maxLength))));
                    return true;
                }
            }

            createReport(player, targetName, reason);
            return true;
        }

        // If no reason provided, show GUI
        showReportCategories(player, targetName);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.report")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            String partialName = args[0].toLowerCase();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    playerNames.add(player.getName());
                }
            }

            if (plugin.getConfig().getBoolean("reports.allow-offline-player-reporting", true)) {
                refreshOfflineNamesCacheIfNeeded();
                for (String name : cachedOfflineNames) {
                    if (name.toLowerCase().startsWith(partialName) && !playerNames.contains(name)) {
                        playerNames.add(name);
                    }
                }
            }

            return playerNames;
        } else if (args.length == 2) {
            LanguageManager lang = LanguageManager.get(plugin);
            List<String> suggestions = new ArrayList<>(lang.getReasonList());
            if (plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)) {
                suggestions.add("custom");
            }

            String partialReason = args[1].toLowerCase();
            return suggestions.stream()
                    .filter(reason -> reason.toLowerCase().startsWith(partialReason))
                    .toList();
        }

        return new ArrayList<>();
    }

    public void createReport(Player reporter, String targetPlayer, String category) {
        LanguageManager lang = LanguageManager.get(plugin);
        LocalDateTime now = LocalDateTime.now();
        Report report = Report.builder()
                .reporterUuid(reporter.getUniqueId())
                .reportedUuid(plugin.getServer().getOfflinePlayer(targetPlayer).getUniqueId())
                .reporterName(reporter.getName())
                .reportedPlayerName(targetPlayer)
                .reason(category)
                .serverName(plugin.getConfigManager().getConfig().getServerName())
                .status(Report.ReportStatus.PENDING)
                .coordinates(String.format("%.1f, %.1f, %.1f", reporter.getLocation().getX(),
                        reporter.getLocation().getY(), reporter.getLocation().getZ()))
                .world(reporter.getWorld().getName())
                .isAnonymous(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Apply cooldown and reservation immediately to prevent spamming while the save is in flight
        cooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
        pendingReservations.merge(reporter.getUniqueId(), 1, Integer::sum);

        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            cooldowns.remove(reporter.getUniqueId());
            pendingReservations.merge(reporter.getUniqueId(), -1, Integer::sum);
            return;
        }

        // Save report to database off the main thread, then notify on the main thread
        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            try {
                db.saveReport(report);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save report: " + e.getMessage());
                SchedulerUtils.runTask(plugin, reporter, () -> {
                    cooldowns.remove(reporter.getUniqueId());
                    pendingReservations.merge(reporter.getUniqueId(), -1, Integer::sum);
                    if (reporter.isOnline()) {
                        MessageUtils.sendMessage(reporter, lang.getMessage("messages.error.database-unavailable", Map.of("id", "none")));
                    }
                });
                return;
            }
            
            // Save successful, remove reservation
            pendingReservations.merge(reporter.getUniqueId(), -1, Integer::sum);

            // Send Discord notification
            if (plugin.getDiscordManager() != null) {
                plugin.getDiscordManager().sendReportNotification(report);
            }

            // Notify staff members and reporter back on the main thread
            SchedulerUtils.runTask(plugin, reporter, () -> {
                if (!reporter.isOnline()) {
                    return;
                }

                String notification = lang.getMessage("messages.report.notification", Map.of(
                    "reporter", reporter.getName(),
                    "reported", targetPlayer,
                    "category", category
                ));

                for (Player staff : plugin.getServer().getOnlinePlayers()) {
                    if (staff.hasPermission("aevorinreports.notify")) {
                        MessageUtils.sendMessage(staff, notification);
                    }
                }

                MessageUtils.sendMessage(reporter, lang.getMessage("messages.success.report-created"));
            });
        });
    }

    private void refreshOfflineNamesCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - offlineNamesCacheTime < OFFLINE_NAMES_CACHE_TTL_MS) {
            return;
        }
        if (!refreshingOfflineNames.compareAndSet(false, true)) {
            return;
        }
        offlineNamesCacheTime = now;
        
        List<String> namesSnapshot = new ArrayList<>();
        for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name != null) {
                namesSnapshot.add(name);
            }
        }
        
        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            try {
                cachedOfflineNames = namesSnapshot;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to refresh offline player name cache: " + e.getMessage());
            } finally {
                refreshingOfflineNames.set(false);
            }
        });
    }

    private String formatTime(long seconds) {
        if (seconds < 60)
            return seconds + "s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }

    private void showReportCategories(Player player, String targetPlayer) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");

        if (guiType.equalsIgnoreCase("container")) {
            new ReportReasonContainerGUI(plugin).showReasonContainerGUI(player, targetPlayer);
        } else {
            showReportBookGUI(player, targetPlayer);
        }
    }

    private void showReportBookGUI(Player player, String targetPlayer) {
        new BookGUI(plugin).showReportCategories(player, targetPlayer);
    }
}