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

import de.flapdoodle.commons.checks.Preconditions;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.embed.process.types.ProcessArguments;
import de.flapdoodle.os.Platform;
import de.flapdoodle.commons.reverse.State;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.StateLookup;
import de.flapdoodle.commons.reverse.naming.HasLabel;
import org.immutables.value.Value;

import java.util.List;
import java.util.Set;

@Value.Immutable
public abstract class MongodProcessArguments implements CommandProcessArguments<MongodArguments>, HasLabel {

	@Override
	@Value.Auxiliary
	public String transitionLabel() {
		return "Create mongod arguments";
	}

	@Override
	@Value.Default
	public StateID<ProcessArguments> destination() {
		return StateID.of(ProcessArguments.class);
	}

	@Override
	@Value.Default
	public StateID<MongodArguments> arguments() {
		return StateID.of(MongodArguments.class);
	}

	@Value.Default
	public StateID<Platform> platform() {
		return StateID.of(Platform.class);
	}

	@Value.Default
	public StateID<Version> version() {
		return StateID.of(Version.class);
	}

	@Value.Default
	public StateID<Net> net() {
		return StateID.of(Net.class);
	}

	@Value.Default
	public StateID<DatabaseDir> databaseDir() {
		return StateID.of(DatabaseDir.class);
	}

	@Override
	@Value.Auxiliary
	public Set<StateID<?>> sources() {
		return StateID.setOf(arguments(), platform(), version(), net(), databaseDir());
	}

	@Override
	public State<ProcessArguments> result(StateLookup lookup) {
		MongodArguments arguments = lookup.of(arguments());
		Platform platform = lookup.of(platform());
		Version version = lookup.of(version());
		Preconditions.checkArgument(version instanceof IFeatureAwareVersion,"invalid type: %s does not implement %s",version, IFeatureAwareVersion.class);
		IFeatureAwareVersion featureAwareVersion = (IFeatureAwareVersion) version;
		Net net = lookup.of(net());
		DatabaseDir databaseDir=lookup.of(databaseDir());

		List<String> commandLine = arguments.asArguments(platform, featureAwareVersion, net, databaseDir);
		return State.of(ProcessArguments.of(commandLine));
	}

	public static ImmutableMongodProcessArguments withDefaults() {
		return builder().build();
	}

	public static ImmutableMongodProcessArguments.Builder builder() {
		return ImmutableMongodProcessArguments.builder();
	}
}
