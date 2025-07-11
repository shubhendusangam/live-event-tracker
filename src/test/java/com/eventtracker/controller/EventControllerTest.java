package com.eventtracker.controller;

import com.eventtracker.dto.EventStatus;
import com.eventtracker.dto.EventStatusRequest;
import com.eventtracker.service.EventStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventStatusService eventStatusService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUpdateEventStatusToLive() throws Exception {
        // Given
        EventStatusRequest request = new EventStatusRequest("event-1", true);
        doNothing().when(eventStatusService).updateEventStatus("event-1", true);

        // When & Then
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.status").value("live"))
                .andExpect(jsonPath("$.message").value("Event status updated successfully"));

        verify(eventStatusService).updateEventStatus("event-1", true);
    }

    @Test
    void testUpdateEventStatusWithInvalidInput() throws Exception {
        // Given
        EventStatusRequest request = new EventStatusRequest("", null);

        // When & Then
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetEventStatus() throws Exception {
        // Given
        when(eventStatusService.getEventStatus("event-1"))
                .thenReturn(new EventStatus("event-1", true));

        // When & Then
        mockMvc.perform(get("/events/event-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("event-1"))
                .andExpect(jsonPath("$.status").value("live"));
    }

    @Test
    void testGetEventStatusNotFound() throws Exception {
        // Given
        when(eventStatusService.getEventStatus("non-existent"))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/events/non-existent/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.eventId").value("non-existent"))
                .andExpect(jsonPath("$.status").value("unknown"));
    }

    @Test
    void testGetActiveEventCount() throws Exception {
        // Given
        when(eventStatusService.getActiveEventCount()).thenReturn(5);

        // When & Then
        mockMvc.perform(get("/events/active-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}

