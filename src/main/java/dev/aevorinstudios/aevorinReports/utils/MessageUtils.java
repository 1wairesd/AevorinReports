package dev.aevorinstudios.aevorinReports.utils;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SECTION_SERIALIZER =
        LegacyComponentSerializer.legacySection();

    /**
     * Parses a string into a Component, supporting both MiniMessage and legacy codes.
     * @param message The message to parse
     * @return The parsed Component
     */
    public static Component parse(String message) {
        if (message == null || message.isEmpty()) return Component.empty();

        // Standardize on '&' for internal checks, but we'll use the original message for final legacy parsing if needed
        String work = message.replace('§', '&');

        boolean hasMiniMessage =
            work.contains("<") && (work.contains(">") || work.contains("</"));
        boolean hasLegacy = work.matches(".*&[0-9a-fk-or].*");

        if (hasMiniMessage) {
            try {
                // If it has BOTH format styles, convert legacy to MM to support the mix without crashing
                String mmString = hasLegacy
                    ? legacyToMiniMessage(work)
                    : message;
                return MINI_MESSAGE.deserialize(mmString);
            } catch (Exception e) {
                // If MiniMessage fails (malformed tags), we fall back to legacy handling below
            }
        }

        // Otherwise, handle it as legacy (supporting both & and §)
        return LEGACY_SECTION_SERIALIZER.deserialize(
            ChatColor.translateAlternateColorCodes('&', message)
        );
    }

    /**
     * Helper to convert legacy color codes (& and §) to MiniMessage tags.
     */
    private static String legacyToMiniMessage(String input) {
        if (input == null) return null;
        return input
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }

    /**
     * Parses a string with MiniMessage or legacy support and returns a legacy formatted string (§).
     * Useful for older APIs that don't support Components.
     * @param message The message to parse
     * @return The legacy colored string
     */
    public static String parseToLegacy(String message) {
        if (message == null) return "";
        return LEGACY_SECTION_SERIALIZER.serialize(parse(message));
    }

    /**
     * Parses PlaceholderAPI placeholders for a player when PlaceholderAPI is installed.
     * If PlaceholderAPI is unavailable, the original message is returned unchanged.
     *
     * @param player The player context used by PlaceholderAPI
     * @param message The message that may contain PlaceholderAPI placeholders
     * @return The message with PlaceholderAPI placeholders resolved when possible
     */
    public static String parsePlaceholders(Player player, String message) {
        if (message == null || message.isEmpty()) return message;
        if (
            player == null ||
            !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
        ) return message;

        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(
                player,
                message
            );
        } catch (Throwable ignored) {
            return message;
        }
    }

    /**
     * Parses PlaceholderAPI placeholders for each line in a list.
     *
     * @param player The player context used by PlaceholderAPI
     * @param messages The messages that may contain PlaceholderAPI placeholders
     * @return A new list with PlaceholderAPI placeholders resolved when possible
     */
    public static List<String> parsePlaceholders(
        Player player,
        List<String> messages
    ) {
        if (messages == null || messages.isEmpty()) return messages;

        List<String> parsed = new ArrayList<>(messages.size());
        for (String message : messages) {
            parsed.add(parsePlaceholders(player, message));
        }
        return parsed;
    }

    /**
     * Sends a parsed message to a CommandSender
     * @param sender The recipient
     * @param message The message string (supports MiniMessage and legacy & codes)
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parse(message));
        }
    }
}
