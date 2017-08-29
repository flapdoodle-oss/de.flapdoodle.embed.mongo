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
package de.flapdoodle.embed.mongo.tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongosExecutable;
import de.flapdoodle.embed.mongo.MongosProcess;
import de.flapdoodle.embed.mongo.MongosStarter;
import de.flapdoodle.embed.mongo.config.IMongosConfig;
import de.flapdoodle.embed.mongo.config.MongosConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * This class encapsulates everything that would be needed to do embedded
 * MongoDB with sharding testing.
 */
public class MongosForTestsFactory {

	private static Logger logger = LoggerFactory.getLogger(MongosForTestsFactory.class
			.getName());

	public static MongosForTestsFactory with(final IFeatureAwareVersion version)
			throws IOException {
		return new MongosForTestsFactory(version);
	}

	private final MongosExecutable mongoConfigExecutable;

	private final MongosProcess mongoConfigProcess;

	private final MongosExecutable mongosExecutable;

	private final MongosProcess mongosProcess;

	/**
	 * Create the testing utility using the latest production version of
	 * MongoDB.
	 * 
	 * @throws IOException
	 */
	public MongosForTestsFactory() throws IOException {
		this(Version.Main.PRODUCTION);
	}

	/**
	 * Create the testing utility using the specified version of MongoDB.
	 * 
	 * @param version
	 *            version of MongoDB.
	 */
	public MongosForTestsFactory(final IFeatureAwareVersion version) throws IOException {

		final MongosStarter mongoConfigRuntime = MongosStarter.getInstance(new RuntimeConfigBuilder()
			.defaultsWithLogger(Command.MongoS,logger)
			.build());

		int configServerPort = 27019;
		int mongosPort = 27017;
		IMongosConfig config = new MongosConfigBuilder()
			.version(version)
			.net(new Net(configServerPort, Network.localhostIsIPv6()))
			.configDB("testDB")
			.build();
		
		mongoConfigExecutable = mongoConfigRuntime.prepare(config);
		mongoConfigProcess = mongoConfigExecutable.start();

		final MongosStarter runtime = MongosStarter.getInstance(new RuntimeConfigBuilder()
			.defaultsWithLogger(Command.MongoS, logger)
			.build());
		
		config = new MongosConfigBuilder()
			.version(version)
			.net(new Net(mongosPort, Network.localhostIsIPv6()))
			.configDB(Network.getLocalHost().getHostName() + ":" + configServerPort)
			.build();
		
		mongosExecutable = runtime.prepare(config);
		mongosProcess = mongosExecutable.start();
	}

	/**
	 * Creates a new Mongo connection.
	 * 
	 * @throws MongoException
	 * @throws UnknownHostException
	 */
	public Mongo newMongo() throws UnknownHostException, MongoException {
		return new MongoClient(new ServerAddress(mongosProcess.getConfig().net().getServerAddress(),
				mongosProcess.getConfig().net().getPort()));
	}
	
	/**
	 * Creates a new DB with unique name for connection.
	 */
	public DB newDB(Mongo mongo) {
		return mongo.getDB(UUID.randomUUID().toString());
	}

	/**
	 * Cleans up the resources created by the utility.
	 */
	public void shutdown() {
		mongosProcess.stop();
		mongosExecutable.stop();
	}
}
