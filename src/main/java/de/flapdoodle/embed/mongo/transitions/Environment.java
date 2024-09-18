package de.flapdoodle.embed.mongo.transitions;

import de.flapdoodle.embed.mongo.types.SystemEnv;
import de.flapdoodle.embed.mongo.types.SystemProperties;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

public interface Environment {
	@Value.Default
	default Transition<SystemEnv> systemEnv() {
		return Start.to(SystemEnv.class)
			.providedBy(() -> SystemEnv.of(System.getenv()));
	}

	@Value.Default
	default Transition<SystemProperties> systemProperties() {
		return Start.to(SystemProperties.class)
			.providedBy(() -> SystemProperties.of(System.getProperties()));
	}

	@Value.Auxiliary
	default Transitions environment() {
		return Transitions.from(
			systemEnv(),
			systemProperties()
		);
	}
}
