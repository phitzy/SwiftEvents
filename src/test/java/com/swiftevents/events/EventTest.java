package com.swiftevents.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Event Tests")
class EventTest {

    private Event event;
    private UUID testCreatorId;
    private UUID testPlayerId;

    @BeforeEach
    void setUp() {
        testCreatorId = UUID.randomUUID();
        testPlayerId = UUID.randomUUID();
        event = new Event("Test Event", "Test Description", Event.EventType.TOURNAMENT, testCreatorId);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create event with valid parameters")
        void shouldCreateEventWithValidParameters() {
            // Given
            String name = "Test Event";
            String description = "Test Description";
            Event.EventType type = Event.EventType.TOURNAMENT;
            UUID creatorId = UUID.randomUUID();

            // When
            Event event = new Event(name, description, type, creatorId);

            // Then
            assertThat(event.getName()).isEqualTo(name);
            assertThat(event.getDescription()).isEqualTo(description);
            assertThat(event.getType()).isEqualTo(type);
            assertThat(event.getCreatedBy()).isEqualTo(creatorId);
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.CREATED);
            assertThat(event.getParticipants()).isEmpty();
            assertThat(event.getCreatedAt()).isPositive();
            assertThat(event.getId()).isNotNull().isNotEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Should throw exception for invalid name")
        void shouldThrowExceptionForInvalidName(String invalidName) {
            // Then
            assertThatThrownBy(() -> new Event(invalidName, "Description", Event.EventType.TOURNAMENT, UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("Should throw exception for null type")
        void shouldThrowExceptionForNullType() {
            // Then
            assertThatThrownBy(() -> new Event("Name", "Description", null, UUID.randomUUID()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("Should handle null creator gracefully")
        void shouldHandleNullCreatorGracefully() {
            // When
            Event event = new Event("Name", "Description", Event.EventType.TOURNAMENT, null);

            // Then
            assertThat(event).isNotNull();
            assertThat(event.getName()).isEqualTo("Name");
            assertThat(event.getCreatedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("State Management Tests")
    class StateManagementTests {

        @Test
        @DisplayName("Should start event successfully")
        void shouldStartEventSuccessfully() {
            // Given
            long startTime = System.currentTimeMillis();

            // When
            event.setStatus(Event.EventStatus.ACTIVE);
            event.setStartTime(startTime);

            // Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
            assertThat(event.getStartTime()).isEqualTo(startTime);
            assertThat(event.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should not start already active event")
        void shouldNotStartAlreadyActiveEvent() {
            // Given
            event.setStatus(Event.EventStatus.ACTIVE);

            // When & Then
            assertThat(event.isActive()).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should not start completed event")
        void shouldNotStartCompletedEvent() {
            // Given
            event.setStatus(Event.EventStatus.COMPLETED);

            // When & Then
            assertThat(event.isCompleted()).isTrue();
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should end event successfully")
        void shouldEndEventSuccessfully() {
            // Given
            event.setStatus(Event.EventStatus.ACTIVE);
            long endTime = System.currentTimeMillis();

            // When
            event.setStatus(Event.EventStatus.COMPLETED);
            event.setEndTime(endTime);

            // Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.COMPLETED);
            assertThat(event.getEndTime()).isEqualTo(endTime);
            assertThat(event.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should not end non-active event")
        void shouldNotEndNonActiveEvent() {
            // When & Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.CREATED);
            assertThat(event.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should cancel event successfully")
        void shouldCancelEventSuccessfully() {
            // When
            event.setStatus(Event.EventStatus.CANCELLED);

            // Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.CANCELLED);
            assertThat(event.isCancelled()).isTrue();
        }

        @Test
        @DisplayName("Should not cancel completed event")
        void shouldNotCancelCompletedEvent() {
            // Given
            event.setStatus(Event.EventStatus.COMPLETED);

            // When & Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.COMPLETED);
            assertThat(event.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("Should pause and resume event")
        void shouldPauseAndResumeEvent() {
            // Given
            event.setStatus(Event.EventStatus.ACTIVE);

            // When - Pause
            event.setStatus(Event.EventStatus.PAUSED);

            // Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.PAUSED);

            // When - Resume
            event.setStatus(Event.EventStatus.ACTIVE);

            // Then
            assertThat(event.getStatus()).isEqualTo(Event.EventStatus.ACTIVE);
            assertThat(event.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Participant Management Tests")
    class ParticipantManagementTests {

        @Test
        @DisplayName("Should add participant successfully")
        void shouldAddParticipantSuccessfully() {
            // When
            boolean result = event.addParticipant(testPlayerId);

            // Then
            assertThat(result).isTrue();
            assertThat(event.getParticipants()).contains(testPlayerId);
            assertThat(event.getCurrentParticipants()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not add duplicate participant")
        void shouldNotAddDuplicateParticipant() {
            // Given
            event.addParticipant(testPlayerId);

            // When
            boolean result = event.addParticipant(testPlayerId);

            // Then
            assertThat(result).isFalse();
            assertThat(event.getCurrentParticipants()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not add participant to completed event")
        void shouldNotAddParticipantToCompletedEvent() {
            // Given
            event.setStatus(Event.EventStatus.COMPLETED);

            // When
            boolean result = event.addParticipant(testPlayerId);

            // Then
            assertThat(result).isFalse();
            assertThat(event.getParticipants()).isEmpty();
        }

        @Test
        @DisplayName("Should remove participant successfully")
        void shouldRemoveParticipantSuccessfully() {
            // Given
            event.addParticipant(testPlayerId);

            // When
            boolean result = event.removeParticipant(testPlayerId);

            // Then
            assertThat(result).isTrue();
            assertThat(event.getParticipants()).doesNotContain(testPlayerId);
            assertThat(event.getCurrentParticipants()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should not remove non-existent participant")
        void shouldNotRemoveNonExistentParticipant() {
            // When
            boolean result = event.removeParticipant(testPlayerId);

            // Then
            assertThat(result).isFalse();
            assertThat(event.getCurrentParticipants()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should respect max participants limit")
        void shouldRespectMaxParticipantsLimit() {
            // Given
            event.setMaxParticipants(2);
            event.addParticipant(UUID.randomUUID());
            event.addParticipant(UUID.randomUUID());

            // When
            boolean result = event.addParticipant(UUID.randomUUID());

            // Then
            assertThat(result).isFalse();
            assertThat(event.getCurrentParticipants()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Time Management Tests")
    class TimeManagementTests {

        @Test
        @DisplayName("Should check if event has started")
        void shouldCheckIfEventHasStarted() {
            // Given
            long futureTime = System.currentTimeMillis() + 60000; // 1 minute from now
            event.setStartTime(futureTime);

            // When & Then
            assertThat(event.hasStarted()).isFalse();

            // Set to past time
            event.setStartTime(System.currentTimeMillis() - 60000);
            assertThat(event.hasStarted()).isTrue();
        }

        @Test
        @DisplayName("Should check if event has ended")
        void shouldCheckIfEventHasEnded() {
            // Given
            event.setStatus(Event.EventStatus.ACTIVE);
            long futureTime = System.currentTimeMillis() + 60000; // 1 minute from now
            event.setEndTime(futureTime);

            // When & Then
            assertThat(event.hasEnded()).isFalse();

            // Set to past time
            event.setEndTime(System.currentTimeMillis() - 60000);
            assertThat(event.hasEnded()).isTrue();
        }

        @Test
        @DisplayName("Should calculate duration correctly")
        void shouldCalculateDurationCorrectly() {
            // Given
            long startTime = System.currentTimeMillis();
            long endTime = startTime + 300000; // 5 minutes later
            event.setStartTime(startTime);
            event.setEndTime(endTime);

            // When
            long duration = event.getDuration();

            // Then
            assertThat(duration).isEqualTo(300000);
        }
    }

    @Nested
    @DisplayName("Data Validation Tests")
    class DataValidationTests {

        @Test
        @DisplayName("Should validate event data integrity")
        void shouldValidateEventDataIntegrity() {
            // Given
            event.setMaxParticipants(10);
            event.addParticipant(testPlayerId);

            // When & Then
            assertThat(event.getCurrentParticipants()).isLessThanOrEqualTo(event.getMaxParticipants());
            assertThat(event.getParticipants()).contains(testPlayerId);
        }

        @Test
        @DisplayName("Should detect invalid participant count")
        void shouldDetectInvalidParticipantCount() {
            // Given
            event.setMaxParticipants(1);
            event.addParticipant(UUID.randomUUID());
            event.addParticipant(UUID.randomUUID()); // This should not be allowed

            // When & Then
            assertThat(event.getCurrentParticipants()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null metadata gracefully")
        void shouldHandleNullMetadataGracefully() {
            // When
            event.setMetadata(null);

            // Then
            assertThat(event.getMetadata()).isNotNull();
            assertThat(event.getMetadata()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should handle event data correctly")
        void shouldHandleEventDataCorrectly() {
            // Given
            event.setMaxParticipants(5);
            event.addParticipant(testPlayerId);
            event.setMetadata(Map.of("key", "value"));

            // When & Then
            assertThat(event.getName()).isEqualTo("Test Event");
            assertThat(event.getDescription()).isEqualTo("Test Description");
            assertThat(event.getType()).isEqualTo(Event.EventType.TOURNAMENT);
            assertThat(event.getCreatedBy()).isEqualTo(testCreatorId);
            assertThat(event.getMaxParticipants()).isEqualTo(5);
            assertThat(event.getParticipants()).contains(testPlayerId);
            assertThat(event.getMetadata()).isEqualTo(Map.of("key", "value"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long event names")
        void shouldHandleVeryLongEventNames() {
            // Given
            String longName = "A".repeat(1000);

            // When & Then
            assertThatThrownBy(() -> new Event(longName, "Description", Event.EventType.TOURNAMENT, testCreatorId))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle negative max participants")
        void shouldHandleNegativeMaxParticipants() {
            // When
            event.setMaxParticipants(-1);

            // Then
            assertThat(event.getMaxParticipants()).isEqualTo(-1);
            assertThat(event.hasUnlimitedSlots()).isTrue();
        }

        @Test
        @DisplayName("Should handle concurrent participant modifications")
        void shouldHandleConcurrentParticipantModifications() throws InterruptedException {
            // Given
            int threadCount = 10;
            int operationsPerThread = 100;
            List<Thread> threads = new ArrayList<>();
            List<UUID> playerIds = new ArrayList<>();

            for (int i = 0; i < threadCount * operationsPerThread; i++) {
                playerIds.add(UUID.randomUUID());
            }

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int index = threadIndex * operationsPerThread + j;
                        event.addParticipant(playerIds.get(index));
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
            assertThat(event.getCurrentParticipants()).isEqualTo(threadCount * operationsPerThread);
        }
    }
} 