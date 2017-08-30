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

import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.process.builder.AbstractBuilder;
import de.flapdoodle.embed.process.builder.IProperty;
import de.flapdoodle.embed.process.builder.TypedProperty;
import de.flapdoodle.embed.process.config.ISupportConfig;

public abstract class AbstractMongoConfigBuilder<T extends IMongoConfig> extends AbstractBuilder<T> {

	protected static final TypedProperty<IFeatureAwareVersion> VERSION = TypedProperty.with("Version", IFeatureAwareVersion.class);
	protected static final TypedProperty<Timeout> TIMEOUT = TypedProperty.with("Timeout", Timeout.class);
	protected static final TypedProperty<Net> NET = TypedProperty.with("Net", Net.class);
	protected static final TypedProperty<IMongoCmdOptions> CMD_OPTIONS = TypedProperty.with("CmdOptions", IMongoCmdOptions.class);
	protected static final TypedProperty<String> PID_FILE = TypedProperty.with("PidFile", String.class);
	protected static final TypedProperty<String> USERNAME = TypedProperty.with("UserName", String.class);
	protected static final TypedProperty<String> PASSWORD = TypedProperty.with("Password", String.class);
	protected static final TypedProperty<String> DBNAME = TypedProperty.with("DbName", String.class);


	public AbstractMongoConfigBuilder() throws UnknownHostException, IOException  {
		timeout().setDefault(new Timeout());
		net().setDefault(new Net());
		cmdOptions().setDefault(new MongoCmdOptionsBuilder().build());
		username().setDefault("");
		password().setDefault("");
		dbName().setDefault("");
	}

	protected IProperty<IFeatureAwareVersion> version() {
		return property(VERSION);
	}

	protected IProperty<Timeout> timeout() {
		return property(TIMEOUT);
	}

	protected IProperty<String> username() {
		return property(USERNAME);
	}

	protected IProperty<String> password() {
		return property(PASSWORD);
	}

	protected IProperty<String> dbName() {
		return property(DBNAME);
	}

	protected IProperty<Net> net() {
		return property(NET);
	}

	protected IProperty<IMongoCmdOptions> cmdOptions() {
		return property(CMD_OPTIONS);
	}

	protected IProperty<String> pidFile() {
		return property(PID_FILE);
	}

	static class ImmutableMongoConfig implements IMongoConfig {

		private final ISupportConfig _supportConfig;

		private final IFeatureAwareVersion _version;
		private final Timeout _timeout;
		private final Net _net;
		private final IMongoCmdOptions _cmdOptions;
		private final String _pidFile;
		private final String _userName;
		private final String _password;

		public ImmutableMongoConfig(ISupportConfig supportConfig, IFeatureAwareVersion version, Net net,
									String userName, String password,
									Timeout timeout, IMongoCmdOptions cmdOptions, String pidFile) {
			super();
			_supportConfig = supportConfig;

			_version = version;
			_net = net;
			_timeout = timeout;
			_cmdOptions = cmdOptions;
			_pidFile = pidFile;
			_userName = userName;
			_password = password;
		}

		@Override
		public IFeatureAwareVersion version() {
			return _version;
		}

		@Override
		public Timeout timeout() {
			return _timeout;
		}

		@Override
		public Net net() {
			return _net;
		}

		@Override
		public IMongoCmdOptions cmdOptions() {
			return _cmdOptions;
		}

		@Override
		public String password() {
			return _password;
		}

		@Override
		public String userName() {
			return _userName;
		}

		@Override
		public String pidFile() {
			return _pidFile;
		}

		@Override
		public ISupportConfig supportConfig() {
			return _supportConfig;
		}
	}
}
