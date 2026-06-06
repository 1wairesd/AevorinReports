package dev.aevorinstudios.aevorinReports.gui;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.gui.holders.ReportReasonHolder;
import dev.aevorinstudios.aevorinReports.utils.MessageUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ReportReasonContainerGUI {

    private final BukkitPlugin plugin;

    public ReportReasonContainerGUI(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    public void showReasonContainerGUI(Player player, String targetPlayer) {
        showReasonContainerGUI(player, targetPlayer, 1);
    }

    public void showReasonContainerGUI(
        Player player,
        String targetPlayer,
        int page
    ) {
        LanguageManager lang = LanguageManager.get(plugin);

        List<String> reasons = lang.getReasonList();
        if (reasons.isEmpty()) {
            reasons = new ArrayList<>(
                List.of(
                    "hacking_cheating",
                    "harassment_bullying",
                    "inappropriate_language",
                    "spam_advertisement",
                    "griefing_vandalism",
                    "exploiting_bugs",
                    "inappropriate_skin_name"
                )
            );
        }

        reasons = reasons
            .stream()
            .filter(reason -> !reason.equalsIgnoreCase("Other"))
            .collect(java.util.stream.Collectors.toList());

        int size = 54;
        String title = parsePlaceholders(
            player,
            lang.getMessage(
                "gui.container.menus.reason_selector.title",
                Map.of("target", targetPlayer)
            )
        );
        Inventory gui = Bukkit.createInventory(
            new ReportReasonHolder(targetPlayer, page),
            size,
            title
        );

        ItemStack background = new ItemStack(
            Material.LIGHT_GRAY_STAINED_GLASS_PANE
        );
        ItemMeta bgMeta = background.getItemMeta();
        if (bgMeta != null) {
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
        }
        for (int i = 0; i < size; i++) gui.setItem(i, background);

        int[] slots;
        if (page == 1) {
            ItemStack bookIcon = new ItemStack(Material.BOOK);
            ItemMeta bookMeta = bookIcon.getItemMeta();
            if (bookMeta != null) {
                bookMeta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.menus.reason_selector.info_icon.title",
                            Map.of("target", targetPlayer)
                        )
                    )
                );
                bookMeta.setLore(
                    parsePlaceholders(
                        player,
                        lang.getMessageList(
                            "gui.container.menus.reason_selector.info_icon.lore"
                        )
                    )
                );
                bookIcon.setItemMeta(bookMeta);
            }
            gui.setItem(13, bookIcon);
            slots = new int[] { 29, 30, 31, 32, 33, 38, 39, 40, 41, 42 };
        } else {
            slots = new int[] {
                11,
                12,
                13,
                14,
                15,
                20,
                21,
                22,
                23,
                24,
                29,
                30,
                31,
                32,
                33,
                38,
                39,
                40,
                41,
                42,
            };
        }

        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil(
            (double) reasons.size() / itemsPerPage
        );
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(reasons.size(), startIndex + itemsPerPage);

        if (startIndex >= reasons.size() && page > 1) {
            showReasonContainerGUI(player, targetPlayer, 1);
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {
            String reason = reasons.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.menus.reason_selector.reason_item.title",
                            Map.of("reason", reason)
                        )
                    )
                );
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(
                    org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES
                );
                meta.setLore(
                    parsePlaceholders(
                        player,
                        List.of(
                            lang.getMessage(
                                "gui.container.menus.reason_selector.reason_item.lore.target",
                                Map.of("target", targetPlayer)
                            ),
                            lang.getMessage(
                                "gui.container.menus.reason_selector.reason_item.lore.action"
                            )
                        )
                    )
                );
                item.setItemMeta(meta);
                meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "reason_name"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    reason
                );
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(
                    org.bukkit.enchantments.Enchantment.DURABILITY,
                    1
                );
            }
            gui.setItem(slots[i - startIndex], item);
        }

        if (totalPages > 1) {
            if (page < totalPages) {
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                if (nextMeta != null) {
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
                            lang.getMessageList(
                                "gui.container.shared.navigation.next_page.lore",
                                Map.of(
                                    "page",
                                    String.valueOf(page + 1),
                                    "total",
                                    String.valueOf(totalPages)
                                )
                            )
                        )
                    );
                    nextButton.setItemMeta(nextMeta);
                }
                gui.setItem(53, nextButton);
            }
            if (page > 1) {
                ItemStack prevButton = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevButton.getItemMeta();
                if (prevMeta != null) {
                    prevButton.setItemMeta(prevMeta);
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
                            lang.getMessageList(
                                "gui.container.shared.navigation.previous_page.lore",
                                Map.of(
                                    "page",
                                    String.valueOf(page - 1),
                                    "total",
                                    String.valueOf(totalPages)
                                )
                            )
                        )
                    );
                    prevButton.setItemMeta(prevMeta);
                }
                gui.setItem(45, prevButton);
            }
        }
        if (
            plugin.getConfig().getBoolean("reports.allow-custom-reasons", true)
        ) {
            ItemStack customItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta customMeta = customItem.getItemMeta();
            if (customMeta != null) {
                customMeta.setDisplayName(
                    parsePlaceholders(
                        player,
                        lang.getMessage(
                            "gui.container.menus.reason_selector.custom_reason_item.title",
                            "&cCustom Reason"
                        )
                    )
                );
                customMeta.setLore(
                    parsePlaceholders(
                        player,
                        List.of(
                            lang.getMessage(
                                "gui.container.menus.reason_selector.custom_reason_item.lore",
                                "&7Click to enter a custom reason"
                            )
                        )
                    )
                );
                customMeta
                    .getPersistentDataContainer()
                    .set(
                        new org.bukkit.NamespacedKey(plugin, "custom_reason"),
                        org.bukkit.persistence.PersistentDataType.BYTE,
                        (byte) 1
                    );
                customItem.setItemMeta(customMeta);
            }
            gui.setItem(49, customItem);
        }

        player.openInventory(gui);
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
