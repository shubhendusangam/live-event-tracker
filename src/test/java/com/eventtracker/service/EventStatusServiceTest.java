package com.eventtracker.service;

import com.eventtracker.dto.EventStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventStatusServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    @SuppressWarnings("unchecked")
    private ScheduledFuture scheduledFuture;

    private EventStatusService eventStatusService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        eventStatusService = new EventStatusService(restTemplate, kafkaTemplate, taskScheduler, objectMapper);

        // Set private fields using reflection for testing
        try {
            var externalApiUrlField = EventStatusService.class.getDeclaredField("externalApiUrl");
            externalApiUrlField.setAccessible(true);
            externalApiUrlField.set(eventStatusService, "http://localhost:8080/mock-api");

            var kafkaTopicField = EventStatusService.class.getDeclaredField("kafkaTopic");
            kafkaTopicField.setAccessible(true);
            kafkaTopicField.set(eventStatusService, "score-updates");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testUpdateEventStatusToLive() {
        // Given
        String eventId = "event-1";
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(), any()))
                .thenReturn(scheduledFuture);

        // When
        eventStatusService.updateEventStatus(eventId, true);

        // Then
        EventStatus status = eventStatusService.getEventStatus(eventId);
        assertNotNull(status);
        assertTrue(status.isLive());
        assertEquals(eventId, status.getEventId());
        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), any(), any());
    }

    @Test
    void testUpdateEventStatusToNotLive() {
        // Given
        String eventId = "event-1";
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(), any()))
                .thenReturn(scheduledFuture);

        // First set event to live
        eventStatusService.updateEventStatus(eventId, true);

        // When
        eventStatusService.updateEventStatus(eventId, false);

        // Then
        EventStatus status = eventStatusService.getEventStatus(eventId);
        assertNotNull(status);
        assertFalse(status.isLive());
        verify(scheduledFuture).cancel(false);
    }

    @Test
    void testGetActiveEventCount() {
        // Given
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(), any()))
                .thenReturn(scheduledFuture);

        // When
        eventStatusService.updateEventStatus("event-1", true);
        eventStatusService.updateEventStatus("event-2", true);
        eventStatusService.updateEventStatus("event-3", false);

        // Then
        assertEquals(2, eventStatusService.getActiveEventCount());
    }

    @Test
    void testGetEventStatusForNonExistentEvent() {
        // When
        EventStatus status = eventStatusService.getEventStatus("non-existent");

        // Then
        assertNull(status);
    }
}
