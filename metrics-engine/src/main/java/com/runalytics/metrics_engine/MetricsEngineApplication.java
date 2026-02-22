package com.runalytics.metrics_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class MetricsEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetricsEngineApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
