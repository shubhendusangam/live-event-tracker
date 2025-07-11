package com.eventtracker.controller;


import com.eventtracker.dto.EventStatus;
import com.eventtracker.dto.EventStatusRequest;
import com.eventtracker.dto.EventStatusResponse;
import com.eventtracker.service.EventStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventController {

    @Autowired
    private final EventStatusService eventStatusService;

    @PostMapping("/status")
    public ResponseEntity<?> updateEventStatus(@Valid @RequestBody EventStatusRequest request) {

        log.info("Received event status update request: {}", request);

        try {
            eventStatusService.updateEventStatus(request.getEventId(), request.isLive());

            EventStatusResponse response = EventStatusResponse.builder()
                    .eventId(request.getEventId())
                    .status(request.isLive() ? "live" : "not live")
                    .message("Event status updated successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update event status: {}", e.getMessage(), e);

            EventStatusResponse response = EventStatusResponse.builder()
                    .eventId(request.getEventId())
                    .status("error")
                    .message("Failed to update event status: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{eventId}/status")
    public ResponseEntity<EventStatusResponse> getEventStatus(@PathVariable @NotBlank String eventId) {

        log.debug("Getting event status for eventId={}", eventId);

        EventStatus status = eventStatusService.getEventStatus(eventId);

        if (status == null) {
            EventStatusResponse response = EventStatusResponse.builder()
                    .eventId(eventId)
                    .status("unknown")
                    .message("Event not found")
                    .build();

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        EventStatusResponse response = EventStatusResponse.builder()
                .eventId(eventId)
                .status(status.isLive() ? "live" : "not live")
                .message("Event status retrieved successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-count")
    public ResponseEntity<Integer> getActiveEventCount() {
        int count = eventStatusService.getActiveEventCount();
        log.debug("Active event count: {}", count);
        return ResponseEntity.ok(count);
    }
}