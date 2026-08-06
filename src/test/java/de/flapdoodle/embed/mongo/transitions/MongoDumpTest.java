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

import de.flapdoodle.embed.mongo.commands.ImmutableMongoDumpArguments;
import de.flapdoodle.embed.mongo.commands.MongoDumpArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.TransitionMapping;
import de.flapdoodle.commons.reverse.TransitionWalker;
import de.flapdoodle.commons.reverse.Transitions;
import de.flapdoodle.commons.reverse.transitions.Derive;
import de.flapdoodle.commons.reverse.transitions.Start;
import de.flapdoodle.commons.types.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MongoDumpTest {
	private static void dump(Version.Main version, MongoDumpArguments mongoDumpArguments, Runnable afterDump) {
		Transitions transitions = MongoDump.instance().transitions(version)
			.replace(Start.to(MongoDumpArguments.class).initializedWith(mongoDumpArguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoDumpProcess> executedDump = runningMongoD.initState(
				StateID.of(ExecutedMongoDumpProcess.class))) {

				System.out.println("-------------------");
				System.out.println("dump executed: "+executedDump.current().returnCode());
				System.out.println("-------------------");
			}

			afterDump.run();
		}
	}

	@Test
	public void dumpToArchive(@TempDir Path temp) {
		Path archive = temp.resolve("foo.archive.gz");

		ImmutableMongoDumpArguments mongoDumpArguments = MongoDumpArguments.builder()
			.verbose(true)
			.archive(archive.toAbsolutePath().toString())
			.gzip(true)
			.build();

		dump(Version.Main.PRODUCTION, mongoDumpArguments, () -> {
			assertThat(archive).exists()
				.isRegularFile();
		});
	}

	@Test
	public void dumpToDirectory(@TempDir Path temp) {
		Path directory = temp.resolve("dump");

		ImmutableMongoDumpArguments mongoDumpArguments = MongoDumpArguments.builder()
			.verbose(true)
			.dir(directory.toAbsolutePath().toString())
			.build();

		dump(Version.Main.PRODUCTION, mongoDumpArguments, () -> {
			assertThat(directory).exists()
				.isDirectory();
		});
	}
}