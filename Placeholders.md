# AevorinReports PlaceholderAPI Placeholders

All placeholders use the `reports` PlaceholderAPI identifier.

Example:

```text
%reports_total%
```

## Current-Viewer Player Stats

These placeholders use the player who is viewing or receiving the placeholder result. They are useful for holograms, scoreboards, tab lists, chat formats, and other per-player displays.

| Placeholder | Description |
| --- | --- |
| `%reports_submitted%` | Total reports submitted by the viewing player. |
| `%reports_pending_submitted%` | Pending reports submitted by the viewing player. |
| `%reports_resolved_submitted%` | Resolved reports submitted by the viewing player. |
| `%reports_valid_submitted%` | Valid reports submitted by the viewing player. Currently counts resolved reports. |
| `%reports_rejected_submitted%` | Rejected reports submitted by the viewing player. |
| `%reports_against%` | Total reports made against the viewing player. |
| `%reports_pending_against%` | Pending reports made against the viewing player. |
| `%reports_resolved_against%` | Resolved reports made against the viewing player. |
| `%reports_rejected_against%` | Rejected reports made against the viewing player. |

## Specific Player Stats

These placeholders query a named player instead of the viewer. Replace `<player>` with a player name.

The player can be online or offline, but offline players must already be cached by the server.

| Placeholder | Description |
| --- | --- |
| `%reports_submitted_by_<player>%` | Total reports submitted by the specified player. |
| `%reports_pending_submitted_by_<player>%` | Pending reports submitted by the specified player. |
| `%reports_resolved_submitted_by_<player>%` | Resolved reports submitted by the specified player. |
| `%reports_valid_submitted_by_<player>%` | Valid reports submitted by the specified player. Currently counts resolved reports. |
| `%reports_rejected_submitted_by_<player>%` | Rejected reports submitted by the specified player. |
| `%reports_against_<player>%` | Total reports made against the specified player. |
| `%reports_pending_against_<player>%` | Pending reports made against the specified player. |
| `%reports_resolved_against_<player>%` | Resolved reports made against the specified player. |
| `%reports_rejected_against_<player>%` | Rejected reports made against the specified player. |

Examples:

```text
%reports_submitted_by_Notch%
%reports_pending_against_Steve%
```

## Network-Wide Stats

These placeholders count reports across the whole reports database.

| Placeholder | Description |
| --- | --- |
| `%reports_total%` | Total number of reports in the system. |
| `%reports_total_pending%` | Total pending reports. |
| `%reports_total_resolved%` | Total resolved reports. |
| `%reports_total_rejected%` | Total rejected reports. |

## Specific Server Stats

These placeholders count reports for one configured server name. Replace `<server>` with the server name stored in `config.yml` under `server-name`.

These placeholders only return server-specific counts when AevorinReports is using MySQL and multiple servers are connected to the same database.

| Placeholder | Description |
| --- | --- |
| `%reports_total_on_<server>%` | Total reports created on the specified server. |
| `%reports_total_pending_on_<server>%` | Total pending reports on the specified server. |
| `%reports_total_resolved_on_<server>%` | Total resolved reports on the specified server. |
| `%reports_total_rejected_on_<server>%` | Total rejected reports on the specified server. |

Examples:

```text
%reports_total_on_survival%
%reports_total_pending_on_lobby%
```

## Notes

- PlaceholderAPI must be installed for these placeholders to work.
- Current-viewer placeholders need a player context. If the plugin requesting the placeholder does not provide a player, player-based placeholders may return nothing.
- Specific-player placeholders use player names, not UUIDs.
- `valid` currently means resolved, because AevorinReports does not have a separate valid status.
