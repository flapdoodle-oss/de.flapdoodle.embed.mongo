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

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.config.SupportConfig;
import de.flapdoodle.embed.process.types.RunningProcessFactory;
import de.flapdoodle.os.Platform;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.naming.HasLabel;
import org.immutables.value.Value;

@Value.Immutable                  
public abstract class MongodStarter extends MongoServerStarter<RunningMongodProcess> implements HasLabel {

	@Override
	@Value.Default
	public String transitionLabel() {
		return "Start Mongod";
	}

	@Override
	@Value.Default
	public StateID<RunningMongodProcess> destination() {
		return StateID.of(RunningMongodProcess.class);
	}

	@Override
	protected RunningProcessFactory<RunningMongodProcess> factory(long startupTimeout, SupportConfig supportConfig, Platform platform, Net net) {
		return RunningMongodProcess.factory(startupTimeout, supportConfig, platform, net);
	}
	
	public static ImmutableMongodStarter.Builder builder() {
		return ImmutableMongodStarter.builder();
	}

	public static ImmutableMongodStarter withDefaults() {
		return builder().build();
	}
}
