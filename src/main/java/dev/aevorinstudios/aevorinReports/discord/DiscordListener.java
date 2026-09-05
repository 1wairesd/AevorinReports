package dev.aevorinstudios.aevorinReports.discord;

import dev.aevorinstudios.aevorinReports.bukkit.BukkitPlugin;
import dev.aevorinstudios.aevorinReports.config.LanguageManager;
import dev.aevorinstudios.aevorinReports.reports.Report;
import dev.aevorinstudios.aevorinReports.utils.PlayerNameResolver;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiscordListener extends ListenerAdapter {
    private final BukkitPlugin plugin;

    public DiscordListener(BukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply(lang("discord.responses.guild-only", "Commands can only be used in a server."))
                    .setEphemeral(true).queue();
            return;
        }

        if (!hasPermission(event.getMember())) {
            event.reply(lang("discord.responses.no-permission", "You don't have permission to manage reports."))
                    .setEphemeral(true).queue();
            return;
        }

        if (plugin.getDatabaseManager() == null) {
            event.reply(lang("discord.responses.database-unavailable",
                    "The report database is currently unavailable. Please contact an administrator or check the server console."))
                    .setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "resolve" -> handleSetStatusSlash(event, Report.ReportStatus.RESOLVED);
            case "reject" -> handleSetStatusSlash(event, Report.ReportStatus.REJECTED);
            case "pending" -> handleSetStatusSlash(event, Report.ReportStatus.PENDING);
            case "lookup" -> handleLookupSlash(event);
            case "reports" -> handleListReportsSlash(event);
            case "help" -> handleHelpSlash(event);
        }
    }

    private void handleHelpSlash(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(lang("discord.help.title", "AevorinReports Help"))
                .setDescription(lang("discord.help.description",
                        "Manage Minecraft reports from Discord using official Slash Commands."))
                .addField("/reports", lang("discord.help.reports", "List all active pending reports."), false)
                .addField("/lookup <id>", lang("discord.help.lookup", "Show detailed info about a report."), false)
                .addField("/resolve <id>", lang("discord.help.resolve", "Mark a report as resolved."), false)
                .addField("/reject <id>", lang("discord.help.reject", "Mark a report as rejected."), false)
                .addField("/pending <id>", lang("discord.help.pending", "Move a report back to pending."), false)
                .addField("/help", lang("discord.help.help", "Show this help menu."), false)
                .setColor(Color.WHITE);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleSetStatusSlash(SlashCommandInteractionEvent event, Report.ReportStatus status) {
        long id = event.getOption("id").getAsLong();
        Report report = plugin.getDatabaseManager().getReport(id);

        if (report == null) {
            event.reply(lang("discord.responses.report-not-found", "Report #{id} not found.",
                    Map.of("id", String.valueOf(id)))).setEphemeral(true).queue();
            return;
        }

        if (report.getStatus() == status) {
            event.reply(lang("discord.responses.status-already-set", "Report #{id} is already {status}.",
                    Map.of("id", String.valueOf(id), "status", localizedStatus(status)))).setEphemeral(true)
                    .queue();
            return;
        }

        report.setStatus(status);
        report.setLastUpdatedBy("Discord:" + event.getUser().getName());
        plugin.getDatabaseManager().updateReport(report);

        // Professional Embed for the Ephemeral success message
        EmbedBuilder successEmbed = new EmbedBuilder()
                .setTitle(lang("discord.responses.status-updated-title", "Report Updated"))
                .setDescription(
                        lang("discord.responses.status-updated-description",
                                "Successfully updated Report **#{id}** to **{status}**.",
                                Map.of("id", String.valueOf(id), "status", localizedStatus(status))))
                .setColor(status == Report.ReportStatus.RESOLVED ? Color.GREEN
                        : (status == Report.ReportStatus.REJECTED ? Color.RED : Color.ORANGE));

        event.replyEmbeds(successEmbed.build()).setEphemeral(true).queue();

        // Public log message to log channel
        plugin.getDiscordManager().sendLogUpdate(report, event.getUser().getAsMention());
    }

    private void handleLookupSlash(SlashCommandInteractionEvent event) {
        long id = event.getOption("id").getAsLong();
        Report report = plugin.getDatabaseManager().getReport(id);

        if (report == null) {
            event.reply(lang("discord.responses.report-not-found", "Report #{id} not found.",
                    Map.of("id", String.valueOf(id)))).setEphemeral(true).queue();
            return;
        }

        String reporter = PlayerNameResolver.resolveReporterName(report);
        String reported = PlayerNameResolver.resolveReportedName(report);

        String colorHex = plugin.getConfig().getString("discord.lookup-color", "#00ffff");
        Color color = Color.CYAN;
        try {
            color = Color.decode(colorHex);
        } catch (NumberFormatException ignored) {
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(lang("discord.lookup.title", "Report Details: #{id}", Map.of("id", String.valueOf(id))))
                .addField(lang("discord.fields.reporter", "Reporter"), reporter, true)
                .addField(lang("discord.fields.reported-player", "Reported Player"), reported, true)
                .addField(lang("discord.fields.reason", "Reason"), report.getReason(), false);

        if (plugin.getDatabaseManager().hasMultipleServers()) {
            embed.addField(lang("discord.fields.server", "Server"), report.getServerName(), true);
        }

        embed.addField(lang("discord.fields.status", "Status"), localizedStatus(report.getStatus()), true)
                .addField(lang("discord.fields.location", "Location"),
                        (report.getWorld() != null ? report.getWorld() : unknown()) + " ("
                                + (report.getCoordinates() != null ? report.getCoordinates() : unknown()) + ")",
                        false)
                .addField(lang("discord.fields.submitted-at", "Submitted At"),
                        report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false)
                .setColor(color);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void handleListReportsSlash(SlashCommandInteractionEvent event) {
        List<Report> activeReports = plugin.getDatabaseManager().getActiveReports();

        if (activeReports.isEmpty()) {
            event.reply(lang("discord.responses.no-active-reports", "There are no active reports."))
                    .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(lang("discord.reports.title", "Active Reports"))
                .setColor(Color.ORANGE);

        StringBuilder sb = new StringBuilder();
        for (Report report : activeReports) {
            String reported = PlayerNameResolver.resolveReportedName(report);
            sb.append("`#").append(report.getId()).append("` - **").append(reported).append("** (")
                    .append(report.getReason()).append(")\n");

            if (sb.length() > 1800) {
                sb.append(lang("discord.reports.and-more", "*...and more*"));
                break;
            }
        }

        embed.setDescription(sb.toString());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private boolean hasPermission(Member member) {
        if (member == null)
            return false;
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER))
            return true;

        String roleId = plugin.getConfig().getString("discord.staff-role-id");
        if (roleId != null && !roleId.isEmpty()) {
            for (Role role : member.getRoles()) {
                if (role.getId().equals(roleId))
                    return true;
            }
        }

        return false;
    }

    private String localizedStatus(Report.ReportStatus status) {
        return lang("common.status." + status.name().toLowerCase(Locale.ROOT), status.name().toLowerCase(Locale.ROOT));
    }

    private String unknown() {
        return lang("common.unknown", "Unknown");
    }

    private String lang(String path, String fallback) {
        String value = LanguageManager.get(plugin).getRawMessage(path);
        return value.startsWith("Missing lang: ") ? fallback : value;
    }

    private String lang(String path, String fallback, Map<String, String> placeholders) {
        String value = lang(path, fallback);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
            value = value.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return value;
    }
}
