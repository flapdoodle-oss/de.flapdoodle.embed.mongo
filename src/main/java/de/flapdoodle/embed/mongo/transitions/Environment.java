/*
 * Copyright (C) 2011
 *   Michael Mosmann <michael@mosmann.de>
 *   Martin Jöhren <m.joehren@googlemail.com>
 *
 * with contributions from
 * 	konstantin-ba@github,Archimedes Trajano	(trajano@github)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.flapdoodle.embed.mongo.transitions;

import de.flapdoodle.embed.mongo.types.DistributionBaseUrl;
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
