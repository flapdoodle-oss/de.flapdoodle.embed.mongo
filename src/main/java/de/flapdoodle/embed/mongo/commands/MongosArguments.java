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
package de.flapdoodle.embed.mongo.commands;

import de.flapdoodle.commons.checks.Preconditions;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.os.Platform;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public abstract class MongosArguments {

	@Value.Default
	public boolean verbose() {
		return false;
	}

	public abstract Optional<String> configDB();

	public abstract Optional<String> replicaSet();

	@Value.Auxiliary
	public List<String> asArguments(
		Platform platform,
		IFeatureAwareVersion version,
		Net net
	) {
		return getCommandLine(this, version, net);
	}

	public static ImmutableMongosArguments.Builder builder() {
		return ImmutableMongosArguments.builder();
	}

	public static ImmutableMongosArguments defaults() {
		return builder().build();
	}

	private static List<String> getCommandLine(
		MongosArguments config,
		IFeatureAwareVersion version,
		Net net
	) {
		Arguments.Builder ret = Arguments.builder();

		ret.addIf(!version.enabled(Feature.NO_CHUNKSIZE_ARG),"--chunkSize","1");
		ret.addIf(config.verbose(),"-v");
		ret.addIf(!version.enabled(Feature.NO_HTTP_INTERFACE_ARG),"--nohttpinterface");

		ret.add("--port");
		ret.add("" + net.getPort());
		ret.addIf(net.isIpv6(), "--ipv6");

		if (config.configDB().isPresent()) {
			ret.add("--configdb");
			if (version.enabled(Feature.MONGOS_CONFIGDB_SET_STYLE)) {
				Preconditions.checkArgument(config.replicaSet().isPresent(),"you must define a replicaSet");
				ret.add(config.replicaSet().get()+"/"+config.configDB().get());
			} else {
				ret.add(config.configDB().get());
			}
		}

		return ret.build();
	}

}
