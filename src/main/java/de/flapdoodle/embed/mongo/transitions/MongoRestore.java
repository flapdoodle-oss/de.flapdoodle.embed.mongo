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

import de.flapdoodle.embed.mongo.commands.MongoRestoreArguments;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

@Value.Immutable
public class MongoRestore implements Environment, WorkspaceDefaults, VersionAndPlatform, ProcessDefaults, CommandName, ExtractFileSet {

	public Transitions transitions(de.flapdoodle.embed.process.distribution.Version version) {
		return workspaceDefaults()
			.addAll(environment())
			.addAll(versionAndPlatform())
			.addAll(processDefaults())
			.addAll(commandNames())
			.addAll(extractFileSet())
			.addAll(
				Start.to(Command.class).initializedWith(Command.MongoRestore).withTransitionLabel("provide Command"),
				Start.to(de.flapdoodle.embed.process.distribution.Version.class).initializedWith(version),
				Start.to(MongoRestoreArguments.class).initializedWith(MongoRestoreArguments.defaults()),
				MongoRestoreProcessArguments.withDefaults(),
				ExecutedMongoRestoreProcess.withDefaults()
			);
	}

	public TransitionWalker.ReachedState<ExecutedMongoRestoreProcess> start(Version version) {
		return transitions(version)
			.walker()
			.initState(StateID.of(ExecutedMongoRestoreProcess.class));
	}

	public static ImmutableMongoRestore instance() {
		return builder().build();
	}

	public static ImmutableMongoRestore.Builder builder() {
		return ImmutableMongoRestore.builder();
	}
}
