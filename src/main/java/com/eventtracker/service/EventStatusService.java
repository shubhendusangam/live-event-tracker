package com.eventtracker.service;

import com.eventtracker.dto.EventStatus;
import com.eventtracker.dto.ScoreData;
import com.eventtracker.dto.ScoreMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventStatusService {

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;

    @Value("${app.external-api.url}")
    private String externalApiUrl;

    @Value("${app.kafka.topic}")
    private String kafkaTopic;

    // In-memory storage for event statuses and scheduled tasks
    private final ConcurrentHashMap<String, EventStatus> eventStatuses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void updateEventStatus(String eventId, boolean isLive) {
        log.info("Updating event status: eventId={}, isLive={}", eventId, isLive);

        EventStatus currentStatus = eventStatuses.get(eventId);

        if (currentStatus != null && currentStatus.isLive() == isLive) {
            log.debug("Event status unchanged for eventId={}", eventId);
            return;
        }

        eventStatuses.put(eventId, new EventStatus(eventId, isLive));

        if (isLive) {
            startScheduledTask(eventId);
        } else {
            stopScheduledTask(eventId);
        }
    }

    private void startScheduledTask(String eventId) {
        // Cancel existing task if any
        stopScheduledTask(eventId);

        log.info("Starting scheduled task for eventId={}", eventId);

        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                () -> fetchAndPublishScore(eventId),
                Instant.now().plusSeconds(1), // Start after 1 second
                java.time.Duration.ofSeconds(10) // Repeat every 10 seconds
        );

        scheduledTasks.put(eventId, scheduledTask);
    }

    private void stopScheduledTask(String eventId) {
        ScheduledFuture<?> existingTask = scheduledTasks.remove(eventId);
        if (existingTask != null) {
            log.info("Stopping scheduled task for eventId={}", eventId);
            existingTask.cancel(false);
        }
    }

    private void fetchAndPublishScore(String eventId) {
        try {
            log.debug("Fetching score for eventId={}", eventId);

            // Check if event is still live before making API call
            EventStatus status = eventStatuses.get(eventId);
            if (status == null || !status.isLive()) {
                log.warn("Event {} is no longer live, stopping task", eventId);
                stopScheduledTask(eventId);
                return;
            }

            ScoreData scoreData = fetchScoreFromExternalApi(eventId);
            publishScoreMessage(scoreData);

        } catch (Exception e) {
            log.error("Error processing score for eventId={}: {}", eventId, e.getMessage(), e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private ScoreData fetchScoreFromExternalApi(String eventId) {
        try {
            String url = String.format("%s/events/%s/score", externalApiUrl, eventId);
            log.debug("Calling external API: {}", url);

            ScoreData scoreData = restTemplate.getForObject(url, ScoreData.class);

            if (scoreData == null) {
                throw new RuntimeException("Received null response from external API");
            }

            log.debug("Received score data: {}", scoreData);
            return scoreData;

        } catch (Exception e) {
            log.error("Failed to fetch score from external API for eventId={}: {}", eventId, e.getMessage());
            throw e;
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    private void publishScoreMessage(ScoreData scoreData) {
        try {
            ScoreMessage message = ScoreMessage.builder()
                    .eventId(scoreData.getEventId())
                    .currentScore(scoreData.getCurrentScore())
                    .timestamp(Instant.now())
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(kafkaTopic, scoreData.getEventId(), messageJson);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish message for eventId={}: {}",
                            scoreData.getEventId(), ex.getMessage());
                } else {
                    log.debug("Successfully published message for eventId={} to topic={}",
                            scoreData.getEventId(), kafkaTopic);
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for eventId={}: {}",
                    scoreData.getEventId(), e.getMessage());
            throw new RuntimeException("Message serialization failed", e);
        }
    }

    public EventStatus getEventStatus(String eventId) {
        return eventStatuses.get(eventId);
    }

    public int getActiveEventCount() {
        return (int) eventStatuses.values().stream()
                .filter(EventStatus::isLive)
                .count();
    }

    public void shutdown() {
        log.info("Shutting down event status service, cancelling {} scheduled tasks",
                scheduledTasks.size());
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }
}
