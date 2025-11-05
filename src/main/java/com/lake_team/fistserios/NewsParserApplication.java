package com.lake_team.fistserios;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling      // keeps your scheduled jobs (already present)
@EnableAsync           // enables @Async for background tasks
public class NewsParserApplication {

    public static void main(String[] args) {
        // Launch JavaFX starter
        JavaFxApp.launchApp(args);
    }
}
