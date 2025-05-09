package wix3y.dungeonDesigner.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.datastructures.AmountWriteChecked;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final DungeonDesigner plugin;

    public DatabaseManager(DungeonDesigner plugin) {
        this.plugin = plugin;
    }

    /**
     * Connect to MySQL database
     *
     * @param ip the ip
     * @param port the port
     * @param database the database
     * @param username the username
     * @param password the password
     * @param poolSize the pool size
     */
    public void connect(String ip, String port, String database, String username, String password, int poolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + ip + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);

        dataSource = new HikariDataSource(config);

        plugin.getLogger().info("Database connection established.");
    }

    /**
     * Add any missing table or columns
     *
     * @param table the table name
     * @param columnNames list of quest column names
     * @param defaultValue the default value to set if the column does not exist
     */
    public void initialize(String table, List<String> columnNames, int defaultValue) {
        if (columnNames.isEmpty()) {
            plugin.getLogger().info("No dungeons found.");
            return;
        }

        StringBuilder columns = new StringBuilder();
        for (String columnName : columnNames) {
            columns.append(columnName).append(" INT DEFAULT " + defaultValue + ", ");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement sql = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS " + table + " (" +
                             "Player_UUID VARCHAR(36) PRIMARY KEY, " + columns.substring(0, columns.length() - 2) + ")")) {
            sql.execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing the " + table + " table!");
            e.printStackTrace();
        }

        addMissingColumns(table, columnNames, defaultValue);
    }

    /**
     * Add any missing table columns to database
     *
     * @param table the table name
     * @param columns list of columns to be added if they don't exist
     * @param defaultValue the default value to set
     */
    private void addMissingColumns(String table, List<String> columns, int defaultValue) {
        for (String column: columns) {
            try (Connection connection = dataSource.getConnection()) {
                // Check if the column exists in the table
                PreparedStatement checkColumnExistQuery = connection.prepareStatement(
                        "SELECT COUNT(*) " +
                                "FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE table_name = '" + table + "' " +
                                "AND column_name = ? "
                );
                checkColumnExistQuery.setString(1, column);
                ResultSet resultSet = checkColumnExistQuery.executeQuery();

                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    // The column doesn't exist, so add it
                    PreparedStatement addColumnQuery;
                    addColumnQuery = connection.prepareStatement(
                            "ALTER TABLE " + table + " ADD COLUMN " + column + " INT DEFAULT " + defaultValue);
                    addColumnQuery.executeUpdate();
                    plugin.getLogger().info("Column " + column + " added to table " + table + "!");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to check or add columns to table " + table + "!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Write integer to specific row in database column
     *
     * @param table the table name
     * @param uuid the player row identifier
     * @param column the column name
     * @param value the integer value
     */
    public void writeInt(String table, String uuid, String column, int value) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement updateValueQuery = connection.prepareStatement("INSERT INTO " + table +
                    " (Player_UUID, " + column + ") VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE " + column + " = ?");

            updateValueQuery.setString(1, uuid);
            updateValueQuery.setInt(2, value);
            updateValueQuery.setInt(3, value);
            updateValueQuery.executeUpdate();

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to write value " + value + " for player " + Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName() + " to column " + column + " in table " + table + "!");
            e.printStackTrace();
        }
    }

    /**
     * Get data belonging to player from MySQL database
     *
     * @param table the database table
     * @param columns list of columns of interest
     * @param uuid uuid of the player
     * @return the player data
     */
    public Map<String, AmountWriteChecked> getPlayerData(String table, String uuid, List<String> columns) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement getPlayerDataQuery = connection.prepareStatement(
                    "SELECT * FROM " + table + " WHERE Player_UUID = ?"
            );

            getPlayerDataQuery.setString(1, uuid);
            ResultSet rs = getPlayerDataQuery.executeQuery();

            if (!rs.next()) { // Check that player exists in database, if not add the player
                PreparedStatement insertPlayerQuery = connection.prepareStatement("INSERT INTO " + table + " (Player_UUID) VALUES (?)");
                insertPlayerQuery.setString(1, uuid);
                insertPlayerQuery.executeUpdate();
                rs = getPlayerDataQuery.executeQuery();
                if (!rs.next()) {
                    plugin.getLogger().severe("Failed to retrieve player data for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " from table " + table + " even after inserting!");
                    return null;
                }
            }

            Map<String, AmountWriteChecked> playerData = new ConcurrentHashMap<>();
            for (String column: columns) {
                try {
                    playerData.put(column, new AmountWriteChecked(rs.getInt(column), false));
                } catch (SQLException e) {
                    plugin.getLogger().severe("Failed to fetch data for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " from column " + column + "!");
                    e.printStackTrace();
                }
            }
            return playerData;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to fetch data for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " from table " + table + "!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Fetch all player data for specific player from specified table in MySQL database
     *
     * @param uuid the player's uuid
     * @param table the name of the table
     * @param database the name of the database
     * @return the player data
     */
    public List<String> getPlayerDataFromTabel(String uuid, String table, String database) {
        try (Connection connection = dataSource.getConnection()) {
            // Check if the table exists
            PreparedStatement checkTableExistQuery = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = ? AND table_name = ?"
            );
            checkTableExistQuery.setString(1, database);
            checkTableExistQuery.setString(2, table);
            ResultSet tableExistsRS = checkTableExistQuery.executeQuery();

            tableExistsRS.next();
            boolean tableExists = tableExistsRS.getInt(1) > 0;
            if (!tableExists) {
                return List.of("<red>Table " + table + " does not exist.");
            }

            // Check if the row exists in the table
            PreparedStatement checkRowExistQuery = connection.prepareStatement(
                    "SELECT * FROM " + table + " WHERE Player_UUID = ?"
            );
            checkRowExistQuery.setString(1, uuid);
            ResultSet rowExistsRS = checkRowExistQuery.executeQuery();

            boolean rowExists = rowExistsRS.next();
            if(!rowExists) {
                return List.of("<red>Player " + Bukkit.getOfflinePlayer(UUID.fromString(uuid)) + " does not exist in table " + table + ".");
            }

            List<String> playerData = new ArrayList<>();
            ResultSetMetaData meta = rowExistsRS.getMetaData();
            int columnCount = meta.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = meta.getColumnName(i);
                String value = String.valueOf(rowExistsRS.getObject(i));
                playerData.add("<reset><gray>" + columnName + ": <yellow>" + value);
            }
            return playerData;

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to fetch player data.");
            e.printStackTrace();
            return List.of("<red>Failed to fetch player data.");
        }
    }

    /**
     * Disconnect from database
     *
     */
    public void disconnect() {
        if (dataSource != null) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed.");
        }
    }
}