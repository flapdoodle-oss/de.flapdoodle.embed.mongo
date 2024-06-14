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

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.embed.mongo.commands.MongoShellArguments;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.packageresolver.FeatureSetResolver;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

@Value.Immutable
public class MongoShell implements WorkspaceDefaults, VersionAndPlatform, ProcessDefaults, CommandName, ExtractFileSet {

	public Transitions transitions(de.flapdoodle.embed.process.distribution.Version version) {
		boolean hasMongoShellBinary = FeatureSetResolver.defaultInstance().featuresOf(version).enabled(Feature.HAS_MONGO_SHELL_BINRAY);
		Preconditions.checkArgument(hasMongoShellBinary,"distribution for "+version+" does not provide a mongo shell binary");

		return workspaceDefaults()
			.addAll(versionAndPlatform())
			.addAll(processDefaults())
			.addAll(commandNames())
			.addAll(extractFileSet())
			.addAll(
				Start.to(Command.class).initializedWith(Command.Mongo).withTransitionLabel("provide Command"),
				Start.to(de.flapdoodle.embed.process.distribution.Version.class).initializedWith(version),
				Start.to(MongoShellArguments.class).initializedWith(MongoShellArguments.defaults()),
				MongoShellProcessArguments.withDefaults(),
				ExecutedMongoShellProcess.withDefaults()
			);
	}

	public TransitionWalker.ReachedState<ExecutedMongoShellProcess> start(Version version) {
		return transitions(version)
			.walker()
			.initState(StateID.of(ExecutedMongoShellProcess.class));
	}

	public static ImmutableMongoShell instance() {
		return builder().build();
	}

	public static ImmutableMongoShell.Builder builder() {
		return ImmutableMongoShell.builder();
	}
}
