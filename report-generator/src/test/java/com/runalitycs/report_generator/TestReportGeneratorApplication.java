package com.runalitycs.report_generator;

import org.springframework.boot.SpringApplication;

public class TestReportGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReportGeneratorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
