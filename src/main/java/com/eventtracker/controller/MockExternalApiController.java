// for testing
package com.eventtracker.controller;


import com.eventtracker.dto.ScoreData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@RequestMapping("/mock-api")
@Slf4j
@ConditionalOnProperty(name = "app.mock-api.enabled", havingValue = "true", matchIfMissing = true)
public class MockExternalApiController {

    private final Random random = new Random();

    @GetMapping("/events/{eventId}/score")
    public ScoreData getScore(@PathVariable String eventId) {
        log.debug("Mock API called for eventId={}", eventId);

        // Simulate some processing time
        try {
            Thread.sleep(100 + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generate random score
        int homeScore = random.nextInt(5);
        int awayScore = random.nextInt(5);
        String score = homeScore + ":" + awayScore;

        return new ScoreData(eventId, score);
    }
}