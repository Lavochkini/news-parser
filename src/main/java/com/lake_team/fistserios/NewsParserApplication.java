package com.lake_team.fistserios;

//import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsParserApplication {

	public static void main(String[] args) {
		// Виклик стартера JavaFX
		JavaFxApp.launchApp(args);
	}
}
