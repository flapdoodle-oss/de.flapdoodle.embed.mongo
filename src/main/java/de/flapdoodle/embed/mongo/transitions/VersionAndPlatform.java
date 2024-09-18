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
import de.flapdoodle.embed.mongo.types.SystemProperties;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.Platform;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

public interface VersionAndPlatform {
	String BASE_URL="de.flapdoodle.embed.mongo.baseUrl";

	@Value.Default
	default Transition<Platform> platform() {
		return Start.to(Platform.class).providedBy(() -> Platform.detect(CommonOS.list()));
	}

	@Value.Default
	default Transition<Distribution> distribution() {
		return Join.given(de.flapdoodle.embed.process.distribution.Version.class).and(Platform.class).state(Distribution.class)
			.deriveBy(Distribution::of)
			.withTransitionLabel("version + platform");
	}

	@Value.Default
	default Transition<DistributionBaseUrl> distributionBaseUrl() {
		return Derive.given(SystemProperties.class)
			.state(DistributionBaseUrl.class)
			.deriveBy(systemProperties -> DistributionBaseUrl.of(systemProperties.value()
				.getOrDefault(BASE_URL, "https://fastdl.mongodb.org")));
	}

	@Value.Auxiliary
	default Transitions versionAndPlatform() {
		return Transitions.from(
			platform(),
			distribution(),
			distributionBaseUrl()
		);
	}
}
