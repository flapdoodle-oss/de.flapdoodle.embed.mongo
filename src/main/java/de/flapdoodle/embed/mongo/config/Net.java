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
package de.flapdoodle.embed.mongo.config;

import org.immutables.value.Value;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

@Value.Immutable
public abstract class Net {

	public abstract Optional<String> getBindIp();

	public abstract int getPort();

	public abstract boolean isIpv6();

	@Value.Auxiliary
	public Net withPort(int port) {
		return ImmutableNet.copyOf(this).withPort(port);
	}

	@Value.Auxiliary
	public InetAddress getServerAddress() throws UnknownHostException {
		if (getBindIp().isPresent()) {
			return InetAddress.getByName(getBindIp().get());
		}
		return de.flapdoodle.commons.net.Net.getLocalHost();
	}

	public static Net of(String bindIp, int port, boolean ipv6) {
		return builder()
			.bindIp(bindIp)
			.port(port)
			.isIpv6(ipv6)
			.build();
	}

	public static ImmutableNet.Builder builder() {
		return ImmutableNet.builder();
	}

	public static Net defaults() {
		try {
			InetAddress localHost = de.flapdoodle.commons.net.Net.getLocalHost();
			int freeServerPort = de.flapdoodle.commons.net.Net.freeServerPort(localHost);
			boolean localhostIsIPv6 = de.flapdoodle.commons.net.Net.localhostIsIPv6();

			return builder()
				.port(freeServerPort)
				.isIpv6(localhostIsIPv6)
				.build();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}