package com.runalitycs.normalizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

import java.time.Clock;

@SpringBootApplication
@EnableKafka
public class ActivityNormalizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActivityNormalizerApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
