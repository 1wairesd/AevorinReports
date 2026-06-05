package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class ReportManageGUI {
    private final BukkitPlugin plugin;
    public ReportManageGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }
    public void open(Player player, Report report) {
        String guiType = plugin.getConfig().getString("reports.gui.type", "book").toLowerCase();
        if (guiType.equals("book")) {
            openBookGUI(player, report);
            return;
        }

        LanguageManager lang = LanguageManager.get(plugin);
        String title = lang.getMessage("gui.container.manage_report.title", Map.of("id", String.valueOf(report.getId())));
        Inventory gui = Bukkit.createInventory(new dev.aevorinstudios.aevorinReports.gui.holders.ReportManageHolder(report), 54, title);

        // Fill a background with light-gray glass panes
        ItemStack background = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Convert UUIDs to player names
        String reporterName = dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver.resolvePlayerName(report.getReporter());
        String reportedName = dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver.resolvePlayerName(report.getReported());

        if (reporterName == null) reporterName = lang.getMessage("common.unknown");
        if (reportedName == null) reportedName = lang.getMessage("common.unknown");

        // Info item
        ItemStack info = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(lang.getMessage("gui.container.manage_report.details.title"));
        infoMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        infoMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        // Get server name with fallback
        String serverName = report.getServerName();
        if (serverName == null || serverName.isEmpty()) {
            serverName = lang.getMessage("common.unknown");
        }

        infoMeta.setLore(java.util.List.of(
            lang.getMessage("gui.container.shared.separator"),
            lang.getMessage("gui.container.manage_report.details.lore.reporter", Map.of("reporter", reporterName)),
            lang.getMessage("gui.container.manage_report.details.lore.reported", Map.of("reported", reportedName)),
            lang.getMessage("gui.container.manage_report.details.lore.reason", Map.of("reason", lang.getLocalizedReason(report.getReason()))),
            lang.getMessage("gui.container.manage_report.details.lore.status", Map.of("status", lang.getLocalizedStatus(report.getStatus()))),
            lang.getMessage("gui.container.manage_report.details.lore.id", Map.of("id", String.valueOf(report.getId()))),
            lang.getMessage("gui.container.manage_report.details.lore.server", Map.of("server", serverName)),
            lang.getMessage("gui.container.shared.separator")
        ));
        info.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.DURABILITY, 1);
        info.setItemMeta(infoMeta);

        // Center the book in the GUI (slot 22 is the center of a chest inventory)
        gui.setItem(22, info);

        // Add decorative items around the main info for a centered frame
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }

        // Create a visual frame around the centered report details
        for (int slot : new int[]{12, 13, 14, 21, 23, 30, 31, 32}) {
            gui.setItem(slot, glass);
        }

        // Only show management buttons if player has permission
        if (player.hasPermission("aevorinreports.manage")) {
            // Move to Pending
            if (report.getStatus() != Report.ReportStatus.PENDING) {
                ItemStack pending = new ItemStack(Material.HOPPER);
                ItemMeta pendingMeta = pending.getItemMeta();
                pendingMeta.setDisplayName(lang.getMessage("gui.container.manage_report.pending.title"));
                pendingMeta.setLore(java.util.List.of(
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.pending.lore.description"),
                    lang.getMessage("gui.container.manage_report.pending.lore.current_status", Map.of("status", lang.getLocalizedStatus(report.getStatus()))),
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.pending.lore.action")
                ));
                pending.setItemMeta(pendingMeta);
                gui.setItem(45, pending);
            }
            // Move to Resolved
            if (report.getStatus() != Report.ReportStatus.RESOLVED) {
                ItemStack resolved = new ItemStack(Material.EMERALD_BLOCK);
                ItemMeta resolvedMeta = resolved.getItemMeta();
                resolvedMeta.setDisplayName(lang.getMessage("gui.container.manage_report.resolved.title"));
                resolvedMeta.setLore(java.util.List.of(
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.resolved.lore.description"),
                    lang.getMessage("gui.container.manage_report.resolved.lore.current_status", Map.of("status", lang.getLocalizedStatus(report.getStatus()))),
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.resolved.lore.action")
                ));
                resolved.setItemMeta(resolvedMeta);
                gui.setItem(49, resolved);
            }
            // Move to Rejected
            if (report.getStatus() != Report.ReportStatus.REJECTED) {
                ItemStack rejected = new ItemStack(Material.BARRIER);
                ItemMeta rejectedMeta = rejected.getItemMeta();
                rejectedMeta.setDisplayName(lang.getMessage("gui.container.manage_report.rejected.title"));
                rejectedMeta.setLore(java.util.List.of(
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.rejected.lore.description"),
                    lang.getMessage("gui.container.manage_report.rejected.lore.current_status", Map.of("status", lang.getLocalizedStatus(report.getStatus()))),
                    lang.getMessage("gui.container.shared.separator"),
                    lang.getMessage("gui.container.manage_report.rejected.lore.action")
                ));
                rejected.setItemMeta(rejectedMeta);
                gui.setItem(53, rejected);
            }
        }

        // Add back button to go back to category reports
        ItemStack backButton = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(lang.getMessage("gui.container.manage_report.back_button.title", "&c&lBack"));
            backMeta.setLore(java.util.List.of(lang.getMessage("gui.container.manage_report.back_button.lore", Map.of("status", lang.getLocalizedStatus(report.getStatus())))));
            backButton.setItemMeta(backMeta);
        }
        gui.setItem(36, backButton);

        player.openInventory(gui);
    }

    private void openBookGUI(Player player, Report report) {
        LanguageManager lang = LanguageManager.get(plugin);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta == null) {
            player.sendMessage(lang.getMessage("gui.book.error_creating"));
            return;
        }

        meta.setTitle("Report #" + report.getId());
        meta.setAuthor("Report System");

        String reporterName = Bukkit.getOfflinePlayer(report.getReporter()).getName();
        String reportedName = Bukkit.getOfflinePlayer(report.getReported()).getName();
        if (reporterName == null) reporterName = lang.getMessage("common.unknown");
        if (reportedName == null) reportedName = lang.getMessage("common.unknown");

        // Create a status indicator based on current status
        String statusColor = switch(report.getStatus()) {
            case PENDING -> "&6";
            case RESOLVED -> "&a";
            case REJECTED -> "&c";
        };

        String statusName = lang.getLocalizedStatus(report.getStatus());

        class BookPaginator {
            java.util.List<java.util.List<net.md_5.bungee.api.chat.BaseComponent>> pages = new java.util.ArrayList<>();
            java.util.List<net.md_5.bungee.api.chat.BaseComponent> currentPage = new java.util.ArrayList<>();
            int linesUsed = 0;

            void add(net.md_5.bungee.api.chat.TextComponent component, String rawText) {
                int estimatedLines = 0;

                // Only count actual visual breaks and long wrapping sentences
                if (rawText.equals("\n")) {
                    estimatedLines = 1;
                } else {
                    String stripped = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', rawText));
                    for (String line : stripped.split("\n", -1)) {
                        // Very conservative line limit: Assume ~23 chars per line for normal text
                        // Only add extra lines if the string forces a wrap
                        estimatedLines += Math.max(1, (int) Math.ceil(line.length() / 23.0));
                    }
                }

                // A Minecraft page comfortably fits 14 lines. Break if we exceed that.
                if (linesUsed + estimatedLines > 14 && !currentPage.isEmpty()) {
                    pages.add(new java.util.ArrayList<>(currentPage));
                    currentPage.clear();
                    linesUsed = 0;
                }

                currentPage.add(component);
                linesUsed += estimatedLines;
            }

            net.md_5.bungee.api.chat.BaseComponent[][] getPagesArray() {
                if (!currentPage.isEmpty()) {
                    pages.add(currentPage);
                }
                net.md_5.bungee.api.chat.BaseComponent[][] array = new net.md_5.bungee.api.chat.BaseComponent[pages.size()][];
                for (int i = 0; i < pages.size(); i++) {
                    array[i] = pages.get(i).toArray(new net.md_5.bungee.api.chat.BaseComponent[0]);
                }
                return array;
            }
        }

        BookPaginator paginator = new BookPaginator();

        String titleText = lang.getMessage("gui.book.page.title");
        paginator.add(createLegacy(titleText), titleText);

        String reporterText = lang.getMessage("gui.book.page.reporter", Map.of("reporter", reporterName != null ? reporterName : "Unknown"));
        paginator.add(createLegacy(reporterText), reporterText);

        String reportedText = lang.getMessage("gui.book.page.reported", Map.of("reported", reportedName != null ? reportedName : "Unknown"));
        paginator.add(createLegacy(reportedText), reportedText);

        String reasonPrefixText = lang.getMessage("gui.book.page.reason");
        paginator.add(createLegacy(reasonPrefixText), reasonPrefixText);

        String reasonHoverText = lang.getMessage("gui.book.page.reason_hover");
        paginator.add(createInteractiveLegacy(
            reasonHoverText,
            null,
            lang.getMessage("gui.book.page.reason_hover_content", Map.of("reason", lang.getLocalizedReason(report.getReason())))
        ), reasonHoverText);

        paginator.add(createLegacy("\n"), "\n");

        String statusText = lang.getMessage("gui.book.page.status", Map.of("color", statusColor, "status", statusName));
        paginator.add(createLegacy(statusText), statusText);

        String idText = lang.getMessage("gui.book.page.id", Map.of("id", String.valueOf(report.getId())));
        paginator.add(createLegacy(idText), idText);

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            String serverName = report.getServerName();
            if (serverName == null || serverName.isEmpty()) serverName = lang.getMessage("common.unknown");
            String serverText = lang.getMessage("gui.book.page.server", Map.of("server", serverName));
            paginator.add(createLegacy(serverText), serverText);
        }

        paginator.add(createLegacy("\n"), "\n");

        if (player.hasPermission("aevorinreports.manage")) {
            String clickChangeText = lang.getMessage("gui.book.page.click_to_change");
            paginator.add(createLegacy(clickChangeText), clickChangeText);

            if (report.getStatus() != Report.ReportStatus.PENDING) {
                String pendingText = lang.getMessage("gui.book.status_options.pending");
                paginator.add(createInteractiveLegacy(
                    pendingText,
                    "/setreportstatus " + report.getId() + " to PENDING",
                    lang.getMessage("gui.book.hover_text.pending", Map.of("status", lang.getLocalizedStatus(report.getStatus())))
                ), pendingText);
            }

            if (report.getStatus() != Report.ReportStatus.RESOLVED) {
                String resolvedText = lang.getMessage("gui.book.status_options.resolved");
                paginator.add(createInteractiveLegacy(
                    resolvedText,
                    "/setreportstatus " + report.getId() + " to RESOLVED",
                    lang.getMessage("gui.book.hover_text.resolved", Map.of("status", lang.getLocalizedStatus(report.getStatus())))
                ), resolvedText);
            }

            if (report.getStatus() != Report.ReportStatus.REJECTED) {
                String rejectedText = lang.getMessage("gui.book.status_options.rejected");
                paginator.add(createInteractiveLegacy(
                    rejectedText,
                    "/setreportstatus " + report.getId() + " to REJECTED",
                    lang.getMessage("gui.book.hover_text.rejected", Map.of("status", lang.getLocalizedStatus(report.getStatus())))
                ), rejectedText);
            }
        }

        paginator.add(createLegacy("\n"), "\n");

        String backButtonText = lang.getMessage("gui.book.back_button");
        paginator.add(createInteractiveLegacy(
            backButtonText,
            "/reports",
            lang.getMessage("gui.book.hover_text.back")
        ), backButtonText);

        meta.spigot().setPages(paginator.getPagesArray());
        book.setItemMeta(meta);
        player.openBook(book);
    }

    private net.md_5.bungee.api.chat.TextComponent createLegacy(String text) {
        net.md_5.bungee.api.chat.TextComponent parent = new net.md_5.bungee.api.chat.TextComponent("");
        for (net.md_5.bungee.api.chat.BaseComponent c : net.md_5.bungee.api.chat.TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', text))) {
            parent.addExtra(c);
        }
        return parent;
    }

    private net.md_5.bungee.api.chat.TextComponent createInteractiveLegacy(String text, String command, String hoverText) {
        net.md_5.bungee.api.chat.TextComponent parent = createLegacy(text);
        if (command != null) {
            parent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, command));
        }
        if (hoverText != null) {
            parent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(org.bukkit.ChatColor.translateAlternateColorCodes('&', hoverText))));
        }
        return parent;
    }
}
