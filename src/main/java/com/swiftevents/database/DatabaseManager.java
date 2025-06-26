package com.swiftevents.database;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseManager {
    
    private final SwiftEventsPlugin plugin;
    private BasicDataSource dataSource;
    private final Gson gson;
    private File jsonFolder;
    
    // Optimization: Dedicated thread pool for database operations
    private ExecutorService databaseExecutor;
    
    // Optimization: Batch operation support
    private static final int BATCH_SIZE = 100;
    
    public DatabaseManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        // Initialize dedicated thread pool for database operations
        this.databaseExecutor = Executors.newFixedThreadPool(4, new DatabaseThreadFactory());
    }
    
    // Custom thread factory for database operations
    private static class DatabaseThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SwiftEvents-DB-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
    
    public void initialize() {
        if (plugin.getConfigManager().isDatabaseEnabled()) {
            initializeDatabase();
        } else {
            initializeJsonStorage();
        }
    }
    
    private void initializeDatabase() {
        try {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&failOverReadOnly=false&maxReconnects=3",
                    plugin.getConfigManager().getDatabaseHost(),
                    plugin.getConfigManager().getDatabasePort(),
                    plugin.getConfigManager().getDatabaseName());
            
            dataSource.setUrl(url);
            dataSource.setUsername(plugin.getConfigManager().getDatabaseUsername());
            dataSource.setPassword(plugin.getConfigManager().getDatabasePassword());
            
            // Optimized connection pool settings
            dataSource.setInitialSize(2);
            dataSource.setMaxTotal(10);
            dataSource.setMaxIdle(5);
            dataSource.setMinIdle(2);
            dataSource.setMaxWaitMillis(30000);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(true);
            dataSource.setTestWhileIdle(true);
            dataSource.setTimeBetweenEvictionRunsMillis(30000);
            dataSource.setMinEvictableIdleTimeMillis(60000);
            dataSource.setRemoveAbandonedOnBorrow(true);
            dataSource.setRemoveAbandonedTimeout(300);
            dataSource.setLogAbandoned(false);
            
            // Create tables
            createTables();
            
            plugin.getLogger().info("MySQL database initialized successfully with optimized connection pool!");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL database: " + e.getMessage());
            if (plugin.getConfigManager().isDebugMode()) {
                e.printStackTrace();
            }
            plugin.getLogger().info("Falling back to JSON storage...");
            initializeJsonStorage();
        }
    }
    
    private void initializeJsonStorage() {
        jsonFolder = new File(plugin.getDataFolder(), plugin.getConfigManager().getJsonFolder());
        if (!jsonFolder.exists()) {
            jsonFolder.mkdirs();
        }
        plugin.getLogger().info("JSON storage initialized successfully!");
    }
    
    // Optimized table creation with proper indexes
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // Events table with optimized schema
            String createEventsTable = """
                CREATE TABLE IF NOT EXISTS events (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    max_participants INT,
                    current_participants INT DEFAULT 0,
                    start_time TIMESTAMP NULL,
                    end_time TIMESTAMP NULL,
                    created_by VARCHAR(36),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    world VARCHAR(255),
                    x DOUBLE,
                    y DOUBLE,
                    z DOUBLE,
                    rewards TEXT,
                    requirements TEXT,
                    metadata TEXT,
                    INDEX idx_status (status),
                    INDEX idx_type (type),
                    INDEX idx_created_by (created_by),
                    INDEX idx_start_time (start_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
            
            // Event participants table with optimized indexes
            String createParticipantsTable = """
                CREATE TABLE IF NOT EXISTS event_participants (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    event_id VARCHAR(36) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(255) NOT NULL,
                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_participation (event_id, player_uuid),
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_event_id (event_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createEventsTable);
                stmt.execute(createParticipantsTable);
            }
        }
    }
    
    // Event CRUD operations with improved async handling
    public CompletableFuture<Boolean> saveEvent(Event event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return saveEventToDatabase(event);
                } else {
                    return saveEventToJson(event);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error saving event " + event.getId() + ": " + e.getMessage());
                return false;
            }
        }, databaseExecutor);
    }
    
    // Batch save operation for better performance
    public CompletableFuture<Boolean> saveEvents(Collection<Event> events) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return saveEventsBatchToDatabase(events);
                } else {
                    return saveEventsBatchToJson(events);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error saving events batch: " + e.getMessage());
                return false;
            }
        }, databaseExecutor);
    }
    
    public CompletableFuture<Event> loadEvent(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return loadEventFromDatabase(eventId);
                } else {
                    return loadEventFromJson(eventId);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading event " + eventId + ": " + e.getMessage());
                return null;
            }
        }, databaseExecutor);
    }
    
    public CompletableFuture<List<Event>> loadAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return loadAllEventsFromDatabase();
                } else {
                    return loadAllEventsFromJson();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading all events: " + e.getMessage());
                return new ArrayList<>();
            }
        }, databaseExecutor);
    }
    
    public CompletableFuture<Boolean> deleteEvent(String eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return deleteEventFromDatabase(eventId);
                } else {
                    return deleteEventFromJson(eventId);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error deleting event " + eventId + ": " + e.getMessage());
                return false;
            }
        }, databaseExecutor);
    }
    
    // Optimized batch save to database
    private boolean saveEventsBatchToDatabase(Collection<Event> events) {
        if (events.isEmpty()) return true;
        
        String sql = """
            INSERT INTO events (id, name, description, type, status, max_participants, 
                              current_participants, start_time, end_time, created_by, 
                              world, x, y, z, rewards, requirements, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                type = VALUES(type),
                status = VALUES(status),
                max_participants = VALUES(max_participants),
                current_participants = VALUES(current_participants),
                start_time = VALUES(start_time),
                end_time = VALUES(end_time),
                world = VALUES(world),
                x = VALUES(x),
                y = VALUES(y),
                z = VALUES(z),
                rewards = VALUES(rewards),
                requirements = VALUES(requirements),
                metadata = VALUES(metadata)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            int batchCount = 0;
            for (Event event : events) {
                setEventParameters(stmt, event);
                stmt.addBatch();
                batchCount++;
                
                if (batchCount % BATCH_SIZE == 0) {
                    stmt.executeBatch();
                    conn.commit();
                }
            }
            
            // Execute remaining batch
            if (batchCount % BATCH_SIZE != 0) {
                stmt.executeBatch();
                conn.commit();
            }
            
            conn.setAutoCommit(true);
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving events batch to database: " + e.getMessage());
            return false;
        }
    }
    
    // Helper method to set event parameters
    private void setEventParameters(PreparedStatement stmt, Event event) throws SQLException {
        stmt.setString(1, event.getId());
        stmt.setString(2, event.getName());
        stmt.setString(3, event.getDescription());
        stmt.setString(4, event.getType().name());
        stmt.setString(5, event.getStatus().name());
        stmt.setInt(6, event.getMaxParticipants());
        stmt.setInt(7, event.getCurrentParticipants());
        stmt.setTimestamp(8, event.getStartTime() != null ? new Timestamp(event.getStartTime()) : null);
        stmt.setTimestamp(9, event.getEndTime() != null ? new Timestamp(event.getEndTime()) : null);
        stmt.setString(10, event.getCreatedBy() != null ? event.getCreatedBy().toString() : null);
        stmt.setString(11, event.getWorld());
        stmt.setDouble(12, event.getX());
        stmt.setDouble(13, event.getY());
        stmt.setDouble(14, event.getZ());
        stmt.setString(15, gson.toJson(event.getRewards()));
        stmt.setString(16, gson.toJson(event.getRequirements()));
        stmt.setString(17, gson.toJson(event.getMetadata()));
    }
    
    // JSON batch operations
    private boolean saveEventsBatchToJson(Collection<Event> events) {
        boolean success = true;
        for (Event event : events) {
            if (!saveEventToJson(event)) {
                success = false;
            }
        }
        return success;
    }
    
    // Database implementation
    private boolean saveEventToDatabase(Event event) {
        String sql = """
            INSERT INTO events (id, name, description, type, status, max_participants, 
                              current_participants, start_time, end_time, created_by, 
                              world, x, y, z, rewards, requirements, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                type = VALUES(type),
                status = VALUES(status),
                max_participants = VALUES(max_participants),
                current_participants = VALUES(current_participants),
                start_time = VALUES(start_time),
                end_time = VALUES(end_time),
                world = VALUES(world),
                x = VALUES(x),
                y = VALUES(y),
                z = VALUES(z),
                rewards = VALUES(rewards),
                requirements = VALUES(requirements),
                metadata = VALUES(metadata)
        """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, event.getId());
            stmt.setString(2, event.getName());
            stmt.setString(3, event.getDescription());
            stmt.setString(4, event.getType().name());
            stmt.setString(5, event.getStatus().name());
            stmt.setInt(6, event.getMaxParticipants());
            stmt.setInt(7, event.getCurrentParticipants());
            stmt.setTimestamp(8, event.getStartTime() != null ? new Timestamp(event.getStartTime()) : null);
            stmt.setTimestamp(9, event.getEndTime() != null ? new Timestamp(event.getEndTime()) : null);
            stmt.setString(10, event.getCreatedBy() != null ? event.getCreatedBy().toString() : null);
            stmt.setString(11, event.getWorld());
            stmt.setDouble(12, event.getX());
            stmt.setDouble(13, event.getY());
            stmt.setDouble(14, event.getZ());
            stmt.setString(15, gson.toJson(event.getRewards()));
            stmt.setString(16, gson.toJson(event.getRequirements()));
            stmt.setString(17, gson.toJson(event.getMetadata()));
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save event to database: " + e.getMessage());
            return false;
        }
    }
    
    private Event loadEventFromDatabase(String eventId) {
        String sql = "SELECT * FROM events WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, eventId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return createEventFromResultSet(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load event from database: " + e.getMessage());
        }
        
        return null;
    }
    
    private List<Event> loadAllEventsFromDatabase() {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM events";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                events.add(createEventFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load events from database: " + e.getMessage());
        }
        
        return events;
    }
    
    private boolean deleteEventFromDatabase(String eventId) {
        String sql = "DELETE FROM events WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete event from database: " + e.getMessage());
            return false;
        }
    }
    
    private Event createEventFromResultSet(ResultSet rs) throws SQLException {
        Event event = new Event(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                Event.EventType.valueOf(rs.getString("type"))
        );
        
        event.setStatus(Event.EventStatus.valueOf(rs.getString("status")));
        event.setMaxParticipants(rs.getInt("max_participants"));
        event.setCurrentParticipants(rs.getInt("current_participants"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            event.setStartTime(startTime.getTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            event.setEndTime(endTime.getTime());
        }
        
        String createdBy = rs.getString("created_by");
        if (createdBy != null) {
            event.setCreatedBy(UUID.fromString(createdBy));
        }
        
        event.setLocation(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
        
        // Parse JSON fields
        Type listType = new TypeToken<List<String>>(){}.getType();
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        
        String rewardsJson = rs.getString("rewards");
        if (rewardsJson != null) {
            event.setRewards(gson.fromJson(rewardsJson, listType));
        }
        
        String requirementsJson = rs.getString("requirements");
        if (requirementsJson != null) {
            event.setRequirements(gson.fromJson(requirementsJson, mapType));
        }
        
        String metadataJson = rs.getString("metadata");
        if (metadataJson != null) {
            event.setMetadata(gson.fromJson(metadataJson, mapType));
        }
        
        return event;
    }
    
    // JSON implementation
    private boolean saveEventToJson(Event event) {
        File eventFile = new File(jsonFolder, event.getId() + ".json");
        try (FileWriter writer = new FileWriter(eventFile)) {
            gson.toJson(event, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save event to JSON: " + e.getMessage());
            return false;
        }
    }
    
    private Event loadEventFromJson(String eventId) {
        File eventFile = new File(jsonFolder, eventId + ".json");
        if (!eventFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(eventFile)) {
            return gson.fromJson(reader, Event.class);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load event from JSON: " + e.getMessage());
            return null;
        }
    }
    
    private List<Event> loadAllEventsFromJson() {
        List<Event> events = new ArrayList<>();
        File[] eventFiles = jsonFolder.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (eventFiles != null) {
            for (File file : eventFiles) {
                try (FileReader reader = new FileReader(file)) {
                    Event event = gson.fromJson(reader, Event.class);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to load event file: " + file.getName());
                }
            }
        }
        
        return events;
    }
    
    private boolean deleteEventFromJson(String eventId) {
        File eventFile = new File(jsonFolder, eventId + ".json");
        return eventFile.exists() && eventFile.delete();
    }
    
    public void closeConnections() {
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (dataSource != null) {
            try {
                dataSource.close();
                plugin.getLogger().info("Database connections closed successfully");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connections: " + e.getMessage());
            }
        }
    }
    
    /**
     * Validates database connection health
     * @return true if connection is healthy
     */
    public boolean isConnectionHealthy() {
        if (!plugin.getConfigManager().isDatabaseEnabled() || dataSource == null) {
            return true; // JSON mode is always "healthy"
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            plugin.getLogger().warning("Database connection health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Attempts to reconnect to database if connection is lost
     */
    public void attemptReconnection() {
        if (!plugin.getConfigManager().isDatabaseEnabled()) {
            return;
        }
        
        plugin.getLogger().info("Attempting database reconnection...");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
            initializeDatabase();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reconnect to database: " + e.getMessage());
            plugin.getLogger().info("Falling back to JSON storage...");
            initializeJsonStorage();
        }
    }
} 