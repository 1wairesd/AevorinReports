package dev.aevorinstudios.aevorinReports.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.aevorinstudios.aevorinReports.reports.Report;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private HikariDataSource dataSource;
    @Getter
    private static DatabaseManager instance;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        instance = this;
        initializeDataSource(host, port, database, username, password);
        createTables();
    }

    public DatabaseManager(String filePath) {
        instance = this;
        initializeSQLiteDataSource(filePath);
        createTables();
    }

    public List<Report> getResolvedReportsBefore(LocalDateTime cutoff) {
        String sql = "SELECT * FROM reports WHERE status = 'RESOLVED' AND updated_at < ?";
        List<Report> reports = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch resolved reports", e);
        }

        return reports;
    }

    public List<Report> getRejectedReportsBefore(LocalDateTime cutoff) {
        String sql = "SELECT * FROM reports WHERE status = 'INVALID' AND updated_at < ?";
        List<Report> reports = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(cutoff));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rejected reports", e);
        }

        return reports;
    }

    public void deleteReport(Long id) {
        String sql = "DELETE FROM reports WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete report", e);
        }
    }

    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        String sql = "SELECT * FROM reports WHERE status = ?";
        List<Report> reports = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reports by status: " + status, e);
        }

        return reports;
    }

    public List<Report> getReportsByReporter(UUID reporterUuid) {
        String sql = "SELECT * FROM reports WHERE reporter_uuid = ? ORDER BY created_at DESC";
        List<Report> reports = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reporterUuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reports by reporter: " + reporterUuid, e);
        }

        return reports;
    }

    public void deleteOldReports(Report.ReportStatus status, LocalDateTime threshold) {
        String sql = "DELETE FROM reports WHERE status = ? AND updated_at < ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setTimestamp(2, Timestamp.valueOf(threshold));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete old reports with status: " + status, e);
        }
    }

    public List<Report> getActiveReports() {
        return getReportsByStatus(Report.ReportStatus.PENDING);
    }

    public int getReportCountByStatus(Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for status {}: {}", status, e.getMessage());
        }
        return 0;
    }

    public int getTotalReportsCount() {
        String sql = "SELECT COUNT(*) FROM reports";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to get total reports count: {}", e.getMessage());
        }
        return 0;
    }

    public int getReportCountByServer(String serverName) {
        String sql = "SELECT COUNT(*) FROM reports WHERE server_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serverName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for server {}: {}", serverName, e.getMessage());
        }
        return 0;
    }

    public int getReportCountByServerAndStatus(String serverName, Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE server_name = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serverName);
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for server {} and status {}: {}", serverName, status, e.getMessage());
        }
        return 0;
    }

    /**
     * Get the highest report ID currently in the database.
     * 
     * @return The maximum report ID, or 0 if no reports exist.
     */
    public long getMaxReportId() {
        String sql = "SELECT MAX(id) FROM reports";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get all reports with an ID higher than the specified ID (for network polling)
     * 
     * @param lastId The last ID that was processed
     * @return List of reports with IDs higher than lastId
     */
    public List<Report> getReportsAfterId(long lastId) {
        String sql = "SELECT * FROM reports WHERE id > ? ORDER BY id ASC";
        List<Report> reports = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, lastId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reports after ID", e);
        }

        return reports;
    }

    public Report getReport(long id) {
        String sql = "SELECT * FROM reports WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Report.builder()
                            .id(rs.getLong("id"))
                            .reporterUuid(UUID.fromString(rs.getString("reporter_uuid")))
                            .reportedUuid(UUID.fromString(rs.getString("reported_uuid")))
                            .reason(rs.getString("reason"))
                            .serverName(rs.getString("server_name"))
                            .status(Report.ReportStatus.valueOf(rs.getString("status")))
                            .isAnonymous(rs.getBoolean("is_anonymous"))
                            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                            .evidenceData(rs.getString("evidence_data"))
                            .coordinates(rs.getString("coordinates"))
                            .world(rs.getString("world"))
                            .build();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch report with id: " + id, e);
        }

        return null;
    }

    private void initializeSQLiteDataSource(String filePath) {
        try {
            // Create a database directory if it doesn't exist
            java.io.File dbFile = new java.io.File(filePath);
            java.io.File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                if (!dbDir.mkdirs()) {
                    throw new RuntimeException("Failed to create database directory: " + dbDir.getAbsolutePath());
                }
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + filePath);
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);

            // Test the connection
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    throw new SQLException("Failed to validate database connection");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database connection", e);
        }
    }

    private void initializeDataSource(String host, int port, String database, String username, String password) {
        int maxRetries = 3;
        int retryDelay = 5000; // 5 seconds
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host,
                        port, database));
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(5);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setConnectionTestQuery("SELECT 1");
                config.setValidationTimeout(5000);
                config.setInitializationFailTimeout(1);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useUnicode", "true");
                config.addDataSourceProperty("characterEncoding", "utf8");
                config.addDataSourceProperty("serverTimezone", "UTC");

                dataSource = new HikariDataSource(config);

                // Test the connection
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        throw new SQLException("Failed to validate database connection");
                    }
                }
                return; // Successfully connected
            } catch (SQLException e) {
                attempts++;
                if (attempts < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("Failed to initialize database connection pool after " + maxRetries + " attempts");
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            // Reports table
            String createReportsTable = dataSource.getJdbcUrl().contains("sqlite") ? """
                    CREATE TABLE IF NOT EXISTS reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        reporter_uuid VARCHAR(36) NOT NULL,
                        reported_uuid VARCHAR(36) NOT NULL,
                        reason TEXT NOT NULL,
                        server_name VARCHAR(64) NOT NULL,
                        status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                        is_anonymous BOOLEAN DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        evidence_data TEXT,
                        coordinates VARCHAR(64),
                        world VARCHAR(64)
                    )
                    """
                    : """
                            CREATE TABLE IF NOT EXISTS reports (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                reporter_uuid VARCHAR(36) NOT NULL,
                                reported_uuid VARCHAR(36) NOT NULL,
                                reason TEXT NOT NULL,
                                server_name VARCHAR(64) NOT NULL,
                                status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                                is_anonymous BOOLEAN DEFAULT 0
                            )
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(createReportsTable)) {
                stmt.executeUpdate();
            }

            // Report comments table
            String createCommentsTable = dataSource.getJdbcUrl().contains("sqlite") ? """
                    CREATE TABLE IF NOT EXISTS report_comments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id BIGINT NOT NULL,
                        staff_uuid VARCHAR(36) NOT NULL,
                        comment TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                    )
                    """
                    : """
                            CREATE TABLE IF NOT EXISTS report_comments (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                report_id BIGINT NOT NULL,
                                staff_uuid VARCHAR(36) NOT NULL,
                                comment TEXT NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                            )
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(createCommentsTable)) {
                stmt.executeUpdate();
            }

            // Report history table for audit logs
            String createHistoryTable = dataSource.getJdbcUrl().contains("sqlite") ? """
                    CREATE TABLE IF NOT EXISTS report_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id BIGINT NOT NULL,
                        staff_uuid VARCHAR(36) NOT NULL,
                        action VARCHAR(32) NOT NULL,
                        details TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                    )
                    """
                    : """
                            CREATE TABLE IF NOT EXISTS report_history (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                report_id BIGINT NOT NULL,
                                staff_uuid VARCHAR(36) NOT NULL,
                                action VARCHAR(32) NOT NULL,
                                details TEXT,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
                            )
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(createHistoryTable)) {
                stmt.executeUpdate();
            }

            // Server tokens table for proxy-server authentication
            String createTokensTable = dataSource.getJdbcUrl().contains("sqlite") ? """
                    CREATE TABLE IF NOT EXISTS server_tokens (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        server_name VARCHAR(64) UNIQUE NOT NULL,
                        token VARCHAR(128) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """
                    : """
                            CREATE TABLE IF NOT EXISTS server_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                server_name VARCHAR(64) UNIQUE NOT NULL,
                                token VARCHAR(128) NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                            )
                            """;

            try (PreparedStatement stmt = conn.prepareStatement(createTokensTable)) {
                stmt.executeUpdate();
            }

            // Ensure schema is up to date (add missing columns for older databases)
            ensureTableSchema(conn);

            createIndexes(conn);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createIndexes(Connection conn) {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports (reporter_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_reports_reported ON reports (reported_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_reports_status ON reports (status)",
            "CREATE INDEX IF NOT EXISTS idx_reports_server_status ON reports (server_name, status)",
            "CREATE INDEX IF NOT EXISTS idx_reports_updated_at ON reports (updated_at)"
        };
        for (String sql : indexes) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                // MySQL < 8.0 does not support IF NOT EXISTS for indexes; ignore duplicates
                logger.debug("Index creation skipped: {}", e.getMessage());
            }
        }
    }

    private void ensureTableSchema(Connection conn) {
        try {
            // Use ResultSetMetaData which is more reliable than DatabaseMetaData across
            // drivers
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM reports WHERE 1=0");
                    ResultSet rs = ps.executeQuery()) {

                java.sql.ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnName(i).toLowerCase());
                }

                boolean isSqlite = dataSource.getJdbcUrl().contains("sqlite");

                // Check and add missing columns with detailed logging
                if (!columns.contains("created_at")) {
                    logger.info("Column 'created_at' missing. Adding...");
                    // Use NULL for updated_at to avoid "only one TIMESTAMP with DEFAULT
                    // CURRENT_TIMESTAMP" error on older MySQL
                    addColumn(conn, "reports", "created_at", "TIMESTAMP NULL");
                }
                if (!columns.contains("updated_at")) {
                    logger.info("Column 'updated_at' missing. Adding...");
                    addColumn(conn, "reports", "updated_at", "TIMESTAMP NULL");
                }
                if (!columns.contains("evidence_data")) {
                    logger.info("Column 'evidence_data' missing. Adding...");
                    addColumn(conn, "reports", "evidence_data", "TEXT");
                }
                if (!columns.contains("coordinates")) {
                    logger.info("Column 'coordinates' missing. Adding...");
                    addColumn(conn, "reports", "coordinates", "VARCHAR(64)");
                }
                if (!columns.contains("world")) {
                    logger.info("Column 'world' missing. Adding...");
                    addColumn(conn, "reports", "world", "VARCHAR(64)");
                }
                if (!columns.contains("server_name")) {
                    logger.info("Column 'server_name' missing. Adding...");
                    addColumn(conn, "reports", "server_name", "VARCHAR(64) NOT NULL DEFAULT 'survival'");
                }
                if (!columns.contains("is_anonymous")) {
                    logger.info("Column 'is_anonymous' missing. Adding...");
                    addColumn(conn, "reports", "is_anonymous", isSqlite ? "BOOLEAN DEFAULT 0" : "BOOLEAN DEFAULT 0");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check/update table schema: {}", e.getMessage(), e);
        }
    }

    private void addColumn(Connection conn, String table, String column, String type) throws SQLException {
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            logger.info("Added missing column '{}' to table '{}'", column, table);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void updateReport(Report report) {
        String sql = "UPDATE reports SET reporter_uuid = ?, reported_uuid = ?, reason = ?, server_name = ?, status = ?, is_anonymous = ?, updated_at = ?, evidence_data = ?, coordinates = ?, world = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, report.getReporterUuid().toString());
            stmt.setString(2, report.getReportedUuid().toString());
            stmt.setString(3, report.getReason());
            stmt.setString(4, report.getServerName());
            stmt.setString(5, report.getStatus().name());
            stmt.setBoolean(6, report.isAnonymous());
            stmt.setTimestamp(7, Timestamp.valueOf(report.getUpdatedAt()));
            stmt.setString(8, report.getEvidenceData());
            stmt.setString(9, report.getCoordinates());
            stmt.setString(10, report.getWorld());
            stmt.setLong(11, report.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Failed to update report: Report not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update report", e);
        }
    }

    public void saveReport(Report report) {
        String sql = "INSERT INTO reports (reporter_uuid, reported_uuid, reason, server_name, status, is_anonymous, created_at, updated_at, evidence_data, coordinates, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, report.getReporterUuid().toString());
            stmt.setString(2, report.getReportedUuid().toString());
            stmt.setString(3, report.getReason());
            stmt.setString(4, report.getServerName());
            stmt.setString(5, report.getStatus().name());
            stmt.setBoolean(6, report.isAnonymous());
            stmt.setTimestamp(7, Timestamp.valueOf(report.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(report.getUpdatedAt()));
            stmt.setString(9, report.getEvidenceData());
            stmt.setString(10, report.getCoordinates());
            stmt.setString(11, report.getWorld());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    report.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save report", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Server Registration and Identification
    public void syncServerIdentity(String token, String currentServerName) {
        String query = "SELECT server_name FROM server_tokens WHERE token = ?";

        try (Connection conn = getConnection()) {
            // Check if this token already exists
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, token);
                try (ResultSet rs = stmt.executeQuery()) {
                    boolean tokenExists = rs.next();

                    if (tokenExists) {
                        String dbServerName = rs.getString("server_name");
                        if (!dbServerName.equals(currentServerName)) {
                            // Server name changed! Update it.
                            handleServerRename(conn, token, dbServerName, currentServerName);
                        }
                    } else {
                        // New server registration (Token not in DB)
                        // Should we check if server name exists?
                        // If users reinstalled plugin but kept DB, server name might be taken by OLD
                        // token
                        // We will allow duplicates or handle it? UNIQUE constraint on server_name
                        // prevents duplicates.
                        // So we must check if name is taken.

                        if (isServerNameTaken(conn, currentServerName)) {
                            // Name taken by another token.
                            // We have a new token (file) but name is old.
                            // We should probably adopt the OLD token? No, file is authority for "Unique
                            // Server Instance".
                            // BUT if valid valid approach: simple overwrite or error?
                            // User wants simplicity. Let's delete the old mapping for this name if it
                            // exists?
                            // No, that might break other server history.
                            // Simple: Just Insert. If fail, log warn.
                        }

                        String insert = "INSERT INTO server_tokens (server_name, token) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                            insertStmt.setString(1, currentServerName);
                            insertStmt.setString(2, token);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // If unique constraint violation on server_name (Token is new, but name exists)
            // It effectively means: This is a "new" server claiming an existing name.
            // In a pro system we'd block. Here we assume maybe they deleted the token file
            // but want to be "survival" again.
            // If so, we should UPDATE the old record with the NEW token?
            // "If they like delete the plugin configs n shit but database there they can
            // reconnect"
            // If they delete file, they get new token. They want to be 'survival'.
            // So we should UPDATE the token for 'survival' to the new one.
            if (e.getMessage().contains("UNIQUE") || e.getMessage().contains("unique")) {
                forceUpdateTokenForServer(currentServerName, token);
            } else {
                logger.error("Failed to sync server identity: {}", e.getMessage(), e);
            }
        }
    }

    private void handleServerRename(Connection conn, String token, String oldName, String newName) throws SQLException {
        // Update valid token mapping
        String updateToken = "UPDATE server_tokens SET server_name = ? WHERE token = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateToken)) {
            ps.setString(1, newName);
            ps.setString(2, token);
            ps.executeUpdate();
        }

        // Update all reports
        String updateReports = "UPDATE reports SET server_name = ? WHERE server_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateReports)) {
            ps.setString(1, newName);
            ps.setString(2, oldName);
            ps.executeUpdate();
        }

        logger.info("Server renamed from {} to {}. Historic reports updated.", oldName, newName);
    }

    private boolean isServerNameTaken(Connection conn, String serverName) throws SQLException {
        String q = "SELECT 1 FROM server_tokens WHERE server_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void forceUpdateTokenForServer(String serverName, String newToken) {
        try (Connection conn = getConnection()) {
            String update = "UPDATE server_tokens SET token = ? WHERE server_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(update)) {
                ps.setString(1, newToken);
                ps.setString(2, serverName);
                ps.executeUpdate();
            }
            logger.info("Re-registered server '{}' with new token.", serverName);
        } catch (SQLException ex) {
            logger.error("Failed to force update token for server {}: {}", serverName, ex.getMessage(), ex);
        }
    }

    public boolean hasMultipleServers() {
        String query = "SELECT COUNT(DISTINCT server_name) FROM server_tokens";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1) > 1;
            }
        } catch (SQLException e) {
            logger.error("Failed to check for multiple servers: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Get the total number of reports submitted by a player
     *
     * @param reporterUuid The UUID of the reporter
     * @return The number of reports submitted
     */
    public int getReportsCountByReporter(UUID reporterUuid) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reporterUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reporter {}: {}", reporterUuid, e.getMessage());
        }
        return 0;
    }

    /**
     * Get the number of reports submitted by a player with a specific status
     *
     * @param reporterUuid The UUID of the reporter
     * @param status The report status to count
     * @return The number of reports with the specified status
     */
    public int getReportsCountByReporterAndStatus(UUID reporterUuid, Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reporter_uuid = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reporterUuid.toString());
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reporter {} with status {}: {}", reporterUuid, status, e.getMessage());
        }
        return 0;
    }

    /**
     * Get the total number of reports against a player
     *
     * @param reportedUuid The UUID of the reported player
     * @return The number of reports against the player
     */
    public int getReportsCountByReported(UUID reportedUuid) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reported_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportedUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reported player {}: {}", reportedUuid, e.getMessage());
        }
        return 0;
    }

    /**
     * Get the number of reports against a player with a specific status
     *
     * @param reportedUuid The UUID of the reported player
     * @param status The report status to count
     * @return The number of reports with the specified status
     */
    public int getReportsCountByReportedAndStatus(UUID reportedUuid, Report.ReportStatus status) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reported_uuid = ? AND status = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reportedUuid.toString());
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get report count for reported player {} with status {}: {}", reportedUuid, status, e.getMessage());
        }
        return 0;
    }
}
