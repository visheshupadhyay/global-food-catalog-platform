package com.vishesh.foodcatalog;

import org.springframework.boot.SpringApplication;

public class TestGlobalFoodCatalogPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.from(GlobalFoodCatalogPlatformApplication::main).with(TestcontainersConfiguration.class)
				.run(args);
	}

}
