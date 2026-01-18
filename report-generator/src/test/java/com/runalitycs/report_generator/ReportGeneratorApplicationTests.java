package com.runalitycs.report_generator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReportGeneratorApplicationTests {

	@Test
	void contextLoads() {
	}

}
