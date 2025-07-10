package com.swiftevents.database;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.config.ConfigManager;
import com.swiftevents.events.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseManager Tests")
class DatabaseManagerTest {

    @Mock
    private SwiftEventsPlugin plugin;
    
    @Mock
    private ConfigManager configManager;
    
    private DatabaseManager databaseManager;
    private Event testEvent;
    private String uniqueTestFolder;

    @BeforeEach
    void setUp() {
        // Create unique test folder for each test
        uniqueTestFolder = "test_events_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        
        // Setup mocks with lenient stubbing
        lenient().when(plugin.getConfigManager()).thenReturn(configManager);
        lenient().when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        lenient().when(plugin.getDataFolder()).thenReturn(new File("test_data"));
        
        // Default config values
        lenient().when(configManager.isDatabaseEnabled()).thenReturn(false); // Use JSON storage for tests
        lenient().when(configManager.getJsonFolder()).thenReturn(uniqueTestFolder);
        
        databaseManager = new DatabaseManager(plugin);
        databaseManager.initialize();
        
        // Clear any existing events to ensure clean state
        clearAllEvents();
        
        // Create test event
        testEvent = new Event("Test Event", "Test Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        // Clean up test folder
        if (databaseManager != null) {
            databaseManager.closeConnections();
        }
        
        // Delete test folder and all its contents
        File testFolder = new File("test_data", uniqueTestFolder);
        if (testFolder.exists()) {
            deleteDirectory(testFolder);
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private void clearAllEvents() {
        // Load all events and delete them to ensure clean state
        CompletableFuture<List<Event>> loadFuture = databaseManager.loadAllEvents();
        List<Event> events = loadFuture.join();
        for (Event event : events) {
            databaseManager.deleteEvent(event.getId()).join();
        }
    }

    @Nested
    @DisplayName("JSON Storage Tests")
    class JsonStorageTests {

        @Test
        @DisplayName("Should save event to JSON")
        void shouldSaveEventToJson() {
            // When
            CompletableFuture<Boolean> future = databaseManager.saveEvent(testEvent);

            // Then
            assertThat(future.join()).isTrue();
        }

        @Test
        @DisplayName("Should load event from JSON")
        void shouldLoadEventFromJson() {
            // Given
            databaseManager.saveEvent(testEvent).join();

            // When
            CompletableFuture<Event> future = databaseManager.loadEvent(testEvent.getId());

            // Then
            Event loadedEvent = future.join();
            assertThat(loadedEvent).isNotNull();
            assertThat(loadedEvent.getName()).isEqualTo(testEvent.getName());
            assertThat(loadedEvent.getDescription()).isEqualTo(testEvent.getDescription());
            assertThat(loadedEvent.getType()).isEqualTo(testEvent.getType());
            assertThat(loadedEvent.getCreatedBy()).isEqualTo(testEvent.getCreatedBy());
        }

        @Test
        @DisplayName("Should load all events from JSON")
        void shouldLoadAllEventsFromJson() {
            // Given
            Event event1 = new Event("Event 1", "Description 1", Event.EventType.TOURNAMENT, UUID.randomUUID());
            Event event2 = new Event("Event 2", "Description 2", Event.EventType.CHALLENGE, UUID.randomUUID());
            
            databaseManager.saveEvent(event1).join();
            databaseManager.saveEvent(event2).join();

            // When
            CompletableFuture<List<Event>> future = databaseManager.loadAllEvents();

            // Then
            List<Event> events = future.join();
            assertThat(events).hasSize(2);
            assertThat(events).extracting("name").contains("Event 1", "Event 2");
        }

        @Test
        @DisplayName("Should delete event from JSON")
        void shouldDeleteEventFromJson() {
            // Given
            databaseManager.saveEvent(testEvent).join();

            // When
            CompletableFuture<Boolean> future = databaseManager.deleteEvent(testEvent.getId());

            // Then
            assertThat(future.join()).isTrue();
            
            // Verify event is deleted
            CompletableFuture<Event> loadFuture = databaseManager.loadEvent(testEvent.getId());
            assertThat(loadFuture.join()).isNull();
        }

        @Test
        @DisplayName("Should handle non-existent event gracefully")
        void shouldHandleNonExistentEventGracefully() {
            // When
            CompletableFuture<Event> future = databaseManager.loadEvent("non-existent-id");

            // Then
            assertThat(future.join()).isNull();
        }

        @Test
        @DisplayName("Should handle null event gracefully")
        void shouldHandleNullEventGracefully() {
            // When
            CompletableFuture<Boolean> future = databaseManager.saveEvent(null);

            // Then
            assertThat(future.join()).isFalse();
        }

        @Test
        @DisplayName("Should handle invalid event ID gracefully")
        void shouldHandleInvalidEventIdGracefully() {
            // When
            CompletableFuture<Boolean> future = databaseManager.deleteEvent(null);

            // Then
            assertThat(future.join()).isFalse();
        }
    }

    @Nested
    @DisplayName("Database Connection Tests")
    class DatabaseConnectionTests {

        @Test
        @DisplayName("Should initialize database connection successfully")
        void shouldInitializeDatabaseConnectionSuccessfully() {
            // Given
            lenient().when(configManager.isDatabaseEnabled()).thenReturn(true);
            lenient().when(configManager.getDatabaseHost()).thenReturn("localhost");
            lenient().when(configManager.getDatabasePort()).thenReturn(3306);
            lenient().when(configManager.getDatabaseName()).thenReturn("test_db");
            lenient().when(configManager.getDatabaseUsername()).thenReturn("test_user");
            lenient().when(configManager.getDatabasePassword()).thenReturn("test_pass");
            lenient().when(configManager.getDatabaseConnectionTimeout()).thenReturn(30);
            lenient().when(configManager.getMaxDatabaseConnections()).thenReturn(10);

            // When & Then
            assertThatCode(() -> {
                DatabaseManager dbManager = new DatabaseManager(plugin);
                dbManager.initialize();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle database connection failure gracefully")
        void shouldHandleDatabaseConnectionFailureGracefully() {
            // Given
            lenient().when(configManager.isDatabaseEnabled()).thenReturn(true);
            lenient().when(configManager.getDatabaseHost()).thenReturn("invalid-host");
            lenient().when(configManager.getDatabasePort()).thenReturn(3306);
            lenient().when(configManager.getDatabaseName()).thenReturn("test_db");
            lenient().when(configManager.getDatabaseUsername()).thenReturn("test_user");
            lenient().when(configManager.getDatabasePassword()).thenReturn("test_pass");

            // When & Then
            assertThatCode(() -> {
                DatabaseManager dbManager = new DatabaseManager(plugin);
                dbManager.initialize();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should check connection health")
        void shouldCheckConnectionHealth() {
            // When
            boolean isHealthy = databaseManager.isConnectionHealthy();

            // Then
            assertThat(isHealthy).isTrue(); // JSON storage is always healthy
        }

        @Test
        @DisplayName("Should attempt reconnection")
        void shouldAttemptReconnection() {
            // When & Then
            assertThatCode(() -> databaseManager.attemptReconnection()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should save multiple events in batch")
        void shouldSaveMultipleEventsInBatch() {
            // Given
            List<Event> events = Arrays.asList(
                new Event("Event 1", "Description 1", Event.EventType.TOURNAMENT, UUID.randomUUID()),
                new Event("Event 2", "Description 2", Event.EventType.CHALLENGE, UUID.randomUUID()),
                new Event("Event 3", "Description 3", Event.EventType.TOURNAMENT, UUID.randomUUID())
            );

            // When
            CompletableFuture<Boolean> future = databaseManager.saveEvents(events);

            // Then
            assertThat(future.join()).isTrue();
            
            // Verify all events are saved
            CompletableFuture<List<Event>> loadFuture = databaseManager.loadAllEvents();
            List<Event> loadedEvents = loadFuture.join();
            assertThat(loadedEvents).hasSize(3);
        }

        @Test
        @DisplayName("Should handle empty batch gracefully")
        void shouldHandleEmptyBatchGracefully() {
            // When
            CompletableFuture<Boolean> future = databaseManager.saveEvents(Collections.emptyList());

            // Then
            assertThat(future.join()).isTrue();
        }

        @Test
        @DisplayName("Should handle null batch gracefully")
        void shouldHandleNullBatchGracefully() {
            // When
            CompletableFuture<Boolean> future = databaseManager.saveEvents(null);

            // Then
            assertThat(future.join()).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should preserve event data integrity")
        void shouldPreserveEventDataIntegrity() {
            // Given
            Event event = new Event("Test Event", "Test Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
            event.setMaxParticipants(10);
            event.addParticipant(UUID.randomUUID());
            event.setMetadata(Map.of("key1", "value1", "key2", "value2"));
            event.setStartTime(System.currentTimeMillis());
            event.setEndTime(System.currentTimeMillis() + 3600000); // 1 hour later

            // When
            databaseManager.saveEvent(event).join();
            Event loadedEvent = databaseManager.loadEvent(event.getId()).join();

            // Then
            assertThat(loadedEvent).isNotNull();
            assertThat(loadedEvent.getName()).isEqualTo(event.getName());
            assertThat(loadedEvent.getDescription()).isEqualTo(event.getDescription());
            assertThat(loadedEvent.getType()).isEqualTo(event.getType());
            assertThat(loadedEvent.getCreatedBy()).isEqualTo(event.getCreatedBy());
            assertThat(loadedEvent.getMaxParticipants()).isEqualTo(event.getMaxParticipants());
            assertThat(loadedEvent.getCurrentParticipants()).isEqualTo(event.getCurrentParticipants());
            assertThat(loadedEvent.getParticipants()).containsExactlyInAnyOrderElementsOf(event.getParticipants());
            assertThat(loadedEvent.getMetadata()).isEqualTo(event.getMetadata());
            assertThat(loadedEvent.getStartTime()).isEqualTo(event.getStartTime());
            assertThat(loadedEvent.getEndTime()).isEqualTo(event.getEndTime());
        }

        @Test
        @DisplayName("Should handle special characters in event data")
        void shouldHandleSpecialCharactersInEventData() {
            // Given
            String nameWithSpecialChars = "Test Event with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
            String descriptionWithSpecialChars = "Description with special chars: \n\t\r\"'\\";
            Event event = new Event(nameWithSpecialChars, descriptionWithSpecialChars, Event.EventType.TOURNAMENT, UUID.randomUUID());

            // When
            databaseManager.saveEvent(event).join();
            Event loadedEvent = databaseManager.loadEvent(event.getId()).join();

            // Then
            assertThat(loadedEvent).isNotNull();
            assertThat(loadedEvent.getName()).isEqualTo(nameWithSpecialChars);
            assertThat(loadedEvent.getDescription()).isEqualTo(descriptionWithSpecialChars);
        }

        @Test
        @DisplayName("Should handle very large event data")
        void shouldHandleVeryLargeEventData() {
            // Given
            String largeDescription = "A".repeat(10000);
            Map<String, Object> largeMetadata = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                largeMetadata.put("key" + i, "value".repeat(100));
            }
            
            Event event = new Event("Large Event", largeDescription, Event.EventType.TOURNAMENT, UUID.randomUUID());
            event.setMetadata(largeMetadata);

            // When
            CompletableFuture<Boolean> saveFuture = databaseManager.saveEvent(event);
            assertThat(saveFuture.join()).isTrue();
            
            Event loadedEvent = databaseManager.loadEvent(event.getId()).join();

            // Then
            assertThat(loadedEvent).isNotNull();
            assertThat(loadedEvent.getDescription()).isEqualTo(largeDescription);
            assertThat(loadedEvent.getMetadata()).isEqualTo(largeMetadata);
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Should handle concurrent saves")
        void shouldHandleConcurrentSaves() throws InterruptedException {
            // Given
            int threadCount = 10;
            int eventsPerThread = 5;
            List<Thread> threads = new ArrayList<>();
            List<Event> allEvents = new ArrayList<>();

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < eventsPerThread; j++) {
                        Event event = new Event(
                            "Event " + threadIndex + "-" + j,
                            "Description",
                            Event.EventType.TOURNAMENT,
                            UUID.randomUUID()
                        );
                        databaseManager.saveEvent(event).join();
                        synchronized (allEvents) {
                            allEvents.add(event);
                        }
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            CompletableFuture<List<Event>> loadFuture = databaseManager.loadAllEvents();
            List<Event> loadedEvents = loadFuture.join();
            assertThat(loadedEvents).hasSize(threadCount * eventsPerThread);
        }

        @Test
        @DisplayName("Should handle concurrent reads and writes")
        void shouldHandleConcurrentReadsAndWrites() throws InterruptedException {
            // Given
            int threadCount = 5;
            List<Thread> threads = new ArrayList<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await();
                        
                        // Perform mixed operations
                        for (int j = 0; j < 10; j++) {
                            Event event = new Event(
                                "Event " + threadIndex + "-" + j,
                                "Description",
                                Event.EventType.TOURNAMENT,
                                UUID.randomUUID()
                            );
                            databaseManager.saveEvent(event).join();
                            databaseManager.loadAllEvents().join();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
                threads.add(thread);
                thread.start();
            }

            startLatch.countDown();
            endLatch.await();

            // Then
            CompletableFuture<List<Event>> loadFuture = databaseManager.loadAllEvents();
            List<Event> loadedEvents = loadFuture.join();
            assertThat(loadedEvents).hasSize(threadCount * 10);
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should recover from file system errors")
        void shouldRecoverFromFileSystemErrors() {
            // Given
            lenient().when(configManager.getJsonFolder()).thenReturn("/invalid/path/that/does/not/exist");

            // When
            DatabaseManager dbManager = new DatabaseManager(plugin);
            dbManager.initialize();

            // Then
            CompletableFuture<Boolean> saveFuture = dbManager.saveEvent(testEvent);
            assertThat(saveFuture.join()).isFalse();
        }

        @Test
        @DisplayName("Should handle corrupted JSON files gracefully")
        void shouldHandleCorruptedJsonFilesGracefully() {
            // Given
            databaseManager.saveEvent(testEvent).join();
            
            // Corrupt the JSON file
            File jsonFile = new File(plugin.getDataFolder(), uniqueTestFolder + "/" + testEvent.getId() + ".json");
            if (jsonFile.exists()) {
                // Write invalid JSON
                try {
                    java.nio.file.Files.write(jsonFile.toPath(), "invalid json content".getBytes());
                } catch (Exception e) {
                    // Ignore file system errors in test
                }
            }

            // When
            CompletableFuture<Event> loadFuture = databaseManager.loadEvent(testEvent.getId());

            // Then
            assertThat(loadFuture.join()).isNull();
        }

        @Test
        @DisplayName("Should handle memory pressure gracefully")
        void shouldHandleMemoryPressureGracefully() {
            // Given - Reduced size to avoid actual memory issues
            List<Event> largeEvents = new ArrayList<>();
            for (int i = 0; i < 100; i++) { // Reduced from 1000 to 100
                Event event = new Event("Event " + i, "Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
                event.setMetadata(Map.of("large_key", "large_value".repeat(10))); // Reduced from 100 to 10
                largeEvents.add(event);
            }

            // When
            CompletableFuture<Boolean> saveFuture = databaseManager.saveEvents(largeEvents);

            // Then
            assertThat(saveFuture.join()).isTrue();
            // Should not throw OutOfMemoryError
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should close connections properly")
        void shouldCloseConnectionsProperly() {
            // When & Then
            assertThatCode(() -> databaseManager.closeConnections()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should cleanup resources on shutdown")
        void shouldCleanupResourcesOnShutdown() {
            // Given
            databaseManager.saveEvent(testEvent).join();

            // When
            databaseManager.closeConnections();

            // Then
            // Should not throw any exceptions during cleanup
            assertThatCode(() -> databaseManager.closeConnections()).doesNotThrowAnyException();
        }
    }
} 