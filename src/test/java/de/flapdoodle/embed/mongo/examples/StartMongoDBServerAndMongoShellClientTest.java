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
package de.flapdoodle.embed.mongo.examples;

import de.flapdoodle.embed.mongo.commands.MongoShellArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.ExecutedMongoShellProcess;
import de.flapdoodle.embed.mongo.transitions.MongoShell;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.TransitionMapping;
import de.flapdoodle.commons.reverse.TransitionWalker;
import de.flapdoodle.commons.reverse.Transitions;
import de.flapdoodle.commons.reverse.transitions.Derive;
import de.flapdoodle.commons.reverse.transitions.Start;
import de.flapdoodle.commons.types.Try;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StartMongoDBServerAndMongoShellClientTest {

	@Test
	public void startMongoShell() {
		Version.Main version= Version.Main.PRODUCTION;
		MongoShellArguments mongoShellArguments=MongoShellArguments.builder()
			.addScriptParameters("var hight=3","var width=2","function multip() { print('area ' + hight * width); }","multip()")
			.build();

		Transitions transitions = MongoShell.instance().transitions(version)
			.replace(Start.to(MongoShellArguments.class).initializedWith(mongoShellArguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoShellProcess> executedShell = runningMongoD.initState(
				StateID.of(ExecutedMongoShellProcess.class))) {

				assertThat(executedShell.current().returnCode()).isEqualTo(0);
			}
		}
	}
}
