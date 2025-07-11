package com.eventtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class
})
class LiveEventTrackerApplicationTests {

    @Test
    void contextLoads() {
    }

}
