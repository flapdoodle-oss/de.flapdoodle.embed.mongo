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

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.embed.mongo.util.FileUtils;
import de.flapdoodle.commons.reverse.Listener;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.TransitionWalker;
import de.flapdoodle.commons.types.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotDbFilesTest {

	@Test
	public void snapshotDbFiles(@TempDir Path destination) {
		Listener listener = Listener.typedBuilder()
			.onStateTearDown(StateID.of(DatabaseDir.class), databaseDir -> {
				Try.run(() -> FileUtils.copyDirectory(databaseDir.value(), destination));
			})
			.build();

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().transitions(Version.Main.PRODUCTION).walker()
			.initState(StateID.of(RunningMongodProcess.class), listener)) {

		}

		assertThat(destination)
			.isDirectory()
			.isDirectoryContaining(path -> path.getFileName().toString().startsWith("WiredTiger.lock"));
	}
}
