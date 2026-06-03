package dev.aevorinstudios.aevorinReports.placeholders;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.database.DatabaseManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PlaceholderAPI Expansion for AevorinReports
 * Provides player report statistics as placeholders
 */
public class AevorinReportsExpansion extends PlaceholderExpansion {

    private final BukkitPlugin plugin;

    public AevorinReportsExpansion(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "reports";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            return "0";
        }

        String id = identifier.toLowerCase();

        String serverName = extractServerName(identifier);
        if (serverName != null) {
            if (!supportsNetworkServerPlaceholders(db)) {
                return "0";
            }
            id = id.substring(0, id.lastIndexOf("_on_" + serverName.toLowerCase()));
        }

        // Check for player-specific placeholders (format: placeholder_playername)
        UUID targetUuid = null;
        if (player != null) {
            targetUuid = player.getUniqueId();
        }

        // Parse player name suffix if present (e.g., "submitted_by_Notch" or "against_Notch")
        String playerName = extractPlayerName(identifier);
        if (playerName != null) {
            Player targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer != null) {
                targetUuid = targetPlayer.getUniqueId();
                // Remove player name from identifier for processing
                id = id.substring(0, id.lastIndexOf("_" + playerName.toLowerCase()));
            } else {
                // Player not online, try to get offline player
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(playerName);
                if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                    targetUuid = offlinePlayer.getUniqueId();
                    id = id.substring(0, id.lastIndexOf("_" + playerName.toLowerCase()));
                } else {
                    return "0"; // Player not found
                }
            }
        }

        // If we need a player UUID but don't have one, return null
        if (targetUuid == null && !isServerWidePlaceholder(id)) {
            return null;
        }

        final UUID uuid = targetUuid;

        switch (id) {
            // Reports submitted by the player (as reporter)
            case "submitted_by":
                if (playerName == null) {
                    return null;
                }
                return String.valueOf(db.getReportsCountByReporter(uuid));
            case "submitted":
                return String.valueOf(db.getReportsCountByReporter(uuid));

            case "pending_submitted_by":
                if (playerName == null) {
                    return null;
                }
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.PENDING));
            case "pending_submitted":
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.PENDING));

            case "resolved_submitted_by":
                if (playerName == null) {
                    return null;
                }
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.RESOLVED));
            case "resolved_submitted":
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.RESOLVED));

            case "rejected_submitted_by":
                if (playerName == null) {
                    return null;
                }
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.REJECTED));
            case "rejected_submitted":
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.REJECTED));

            case "valid_submitted_by":
                if (playerName == null) {
                    return null;
                }
                // Valid reports = resolved reports (reports that were acted upon)
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.RESOLVED));
            case "valid_submitted":
                // Valid reports = resolved reports (reports that were acted upon)
                return String.valueOf(db.getReportsCountByReporterAndStatus(uuid, Report.ReportStatus.RESOLVED));

            // Reports received against the player (as reported)
            case "against":
                return String.valueOf(db.getReportsCountByReported(uuid));

            case "pending_against":
                return String.valueOf(db.getReportsCountByReportedAndStatus(uuid, Report.ReportStatus.PENDING));

            case "resolved_against":
                return String.valueOf(db.getReportsCountByReportedAndStatus(uuid, Report.ReportStatus.RESOLVED));

            case "rejected_against":
                return String.valueOf(db.getReportsCountByReportedAndStatus(uuid, Report.ReportStatus.REJECTED));

            // Server-wide statistics (player-independent)
            case "total":
                if (serverName != null) {
                    return String.valueOf(db.getReportCountByServer(serverName));
                }
                return String.valueOf(db.getTotalReportsCount());

            case "total_pending":
                if (serverName != null) {
                    return String.valueOf(db.getReportCountByServerAndStatus(serverName, Report.ReportStatus.PENDING));
                }
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.PENDING));

            case "total_resolved":
                if (serverName != null) {
                    return String.valueOf(db.getReportCountByServerAndStatus(serverName, Report.ReportStatus.RESOLVED));
                }
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.RESOLVED));

            case "total_rejected":
                if (serverName != null) {
                    return String.valueOf(db.getReportCountByServerAndStatus(serverName, Report.ReportStatus.REJECTED));
                }
                return String.valueOf(db.getReportCountByStatus(Report.ReportStatus.REJECTED));

            default:
                return null;
        }
    }

    /**
     * Extracts player name from identifier if present.
     * Looks for known placeholder prefixes and extracts the player name after the last underscore.
     *
     * @param identifier the placeholder identifier
     * @return the player name if found, null otherwise
     */
    private String extractPlayerName(String identifier) {
        String lowerIdentifier = identifier.toLowerCase();

        // Known prefixes that can have player suffixes
        String[] prefixes = {
            "pending_submitted_by", "resolved_submitted_by",
            "rejected_submitted_by", "valid_submitted_by",
            "pending_against", "resolved_against", "rejected_against",
            "submitted_by", "against"
        };

        for (String prefix : prefixes) {
            if (lowerIdentifier.startsWith(prefix + "_")) {
                return identifier.substring(prefix.length() + 1);
            }
        }
        return null;
    }

    /**
     * Extracts server name from server-specific statistics placeholders.
     *
     * @param identifier the placeholder identifier
     * @return the server name if found, null otherwise
     */
    private String extractServerName(String identifier) {
        String lowerIdentifier = identifier.toLowerCase();
        String[] prefixes = {
            "total_pending", "total_resolved", "total_rejected",
            "total"
        };

        for (String prefix : prefixes) {
            String marker = prefix + "_on_";
            if (lowerIdentifier.startsWith(marker)) {
                return identifier.substring(marker.length());
            }
        }
        return null;
    }

    private boolean supportsNetworkServerPlaceholders(DatabaseManager db) {
        return plugin.getConfigManager() != null &&
               plugin.getConfigManager().getConfig() != null &&
               plugin.getConfigManager().getConfig().getDatabase() != null &&
               "mysql".equalsIgnoreCase(plugin.getConfigManager().getConfig().getDatabase().getType()) &&
               db.hasMultipleServers();
    }

    /**
     * Checks if a placeholder is server-wide (doesn't require a player)
     *
     * @param identifier the placeholder identifier
     * @return true if the placeholder is server-wide
     */
    private boolean isServerWidePlaceholder(String identifier) {
        return identifier.equals("total") ||
               identifier.equals("total_pending") ||
               identifier.equals("total_resolved") ||
               identifier.equals("total_rejected");
    }
}
