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
import com.mongodb.client.result.DeleteResult;
import de.flapdoodle.embed.mongo.commands.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.transitions.*;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.reverse.*;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

public class UseCasesTest {

	Version.Main version = Version.Main.V7_0;

	@RegisterExtension
	public static final Recording recording = Recorder.with("UseCases.md", TabSize.spaces(2));

	@Test
	public void startMongoD() {
		recording.begin();
		Transitions transitions = Mongod.instance().transitions(version);

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
			.transitions(version);

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

	@Test
	public void startMongoShell(@TempDir Path tempDir) throws IOException {
		recording.begin();
		String script = "db.mongoShellTest.insertOne( { name: 'a' } );\n"
			+ "db.mongoShellTest.insertOne( { name: 'B' } );\n"
			+ "db.mongoShellTest.insertOne( { name: 'cc' } );\n";

		Path scriptFile = Files.createTempFile(tempDir, "mongoshell", "");
		Files.write(scriptFile, script.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

		ImmutableMongoShellArguments mongoShellArguments = MongoShellArguments.builder()
			.dbName("db")
			.scriptName(scriptFile.toAbsolutePath().toString())
			.build();

		try (TransitionWalker.ReachedState<RunningMongodProcess> mongoD = Mongod.instance().transitions(version)
			.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			// mongo shell support removed with version >=6.x.x
			Transitions mongoShellTransitions = MongoShell.instance().transitions(Version.Main.V5_0)
				.replace(Start.to(MongoShellArguments.class)
					.initializedWith(mongoShellArguments))
				.addAll(Start.to(ServerAddress.class).initializedWith(mongoD.current().getServerAddress()));

			try (TransitionWalker.ReachedState<ExecutedMongoShellProcess> executed = mongoShellTransitions.walker()
				.initState(StateID.of(ExecutedMongoShellProcess.class))) {
				recording.end();
				assertThat(executed.current().returnCode())
					.describedAs("mongo shell was successful")
					.isEqualTo(0);

				String dot = TransitionGraph.edgeGraphAsDot("mongoShell", mongoShellTransitions);
				recording.file("graph.svg", "UseCase-MongoShell.svg", asSvg(dot));

				recording.begin();
			}

			com.mongodb.ServerAddress serverAddress = serverAddress(mongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("db");
				MongoCollection<Document> col = db.getCollection("mongoShellTest");

				ArrayList<String> names = col.find()
					.map(doc -> doc.getString("name"))
					.into(new ArrayList<>());

				assertThat(names).containsExactlyInAnyOrder("a", "B", "cc");
			}
		}
		recording.end();
	}

	@Test
	public void emulateMongoShell() {
		recording.begin();
		Listener listener= Listener.typedBuilder()
			.onStateReached(StateID.of(RunningMongodProcess.class), rm -> {
				com.mongodb.ServerAddress serverAddress = serverAddress(rm.getServerAddress());
				try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
					MongoDatabase db = mongo.getDatabase("db");
					MongoCollection<Document> col = db.getCollection("mongoShellEmulationTest");
					col.insertOne(Document.parse("{name: 'a'}"));
					col.insertOne(Document.parse("{name: 'B'}"));
					col.insertOne(Document.parse("{name: 'cc'}"));
				}
			})
			.onStateTearDown(StateID.of(RunningMongodProcess.class), rm -> {
				com.mongodb.ServerAddress serverAddress = serverAddress(rm.getServerAddress());
				try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
					MongoDatabase db = mongo.getDatabase("db");
					MongoCollection<Document> col = db.getCollection("mongoShellEmulationTest");
					DeleteResult deleted = col.deleteMany(Document.parse("{}"));
					assertThat(deleted.getDeletedCount()).isEqualTo(3);
				}
			})
			.build();

		try (TransitionWalker.ReachedState<RunningMongodProcess> mongoD = Mongod.instance().transitions(version)
			.walker()
			.initState(StateID.of(RunningMongodProcess.class), listener)) {

			com.mongodb.ServerAddress serverAddress = serverAddress(mongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("db");
				MongoCollection<Document> col = db.getCollection("mongoShellEmulationTest");

				ArrayList<String> names = col.find()
					.map(doc -> doc.getString("name"))
					.into(new ArrayList<>());

				assertThat(names).containsExactlyInAnyOrder("a", "B", "cc");
			}
		}
		recording.end();
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
