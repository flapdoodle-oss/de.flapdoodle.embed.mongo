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
package de.flapdoodle.embed.mongo.doc;

import com.google.common.io.Resources;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.ImmutableMongoImportArguments;
import de.flapdoodle.embed.mongo.commands.MongoImportArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.ExecutedMongoImportProcess;
import de.flapdoodle.embed.mongo.transitions.MongoImport;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionMapping;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.graph.TransitionGraph;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.types.Try;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

public class UseCasesTest {

	@RegisterExtension
	public static final Recording recording = Recorder.with("UseCases.md", TabSize.spaces(2));

	@Test
	public void startMongoD() {
		recording.begin();
		Transitions transitions = Mongod.instance().transitions(Version.Main.PRODUCTION);

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				recording.end();
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));
				assertThat(col.countDocuments()).isEqualTo(1L);
				recording.begin();
			}
		}

		recording.end();
		String dot = TransitionGraph.edgeGraphAsDot("mongod", transitions);
		recording.file("graph.svg", "UseCase-Mongod.svg", asSvg(dot));
	}

	@Test
	public void startMongoDWithPersistentDatabase(@TempDir Path tempDir) throws IOException {
		Path persistentDir = tempDir.resolve("mongo-db-" + UUID.randomUUID());
		Files.createDirectory(persistentDir);

		recording.begin();
		Transitions transitions = Mongod.instance()
			.withDatabaseDir(Start.to(DatabaseDir.class)
				.initializedWith(DatabaseDir.of(persistentDir)))
			.transitions(Version.Main.PRODUCTION);

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));
				assertThat(col.countDocuments()).isEqualTo(1L);
			}
		}

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				assertThat(col.countDocuments()).isEqualTo(1L);
			}
		}

		recording.end();
		String dot = TransitionGraph.edgeGraphAsDot("mongod", transitions);
		recording.file("graph.svg", "UseCase-Mongod-PersistentDir.svg", asSvg(dot));
	}

	@Test
	public void startMongoImport() {
		recording.begin();
		MongoImportArguments arguments = MongoImportArguments.builder()
			.databaseName("importDatabase")
			.collectionName("importCollection")
			.importFile(Resources.getResource("sample.json").getFile())
			.isJsonArray(true)
			.upsertDocuments(true)
			.build();

		Version.Main version = Version.Main.PRODUCTION;

		try (TransitionWalker.ReachedState<RunningMongodProcess> mongoD = Mongod.instance().transitions(version)
			.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			Transitions mongoImportTransitions = MongoImport.instance()
				.transitions(version)
				.replace(Start.to(MongoImportArguments.class).initializedWith(arguments))
				.addAll(Start.to(ServerAddress.class).initializedWith(mongoD.current().getServerAddress()));

			try (TransitionWalker.ReachedState<ExecutedMongoImportProcess> executed = mongoImportTransitions.walker()
				.initState(StateID.of(ExecutedMongoImportProcess.class))) {
				recording.end();
				assertThat(executed.current().returnCode())
					.describedAs("mongo import was successful")
					.isEqualTo(0);

				String dot = TransitionGraph.edgeGraphAsDot("mongoImport", mongoImportTransitions);
				recording.file("graph.svg", "UseCase-MongoImport.svg", asSvg(dot));

				recording.begin();
			}

			com.mongodb.ServerAddress serverAddress = serverAddress(mongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("importDatabase");
				MongoCollection<Document> col = db.getCollection("importCollection");

				ArrayList<String> names = col.find()
					.map(doc -> doc.getString("name"))
					.into(new ArrayList<>());

				assertThat(names).containsExactlyInAnyOrder("Cassandra", "HBase", "MongoDB");
			}
		}
		recording.end();
	}

	@Test
	public void startMongoImportAsOneTransition() {
		recording.begin();
		ImmutableMongoImportArguments arguments = MongoImportArguments.builder()
			.databaseName("importDatabase")
			.collectionName("importCollection")
			.importFile(Resources.getResource("sample.json").getFile())
			.isJsonArray(true)
			.upsertDocuments(true)
			.build();

		Version.Main version = Version.Main.PRODUCTION;

		Transitions mongoImportTransitions = MongoImport.instance().transitions(version)
			.replace(Start.to(MongoImportArguments.class).initializedWith(arguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> mongoD = mongoImportTransitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoImportProcess> running = mongoD.initState(StateID.of(ExecutedMongoImportProcess.class))) {
				recording.end();
				assertThat(running.current().returnCode())
					.describedAs("import successful")
					.isEqualTo(0);
				recording.begin();
			}

			com.mongodb.ServerAddress serverAddress = serverAddress(mongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("importDatabase");
				MongoCollection<Document> col = db.getCollection("importCollection");

				ArrayList<String> names = col.find()
					.map(doc -> doc.getString("name"))
					.into(new ArrayList<>());

				assertThat(names).containsExactlyInAnyOrder("Cassandra", "HBase", "MongoDB");
			}
		}
		recording.end();

		String dot = TransitionGraph.edgeGraphAsDot("mongoimport", mongoImportTransitions);
		recording.file("graph.svg", "UseCase-Mongod-MongoImport.svg", asSvg(dot));
	}


	private byte[] asSvg(String dot) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			Graphviz.fromString(dot)
//				.width(3200)
				.render(Format.SVG_STANDALONE)
				.toOutputStream(os);
			return os.toByteArray();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
