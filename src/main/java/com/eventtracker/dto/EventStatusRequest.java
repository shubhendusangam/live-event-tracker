package com.eventtracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "Status is required")
    private Boolean live;

    public boolean isLive() {
        return live != null && live;
    }
}
