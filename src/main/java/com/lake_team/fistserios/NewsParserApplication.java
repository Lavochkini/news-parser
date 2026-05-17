package com.lake_team.fistserios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableMongoAuditing
public class NewsParserApplication {
    private static String[] savedArgs;
    private ConfigurableApplicationContext springContext;


    public static void main(String[] args) {
        SpringApplication.run(NewsParserApplication.class, args);
    }
}
