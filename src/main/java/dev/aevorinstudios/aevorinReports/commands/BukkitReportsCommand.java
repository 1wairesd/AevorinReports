package dev.aevorinstudios.aevorinReports.commands;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.gui.BookGUI;
import dev.aevorinstudios.aevorinReports.gui.CategoryContainerGUI;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BukkitReportsCommand implements CommandExecutor, TabCompleter {
    private final BukkitPlugin plugin;
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();
    private final Map<Report.ReportStatus, List<Report>> reportCache = new HashMap<>();
    private static final long COMMAND_COOLDOWN = 500;
    private static final long CACHE_DURATION = 5000;
    private long lastCacheUpdate = 0;

    public BukkitReportsCommand(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("aevorinreports.manage")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> groups = new ArrayList<>();
            String partialGroup = args[0].toUpperCase();

            for (Report.ReportStatus status : Report.ReportStatus.values()) {
                if (status.name().startsWith(partialGroup)) {
                    groups.add(status.name());
                }
            }

            return groups;
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = LanguageManager.get(plugin);

        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, lang.getMessage("messages.error.player-only"));
            return true;
        }

        if (!player.hasPermission("aevorinreports.manage")) {
            String guiType = plugin.getConfig().getString("reports.gui.type", "book");
            if (guiType.equalsIgnoreCase("container")) {
                List<Report> reports = plugin.getDatabaseManager().getReportsByReporter(player.getUniqueId());
                if (reports.isEmpty()) {
                    MessageUtils.sendMessage(player, lang.getMessage("messages.error.no-reports"));
                    return true;
                }
                new dev.aevorinstudios.aevorinReports.gui.CategoryContainerGUI(plugin).openPlayerReportsGUI(player, reports, 0);
            } else {
                new BookGUI(plugin).showPlayerReports(player);
            }
            return true;
        }

        if (args.length == 0) {
            showReportsBook(player);
            return true;
        }

        String group = args[0].toUpperCase();
        Report.ReportStatus status;
        try {
            status = Report.ReportStatus.valueOf(group);
        } catch (IllegalArgumentException e) {
            MessageUtils.sendMessage(player, lang.getMessage("messages.error.invalid-group"));
            return true;
        }

        showReportsBookByStatus(player, status);
        return true;
    }

    private void showReportsBook(Player player) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        if (guiType.equalsIgnoreCase("container")) {
            showReportsContainerGUI(player);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        new BookGUI(plugin).showReportsBook(player);
    }

    private void showReportsBookByStatus(Player player, Report.ReportStatus status) {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastCommandTime.getOrDefault(player.getUniqueId(), 0L);
        if (currentTime - lastTime < COMMAND_COOLDOWN) {
            return;
        }
        lastCommandTime.put(player.getUniqueId(), currentTime);

        if (currentTime - lastCacheUpdate > CACHE_DURATION) {
            reportCache.clear();
            lastCacheUpdate = currentTime;
        }

        String guiType = plugin.getConfig().getString("reports.gui.type", "book");
        if (guiType.equalsIgnoreCase("container")) {
            List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(status);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, status, reports);
            return;
        }

        new BookGUI(plugin).showReportsByStatus(player, status);
    }

    private void showReportsContainerGUI(Player player) {
        new CategoryContainerGUI(plugin).openMainMenu(player);
    }
}
