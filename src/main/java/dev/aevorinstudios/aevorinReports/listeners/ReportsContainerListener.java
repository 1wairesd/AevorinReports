package dev.aevorinstudios.aevorinReports.listeners;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.gui.CategoryContainerGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportManageGUI;
import dev.aevorinstudios.aevorinReports.gui.ReportReasonContainerGUI;
import dev.aevorinstudios.aevorinReports.gui.holders.*;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ReportsContainerListener implements Listener {
    private final BukkitPlugin plugin;

    public ReportsContainerListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        Object holder = event.getInventory().getHolder();
        if (holder == null) return;

        // If the click is in our custom inventory, cancel it by default
        if (holder instanceof ReportsMenuHolder ||
            holder instanceof CategoryReportsHolder ||
            holder instanceof ReportManageHolder ||
            holder instanceof ReportReasonHolder ||
            holder instanceof PlayerReportsHolder) {

            event.setCancelled(true);

            // If they clicked outside the top inventory (e.g. in their own inventory), just cancel and return
            if (inv != event.getView().getTopInventory()) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (holder instanceof ReportsMenuHolder) {
                handleMainMenuClick(player, event.getSlot());
            } else if (holder instanceof CategoryReportsHolder) {
                handleCategoryClick(player, (CategoryReportsHolder) holder, event.getSlot(), clicked);
            } else if (holder instanceof PlayerReportsHolder) {
                handlePlayerReportsClick(player, (PlayerReportsHolder) holder, event.getSlot(), clicked);
            } else if (holder instanceof ReportManageHolder) {
                handleManageClick(player, (ReportManageHolder) holder, event.getSlot());
            } else if (holder instanceof ReportReasonHolder) {
                handleReasonSelectorClick(player, (ReportReasonHolder) holder, event.getSlot(), clicked);
            }
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        Report.ReportStatus status = null;
        if (slot == 10) status = Report.ReportStatus.PENDING;
        else if (slot == 13) status = Report.ReportStatus.RESOLVED;
        else if (slot == 16) status = Report.ReportStatus.REJECTED;

        if (status != null) {
            List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(status);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, status, reports);
        }
    }

    private void handleCategoryClick(Player player, CategoryReportsHolder holder, int slot, ItemStack clicked) {
        // Handle pagination arrows
        if (slot == 48 || slot == 50) {
            if (clicked.getType() == Material.ARROW) {
                int targetPage = holder.getPage() + (slot == 50 ? 1 : -1);
                if (targetPage >= 0) {
                    List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(holder.getStatus());
                    new CategoryContainerGUI(plugin).openCategoryGUI(player, holder.getStatus(), reports, targetPage);
                }
            }
            return;
        }

        // Handle back button
        if (slot == 45 && clicked.getType() == Material.DARK_OAK_DOOR) {
            new CategoryContainerGUI(plugin).openMainMenu(player);
            return;
        }

        // Handle report item clicks using PersistentDataContainer (robust)
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "report_id");
            if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
                long id = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.LONG);
                Report report = plugin.getDatabaseManager().getReport(id);
                if (report != null) {
                    new ReportManageGUI(plugin).open(player, report);
                }
                return;
            }
        }

        // Fallback to lore parsing if PDC is missing (for legacy items if any)
        if (meta != null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                String plain = org.bukkit.ChatColor.stripColor(line);
                if (plain.contains(": ")) { // Search for any ": <number>" pattern
                    try {
                        String[] parts = plain.split(": ");
                        if (parts.length > 1) {
                            long id = Long.parseLong(parts[parts.length-1].trim());
                            Report report = plugin.getDatabaseManager().getReport(id);
                            if (report != null) {
                                new ReportManageGUI(plugin).open(player, report);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void handleManageClick(Player player, ReportManageHolder holder, int slot) {
        Report report = holder.getReport();

        if (slot == 36) {
            java.util.List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(report.getStatus());
            new CategoryContainerGUI(plugin).openCategoryGUI(player, report.getStatus(), reports, 0);
            return;
        }

        Report.ReportStatus newStatus = null;

        if (slot == 45) newStatus = Report.ReportStatus.PENDING;
        else if (slot == 49) newStatus = Report.ReportStatus.RESOLVED;
        else if (slot == 53) newStatus = Report.ReportStatus.REJECTED;

        if (newStatus != null && newStatus != report.getStatus()) {
            report.setStatus(newStatus);
            plugin.getDatabaseManager().updateReport(report);

            // Reopen category view
            List<Report> reports = plugin.getDatabaseManager().getReportsByStatus(newStatus);
            new CategoryContainerGUI(plugin).openCategoryGUI(player, newStatus, reports);

            LanguageManager lang = LanguageManager.get(plugin);
            String statusColor = switch(newStatus) {
                case PENDING -> "&6";
                case RESOLVED -> "&a";
                case REJECTED -> "&c";
            };
            dev.aevorinstudios.aevorinReports.utils.MessageUtils.sendMessage(player, lang.getMessage("messages.report.status-change", java.util.Map.of(
                "id", String.valueOf(report.getId()),
                "status", lang.getLocalizedStatus(newStatus),
                "color", statusColor
            )));
        }
    }

    private void handleReasonSelectorClick(Player player, ReportReasonHolder holder, int slot, ItemStack clicked) {
        // Handle navigation
        if (clicked.getType() == Material.ARROW) {
            int targetPage = holder.getPage() + (slot == 53 ? 1 : -1);
            new ReportReasonContainerGUI(plugin).showReasonContainerGUI(player, holder.getTargetPlayer(), targetPage);
            return;
        }

        // Handle custom reason selection
        if (clicked.getType() == Material.WRITABLE_BOOK) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "custom_reason"), org.bukkit.persistence.PersistentDataType.BYTE)) {
                player.closeInventory();
                // Execute command as player for consistency
                player.performCommand("report " + holder.getTargetPlayer() + " custom");
                return;
            }
        }

        // Handle reason selection
        if (clicked.getType() == Material.PAPER) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null) {
                String reasonName = meta.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey(plugin, "reason_name"),
                    org.bukkit.persistence.PersistentDataType.STRING
                );

                if (reasonName == null) {
                    // Fallback to display name if PDC is missing for some reason
                    reasonName = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
                }

                player.closeInventory();
                // Execute command as player for consistency
                player.performCommand("report " + holder.getTargetPlayer() + " " + reasonName);
            }
        }
    }

    private void handlePlayerReportsClick(Player player, PlayerReportsHolder holder, int slot, ItemStack clicked) {
        // Handle pagination arrows
        if (slot == 48 || slot == 50) {
            if (clicked.getType() == Material.ARROW) {
                int targetPage = holder.getPage() + (slot == 50 ? 1 : -1);
                if (targetPage >= 0) {
                    List<Report> reports = plugin.getDatabaseManager().getReportsByReporter(player.getUniqueId());
                    new CategoryContainerGUI(plugin).openPlayerReportsGUI(player, reports, targetPage);
                }
            }
        }
    }
}
