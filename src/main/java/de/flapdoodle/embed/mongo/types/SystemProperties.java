package de.flapdoodle.embed.mongo.types;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value.Immutable
public abstract class SystemProperties {
	public abstract Map<String, String> value();

	public static SystemProperties of(Properties properties) {
		return of(properties.stringPropertyNames().stream()
			.collect(Collectors.toMap(Function.identity(), properties::getProperty)));
	}

	public static SystemProperties of(Map<String, String> properties) {
		return ImmutableSystemProperties.builder()
			.value(properties)
			.build();
	}
}
