package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles all GUI-related functionality for the AevorinReports plugin.
 * This class centralizes the creation and display of both book and chest GUIs for report management.
 */
public class BookGUI {

    private final BukkitPlugin plugin;
    private static final int ITEMS_PER_PAGE = 8;

    public BookGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Shows the report categories book GUI to a player.
     * @param player The player to show the GUI to
     * @param targetPlayer The player being reported
     */
    public void showReportCategories(Player player, String targetPlayer) {
        LanguageManager lang = LanguageManager.get(plugin);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("Report Categories");
        meta.setAuthor("Report System");

        List<String> categories = lang.getReasonList();
        List<BaseComponent[]> pages = new ArrayList<>();

        ComponentBuilder currentPage = new ComponentBuilder("");
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.reporting_header",
                    Map.of("target", targetPlayer)
                ) + "\n\n"
            )
        );
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage("gui.book.reasons_header", "Reasons:") + "\n"
            )
        );
        int itemsOnPage = 0;

        for (String category : categories) {
            if (itemsOnPage >= ITEMS_PER_PAGE) {
                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            TextComponent categoryText = createInteractiveLegacy(
                player,
                "&c• " + category,
                "/report " + targetPlayer + " " + category,
                lang.getMessage(
                    "gui.book.hover_text.report_for",
                    Map.of("category", category)
                )
            );
            currentPage.append(categoryText);

            if (itemsOnPage < ITEMS_PER_PAGE - 1) {
                currentPage.append(createLegacy(player, "\n"));
            }
            itemsOnPage++;
        }

        if (
            plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)
        ) {
            if (itemsOnPage >= ITEMS_PER_PAGE) {
                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            TextComponent customText = createInteractiveLegacy(
                player,
                lang.getMessage("gui.book.custom_reason", "&c• Custom Reason"),
                "/report " + targetPlayer + " custom",
                lang.getMessage(
                    "gui.book.hover_text.custom_reason",
                    "&7Click to provide a custom reason"
                )
            );
            currentPage.append(customText);
            itemsOnPage++;
        }

        if (itemsOnPage > 0) {
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows the reports book GUI to a player.
     * @param player The player to show the GUI to
     */
    public void showReportsBook(Player player) {
        LanguageManager lang = LanguageManager.get(plugin);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("Report Categories");
        meta.setAuthor("Report System");

        ComponentBuilder page = new ComponentBuilder("");
        page.append(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.page.reports_category_header",
                    "&4Reports Category\n\n"
                )
            )
        );

        // Pending Reports Button
        TextComponent pendingButton = createInteractiveLegacy(
            player,
            lang.getMessage("gui.book.main_menu.pending.title"),
            "/reports pending",
            lang.getMessage("gui.book.main_menu.pending.hover")
        );
        page.append(pendingButton).append(createLegacy(player, "\n\n"));

        // Resolved Reports Button
        TextComponent resolvedButton = createInteractiveLegacy(
            player,
            lang.getMessage("gui.book.main_menu.resolved.title"),
            "/reports resolved",
            lang.getMessage("gui.book.main_menu.resolved.hover")
        );
        page.append(resolvedButton).append(createLegacy(player, "\n\n"));

        // Rejected Reports Button
        TextComponent rejectedButton = createInteractiveLegacy(
            player,
            lang.getMessage("gui.book.main_menu.rejected.title"),
            "/reports rejected",
            lang.getMessage("gui.book.main_menu.rejected.hover")
        );
        page.append(rejectedButton);

        meta.spigot().setPages(page.create());
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows reports filtered by status in a book GUI.
     * @param player The player to show the GUI to
     * @param status The status to filter reports by
     */
    public void showReportsByStatus(Player player, Report.ReportStatus status) {
        LanguageManager lang = LanguageManager.get(plugin);
        List<Report> reports = plugin
            .getDatabaseManager()
            .getReportsByStatus(status);
        if (reports.isEmpty()) {
            MessageUtils.sendMessage(
                player,
                lang.getMessage(
                    "messages.error.no-status-reports",
                    Map.of("status", lang.getLocalizedStatus(status))
                )
            );
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        String formattedStatus =
            status.name().substring(0, 1) +
            status.name().substring(1).toLowerCase();
        meta.setTitle(formattedStatus + " Reports");
        meta.setAuthor("Report System");

        List<BaseComponent[]> pages = new ArrayList<>();
        int itemsOnPage = 0;
        int reportNumber = 1;

        String statusColor = getStatusColor(status);
        String statusName = lang.getLocalizedStatus(status);
        ComponentBuilder currentPage = new ComponentBuilder("");
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.report_list.title",
                    Map.of("color", statusColor, "status", statusName)
                )
            )
        );
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.report_list.total",
                    Map.of("total", String.valueOf(reports.size()))
                )
            )
        );

        for (Report report : reports) {
            if (itemsOnPage >= ITEMS_PER_PAGE - 1) {
                currentPage.append(createLegacy(player, "\n\n"));
                TextComponent backButton = createInteractiveLegacy(
                    player,
                    lang.getMessage("gui.book.back_button"),
                    "/reports",
                    lang.getMessage("gui.book.hover_text.back")
                );
                currentPage.append(backButton);

                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            String reportedName = PlayerNameResolver.resolveReportedName(report);

            TextComponent reportEntry = createInteractiveLegacy(
                player,
                lang.getMessage(
                    "gui.book.report_list.entry",
                    Map.of(
                        "index",
                        String.valueOf(reportNumber),
                        "reported",
                        reportedName != null
                            ? reportedName
                            : lang.getMessage("common.unknown")
                    )
                ),
                "/viewreport " + report.getId(),
                lang.getMessage(
                    "gui.book.page.view_details",
                    "&7Click to view details"
                )
            );
            currentPage.append(reportEntry).append(createLegacy(player, "\n"));

            itemsOnPage++;
            reportNumber++;
        }

        if (itemsOnPage > 0) {
            currentPage.append(createLegacy(player, "\n\n"));
            TextComponent backButton = createInteractiveLegacy(
                player,
                lang.getMessage("gui.book.back_button"),
                "/reports",
                lang.getMessage("gui.book.hover_text.back")
            );
            currentPage.append(backButton);
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows reports submitted by the player in a book GUI.
     * @param player The player to show the GUI to
     */
    public void showPlayerReports(Player player) {
        LanguageManager lang = LanguageManager.get(plugin);
        List<Report> reports = plugin
            .getDatabaseManager()
            .getReportsByReporter(player.getUniqueId());
        if (reports.isEmpty()) {
            MessageUtils.sendMessage(
                player,
                lang.getMessage("messages.error.no-reports")
            );
            return;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("Your Reports");
        meta.setAuthor("Report System");

        List<BaseComponent[]> pages = new ArrayList<>();
        int itemsOnPage = 0;
        int reportNumber = 1;

        ComponentBuilder currentPage = new ComponentBuilder("");
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage("gui.book.report_list.user_title")
            )
        );
        currentPage.append(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.report_list.total",
                    Map.of("total", String.valueOf(reports.size()))
                )
            )
        );

        for (Report report : reports) {
            if (itemsOnPage >= ITEMS_PER_PAGE - 1) {
                pages.add(currentPage.create());
                currentPage = new ComponentBuilder("");
                itemsOnPage = 0;
            }

            String reportedName = PlayerNameResolver.resolveReportedName(report);
            String statusColor = getStatusColor(report.getStatus());

            TextComponent reportEntry = createInteractiveLegacy(
                player,
                lang.getMessage(
                    "gui.book.report_list.user_entry",
                    Map.of(
                        "index",
                        String.valueOf(reportNumber),
                        "reported",
                        reportedName != null
                            ? reportedName
                            : lang.getMessage("common.unknown"),
                        "color",
                        statusColor,
                        "status",
                        lang.getLocalizedStatus(report.getStatus())
                    )
                ),
                "/viewreport " + report.getId(),
                lang.getMessage(
                    "gui.book.page.view_details",
                    "&7Click to view details"
                )
            );
            currentPage.append(reportEntry).append(createLegacy(player, "\n"));

            itemsOnPage++;
            reportNumber++;
        }

        if (itemsOnPage > 0) {
            pages.add(currentPage.create());
        }

        meta.spigot().setPages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows detailed report information in a GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    public void showReportDetails(Player player, Report report) {
        String guiType = plugin
            .getConfig()
            .getString("reports.gui.type", "book")
            .toLowerCase();
        if (guiType.equals("book")) {
            showReportDetailsBook(player, report);
        } else {
            showReportDetailsChest(player, report);
        }
    }

    /**
     * Shows detailed report information in a book GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    private void showReportDetailsBook(Player player, Report report) {
        LanguageManager lang = LanguageManager.get(plugin);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setTitle("Report Details");
        meta.setAuthor("Report System");

        String reporterName = report.isAnonymous()
            ? lang.getMessage("common.anonymous", "Anonymous")
            : PlayerNameResolver.resolveReporterName(report);
        String reportedName = PlayerNameResolver.resolveReportedName(report);

        List<BaseComponent> components = new ArrayList<>();
        components.add(
            createLegacy(player, lang.getMessage("gui.book.page.title"))
        );
        components.add(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.page.reporter",
                    Map.of(
                        "reporter",
                        reporterName != null ? reporterName : "Unknown"
                    )
                )
            )
        );
        components.add(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.page.reported",
                    Map.of(
                        "reported",
                        reportedName != null ? reportedName : "Unknown"
                    )
                )
            )
        );

        components.add(
            createLegacy(player, lang.getMessage("gui.book.page.reason"))
        );
        TextComponent reasonHover = createInteractiveLegacy(
            player,
            lang.getMessage("gui.book.page.reason_hover"),
            null,
            lang.getMessage(
                "gui.book.page.reason_hover_content",
                Map.of("reason", lang.getLocalizedReason(report.getReason()))
            )
        );
        components.add(reasonHover);
        components.add(createLegacy(player, "\n"));

        String statusColor = getStatusColor(report.getStatus());
        String statusName = lang.getLocalizedStatus(report.getStatus());
        components.add(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.page.status",
                    Map.of("color", statusColor, "status", statusName)
                )
            )
        );
        components.add(
            createLegacy(
                player,
                lang.getMessage(
                    "gui.book.page.id",
                    Map.of("id", String.valueOf(report.getId()))
                )
            )
        );

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            String serverName = report.getServerName();
            if (serverName == null || serverName.isEmpty()) serverName =
                lang.getMessage("common.unknown");
            components.add(
                createLegacy(
                    player,
                    lang.getMessage(
                        "gui.book.page.server",
                        Map.of("server", serverName)
                    )
                )
            );
        } else {
            components.add(createLegacy(player, "\n"));
        }

        components.add(createLegacy(player, "\n"));

        if (player.hasPermission("aevorinreports.manage")) {
            components.add(
                createLegacy(
                    player,
                    lang.getMessage("gui.book.page.click_to_change")
                )
            );

            if (report.getStatus() == Report.ReportStatus.PENDING) {
                TextComponent resolveButton = createInteractiveLegacy(
                    player,
                    lang.getMessage("gui.book.status_options.resolved"),
                    "/setreportstatus " + report.getId() + " to RESOLVED",
                    lang.getMessage(
                        "gui.book.hover_text.resolved",
                        Map.of(
                            "status",
                            lang.getLocalizedStatus(report.getStatus())
                        )
                    )
                );
                components.add(resolveButton);

                TextComponent rejectButton = createInteractiveLegacy(
                    player,
                    lang.getMessage("gui.book.status_options.rejected"),
                    "/setreportstatus " + report.getId() + " to REJECTED",
                    lang.getMessage(
                        "gui.book.hover_text.rejected",
                        Map.of(
                            "status",
                            lang.getLocalizedStatus(report.getStatus())
                        )
                    )
                );
                components.add(rejectButton);
            } else {
                TextComponent pendingButton = createInteractiveLegacy(
                    player,
                    lang.getMessage("gui.book.status_options.pending"),
                    "/setreportstatus " + report.getId() + " to PENDING",
                    lang.getMessage(
                        "gui.book.hover_text.pending",
                        Map.of(
                            "status",
                            lang.getLocalizedStatus(report.getStatus())
                        )
                    )
                );
                components.add(pendingButton);
            }
        }

        components.add(createLegacy(player, "\n"));
        TextComponent backButton = createInteractiveLegacy(
            player,
            lang.getMessage("gui.book.back_button"),
            "/reports " + report.getStatus().toString().toLowerCase(),
            lang.getMessage("gui.book.hover_text.back")
        );
        components.add(backButton);

        BaseComponent[] pageComponents = components.toArray(
            new BaseComponent[0]
        );
        meta.spigot().setPages(pageComponents);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    /**
     * Shows detailed report information in a chest GUI.
     * @param player The player to show the GUI to
     * @param report The report to show details for
     */
    private void showReportDetailsChest(Player player, Report report) {
        LanguageManager lang = LanguageManager.get(plugin);
        Inventory gui = Bukkit.createInventory(
            null,
            54,
            lang.getMessage(
                "gui.container.manage_report.title",
                Map.of("id", String.valueOf(report.getId()))
            )
        );

        ItemStack background = new ItemStack(
            Material.LIGHT_GRAY_STAINED_GLASS_PANE
        );
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        for (int i = 0; i < 54; i++) gui.setItem(i, background);

        String reporterName = report.isAnonymous()
            ? lang.getMessage("common.anonymous", "Anonymous")
            : PlayerNameResolver.resolveReporterName(report);
        String reportedName = PlayerNameResolver.resolveReportedName(report);
        if (reporterName == null) reporterName = lang.getMessage(
            "common.unknown"
        );
        if (reportedName == null) reportedName = lang.getMessage(
            "common.unknown"
        );

        ItemStack info = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(
            lang.getMessage("gui.container.manage_report.details.title")
        );
        infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        infoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        String serverName = report.getServerName();
        if (serverName == null || serverName.isEmpty()) serverName =
            lang.getMessage("common.unknown");

        List<String> lore = new ArrayList<>();
        lore.add(lang.getMessage("gui.container.separator"));
        lore.add(
            lang.getMessage(
                "gui.container.manage_report.details.lore.reporter",
                Map.of("reporter", reporterName)
            )
        );
        lore.add(
            lang.getMessage(
                "gui.container.manage_report.details.lore.reported",
                Map.of("reported", reportedName)
            )
        );
        lore.add(
            lang.getMessage(
                "gui.container.manage_report.details.lore.reason",
                Map.of("reason", report.getReason())
            )
        );
        lore.add(
            lang.getMessage(
                "gui.container.manage_report.details.lore.status",
                Map.of("status", lang.getLocalizedStatus(report.getStatus()))
            )
        );
        lore.add(
            lang.getMessage(
                "gui.container.manage_report.details.lore.id",
                Map.of("id", String.valueOf(report.getId()))
            )
        );

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            lore.add(
                lang.getMessage(
                    "gui.container.manage_report.details.lore.server",
                    Map.of("server", serverName)
                )
            );
        }

        lore.add(lang.getMessage("gui.container.separator"));
        infoMeta.setLore(lore);
        info.addUnsafeEnchantment(
            org.bukkit.enchantments.Enchantment.DURABILITY,
            1
        );
        info.setItemMeta(infoMeta);

        gui.setItem(22, info);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int slot : new int[] { 12, 13, 14, 21, 23, 30, 31, 32 })
            gui.setItem(slot, glass);

        if (player.hasPermission("aevorinreports.manage")) {
            if (report.getStatus() != Report.ReportStatus.PENDING) {
                ItemStack pending = new ItemStack(Material.HOPPER);
                ItemMeta pendingMeta = pending.getItemMeta();
                pendingMeta.setDisplayName(
                    lang.getMessage("gui.container.manage_report.pending.title")
                );
                pendingMeta.setLore(
                    List.of(
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.pending.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.manage_report.pending.lore.current_status",
                            Map.of(
                                "status",
                                lang.getLocalizedStatus(report.getStatus())
                            )
                        ),
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.pending.lore.action"
                        )
                    )
                );
                pending.setItemMeta(pendingMeta);
                gui.setItem(45, pending);
            }
            if (report.getStatus() != Report.ReportStatus.RESOLVED) {
                ItemStack resolved = new ItemStack(Material.EMERALD_BLOCK);
                ItemMeta resolvedMeta = resolved.getItemMeta();
                resolvedMeta.setDisplayName(
                    lang.getMessage(
                        "gui.container.manage_report.resolved.title"
                    )
                );
                resolvedMeta.setLore(
                    List.of(
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.resolved.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.manage_report.resolved.lore.current_status",
                            Map.of(
                                "status",
                                lang.getLocalizedStatus(report.getStatus())
                            )
                        ),
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.resolved.lore.action"
                        )
                    )
                );
                resolved.setItemMeta(resolvedMeta);
                gui.setItem(49, resolved);
            }
            if (report.getStatus() != Report.ReportStatus.REJECTED) {
                ItemStack rejected = new ItemStack(Material.BARRIER);
                ItemMeta rejectedMeta = rejected.getItemMeta();
                rejectedMeta.setDisplayName(
                    lang.getMessage(
                        "gui.container.manage_report.rejected.title"
                    )
                );
                rejectedMeta.setLore(
                    List.of(
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.rejected.lore.description"
                        ),
                        lang.getMessage(
                            "gui.container.manage_report.rejected.lore.current_status",
                            Map.of(
                                "status",
                                lang.getLocalizedStatus(report.getStatus())
                            )
                        ),
                        lang.getMessage("gui.container.separator"),
                        lang.getMessage(
                            "gui.container.manage_report.rejected.lore.action"
                        )
                    )
                );
                rejected.setItemMeta(rejectedMeta);
                gui.setItem(53, rejected);
            }
        }
        player.openInventory(gui);
    }

    private String getStatusColor(Report.ReportStatus status) {
        return switch (status) {
            case PENDING -> "&6";
            case RESOLVED -> "&a";
            case REJECTED -> "&c";
            default -> "&7";
        };
    }

    private TextComponent createLegacy(Player player, String text) {
        text = MessageUtils.parsePlaceholders(player, text);
        TextComponent parent = new TextComponent("");
        for (net.md_5.bungee.api.chat.BaseComponent c : net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
            org.bukkit.ChatColor.translateAlternateColorCodes('&', text)
        )) {
            parent.addExtra(c);
        }
        return parent;
    }

    private TextComponent createInteractiveLegacy(
        Player player,
        String text,
        String command,
        String hoverText
    ) {
        TextComponent parent = createLegacy(player, text);
        if (command != null) {
            parent.setClickEvent(
                new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                    command
                )
            );
        }
        if (hoverText != null) {
            hoverText = MessageUtils.parsePlaceholders(player, hoverText);
            parent.setHoverEvent(
                new net.md_5.bungee.api.chat.HoverEvent(
                    net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        org.bukkit.ChatColor.translateAlternateColorCodes(
                            '&',
                            hoverText
                        )
                    )
                )
            );
        }
        return parent;
    }
}
