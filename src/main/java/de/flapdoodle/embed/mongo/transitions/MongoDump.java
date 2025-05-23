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

import de.flapdoodle.embed.mongo.commands.MongoDumpArguments;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

@Value.Immutable
public class MongoDump implements Environment, WorkspaceDefaults, VersionAndPlatform, ProcessDefaults, CommandName, ExtractFileSet {
	public Transitions transitions(de.flapdoodle.embed.process.distribution.Version version) {
		return workspaceDefaults()
			.addAll(environment())
			.addAll(versionAndPlatform())
			.addAll(processDefaults())
			.addAll(commandNames())
			.addAll(extractFileSet())
			.addAll(
				Start.to(Command.class).initializedWith(Command.MongoDump).withTransitionLabel("provide Command"),
				Start.to(de.flapdoodle.embed.process.distribution.Version.class).initializedWith(version),
				Start.to(MongoDumpArguments.class).initializedWith(MongoDumpArguments.defaults()),
				MongoDumpProcessArguments.withDefaults(),
				ExecutedMongoDumpProcess.withDefaults()
			);
	}

	@Value.Auxiliary
	public TransitionWalker.ReachedState<ExecutedMongoDumpProcess> start(Version version, Listener... listener) {
		return transitions(version)
			.walker()
			.initState(StateID.of(ExecutedMongoDumpProcess.class), listener);
	}

	@Value.Auxiliary
	public TransitionWalker.ReachedState<ExecutedMongoDumpProcess> start(Version version, Collection<Listener> listener) {
		return transitions(version)
			.walker()
			.initState(StateID.of(ExecutedMongoDumpProcess.class), listener);
	}

	@Value.Auxiliary
	public void start(Version version, Consumer<ExecutedMongoDumpProcess> withRunningMongoDump, Listener... listener) {
		start(version, withRunningMongoDump, Arrays.asList(listener));
	}

	@Value.Auxiliary
	public void start(Version version,  Consumer<ExecutedMongoDumpProcess> withRunningMongoDump, Collection<Listener> listener) {
		try(TransitionWalker.ReachedState<ExecutedMongoDumpProcess> state = start(version, listener)) {
			withRunningMongoDump.accept(state.current());
		}
	}
	
	public static ImmutableMongoDump instance() {
		return builder().build();
	}

	public static ImmutableMongoDump.Builder builder() {
		return ImmutableMongoDump.builder();
	}

}
