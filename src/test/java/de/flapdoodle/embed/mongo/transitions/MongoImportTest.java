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

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.ImmutableMongoImportArguments;
import de.flapdoodle.embed.mongo.commands.MongoImportArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionMapping;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.types.Try;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.function.Consumer;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

class MongoImportTest {
	
	@Test
	public void importPrimerDataset() throws UnknownHostException {
		String jsonFile = Resources.getResource("primer-dataset.json").getFile();

		runImport(Version.Main.PRODUCTION, importJson(jsonFile),
			onTestCollection(col -> assertThat(col.countDocuments()).isEqualTo(0)),
			onTestCollection(col -> assertThat(col.countDocuments()).isEqualTo(5000)));
	}

	@Test
	public void importSampleDataset() throws UnknownHostException {
		String jsonFile = Resources.getResource("sample.json").getFile();

		runImport(Version.Main.PRODUCTION, importJson(jsonFile),
			onTestCollection(col -> assertThat(col.countDocuments()).isEqualTo(0)),
			onTestCollection(col -> {
				ArrayList<Object> names = Lists.newArrayList(col.find().map(doc -> doc.get("name")));

				assertThat(names).containsExactlyInAnyOrder("Cassandra","HBase","MongoDB");
			}));
	}

	private static void runImport(
		Version.Main version,
		MongoImportArguments mongoImportArguments,
		Consumer<ServerAddress> beforeImport,
		Consumer<ServerAddress> afterImport
	) throws UnknownHostException {

		Transitions transitions = MongoImport.instance().transitions(version)
			.replace(Start.to(MongoImportArguments.class).initializedWith(mongoImportArguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			ServerAddress serverAddress = runningMongoD.current().getServerAddress();

			beforeImport.accept(serverAddress);

			try (TransitionWalker.ReachedState<ExecutedMongoImportProcess> executedDump = runningMongoD.initState(
				StateID.of(ExecutedMongoImportProcess.class))) {
				System.out.println("import return code: " + executedDump.current().returnCode());

				assertThat(executedDump.current().returnCode())
					.describedAs("import successful")
					.isEqualTo(0);
			}

			afterImport.accept(serverAddress);
		}
	}

	private static ImmutableMongoImportArguments importJson(String jsonFile) {
		return MongoImportArguments.builder()
			.databaseName("importDatabase")
			.collectionName("importCollection")
			.upsertDocuments(true)
			.dropCollection(true)
			.isJsonArray(true)
			.importFile(jsonFile)
			.build();
	}

	private static Consumer<ServerAddress> onTestCollection(Consumer<MongoCollection<Document>> onCollection) {
		return serverAddress -> {
			com.mongodb.ServerAddress serverAddress1 = serverAddress(serverAddress);
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress1)) {
				MongoDatabase db = mongo.getDatabase("importDatabase");
				MongoCollection<Document> col = db.getCollection("importCollection");

				onCollection.accept(col);
			}
		};
	}
}