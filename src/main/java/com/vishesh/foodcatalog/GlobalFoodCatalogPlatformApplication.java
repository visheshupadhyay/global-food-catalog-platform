package com.vishesh.foodcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Starts the Spring Boot application. */
@SpringBootApplication
@EnableScheduling
public class GlobalFoodCatalogPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(GlobalFoodCatalogPlatformApplication.class, args);
	}

}
