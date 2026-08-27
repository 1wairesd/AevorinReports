package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CategoryContainerGUI {

    private final BukkitPlugin plugin;

    public CategoryContainerGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a paginated category GUI showing reports with the specified status
     *
     * @param player  The player to show the GUI to
     * @param status  The status of reports to display
     * @param reports The list of all reports with this status
     * @param page    The page number to display (0-based)
     */
    /**
     * Opens a paginated GUI showing reports for a player (their own reports)
     */
    public void openPlayerReportsGUI(
        Player player,
        List<Report> reports,
        int page
    ) {
        LanguageManager lang = LanguageManager.get(plugin);
        String title = parsePlaceholders(
            player,
            lang.getMessage(
                "gui.container.menus.player_reports.title",
                "Your Reports"
            )
        );
        Inventory gui = Bukkit.createInventory(
            new dev.aevorinstudios.aevorinReports.gui.holders.PlayerReportsHolder(
                page
            ),
            54,
            title
        );

        openReportsListGUI(gui, player, reports, page, lang);
    }

    public void openMainMenu(Player player) {
        LanguageManager lang = LanguageManager.get(plugin);
        Inventory gui = Bukkit.createInventory(
            new dev.aevorinstudios.aevorinReports.gui.holders.ReportsMenuHolder(),
            27,
            parsePlaceholders(
                player,
                lang.getMessage("gui.container.menus.main_menu.title")
            )
        );

        // Pending Reports
        ItemStack pending = new ItemStack(Material.PAPER);
        ItemMeta pendingMeta = pending.getItemMeta();
        if (pendingMeta != null) {
            pendingMeta.setDisplayName(
                parsePlaceholders(
                    player,
                    lang.getMessage(
                        "gui.container.menus.main_menu.pending.title"
                    )
                )
            );
            pendingMeta.setLore(
                parsePlaceholders(
                    player,
                    java.util.List.of(
                        lang.getMessage(
                            "gui.container.menus.main_menu.pending.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.menus.main_menu.pending.lore.action"
                        )
                    )
                )
            );
            pending.setItemMeta(pendingMeta);
        }
        gui.setItem(10, pending);

        // Resolved Reports
        ItemStack resolved = new ItemStack(Material.BOOK);
        ItemMeta resolvedMeta = resolved.getItemMeta();
        if (resolvedMeta != null) {
            resolvedMeta.setDisplayName(
                parsePlaceholders(
                    player,
                    lang.getMessage(
                        "gui.container.menus.main_menu.resolved.title"
                    )
                )
            );
            resolvedMeta.setLore(
                parsePlaceholders(
                    player,
                    java.util.List.of(
                        lang.getMessage(
                            "gui.container.menus.main_menu.resolved.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.menus.main_menu.resolved.lore.action"
                        )
                    )
                )
            );
            resolved.setItemMeta(resolvedMeta);
        }
        gui.setItem(13, resolved);

        // Rejected Reports
        ItemStack rejected = new ItemStack(Material.BARRIER);
        ItemMeta rejectedMeta = rejected.getItemMeta();
        if (rejectedMeta != null) {
            rejectedMeta.setDisplayName(
                parsePlaceholders(
                    player,
                    lang.getMessage(
                        "gui.container.menus.main_menu.rejected.title"
                    )
                )
            );
            rejectedMeta.setLore(
                parsePlaceholders(
                    player,
                    java.util.List.of(
                        lang.getMessage(
                            "gui.container.menus.main_menu.rejected.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.menus.main_menu.rejected.lore.action"
                        )
                    )
                )
            );
            rejected.setItemMeta(rejectedMeta);
        }
        gui.setItem(16, rejected);

        player.openInventory(gui);
    }

    public void openCategoryGUI(
        Player player,
        Report.ReportStatus status,
        List<Report> reports,
        int page
    ) {
        LanguageManager lang = LanguageManager.get(plugin);
        String statusName = lang.getLocalizedStatus(status);
        String title = parsePlaceholders(
            player,
            lang.getMessage(
                "gui.container.menus.category_view.title",
                Map.of("status", statusName)
            )
        );
        Inventory gui = Bukkit.createInventory(
            new dev.aevorinstudios.aevorinReports.gui.holders.CategoryReportsHolder(
                status,
                page
            ),
            54,
            title
        );

        openReportsListGUI(gui, player, reports, page, lang);
    }

    private void openReportsListGUI(
        Inventory gui,
        Player player,
        List<Report> reports,
        int page,
        LanguageManager lang
    ) {
        int size = 54;

        // Fill base background
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }

        for (int i = 0; i < 9; i++) gui.setItem(i, background);
        for (int i = 45; i < 54; i++) gui.setItem(i, background);
        for (int i = 9; i <= 36; i += 9) gui.setItem(i, background);
        for (int i = 17; i <= 44; i += 9) gui.setItem(i, background);

        java.util.List<Integer> innerSlots = new java.util.ArrayList<>();
        for (int row = 1; row < 5; row++) {
            for (int col = 1; col < 8; col++) {
                innerSlots.add(row * 9 + col);
            }
        }
        innerSlots.remove(Integer.valueOf(49));

        int reportsPerPage = innerSlots.size();
        int totalPages = (int) Math.ceil(
            reports.size() / (double) reportsPerPage
        );
        if (totalPages == 0) totalPages = 1;

        int startIndex = page * reportsPerPage;
        int endIndex = Math.min(startIndex + reportsPerPage, reports.size());

        if (totalPages > 1) {
            ItemStack pageIndicator = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.setDisplayName(
                parsePlaceholders(
                    player,
                    lang.getMessage(
                        "gui.container.shared.navigation.page_indicator.title",
                        Map.of(
                            "page",
                            String.valueOf(page + 1),
                            "total",
                            String.valueOf(totalPages)
                        )
                    )
                )
            );
            pageMeta.setLore(
                parsePlaceholders(
                    player,
                    java.util.List.of(
                        lang.getMessage(
                            "gui.container.shared.navigation.page_indicator.lore",
                            Map.of(
                                "start",
                                String.valueOf(startIndex + 1),
                                "end",
                                String.valueOf(endIndex),
                                "total",
                                String.valueOf(reports.size())
                            )
                        )
                    )
                )
            );
            pageIndicator.setItemMeta(pageMeta);
            gui.setItem(49, pageIndicator);

            if (page > 0) {
                ItemStack prevArrow = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevArrow.getItemMeta();
                prevMeta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.shared.navigation.previous_page.title"
                        )
                    )
                );
                prevMeta.setLore(
                    parsePlaceholders(
                        player,
                        java.util.List.of(
                            lang.getMessage(
                                "gui.container.shared.navigation.previous_page.lore",
                                Map.of("page", String.valueOf(page))
                            )
                        )
                    )
                );
                prevArrow.setItemMeta(prevMeta);
                gui.setItem(48, prevArrow);
            }

            if (page < totalPages - 1) {
                ItemStack nextArrow = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextArrow.getItemMeta();
                nextMeta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.shared.navigation.next_page.title"
                        )
                    )
                );
                nextMeta.setLore(
                    parsePlaceholders(
                        player,
                        java.util.List.of(
                            lang.getMessage(
                                "gui.container.shared.navigation.next_page.lore",
                                Map.of("page", String.valueOf(page + 2))
                            )
                        )
                    )
                );
                nextArrow.setItemMeta(nextMeta);
                gui.setItem(50, nextArrow);
            }
        }

        if (
            gui.getHolder() instanceof
                dev.aevorinstudios.aevorinReports.gui.holders.CategoryReportsHolder
        ) {
            ItemStack backButton = new ItemStack(Material.DARK_OAK_DOOR);
            ItemMeta backMeta = backButton.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.shared.navigation.back_button.title",
                            "&cBack"
                        )
                    )
                );
                backMeta.setLore(
                    parsePlaceholders(
                        player,
                        java.util.List.of(
                            lang.getMessage(
                                "gui.container.shared.navigation.back_button.lore",
                                "&7Go back to categories"
                            )
                        )
                    )
                );
                backButton.setItemMeta(backMeta);
            }
            gui.setItem(45, backButton); // Bottom left corner
        }

        for (int i = startIndex; i < endIndex; i++) {
            Report report = reports.get(i);
            int slotIndex = i - startIndex;
            if (slotIndex >= innerSlots.size()) break;

            String reporterName =
                dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver.resolveReporterName(report);
            String reportedName =
                dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver.resolveReportedName(report);
            if (reporterName == null) reporterName = lang.getMessage(
                "common.unknown"
            );
            if (reportedName == null) reportedName = lang.getMessage(
                "common.unknown"
            );

            ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(
                parsePlaceholders(
                    player,
                    lang.getMessage(
                        "gui.container.menus.category_view.report_item.title",
                        Map.of(
                            "reporter",
                            reporterName,
                            "reported",
                            reportedName
                        )
                    )
                )
            );

            String serverName = report.getServerName();
            if (serverName == null || serverName.isEmpty()) serverName =
                lang.getMessage("common.unknown");

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(lang.getMessage("gui.container.shared.separator"));
            lore.add(
                lang.getMessage(
                    "gui.container.menus.category_view.report_item.lore.reason",
                    Map.of(
                        "reason",
                        lang.getLocalizedReason(report.getReason())
                    )
                )
            );
            lore.add(
                lang.getMessage(
                    "gui.container.menus.category_view.report_item.lore.status",
                    Map.of(
                        "status",
                        lang.getLocalizedStatus(report.getStatus())
                    )
                )
            );
            lore.add(
                lang.getMessage(
                    "gui.container.menus.category_view.report_item.lore.id",
                    Map.of("id", String.valueOf(report.getId()))
                )
            );
            lore.add(
                lang.getMessage(
                    "gui.container.menus.category_view.report_item.lore.server",
                    Map.of("server", serverName)
                )
            );
            lore.add(lang.getMessage("gui.container.shared.separator"));

            if (player.hasPermission("aevorinreports.manage")) {
                lore.add(
                    lang.getMessage(
                        "gui.container.menus.category_view.report_item.lore.action"
                    )
                );
            }

            meta.setLore(parsePlaceholders(player, lore));
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "report_id"),
                org.bukkit.persistence.PersistentDataType.LONG,
                report.getId()
            );

            item.setItemMeta(meta);
            gui.setItem(innerSlots.get(slotIndex), item);
        }
        player.openInventory(gui);
    }

    public void openCategoryGUI(
        Player player,
        Report.ReportStatus status,
        List<Report> reports
    ) {
        openCategoryGUI(player, status, reports, 0);
    }

    private String parsePlaceholders(Player player, String message) {
        return MessageUtils.parsePlaceholders(player, message);
    }

    private List<String> parsePlaceholders(
        Player player,
        List<String> messages
    ) {
        return MessageUtils.parsePlaceholders(player, messages);
    }
}
