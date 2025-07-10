package com.swiftevents.events;

import com.swiftevents.SwiftEventsPlugin;
import com.swiftevents.config.ConfigManager;
import com.swiftevents.database.DatabaseManager;
import com.swiftevents.api.hooks.HookManager;
import com.swiftevents.hud.HUDManager;
import com.swiftevents.chat.ChatManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventManager Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventManagerTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private SwiftEventsPlugin plugin;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private ConfigManager configManager;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private DatabaseManager databaseManager;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private HookManager hookManager;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private HUDManager hudManager;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private ChatManager chatManager;
    
    @Mock(strictness = Mock.Strictness.LENIENT)
    private Server server;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private PluginManager pluginManager;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private BukkitScheduler scheduler;
    
    private EventManager eventManager;
    private UUID testCreatorId;
    private UUID testPlayerId;
    
    private static Server originalServer;
    private static boolean serverInitialized = false;

    @BeforeAll
    static void beforeAll() {
        try {
            // Only initialize server if not already done
            if (!serverInitialized) {
                originalServer = Bukkit.getServer();
                
                // Use reflection to set the server field to null first
                Field serverField = Bukkit.class.getDeclaredField("server");
                serverField.setAccessible(true);
                serverField.set(null, null);
                
                // Now set our mocked server
                Server mockedServer = mock(Server.class, withSettings().lenient());
                BukkitScheduler mockedScheduler = mock(BukkitScheduler.class, withSettings().lenient());
                PluginManager mockedPluginManager = mock(PluginManager.class, withSettings().lenient());
                
                when(mockedServer.getScheduler()).thenReturn(mockedScheduler);
                when(mockedServer.getPluginManager()).thenReturn(mockedPluginManager);
                when(mockedServer.getLogger()).thenReturn(Logger.getLogger("TestLogger"));
                
                // Make the scheduler run tasks immediately for testing
                when(mockedScheduler.runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                    try {
                        invocation.getArgument(1, Runnable.class).run();
                    } catch (Exception e) {
                        // Ignore exceptions in test tasks
                    }
                    return null;
                });
                when(mockedScheduler.runTaskAsynchronously(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
                    try {
                        invocation.getArgument(1, Runnable.class).run();
                    } catch (Exception e) {
                        // Ignore exceptions in test tasks
                    }
                    return null;
                });
                
                serverField.set(null, mockedServer);
                serverInitialized = true;
            }
        } catch (Exception e) {
            // If we can't set up the server, tests will handle it gracefully
        }
    }

    @AfterAll
    static void afterAll() {
        try {
            if (serverInitialized) {
                // Restore original server using reflection
                Field serverField = Bukkit.class.getDeclaredField("server");
                serverField.setAccessible(true);
                serverField.set(null, originalServer);
                serverInitialized = false;
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testCreatorId = UUID.randomUUID();
        testPlayerId = UUID.randomUUID();

        // Configure all mocks with lenient behavior
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDatabaseManager()).thenReturn(databaseManager);
        when(plugin.getHookManager()).thenReturn(hookManager);
        when(plugin.getHUDManager()).thenReturn(hudManager);
        when(plugin.getChatManager()).thenReturn(chatManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getServer()).thenReturn(server);

        // Mock Bukkit APIs
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getPluginManager()).thenReturn(pluginManager);
        
        // Make the scheduler run tasks immediately for testing
        lenient().when(scheduler.runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            try {
                invocation.getArgument(1, Runnable.class).run();
            } catch (Exception e) {
                // Ignore exceptions in test tasks
            }
            return null;
        });
        lenient().when(scheduler.runTaskAsynchronously(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            try {
                invocation.getArgument(1, Runnable.class).run();
            } catch (Exception e) {
                // Ignore exceptions in test tasks
            }
            return null;
        });

        // Default config values
        when(configManager.isDatabaseEnabled()).thenReturn(false);
        when(configManager.getAutoSaveInterval()).thenReturn(300);
        when(configManager.isHUDEnabled()).thenReturn(false);
        when(configManager.getMaxConcurrentEvents()).thenReturn(10);
        when(configManager.getPlayerCooldown()).thenReturn(300);
        when(configManager.getMaxEventsPerPlayer()).thenReturn(5);
        
        // Mock hook manager to allow operations
        when(hookManager.callEventPreStart(any(Event.class))).thenReturn(true);
        doNothing().when(hookManager).callEventCreated(any(Event.class));
        doNothing().when(hookManager).callEventStarted(any(Event.class));
        doNothing().when(hookManager).callEventEnded(any(Event.class), anyString());
        
        // Mock chat manager
        doNothing().when(chatManager).announceEvent(any(Event.class), any());
        
        // Mock database operations
        when(databaseManager.loadAllEvents()).thenReturn(CompletableFuture.completedFuture(new ArrayList<>()));
        when(databaseManager.saveEvent(any(Event.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(databaseManager.deleteEvent(anyString())).thenReturn(CompletableFuture.completedFuture(true));
        
        // Mock HUD manager
        doNothing().when(hudManager).updateActiveEvents();
        
        // Initialize EventManager for each test
        eventManager = new EventManager(plugin);
        
        // Reset mocks to clear interactions from EventManager constructor
        reset(databaseManager, hookManager, hudManager, chatManager);
        
        // Re-setup critical mocks after reset
        when(hookManager.callEventPreStart(any(Event.class))).thenReturn(true);
        doNothing().when(hookManager).callEventCreated(any(Event.class));
        doNothing().when(hookManager).callEventStarted(any(Event.class));
        doNothing().when(hookManager).callEventEnded(any(Event.class), anyString());
        when(databaseManager.saveEvent(any(Event.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(databaseManager.deleteEvent(anyString())).thenReturn(CompletableFuture.completedFuture(true));
        doNothing().when(chatManager).announceEvent(any(Event.class), any());
        doNothing().when(hudManager).updateActiveEvents();
    }

    @AfterEach
    void tearDown() {
        // Clean up event manager
        if (eventManager != null) {
            try {
                eventManager.shutdown();
            } catch (Exception e) {
                // Ignore shutdown errors
            }
        }
    }

    @Nested
    @DisplayName("Event Creation Tests")
    class EventCreationTests {

        @Test
        @DisplayName("Should create event successfully")
        void shouldCreateEventSuccessfully() {
            // Given
            String name = "Test Event";
            String description = "Test Description";
            Event.EventType type = Event.EventType.PVP;

            // When
            Event event = eventManager.createEvent(name, description, type, testCreatorId);

            // Then
            assertThat(event).isNotNull();
            assertThat(event.getName()).isEqualTo(name);
            assertThat(event.getDescription()).isEqualTo(description);
            assertThat(event.getType()).isEqualTo(type);
            assertThat(event.getCreatedBy()).isEqualTo(testCreatorId);
            assertThat(eventManager.getAllEvents()).contains(event);
        }

        @Test
        @DisplayName("Should not create event with invalid parameters")
        void shouldNotCreateEventWithInvalidParameters() {
            // When & Then
            assertThatThrownBy(() -> eventManager.createEvent(null, "Description", Event.EventType.TOURNAMENT, testCreatorId))
                    .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> eventManager.createEvent("", "Description", Event.EventType.TOURNAMENT, testCreatorId))
                    .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> eventManager.createEvent("Name", "Description", null, testCreatorId))
                    .isInstanceOf(IllegalArgumentException.class);
            
            assertThatThrownBy(() -> eventManager.createEvent("Name", "Description", Event.EventType.TOURNAMENT, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should respect max concurrent events limit")
        void shouldRespectMaxConcurrentEventsLimit() {
            // Given
            when(configManager.getMaxConcurrentEvents()).thenReturn(1);
            Event event1 = eventManager.createEvent("Event 1", "Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
            assertThat(event1).isNotNull();
            eventManager.startEvent(event1.getId()); // Must be active to count

            // When
            Event event2 = eventManager.createEvent("Event 2", "Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
            
            // Then
            assertThat(event2).isNull();
        }

        @Test
        @DisplayName("Should respect max events per player limit")
        void shouldRespectMaxEventsPerPlayerLimit() {
            // Given
            when(configManager.getMaxEventsPerPlayer()).thenReturn(1);
            eventManager.createEvent("Event 1", "Description", Event.EventType.TOURNAMENT, testCreatorId);

            // When
            Event event2 = eventManager.createEvent("Event 2", "Description", Event.EventType.TOURNAMENT, testCreatorId);

            // Then
            assertThat(event2).isNull();
        }
    }

    @Nested
    @DisplayName("Event Lifecycle Tests")
    class EventLifecycleTests {

        @Test
        @DisplayName("Should start event successfully")
        void shouldStartEventSuccessfully() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();

            // When
            boolean result = eventManager.startEvent(event.getId());

            // Then
            assertThat(result).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
            assertThat(eventManager.getActiveEvents()).contains(event);
        }

        @Test
        @DisplayName("Should not start non-existent event")
        void shouldNotStartNonExistentEvent() {
            // When
            boolean result = eventManager.startEvent("non-existent-id");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should end event successfully")
        void shouldEndEventSuccessfully() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();
            eventManager.startEvent(event.getId());

            // When
            boolean result = eventManager.endEvent(event.getId());

            // Then
            assertThat(result).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.COMPLETED);
            assertThat(eventManager.getActiveEvents()).doesNotContain(event);
        }

        @Test
        @DisplayName("Should cancel event successfully")
        void shouldCancelEventSuccessfully() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();

            // When
            boolean result = eventManager.cancelEvent(event.getId());

            // Then
            assertThat(result).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should pause and resume event")
        void shouldPauseAndResumeEvent() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();
            eventManager.startEvent(event.getId());
            
            // When - Pause
            boolean pauseResult = eventManager.pauseEvent(event.getId());
            
            // Then - Pause
            assertThat(pauseResult).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.PAUSED);
            
            // When - Resume
            boolean resumeResult = eventManager.resumeEvent(event.getId());
            
            // Then - Resume
            assertThat(resumeResult).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("Participant Management Tests")
    class ParticipantManagementTests {

        private Event event;

        @BeforeEach
        void participantSetup() {
            event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();
        }

        @Test
        @DisplayName("Should join event successfully")
        void shouldJoinEventSuccessfully() {
            // Given
            eventManager.startEvent(event.getId());
            
            // When
            boolean result = eventManager.joinEvent(event.getId(), testPlayerId);

            // Then
            assertThat(result).isTrue();
            assertThat(event.getParticipants()).contains(testPlayerId);
        }

        @Test
        @DisplayName("Should not join non-active event")
        void shouldNotJoinNonActiveEvent() {
            // Given
            // Event is CREATED but not ACTIVE by default
            
            // When
            boolean result = eventManager.joinEvent(event.getId(), testPlayerId);

            // Then
            assertThat(result).isFalse();
            assertThat(event.getParticipants()).isEmpty();
        }

        @Test
        @DisplayName("Should not join full event")
        void shouldNotJoinFullEvent() {
            // Given
            eventManager.startEvent(event.getId());
            event.setMaxParticipants(1);
            eventManager.joinEvent(event.getId(), UUID.randomUUID());

            // When
            boolean result = eventManager.joinEvent(event.getId(), testPlayerId);

            // Then
            assertThat(result).isFalse();
            assertThat(event.getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("Should leave event successfully")
        void shouldLeaveEventSuccessfully() {
            // Given
            eventManager.startEvent(event.getId());
            eventManager.joinEvent(event.getId(), testPlayerId);
            
            // When
            boolean result = eventManager.leaveEvent(event.getId(), testPlayerId);
            
            // Then
            assertThat(result).isTrue();
            assertThat(event.getParticipants()).doesNotContain(testPlayerId);
        }

        @Test
        @DisplayName("Should not leave event when not participant")
        void shouldNotLeaveEventWhenNotParticipant() {
            // Given
            eventManager.startEvent(event.getId());

            // When
            boolean result = eventManager.leaveEvent(event.getId(), testPlayerId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should respect player cooldown")
        void shouldRespectPlayerCooldown() {
            // Given
            when(configManager.getPlayerCooldown()).thenReturn(300); // 5 minutes
            eventManager.startEvent(event.getId());
            eventManager.joinEvent(event.getId(), testPlayerId);
            eventManager.leaveEvent(event.getId(), testPlayerId);
            
            // When
            boolean result = eventManager.joinEvent(event.getId(), testPlayerId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Event Query Tests")
    class EventQueryTests {

        @Test
        @DisplayName("Should get event by ID")
        void shouldGetEventById() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(event).isNotNull();

            // When
            Event found = eventManager.getEvent(event.getId());

            // Then
            assertThat(found).isEqualTo(event);
        }

        @Test
        @DisplayName("Should return null for non-existent event")
        void shouldReturnNullForNonExistentEvent() {
            // When
            Event found = eventManager.getEvent("non-existent-id");

            // Then
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("Should get events by type")
        void shouldGetEventsByType() {
            // Given
            Event tournamentEvent = eventManager.createEvent("Tournament", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            Event challengeEvent = eventManager.createEvent("Challenge", "Description", Event.EventType.CHALLENGE, testCreatorId);

            // When
            List<Event> tournamentEvents = eventManager.getEventsByType(Event.EventType.TOURNAMENT);
            List<Event> challengeEvents = eventManager.getEventsByType(Event.EventType.CHALLENGE);

            // Then
            assertThat(tournamentEvents).contains(tournamentEvent);
            assertThat(tournamentEvents).doesNotContain(challengeEvent);
            assertThat(challengeEvents).contains(challengeEvent);
            assertThat(challengeEvents).doesNotContain(tournamentEvent);
        }

        @Test
        @DisplayName("Should get events by status")
        void shouldGetEventsByStatus() {
            // Given
            Event createdEvent = eventManager.createEvent("Created", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            assertThat(createdEvent).isNotNull();
            
            Event activeEvent = eventManager.createEvent("Active", "Description", Event.EventType.CHALLENGE, testCreatorId);
            assertThat(activeEvent).isNotNull();
            eventManager.startEvent(activeEvent.getId());

            // When
            List<Event> createdEvents = eventManager.getEventsByStatus(Event.EventStatus.CREATED);
            List<Event> activeEvents = eventManager.getEventsByStatus(Event.EventStatus.ACTIVE);

            // Then
            assertThat(createdEvents).contains(createdEvent);
            assertThat(createdEvents).doesNotContain(activeEvent);
            assertThat(activeEvents).contains(activeEvent);
            assertThat(activeEvents).doesNotContain(createdEvent);
        }

        @Test
        @DisplayName("Should get player events")
        void shouldGetPlayerEvents() {
            // Given
            Event event1 = eventManager.createEvent("Event 1", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            Event event2 = eventManager.createEvent("Event 2", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            eventManager.startEvent(event1.getId());
            eventManager.startEvent(event2.getId());
            eventManager.joinEvent(event1.getId(), testPlayerId);
            eventManager.joinEvent(event2.getId(), testPlayerId);

            // When
            List<Event> playerEvents = eventManager.getPlayerEvents(testPlayerId);

            // Then
            assertThat(playerEvents).contains(event1, event2);
        }

        @Test
        @DisplayName("Should check if player is in event")
        void shouldCheckIfPlayerIsInEvent() {
            // Given
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            eventManager.startEvent(event.getId());
            eventManager.joinEvent(event.getId(), testPlayerId);

            // When
            boolean isInEvent = eventManager.isPlayerInEvent(testPlayerId);

            // Then
            assertThat(isInEvent).isTrue();
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent event creation")
        void shouldHandleConcurrentEventCreation() throws InterruptedException {
            // Given
            int numberOfThreads = 10;
            int eventsPerThread = 5;
            int totalEvents = numberOfThreads * eventsPerThread;
            when(configManager.getMaxConcurrentEvents()).thenReturn(totalEvents + 1);
            when(configManager.getMaxEventsPerPlayer()).thenReturn(eventsPerThread + 1);
            
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(totalEvents);

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                final int threadNum = i;
                UUID creator = UUID.randomUUID(); // One creator per thread
                executor.submit(() -> {
                    for (int j = 0; j < eventsPerThread; j++) {
                        try {
                            eventManager.createEvent("Event " + threadNum + "-" + j, "Description", Event.EventType.TOURNAMENT, creator);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(eventManager.getAllEvents()).hasSize(totalEvents);
        }

        @Test
        @DisplayName("Should handle concurrent participant modifications")
        void shouldHandleConcurrentParticipantModifications() throws InterruptedException {
            // Given
            Event event = eventManager.createEvent("Concurrent Event", "Desc", Event.EventType.PVP, testCreatorId);
            assertThat(event).isNotNull();
            eventManager.startEvent(event.getId());

            int numberOfThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch joinLatch = new CountDownLatch(numberOfThreads);
            CountDownLatch leaveLatch = new CountDownLatch(numberOfThreads);
            List<UUID> playerIds = new ArrayList<>();

            // Generate UUIDs first
            for (int i = 0; i < numberOfThreads; i++) {
                playerIds.add(UUID.randomUUID());
            }

            // When - First, do all joins
            for (int i = 0; i < numberOfThreads; i++) {
                final UUID playerId = playerIds.get(i);
                executor.submit(() -> {
                    try {
                        eventManager.joinEvent(event.getId(), playerId);
                    } finally {
                        joinLatch.countDown();
                    }
                });
            }

            // Wait for all joins to complete
            joinLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

            // Then do all leaves
            for (int i = 0; i < numberOfThreads; i++) {
                final UUID playerId = playerIds.get(i);
                executor.submit(() -> {
                    try {
                        eventManager.leaveEvent(event.getId(), playerId);
                    } finally {
                        leaveLatch.countDown();
                    }
                });
            }

            // Wait for all leaves to complete
            leaveLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            // With ordered joins followed by leaves, should be empty
            assertThat(event.getParticipants()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle database errors gracefully")
        void shouldHandleDatabaseErrorsGracefully() {
            // Given
            when(databaseManager.loadAllEvents()).thenReturn(
                CompletableFuture.failedFuture(new RuntimeException("Database error"))
            );

            // When & Then
            assertThatCode(() -> new EventManager(plugin))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null event operations gracefully")
        void shouldHandleNullEventOperationsGracefully() {
            // When & Then
            assertThat(eventManager.getEvent(null)).isNull();
            assertThat(eventManager.startEvent(null)).isFalse();
            assertThat(eventManager.endEvent(null)).isFalse();
            assertThat(eventManager.cancelEvent(null)).isFalse();
            assertThat(eventManager.joinEvent(null, testPlayerId)).isFalse();
            assertThat(eventManager.leaveEvent(null, testPlayerId)).isFalse();
            assertThat(eventManager.joinEvent("some-id", null)).isFalse();
            assertThat(eventManager.leaveEvent("some-id", null)).isFalse();
        }

        @Test
        @DisplayName("Should handle invalid event IDs gracefully")
        void shouldHandleInvalidEventIdsGracefully() {
            // When & Then
            assertThat(eventManager.getEvent("invalid-id")).isNull();
            assertThat(eventManager.startEvent("invalid-id")).isFalse();
            assertThat(eventManager.endEvent("invalid-id")).isFalse();
            assertThat(eventManager.cancelEvent("invalid-id")).isFalse();
            assertThat(eventManager.joinEvent("invalid-id", testPlayerId)).isFalse();
            assertThat(eventManager.leaveEvent("invalid-id", testPlayerId)).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Persistence Tests")
    class DataPersistenceTests {

        @Test
        @DisplayName("Should save event on creation when DB is enabled")
        void shouldSaveEventOnCreationWhenDbIsEnabled() {
            // Given
            when(configManager.isDatabaseEnabled()).thenReturn(true);
            reset(databaseManager); // Reset from constructor load

            // When
            Event event = eventManager.createEvent("Test Event", "Description", Event.EventType.TOURNAMENT, testCreatorId);

            // Then
            verify(databaseManager).saveEvent(event);
        }

        @Test
        @DisplayName("Should save all events")
        void shouldSaveAllEvents() {
            // Given
            when(configManager.isDatabaseEnabled()).thenReturn(true);
            Event event1 = eventManager.createEvent("Event 1", "Description", Event.EventType.TOURNAMENT, testCreatorId);
            Event event2 = eventManager.createEvent("Event 2", "Description", Event.EventType.TOURNAMENT, UUID.randomUUID());
            reset(databaseManager); // Reset from creation saves

            // When
            eventManager.saveAllEvents();

            // Then
            verify(databaseManager).saveEvents(argThat(events -> 
                events.size() == 2 && events.contains(event1) && events.contains(event2)
            ));
        }
    }
} 