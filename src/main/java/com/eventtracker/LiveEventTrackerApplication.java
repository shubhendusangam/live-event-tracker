package com.eventtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LiveEventTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveEventTrackerApplication.class, args);
    }

}
