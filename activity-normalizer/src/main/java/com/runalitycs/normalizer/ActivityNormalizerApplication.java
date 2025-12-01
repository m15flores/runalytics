package com.runalitycs.normalizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ActivityNormalizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ActivityNormalizerApplication.class, args);
	}

}
