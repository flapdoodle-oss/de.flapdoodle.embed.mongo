/**
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

import java.io.IOException;
import java.net.UnknownHostException;

import de.flapdoodle.embed.process.builder.TypedProperty;
import de.flapdoodle.embed.process.distribution.IVersion;

public class MongosConfigBuilder extends AbstractMongoConfigBuilder<IMongosConfig> {

	protected static final TypedProperty<String> CONFIG_DB = TypedProperty.with("ConfigDB", String.class);

	public MongosConfigBuilder() throws UnknownHostException, IOException {
		super();
	}

	public MongosConfigBuilder version(IVersion version) {
		version().set(version);
		return this;
	}

	public MongosConfigBuilder timeout(Timeout timeout) {
		timeout().set(timeout);
		return this;
	}

	public MongosConfigBuilder net(Net net) {
		net().set(net);
		return this;
	}

	public MongosConfigBuilder configDB(String configDB) {
		set(CONFIG_DB, configDB);
		return this;
	}

	@Override
	public IMongosConfig build() {
		IVersion version = version().get();
		Net net = net().get();
		Timeout timeout = timeout().get();
		String configDB = get(CONFIG_DB);

		return new ImmutableMongosConfig(version, net, timeout, configDB);
	}

	static class ImmutableMongosConfig extends ImmutableMongoConfig implements IMongosConfig {

		private final String _configDB;

		public ImmutableMongosConfig(IVersion version, Net net, Timeout timeout, String configDB) {
			super(version, net, timeout);
			_configDB = configDB;
		}

		@Override
		public String getConfigDB() {
			return _configDB;
		}

	}
}