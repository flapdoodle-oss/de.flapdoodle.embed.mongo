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
package de.flapdoodle.embed.mongo.examples;

import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;
import java.util.List;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigServerMongoDBTest {

	@Test
	public void configServerOptionVisibleInAdminDb() throws UnknownHostException {
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().transitions(Version.Main.PRODUCTION)
			.replace(Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults()
				.withIsConfigServer(true)))
			.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {
			ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				List<String> arguments = mongo.getDatabase("admin")
					.runCommand(new Document("getCmdLineOpts", 1))
					.getList("argv", String.class);

				assertThat(arguments).contains("--configsvr");
			}
		}
	}
}
