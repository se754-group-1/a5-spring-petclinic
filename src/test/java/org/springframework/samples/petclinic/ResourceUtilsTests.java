package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.util.ResourceUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceUtilsTests {

	@Test
	void readTestFileReadsClasspathResource() throws IOException {
		String content = ResourceUtils.readTestFile("db/mysql/data.sql");

		assertThat(content).contains("INSERT IGNORE INTO owners");
	}

}