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

import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.packageresolver.PlatformPackageResolver;
import de.flapdoodle.embed.mongo.types.DistributionBaseUrl;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.PackageResolver;
import de.flapdoodle.commons.reverse.State;
import de.flapdoodle.commons.reverse.StateID;
import de.flapdoodle.commons.reverse.StateLookup;
import de.flapdoodle.commons.reverse.Transition;
import de.flapdoodle.commons.reverse.naming.HasLabel;
import org.immutables.value.Value;

import java.util.Set;
import java.util.function.Function;

@Value.Immutable
public abstract class PackageOfCommandDistribution implements Transition<Package>, HasLabel {

	@Override public String transitionLabel() {
		return "Package of Command-Distribution";
	}

	@Value.Default
	protected Function<Command, PackageResolver> commandPackageResolver() {
		return PlatformPackageResolver::new;
	}

	@Value.Auxiliary
	protected Package packageOf(Command command, Distribution distribution, DistributionBaseUrl baseUrl) {
		Package relativePackage = commandPackageResolver().apply(command).packageFor(distribution);
		return Package.builder()
			.archiveType(relativePackage.archiveType())
			.fileSet(relativePackage.fileSet())
			.url(baseUrl.value() /*"https://fastdl.mongodb.org"*/+relativePackage.url())
			.hint(relativePackage.hint())
			.build();
	}

	@Value.Default
	public StateID<Command> command() {
		return StateID.of(Command.class);
	}

	@Value.Default
	public StateID<Distribution> distribution() {
		return StateID.of(Distribution.class);
	}

	@Value.Default
	public StateID<DistributionBaseUrl> distributionBaseUrl() {
		return StateID.of(DistributionBaseUrl.class);
	}

	@Override
	@Value.Default
	public StateID<Package> destination() {
		return StateID.of(Package.class);
	}

	@Override
	public Set<StateID<?>> sources() {
		return StateID.setOf(command(), distribution(), distributionBaseUrl());
	}

	@Override
	public State<Package> result(StateLookup lookup) {
		Command command = lookup.of(command());
		Distribution distribution = lookup.of(distribution());
		DistributionBaseUrl baseUrl = lookup.of(distributionBaseUrl());
		return State.of(packageOf(command,distribution, baseUrl));
	}

	public static ImmutablePackageOfCommandDistribution.Builder builder() {
		return ImmutablePackageOfCommandDistribution.builder();
	}

	public static ImmutablePackageOfCommandDistribution withDefaults() {
		return builder().build();
	}
}
