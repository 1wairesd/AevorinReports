package dev.aevorinstudios.aevorinReports.tasks;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import dev.aevorinstudios.aevorinReports.utils.SchedulerUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

public class NotificationPollingTask extends BukkitRunnable {

    private final BukkitPlugin plugin;
    private long lastSeenId = 0;

    public NotificationPollingTask(BukkitPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize lastSeenId to the current maximum in the database
        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null) {
            this.lastSeenId = db.getMaxReportId();
        }
    }

    @Override
    public void run() {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) return;

        List<Report> newReports = db.getReportsAfterId(lastSeenId);
        if (newReports == null || newReports.isEmpty()) {
            return;
        }

        String localServerName = plugin.getConfigManager().getConfig().getServerName();
        LanguageManager lang = LanguageManager.get(plugin);

        for (Report report : newReports) {
            // Update last seen ID
            if (report.getId() > lastSeenId) {
                lastSeenId = report.getId();
            }

            // If the report originated from another server, notify local admins
            if (!report.getServerName().equalsIgnoreCase(localServerName)) {
                SchedulerUtils.runTask(plugin, null, () -> {
                    String notification = lang.getMessage("messages.report.notification", Map.of(
                        "reporter", report.getReporterName() != null ? report.getReporterName() : "Unknown",
                        "reported", report.getReportedPlayerName() != null ? report.getReportedPlayerName() : "Unknown",
                        "category", report.getReason()
                    ));

                    for (Player staff : plugin.getServer().getOnlinePlayers()) {
                        if (staff.hasPermission("aevorinreports.notify")) {
                            MessageUtils.sendMessage(staff, notification);
                        }
                    }
                });
            }
        }
    }
}
