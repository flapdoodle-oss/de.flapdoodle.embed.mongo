package de.flapdoodle.embed.mongo.types;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPropertiesTest {

	@Test
	void convertEachProperty() {
		Properties properties = System.getProperties();
		SystemProperties testee = SystemProperties.of(properties);

		assertThat(testee.value()).containsOnlyKeys(properties.stringPropertyNames());
	}
}