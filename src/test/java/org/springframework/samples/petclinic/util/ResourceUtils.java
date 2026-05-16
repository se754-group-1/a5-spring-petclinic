package org.springframework.samples.petclinic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ResourceUtils {

	private ResourceUtils() {
	}

	public static String readTestFile(String path) throws IOException {
		ClassLoader classLoader = ResourceUtils.class.getClassLoader();

		try (InputStream inputStream = classLoader.getResourceAsStream(path);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))) {

			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

}