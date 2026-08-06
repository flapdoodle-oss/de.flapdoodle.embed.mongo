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

import de.flapdoodle.embed.mongo.commands.MongoToolsArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.process.types.ProcessArguments;
import de.flapdoodle.commons.reverse.State;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.StateLookup;
import org.immutables.value.Value;

import java.util.List;
import java.util.Set;

public abstract class MongoToolsProcessArguments<T extends MongoToolsArguments> implements CommandProcessArguments<T> {
	@Override
	@Value.Default
	public StateID<ProcessArguments> destination() {
		return StateID.of(ProcessArguments.class);
	}

	@Override
	public abstract StateID<T> arguments();

	@Value.Default
	public StateID<ServerAddress> serverAddress() {
		return StateID.of(ServerAddress.class);
	}

	@Override
	@Value.Auxiliary
	public Set<StateID<?>> sources() {
		return StateID.setOf(arguments(), serverAddress());
	}

	@Override
	public State<ProcessArguments> result(StateLookup lookup) {
		T arguments = lookup.of(arguments());
		ServerAddress serverAddress = lookup.of(serverAddress());

		List<String> commandLine = arguments.asArguments(serverAddress);
		return State.of(ProcessArguments.of(commandLine));
	}
}
