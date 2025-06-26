package com.swiftevents.database;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.events.Event;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.dbcp2.BasicDataSource;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
    
    // Optimization: Dedicated thread pool for database operations with proper sizing
    private ExecutorService databaseExecutor;
    
    // Optimization: Connection and performance settings
    private static final int BATCH_SIZE = 50; // Reduced from 100 for better memory usage
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int STATEMENT_TIMEOUT = 15;
    private static final int MAX_POOL_SIZE = 8; // Reduced for memory efficiency
    
    // Optimization: Pre-allocated buffers for batch operations
    private static final ThreadLocal<List<Event>> BATCH_BUFFER = 
        ThreadLocal.withInitial(() -> new ArrayList<>(BATCH_SIZE));
    
    // Optimization: String builders for SQL generation
    private static final ThreadLocal<StringBuilder> SQL_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(256));
    
    // Optimization: Reusable type reference for JSON operations
    private static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>(){}.getType();
    
    // SQL queries as constants to avoid repeated string creation
    private static final String INSERT_EVENT_SQL = """
        INSERT INTO events (id, name, description, type, status, max_participants, 
                           current_participants, start_time, end_time, created_by, 
                           created_at, world, x, y, z, rewards, requirements, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        name=VALUES(name), description=VALUES(description), status=VALUES(status),
        max_participants=VALUES(max_participants), current_participants=VALUES(current_participants),
        start_time=VALUES(start_time), end_time=VALUES(end_time),
        world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z),
        rewards=VALUES(rewards), requirements=VALUES(requirements), metadata=VALUES(metadata)
        """;
    
    private static final String SELECT_EVENT_SQL = """
        SELECT id, name, description, type, status, max_participants, current_participants,
               start_time, end_time, created_by, created_at, world, x, y, z,
               rewards, requirements, metadata
        FROM events WHERE id = ?
        """;
    
    private static final String SELECT_ALL_EVENTS_SQL = """
        SELECT id, name, description, type, status, max_participants, current_participants,
               start_time, end_time, created_by, created_at, world, x, y, z,
               rewards, requirements, metadata
        FROM events ORDER BY created_at DESC
        """;
    
    private static final String DELETE_EVENT_SQL = "DELETE FROM events WHERE id = ?";
    
    public DatabaseManager(SwiftEventsPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        
        // Initialize dedicated thread pool for database operations
        int threadCount = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        this.databaseExecutor = Executors.newFixedThreadPool(threadCount, new DatabaseThreadFactory());
    }
    
    // Custom thread factory for database operations with proper naming and daemon status
    private static class DatabaseThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SwiftEvents-DB-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
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
            
            StringBuilder urlBuilder = SQL_BUILDER.get();
            urlBuilder.setLength(0);
            urlBuilder.append("jdbc:mysql://")
                     .append(plugin.getConfigManager().getDatabaseHost())
                     .append(":").append(plugin.getConfigManager().getDatabasePort())
                     .append("/").append(plugin.getConfigManager().getDatabaseName())
                     .append("?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true")
                     .append("&characterEncoding=UTF-8&autoReconnect=true&failOverReadOnly=false")
                     .append("&maxReconnects=3&serverTimezone=UTC");
            
            dataSource.setUrl(urlBuilder.toString());
            dataSource.setUsername(plugin.getConfigManager().getDatabaseUsername());
            dataSource.setPassword(plugin.getConfigManager().getDatabasePassword());
            
            // Optimized connection pool settings for memory efficiency
            dataSource.setInitialSize(1);
            dataSource.setMaxTotal(MAX_POOL_SIZE);
            dataSource.setMaxIdle(3);
            dataSource.setMinIdle(1);
            dataSource.setMaxWaitMillis(CONNECTION_TIMEOUT);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestOnBorrow(true);
            dataSource.setTestWhileIdle(true);
            dataSource.setTimeBetweenEvictionRunsMillis(60000); // 1 minute
            dataSource.setMinEvictableIdleTimeMillis(300000); // 5 minutes
            dataSource.setRemoveAbandonedOnBorrow(true);
            dataSource.setRemoveAbandonedTimeout(180); // 3 minutes
            dataSource.setLogAbandoned(false); // Disable for performance
            
            // Additional optimizations
            dataSource.setDefaultAutoCommit(true);
            dataSource.setDefaultReadOnly(false);
            dataSource.setDefaultQueryTimeout(STATEMENT_TIMEOUT);
            
            // Create tables
            createTables();
            
            plugin.getLogger().info("MySQL database initialized with optimized connection pool (" + MAX_POOL_SIZE + " max connections)");
            
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
            boolean created = jsonFolder.mkdirs();
            if (!created) {
                plugin.getLogger().warning("Failed to create JSON storage directory");
            }
        }
        plugin.getLogger().info("JSON storage initialized at: " + jsonFolder.getPath());
    }
    
    // Optimized table creation with minimal memory usage
    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Events table with optimized schema and indexes
            String createEventsTable = """
                CREATE TABLE IF NOT EXISTS events (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    type VARCHAR(50) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    max_participants INT,
                    current_participants INT DEFAULT 0,
                    start_time BIGINT NULL,
                    end_time BIGINT NULL,
                    created_by VARCHAR(36),
                    created_at BIGINT NOT NULL,
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
                    INDEX idx_created_at (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            stmt.execute(createEventsTable);
            
            // Participants table for normalization and better performance
            String createParticipantsTable = """
                CREATE TABLE IF NOT EXISTS event_participants (
                    event_id VARCHAR(36) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    joined_at BIGINT NOT NULL,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    PRIMARY KEY (event_id, player_uuid),
                    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_joined_at (joined_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            stmt.execute(createParticipantsTable);
        }
    }
    
    // Event CRUD operations with improved async handling and batching
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
                if (plugin.getConfigManager().isDebugMode()) {
                    e.printStackTrace();
                }
                return false;
            }
        }, databaseExecutor);
    }
    
    // Optimized batch save operation with better memory management
    public CompletableFuture<Boolean> saveEvents(Collection<Event> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (plugin.getConfigManager().isDatabaseEnabled() && dataSource != null) {
                    return saveEventsBatchToDatabase(events);
                } else {
                    return saveEventsBatchToJson(events);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error saving events batch: " + e.getMessage());
                if (plugin.getConfigManager().isDebugMode()) {
                    e.printStackTrace();
                }
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
    
    // Database operations with optimized memory usage
    private boolean saveEventsBatchToDatabase(Collection<Event> events) {
        if (events.isEmpty()) return true;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            // Process in batches to avoid memory issues
            List<Event> batch = BATCH_BUFFER.get();
            batch.clear();
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
                stmt.setQueryTimeout(STATEMENT_TIMEOUT);
                
                int count = 0;
                for (Event event : events) {
                    setEventParameters(stmt, event);
                    stmt.addBatch();
                    count++;
                    
                    if (count % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                    }
                }
                
                // Execute remaining batch
                if (count % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                }
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to batch save events to database: " + e.getMessage());
            return false;
        }
    }
    
    private void setEventParameters(PreparedStatement stmt, Event event) throws SQLException {
        stmt.setString(1, event.getId());
        stmt.setString(2, event.getName());
        stmt.setString(3, event.getDescription());
        stmt.setString(4, event.getType().name());
        stmt.setString(5, event.getStatus().name());
        stmt.setInt(6, event.getMaxParticipants());
        stmt.setInt(7, event.getCurrentParticipants());
        
        // Handle nullable timestamps
        if (event.getStartTime() > 0) {
            stmt.setLong(8, event.getStartTime());
        } else {
            stmt.setNull(8, Types.BIGINT);
        }
        
        if (event.getEndTime() > 0) {
            stmt.setLong(9, event.getEndTime());
        } else {
            stmt.setNull(9, Types.BIGINT);
        }
        
        if (event.getCreatedBy() != null) {
            stmt.setString(10, event.getCreatedBy().toString());
        } else {
            stmt.setNull(10, Types.VARCHAR);
        }
        
        stmt.setLong(11, event.getCreatedAt());
        
        // Location data
        if (event.hasLocation()) {
            stmt.setString(12, event.getWorld());
            stmt.setDouble(13, event.getX());
            stmt.setDouble(14, event.getY());
            stmt.setDouble(15, event.getZ());
        } else {
            stmt.setNull(12, Types.VARCHAR);
            stmt.setNull(13, Types.DOUBLE);
            stmt.setNull(14, Types.DOUBLE);
            stmt.setNull(15, Types.DOUBLE);
        }
        
        // JSON fields - minimize serialization overhead
        stmt.setString(16, event.getRewards().isEmpty() ? null : gson.toJson(event.getRewards()));
        stmt.setString(17, event.getRequirements().isEmpty() ? null : gson.toJson(event.getRequirements()));
        stmt.setString(18, event.getMetadata().isEmpty() ? null : gson.toJson(event.getMetadata()));
    }
    
    private boolean saveEventsBatchToJson(Collection<Event> events) {
        for (Event event : events) {
            if (!saveEventToJson(event)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean saveEventToDatabase(Event event) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_EVENT_SQL)) {
            
            stmt.setQueryTimeout(STATEMENT_TIMEOUT);
            setEventParameters(stmt, event);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save event to database: " + e.getMessage());
            return false;
        }
    }
    
    private Event loadEventFromDatabase(String eventId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_EVENT_SQL)) {
            
            stmt.setQueryTimeout(STATEMENT_TIMEOUT);
            stmt.setString(1, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return createEventFromResultSet(rs);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load event from database: " + e.getMessage());
        }
        
        return null;
    }
    
    private List<Event> loadAllEventsFromDatabase() {
        List<Event> events = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_EVENTS_SQL)) {
            
            stmt.setQueryTimeout(STATEMENT_TIMEOUT * 2); // Longer timeout for bulk operations
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        Event event = createEventFromResultSet(rs);
                        if (event != null) {
                            events.add(event);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error parsing event from database: " + e.getMessage());
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load events from database: " + e.getMessage());
        }
        
        return events;
    }
    
    private boolean deleteEventFromDatabase(String eventId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_EVENT_SQL)) {
            
            stmt.setQueryTimeout(STATEMENT_TIMEOUT);
            stmt.setString(1, eventId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete event from database: " + e.getMessage());
            return false;
        }
    }
    
    // Optimized ResultSet parsing with reduced object allocation
    private Event createEventFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        Event.EventType type = Event.EventType.valueOf(rs.getString("type"));
        
        Event event = new Event(id, name, description, type);
        
        event.setStatus(Event.EventStatus.valueOf(rs.getString("status")));
        event.setMaxParticipants(rs.getInt("max_participants"));
        event.setCurrentParticipants(rs.getInt("current_participants"));
        
        // Handle nullable timestamps
        long startTime = rs.getLong("start_time");
        if (!rs.wasNull()) {
            event.setStartTime(startTime);
        }
        
        long endTime = rs.getLong("end_time");
        if (!rs.wasNull()) {
            event.setEndTime(endTime);
        }
        
        String createdByStr = rs.getString("created_by");
        if (createdByStr != null) {
            event.setCreatedBy(UUID.fromString(createdByStr));
        }
        
        // Created at timestamp is handled internally by the Event class
        
        // Location data
        String world = rs.getString("world");
        if (world != null) {
            event.setLocation(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
        }
        
        // JSON fields with null checking
        String rewardsJson = rs.getString("rewards");
        if (rewardsJson != null && !rewardsJson.trim().isEmpty()) {
            List<String> rewards = gson.fromJson(rewardsJson, new TypeToken<List<String>>(){}.getType());
            event.setRewards(rewards);
        }
        
        String requirementsJson = rs.getString("requirements");
        if (requirementsJson != null && !requirementsJson.trim().isEmpty()) {
            Map<String, Object> requirements = gson.fromJson(requirementsJson, new TypeToken<Map<String, Object>>(){}.getType());
            event.setRequirements(requirements);
        }
        
        String metadataJson = rs.getString("metadata");
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            Map<String, Object> metadata = gson.fromJson(metadataJson, new TypeToken<Map<String, Object>>(){}.getType());
            event.setMetadata(metadata);
        }
        
        return event;
    }
    
    // JSON operations with optimized file handling
    private boolean saveEventToJson(Event event) {
        File eventFile = new File(jsonFolder, event.getId() + ".json");
        
        try (FileWriter writer = new FileWriter(eventFile, StandardCharsets.UTF_8)) {
            gson.toJson(event, writer);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save event to JSON: " + e.getMessage());
            return false;
        }
    }
    
    private Event loadEventFromJson(String eventId) {
        File eventFile = new File(jsonFolder, eventId + ".json");
        
        if (!eventFile.exists()) {
            return null;
        }
        
        try (FileReader reader = new FileReader(eventFile, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Event.class);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load event from JSON: " + e.getMessage());
            return null;
        }
    }
    
    private List<Event> loadAllEventsFromJson() {
        List<Event> events = new ArrayList<>();
        
        if (!jsonFolder.exists()) {
            return events;
        }
        
        File[] eventFiles = jsonFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (eventFiles == null) {
            return events;
        }
        
        for (File eventFile : eventFiles) {
            try (FileReader reader = new FileReader(eventFile, StandardCharsets.UTF_8)) {
                Event event = gson.fromJson(reader, Event.class);
                if (event != null) {
                    events.add(event);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to load event from JSON file " + eventFile.getName() + ": " + e.getMessage());
            }
        }
        
        return events;
    }
    
    private boolean deleteEventFromJson(String eventId) {
        File eventFile = new File(jsonFolder, eventId + ".json");
        return eventFile.exists() && eventFile.delete();
    }
    
    public void closeConnections() {
        try {
            if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
                databaseExecutor.shutdown();
                try {
                    if (!databaseExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        databaseExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    databaseExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            if (dataSource != null) {
                dataSource.close();
                plugin.getLogger().info("Database connections closed successfully");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database connections: " + e.getMessage());
        }
    }
    
    public boolean isConnectionHealthy() {
        if (!plugin.getConfigManager().isDatabaseEnabled() || dataSource == null) {
            return true; // JSON mode is always "healthy"
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            return false;
        }
    }
    
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
            
            if (isConnectionHealthy()) {
                plugin.getLogger().info("Database reconnection successful!");
            } else {
                plugin.getLogger().warning("Database reconnection failed - health check failed");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Database reconnection failed: " + e.getMessage());
        }
    }
} 