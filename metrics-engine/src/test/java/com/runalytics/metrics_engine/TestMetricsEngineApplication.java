package com.runalytics.metrics_engine;

import org.springframework.boot.SpringApplication;

public class TestMetricsEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(MetricsEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
