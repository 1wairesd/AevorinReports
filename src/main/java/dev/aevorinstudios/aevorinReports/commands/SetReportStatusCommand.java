package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SetReportStatusCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;

    public SetReportStatusCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = LanguageManager.get(plugin);

        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, lang.getMessage("messages.error.player-only"));
            return true;
        }

        if (!player.hasPermission("aevorinreports.manage")) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.no-permission"));
            return true;
        }

        if (args.length != 3 || !args[1].equalsIgnoreCase("to")) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.usage-setstatus"));
            return true;
        }

        try {
            long reportId = Long.parseLong(args[0]);
            String statusStr = args[2].toUpperCase();
            Report.ReportStatus newStatus = Report.ReportStatus.valueOf(statusStr);
            Report report = plugin.getDatabaseManager().getReport(reportId);

            if (report == null) {
                MessageUtils.sendMessage(player, lang.getMessage("messages.error.report-not-found"));
                return true;
            }

            if (report.getStatus() == newStatus) {
                MessageUtils.sendMessage(player, lang.getMessage("messages.error.status-already-set"));
                return true;
            }

            report.setStatus(newStatus);
            plugin.getDatabaseManager().updateReport(report);

            String statusColor = switch(newStatus) {
            case PENDING -> "&6";
            case RESOLVED -> "&a";
            case REJECTED -> "&c";
            };

            MessageUtils.sendMessage(player, lang.getMessage("messages.report.status-change", Map.of(
                "id", String.valueOf(reportId),
                "status", lang.getLocalizedStatus(newStatus),
                "color", statusColor
            )));

            plugin.getLogger().fine("Report " + reportId + " status changed to " + newStatus.name() + " by " + player.getName());

            player.closeInventory();
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.invalid-report-id"));
        } catch (IllegalArgumentException e) {
            String statuses = Arrays.stream(Report.ReportStatus.values()).map(Enum::name).collect(Collectors.joining(", "));
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.status-invalid", Map.of("statuses", statuses)));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("aevorinreports.manage")) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            return List.of("to");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("to")) {
            return Arrays.stream(Report.ReportStatus.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
