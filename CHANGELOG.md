# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-07-20

### Improvements & Bug Fixes

- Added support for reporting offline players who have played on the server before.
  - `/report <player>` now resolves targets through Bukkit `OfflinePlayer`, allowing reports against cached offline players while still rejecting unknown players.
  - Added `reports.allow-offline-player-reporting` to `config.yml`, defaulting to `true`, so servers can disable offline-player reporting if desired.
  - Report creation now stores the resolved offline player's UUID and keeps custom-reason and GUI report flows working with offline targets.
  - Report command tab completion now includes cached offline players in addition to online players when offline-player reporting is enabled.
- PlaceholderAPI placeholders are now parsed in built-in GUI text, including book GUI text and container GUI titles, item names, and lore.
  - This allows placeholders such as `%reports_total_pending%` to display live values in configured GUI text.
- Cleaned up Java imports in the language manager for improved readability.

### Language Support

- Added **German** language support.
- Added **Dutch** language support.
- Added **Vietnamese** language support.

### Contributors

Special thanks to **Qracky** & **Karven** for the Dutch & Vietnamese language translations.

We are looking for translators to add even more language support to AevorinReports! Join our [Discord server](https://discord.gg/SV2dXt5SwF) and open a ticket if you'd like to help!

## [1.0.9.1] - 2026-06-05

### Improvements & Bug Fixes

- Localized status placeholders now use language-file values instead of raw enum names.
  - Affected `{status}` placeholders now display values such as `common.status.pending` instead of `PENDING`.
  - Applies to report details, status-change messages, hover text, current-status lore, and empty status-list messages.
- Fixed `/reports <status>` ignoring `reports.gui.type: container`.
  - Commands such as `/reports REJECTED` now open the configured container GUI directly instead of opening a book first.
- Updated the Shadow build plugin to fix ASM relocation failures during `shadowJar` on newer Gradle/JDK environments.

### Contributors

Special thanks to **feijiwang** A.K.A **nice** for reporting these issues to us.

## [1.0.9] - 2026-06-03

### New Features

- **Player-Specific Placeholder Lookup**: Added support for looking up report statistics for specific players by name.
  - All player-based placeholders now support a `_<playername>` suffix to query any player's statistics.
  - Works with both online and offline players (if they have played before).
  - **Full list of supported player-specific placeholders**:
    - `%reports_submitted%` - Total reports submitted by the player viewing the placeholder.
    - `%reports_pending_submitted%` - Pending reports submitted by the player viewing the placeholder.
    - `%reports_resolved_submitted%` - Resolved reports submitted by the player viewing the placeholder.
    - `%reports_valid_submitted%` - Valid reports submitted by the player viewing the placeholder. Currently counts resolved reports.
    - `%reports_rejected_submitted%` - Rejected reports submitted by the player viewing the placeholder.
    - `%reports_against%` - Total reports made against the player viewing the placeholder.
    - `%reports_pending_against%` - Pending reports made against the player viewing the placeholder.
    - `%reports_resolved_against%` - Resolved reports made against the player viewing the placeholder.
    - `%reports_rejected_against%` - Rejected reports made against the player viewing the placeholder.
    - `%reports_submitted_by_<player>%` - Total reports submitted by the specified player.
    - `%reports_pending_submitted_by_<player>%` - Pending reports submitted by the specified player.
    - `%reports_resolved_submitted_by_<player>%` - Resolved reports submitted by the specified player.
    - `%reports_valid_submitted_by_<player>%` - Valid reports submitted by the specified player. Currently counts resolved reports.
    - `%reports_rejected_submitted_by_<player>%` - Rejected reports submitted by the specified player.
    - `%reports_against_<player>%` - Total reports made against the specified player.
    - `%reports_pending_against_<player>%` - Pending reports made against the specified player.
    - `%reports_resolved_against_<player>%` - Resolved reports made against the specified player.
    - `%reports_rejected_against_<player>%` - Rejected reports made against the specified player.
  - **Server-wide statistics placeholders**:
    - `%reports_total%` - Total number of reports in the entire network.
    - `%reports_total_pending%` - Total pending reports on the network.
    - `%reports_total_resolved%` - Total resolved reports on the network.
    - `%reports_total_rejected%` - Total rejected reports on the network.
  - **Specific-server statistics placeholders**:
    - `%reports_total_on_<server>%` - Total reports created on a specific server.
    - `%reports_total_pending_on_<server>%` - Total pending reports on a specific server.
    - `%reports_total_resolved_on_<server>%` - Total resolved reports on a specific server.
    - `%reports_total_rejected_on_<server>%` - Total rejected reports on a specific server.
  - Specific-server placeholders only return server-specific counts when the plugin is using MySQL with multiple servers connected to the same database.

### Improvements & Bug Fixes

- PlaceholderAPI expansion now gracefully handles offline players by checking player cache.
- Simplified the placeholders for better understanding.
- Discord channel ID settings now validate numeric IDs before sending notifications, preventing console spam when a Discord URL is configured by mistake.

### Contributors

Special thanks to **feijiwang** A.K.A **nice** for the **Simplified Chinese** translation and **GraviTrace** for the **Polish** translation!

We are looking for translators to add even more language support to AevorinReports! Join our [Discord server](https://discord.gg/SV2dXt5SwF) and open a ticket if you'd like to help!

## [1.0.9-Beta-1] - 2026-05-09

> [!NOTE]
> This is a **Beta** version. While it has been tested, this release introduces new external integrations (PlaceholderAPI) that may behave differently across various server setups. Please test thoroughly before deploying to production environments.

### New Features

- **PlaceholderAPI Integration**: Added full PlaceholderAPI support for player report statistics.
  - **Player Submitted Reports**:
    - `%aevorinreports_reports_submitted%` / `%aevorinreports_submitted%` - Total reports submitted by the player.
    - `%aevorinreports_reports_submitted_pending%` / `%aevorinreports_submitted_pending%` - Pending reports submitted by the player.
    - `%aevorinreports_reports_submitted_resolved%` / `%aevorinreports_submitted_resolved%` - Resolved reports submitted by the player.
    - `%aevorinreports_reports_submitted_valid%` / `%aevorinreports_submitted_valid%` - Valid (resolved) reports submitted by the player.
    - `%aevorinreports_reports_submitted_rejected%` / `%aevorinreports_submitted_rejected%` - Rejected reports submitted by the player.
  - **Reports Against Player**:
    - `%aevorinreports_reports_received%` / `%aevorinreports_received%` - Total reports received against the player.
    - `%aevorinreports_reports_received_pending%` / `%aevorinreports_received_pending%` - Pending reports against the player.
    - `%aevorinreports_reports_received_resolved%` / `%aevorinreports_received_resolved%` - Resolved reports against the player.
    - `%aevorinreports_reports_received_rejected%` / `%aevorinreports_received_rejected%` - Rejected reports against the player.
  - **Server-Wide Statistics**:
    - `%aevorinreports_total_reports%` - Total number of reports in the system.
    - `%aevorinreports_pending_reports%` / `%aevorinreports_total_pending%` - Total pending reports on the server.
    - `%aevorinreports_resolved_reports%` / `%aevorinreports_total_resolved%` - Total resolved reports on the server.
    - `%aevorinreports_rejected_reports%` / `%aevorinreports_total_rejected%` - Total rejected reports on the server.
- **Official Language Support**: Added official support for **Polish (pl_PL)**.

### Contributors

Special thanks to **GraviTrace** for the **Polish** translation!

We are looking for translators to add even more language support to AevorinReports! Join our [Discord server](https://discord.gg/SV2dXt5SwF) and open a ticket if you'd like to help!

## [1.0.8] - 2026-03-31

### New Features

- **Standardized Messaging System**:
  - Replaced hardcoded messages with localized strings via `LanguageManager`.
  - Fully localized command usage strings (`/report`, `/viewreport`, `/setreportstatus`) for all supported languages.
  - Added localized feedback for the `/ar reload` command.
- **Custom Reason Support in GUIs**:
  - Added "Custom Reason" option to the **Book GUI**.
  - Added "Custom Reason" (Writable Book) icon to the **Container GUI** for players to submit their own reasons in chat.
- **Enhanced Visual Aesthetics**:
  - Implemented high-fidelity **MiniMessage gradients** for report titles, details, and notifications.
  - Simplified GUI design by removing bold text for a cleaner, modern look.
  - Redesigned the container separator for better visual structure.
- **Improved Global Prefix**: The plugin prefix now supports MiniMessage gradients and is consistently applied across all messages using the `{prefix}` placeholder.

### Improvements & Bug Fixes

- **Hybrid Message Parsing**: Implemented a robust "Hybrid Parser" for messages that contain both MiniMessage tags and legacy color codes (e.g., gradients combined with `&f`). This resolves a critical `ParsingException` where MiniMessage would crash when encountering legacy formatting symbols like `§`.
- **FastStats Reliability**: Fixed a `NullPointerException` (NPE) that occurred during FastStats initialization if the database connection failed or was slow to respond.
- **Streamlined Initialization**: Simplified console output during startup by removing verbose database connection logs and retry spam. Initialization failures are now reported as a single, clear sentence before the plugin disables itself.
- **GUI Pagination Fix**: Resolved a critical indexing error in the `Reason Selector` GUI that prevented "Next Page" from working when reasons spanned multiple pages.
- **Robust Reason Selection**: Implemented `PersistentDataContainer` (PDC) to identify report reasons in the GUI. This makes reason selection immune to translation changes and fixes a bug where non-English reasons would fail to submit.
- **Smart Message Parsing**: Enhanced `MessageUtils` with a heuristic to detect legacy color codes versus MiniMessage tags. This fixes a rendering bug where command usage strings like `<player>` were being incorrectly parsed as broken MiniMessage.
- **Dynamic Prefix Replacement**: Re-engineered the `LanguageManager` to replace `{prefix}` _before_ MiniMessage parsing, preventing crashes when the prefix contains non-legacy characters.
- **Robust Category Handling**: Switched to `PersistentDataContainer` for identifying report categories in GUIs, ensuring stability across all localized versions.
- **Unified Feedback**: Standardized status update notifications so that using the `/setreportstatus` command and the GUI management buttons provide the same detailed feedback.
- **Improved Input Validation**:
  - Validated report categories and custom reasons directly in the `/report` command.
  - Fixed length validation for custom reasons to correctly use localized error messages.
- **Language Cleanup**: Removed redundant and unused keys from language files (`status-update-success`, `description-too-short`, etc.) to reduce clutter and improve maintainability.

## [1.0.7] - 2026-03-11

### New Features

- **Full Localization Support**: Implemented a comprehensive language system.
  - New `lang/` folder for storing language packs (e.g., `en_US.yml`).
  - Active language selection via `language` setting in `config.yml`.
  - Added default `en_US.yml` with all GUI and message strings.
- **Localized GUIs**: Refactored all user interfaces to be fully translatable.
  - `ReportManageGUI`: Localized all labels and status buttons.
  - `CategoryContainerGUI`: Localized navigation and indicators.
  - `ReportReasonContainerGUI`: Localized the entire reason selection flow.
  - `BookGUI`: Localized the classic book-based reporting and management views.
- **Localized Commands**: All command feedback and staff notifications are now localized via the language pack.
- **Dynamic Navigation**: Added "Back" buttons to all container-based GUIs.
  - New "Back to Categories" button in the category view for easier navigation between report groups.
  - New "Back to Category" button in the individual report management view, allowing staff to return quickly to the filtered list.
  - Back buttons use localized display names and lore, including dynamic status placeholders.
- **Official Language Support**: Expanded the built-in language library.
  - Added official support for **Italian (it_IT)** and **Slovak (sk_SK)**.
- **Integrated FastStats Telemetry**: Added advanced server metrics and automated error tracking via FastStats.
  - **Live Metrics**: We now track pending reports, preferred GUI types, and database backends to help improve future updates.
  - **Automated Error Tracking**: Plugin exceptions are now automatically transmitted with context to our developers, allowing us to fix bugs before you even report them.
- **Improved Telemetry Coexistence**: FastStats now runs alongside bStats, providing a more comprehensive overview of plugin usage and health.

### Improvements & Bug Fixes

- **Book GUI Smart Pagination**: Fixed a bug where the text in the Book GUI would overflow and get cut off at the bottom of the page. The plugin now intelligently flows content onto a new page only when necessary.
- **Language Extraction Fix**: Fixed an issue where bundled language files (`it_IT.yml`, `sk_SK.yml`) would not extract to the `lang/` folder unless explicitly set as the active language. Now all supported languages extract automatically on startup.
- **Improved Language Management**:
  - **Custom Language Support**: Custom language files will never be overwritten during plugin updates.
  - **Smart Fallback System**: Missing keys in custom language files automatically fall back to `en_US` defaults.
  - **Missing Key Warnings**: Console warnings are displayed when custom language files have missing fields.
  - **Automatic Internal Updates**: Supported languages are auto-updated when the plugin jar is updated.
- **GUI Interaction Fix**: Fixed a bug where both admins and players could accidentally take items out of the container-based GUIs.
- **Security & Privacy**:
  - Fixed a missing permission check in the Book GUI that allowed non-admin players to see "Click to change status" options.
  - Management buttons are now context-aware and only shown to players with the correct permissions.
- **Build Process Optimization**: Fixed a critical issue where the Gradle build process was corrupting `\n` newline characters in language files.
- **Enhanced GUI Stability**: Re-engineered the internal GUI identification system using `InventoryHolder`.
- **Refined Config Logic**: Updated internal configuration handling to support advanced telemetry metrics.
- **Enhanced Error Handling**: Improved the `ExceptionHandler` to provide better context for remote debugging.
- **Code Refactoring**: Centralized GUI opening logic and standardized the use of `DARK_OAK_DOOR` as the universal "Back" icon.

### Contributors

Special thanks to **clessidra** for the **Italian** translation and **FarmCraft** for the **Slovak** translation!

We are looking for translators to add even more language support to AevorinReports! Join our [Discord server](https://discord.gg/SV2dXt5SwF) and open a ticket if you'd like to help!

## [1.0.7-Beta-2] - 2026-03-10

### New Features

- **Integrated FastStats Telemetry**: Added advanced server metrics and automated error tracking via FastStats.
  - **Live Metrics**: We now track pending reports, preferred GUI types, and database backends to help improve future updates.
  - **Automated Error Tracking**: Plugin exceptions are now automatically transmitted with context to our developers, allowing us to fix bugs before you even report them.
- **Improved Telemetry Coexistence**: FastStats now runs alongside bStats, providing a more comprehensive overview of plugin usage and health.

### Improvements & Bug Fixes

- **Refined Config Logic**: Updated internal configuration handling to support advanced telemetry metrics.
- **Enhanced Error Handling**: Improved the `ExceptionHandler` to provide better context for remote debugging.

## [1.0.7-Beta-1] - 2026-03-08

> [!NOTE]
> This is a **Beta** version. While it is more stable than previous Alpha releases, the entire codebase has been extensively revised to support localization. As a result, **unexpected bugs may still appear**. It is recommended to test this version before deploying to critical production environments.

### New Features

- **Dynamic Navigation**: Added "Back" buttons to all container-based GUIs.
  - New "Back to Categories" button in the category view for easier navigation between report groups.
  - New "Back to Category" button in the individual report management view, allowing staff to return quickly to the filtered list.
  - Back buttons use localized display names and lore, including dynamic status placeholders (e.g., "Go back to PENDING Reports").
- **Official Language Support**: Expanded the built-in language library.
  - Added official support for **Italian (it_IT)** and **Slovak (sk_SK)**.
- **Improved Language Management**:
  - **Custom Language Support**: The plugin now intelligently handles user-created language files.
    - If you create a custom language configuration (any `.yml` file in the `lang/` folder not named after a supported internal language), the plugin will **not** modify or overwrite it during updates.
    - **Smart Fallback System**: If a custom language file is missing specific keys required for the GUIs or messages, the plugin will now automatically use the default `en_US` values as a fallback.
    - **Missing Key Warnings**: A warning will be displayed in the console if your custom configuration is missing fields, helping you identify what needs to be added.
  - **Automatic Internal Updates**: Supported languages (`en_US`, `it_IT`, `sk_SK`) are automatically updated when the plugin jar is updated, while preserving your `config.yml` settings.

### Improvements & Bug Fixes

- **GUI Interaction Fix**: Fixed a bug where both admins and players could accidentally take items (like report books or action icons) out of the container-based GUIs and place them in their own inventory.
- **Build Process Optimization**: Fixed a critical issue where the Gradle build process was corrupting `\n` newline characters in language files, which previously broke book-based GUIs.
- **Security & Privacy**:
  - Fixed a missing permission check in the Book GUI that allowed non-admin players to see "Click to change status" options when viewing their own reports.
  - Improved management button visibility: Navigation and management buttons are now context-aware and only show up for players with the correct permissions.
- **Code Refactoring**:
  - Centralized GUI opening logic in `CategoryContainerGUI` for better maintainability.
  - Standardized the use of `DARK_OAK_DOOR` as the universal "Back" icon across all menu systems.

### Contributors

Special thanks to **clessidra** for the **Italian** translation and **FarmCraft** for the **Slovak** translation!

We are looking for translators to add even more language support to AevorinReports in the future! If you are interested in contributing, the only requirement is that the language must be natively supported by Minecraft.

Join our [Discord server](https://discord.gg/SV2dXt5SwF) and open a ticket if you'd like to help with translations!

---

## [1.0.7-Alpha-1] - 2026-03-07

> [!WARNING]
> This is an **Alpha** version and is **not stable**. It is **not** recommended to install this on your Minecraft server unless you are a developer or a tester.

### New Features

- **Full Localization Support**: Implemented a comprehensive language system.
  - New `lang/` folder for storing language packs (e.g., `en_US.yml`).
  - Active language selection via `language` setting in `config.yml`.
  - Added default `en_US.yml` with all GUI and message strings.
- **Localized GUIs**: Refactored all user interfaces to be fully translatable.
  - `ReportManageGUI`: Localized all labels and status buttons.
  - `CategoryContainerGUI`: Localized navigation and indicators.
  - `ReportReasonContainerGUI`: Localized the entire reason selection flow.
  - `BookGUI`: Localized the classic book-based reporting and management views.
- **Localized Commands**: All command feedback and staff notifications are now localized via the language pack.

### Improvements & Bug Fixes

- **Enhanced GUI Stability**: Re-engineered the internal GUI identification system using `InventoryHolder`.
  - Resolved an issue where users could take items out of the chest GUI if they changed the language settings.
  - Eliminated a bug where multiple report messages were sent simultaneously due to duplicate event listener registrations.
  - Improved compatibility with custom language packs by removing hardcoded title and lore requirements for GUI logic.

---

## [1.0.6] - 2026-03-05

> **Developer's Note**: While this is a relatively small update focused on stability and internal improvements, we are already hard at work on a major upcoming update centered around **localization and language support**. Stay tuned!

### New Features

- **Update Channel Selection**: Added an option in `config.yml` to choose the update notification channel.
  - `all`: Notifies about ALL updates (alpha, beta, and stable releases).
  - `release`: Only notifies about stable releases.
  - `beta`: Notifies about beta releases.
  - `alpha`: Notifies about alpha releases.

### Bug Fixes & Stability

- **Improved Discord Robustness**: Resolved a "Guild does not have a self member" error by implementing explicit member caching and chunking policies.
- **Enhanced Discord Logging**: Added detailed error reporting for Discord message failures, including channel and guild identification to aid troubleshooting.
- **Safety Checks**: Implemented verification steps to ensure the bot's identity is fully loaded before attempting to send report notifications or log updates.
- **Fixed Update Checker**: Resolved a bug in the update checker that caused incorrect behaviour when fetching or comparing version information.
- **Fixed Discord Module Crash**: Resolved a bug in the Discord integration where a malformed `GUILD_CREATE` payload triggered a `ParsingException` in JDA.

### Configuration

- **Config Migration**: Incremented `config-version` to `3` to support new update checker settings and improvements.

### Contributors

Special thanks to **clessidra** for their help in identifying and testing various **Discord-related bugs**.

---

## [1.0.6-Beta-3] - 2026-03-04

### Bug Fixes & Stability

- **Improved Discord Robustness**: Resolved a "Guild does not have a self member" error by implementing explicit member caching and chunking policies.
- **Enhanced Discord Logging**: Added detailed error reporting for Discord message failures, including channel and guild identification to aid troubleshooting.
- **Safety Checks**: Implemented verification steps to ensure the bot's identity is fully loaded before attempting to send report notifications or log updates.

### Contributors

Special thanks to **clessidra** for identifying the Discord "self member" issue.

---

## [1.0.6-Beta-2] - 2026-03-03

### Bug Fixes

- **Fixed Update Checker**: Resolved a bug in the update checker that caused incorrect behaviour when fetching or comparing version information.
- **Fixed Discord Module Crash**: Resolved a bug in the Discord integration where a malformed `GUILD_CREATE` payload (forum channels with a `null` `available_tags` field) triggered a `ParsingException` in JDA, flooding the console with errors and causing the guild to become unavailable, effectively disabling the Discord module entirely.

### Contributors

Special thanks to **clessidra** for helping test and identify the Discord module crash.

---

## [1.0.6-Beta-1] - 2026-02-20

### New Features

- **Update Channel Selection**: Added an option in `config.yml` to choose between `all`, `alpha`, `beta`, and `release` update channels.
  - `all`: Notifies about ALL updates (alpha, beta, and stable).
  - `release`: Only notifies about stable releases.
  - `beta`: Notifies about beta releases.
  - `alpha`: Notifies about alpha releases.

### Configuration

- **Config Migration**: Incremented `config-version` to `2` to support new update checker settings.

## [1.0.5.1] - 2026-01-30

### Bug Fixes & Optimization

- **Fixed "Nag Author" Warnings**: Removed all usage of `System.out.println` and `printStackTrace` in favor of the official plugin logger. This ensures compliance with modern server software (Paper/Spigot) and cleans up console output.
- **Improved Database Logging**: Structured log messages for server renames, table schema updates, and database connectivity issues.
- **Code Audit**: Audited the entire codebase to ensure all component logs follow a standardized logging format.

## [1.0.5] - 2026-01-29

### Configuration & Persistence

- **Next-Gen Configuration Update System**: Re-engineered the entire configuration engine to support automated, comment-preserving updates. When a new plugin version (detected via `config-version`) requires configuration changes, the system now:
  - Creates a safe backup of your existing settings as `config.yml.old`.
  - Seamlessly migrates all user data into the updated structure while perfectly preserving all custom comments, layout, and section headers.
- **Fixed Critical Config Crashing**: Added resilient type-handling to prevent the plugin from shutting down when invalid data types (like numbers in string fields) are encountered in `config.yml`.
- **Improved YAML Aesthetics**: Standardized string quoting and formatting to ensure professional, valid YAML output across all platforms.

## [1.0.4.4] - 2026-01-25

### Bug Fixes & Improvements

- **Fixed Report Cooldown**: Resolved an issue where the report cooldown was not being enforced. Players must now wait the configured time between reports.
- **Fixed Active Report Limit**: Implemented the `max-active-reports` check to prevent players from exceeding the configured limit of pending reports.
- **Corrected Config Keys**: Fixed a bug where some configuration settings (like `cooldown` and `max-active-reports`) were not being read correctly from `config.yml`.
- **Improved Discord Error Handling**:
  - Added simplified, readable error messages for Discord bot initialization failures.
  - Added explicit database availability checks to prevent crashes if the database is disconnected.
  - Stack traces are now hidden by default and can be enabled via `debug.enabled` in `config.yml`.

### Security & Permissions

- **New Bypass Permissions**:
  - `aevorinreports.bypass.cooldown`: Allows specified players/ranks to bypass the reporting cooldown.
  - `aevorinreports.bypass.limit`: Allows specified players/ranks to bypass the active report limit.

## [1.0.4.3] - 2026-01-05

### Discord Network Mode

- **Network Mode for Multi-Server Setups**: Added a new `network-mode` configuration option for Discord integration.
  - Only ONE server in your network should enable `discord.network-mode.enabled: true`
  - This designated server will poll the database for new reports from ALL servers in the network via a reliable ID-based system.
  - Configurable poll interval (default: 10 seconds).
  - Perfect for networks where you want centralized Discord notifications without running the bot on every server.
- **Improved Reliability**: Switched to ID-based tracking to ensure no reports are duplicated or missed, even across server restarts.
- **Duplication Fix**: Resolved issues where reports on the bot-server could be sent twice.

### General & Security

- **Region Restriction**: Added a restriction to prevent the plugin from running on servers located in Israel (via IP geolocation and server timezone checks) with a #FreePalestine message.
- **Improved Database Logs**: Simplified database connection error messages to be cleaner and more readable.
- **Configuration Cleanup**: Removed unused security and encryption sections for a leaner setup.

## [1.0.4.2] - 2026-01-04

### General

- **Discord Integration**: Added a robust Discord integration with a dedicated bot.
- **Improved Database Logs**: Simplified database connection error messages to be cleaner and more readable (no more giant stack traces).
- **Configuration Cleanup**: Removed the unused "Security" section and encryption key logic for a leaner `config.yml`.
- **Folia Compatibility**: Ensured all new features, including the Discord bot's background tasks, are fully compatible with Folia servers.

### Discord Integration Features

- **Dynamic Bot Presence**: Configure the bot's status (Online, Idle, etc.) and activity (Watching, Playing, etc.) with support for the `%online_players%` placeholder.
- **Real-time Notifications**: Receive beautifully formatted embeds for new reports in your Discord channel.
- **Slash Commands**:
  - `/reports`: List all active pending reports.
  - `/lookup <id>`: View detailed information about a specific report.
  - `/resolve <id>`, `/reject <id>`, `/pending <id>`: Manage report statuses directly from Discord.
  - `/help`: Detailed help menu for the bot.
- **Smart Formatting**: The "Server" field in Discord embeds automatically hides itself if you are only running a single server, matching the in-game behavior.
- **Customizable Aesthetics**: Independent color settings for report alerts and lookup results in `config.yml`.

## [1.0.4.1] - 2025-12-20

- **Changed the Book Gui's Reason to be like this: "Reason: Hover to See"**
- **Fixed a bug where the Book Gui's Status colours were always red instead of their own independent colour**

## [1.0.4] - 2025-12-19

- **Added Folia Server Software Support**
- **Made the config.yml cleaner**
- **Removed Velocity Support**
- **Added an System for Network Servers**: All servers should run the plugin (proxy excluded) and connect to the same database to share and manage reports across the entire network.

## [1.0.3] - 2025-10-23

### New Features

- **Enhanced Report Command**: Added `/report <player> <reason>` for direct reporting.
- **Custom Reasons in GUI**: Added a "Custom Reason" option to the Book GUI. Selecting this closes the GUI and prompts the player to enter a reason in chat.
- **Container GUI**: The Container GUI interface is now fully functional.
- **bStats Support**: Added bStats integration for anonymous plugin metrics.
- **Player Report History**: Recent update allows regular players to view their own submitted reports via `/reports`.
- **MiniMessage Support**: Added support for MiniMessage formatting (e.g., `<red>`, `<gradient>`) in configuration and messages.
- **Update Checker**: Implemented an update checker using the Modrinth API.

### Changes & Improvements

- **Command Renaming**: Renamed `/shiftreport <id> <status>` to `/setreportstatus <id> to <status>` for better clarity.
- **Smart Permissions**: The `/viewreport` command now allows players to view their own reports without needing admin permissions.
- **GUI Experience**:
  - Improved Book GUI readability (Fixed invisible Report IDs).
  - Status change actions now close the inventory for a smoother workflow.
  - "Your Reports" view added for non-staff players.
- **Console Output**: Cleaned up console logs by removing emojis that were causing display issues on some terminals.
- **Notifications**: Updated staff notifications and success messages to use modern MiniMessage formatting and allow full customization in `config.yml`.
- **Codebase Optimization**: Comprehensive cleanup of imports, command registration, and error handling logic.

### Removed

- **Legacy Command**: Removed `/report-category <player> <reason>` as it has been replaced by the streamlined `/report` command.
