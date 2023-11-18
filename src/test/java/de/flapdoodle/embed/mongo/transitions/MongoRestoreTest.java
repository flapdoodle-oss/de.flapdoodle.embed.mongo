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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.client.*;
import de.flapdoodle.embed.mongo.commands.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionMapping;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.types.Try;
import org.bson.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;

class MongoRestoreTest {
	@Test
	public void dumpAndRestoreFromDirectory(@TempDir Path temp) throws UnknownHostException {
		Version.Main version = Version.Main.PRODUCTION;
		Path directory = temp.resolve("dump");

		ImmutableMongoDumpArguments mongoDumpArguments = MongoDumpArguments.builder()
			.verbose(true)
			.databaseName("testdb")
			.collectionName("testcol")
			.dir(directory.toAbsolutePath().toString())
			.build();

		ImmutableMongoRestoreArguments mongoRestoreArguments = MongoRestoreArguments.builder()
			.verbose(true)
			.dir(directory.toAbsolutePath().toString())
			.build();

		String name= UUID.randomUUID().toString();

		dumpAndRestore(
			version,
			mongoDumpArguments,
			mongoRestoreArguments,
			onTestCollection(col -> col.insertOne(new Document(ImmutableMap.of("name",name)))),
			onTestCollection(col -> {
				assertThat(col.countDocuments()).isEqualTo(1);
				col.deleteMany(Document.parse("{}"));
				assertThat(col.countDocuments()).isEqualTo(0);
			}),
			onTestCollection(col -> {
				FindIterable<Document> documents = col.find(Document.parse("{}"));
				String docName = documents.map(doc -> doc.get("name", String.class)).first();
				assertThat(docName).isEqualTo(name);

				assertThat(col.countDocuments()).isEqualTo(1);
			}));
	}

	@Test
	public void dumpAndRestoreFromArchive(@TempDir Path temp) throws UnknownHostException {
		Version.Main version = Version.Main.PRODUCTION;
		Path archive = temp.resolve("archive.gz");

		ImmutableMongoDumpArguments mongoDumpArguments = MongoDumpArguments.builder()
			.verbose(true)
			.databaseName("testdb")
			.collectionName("testcol")
			.archive(archive.toAbsolutePath().toString())
			.build();

		ImmutableMongoRestoreArguments mongoRestoreArguments = MongoRestoreArguments.builder()
			.verbose(true)
			.archive(archive.toAbsolutePath().toString())
			.build();

		String name= UUID.randomUUID().toString();

		dumpAndRestore(
			version,
			mongoDumpArguments,
			mongoRestoreArguments,
			onTestCollection(col -> col.insertOne(new Document(ImmutableMap.of("name",name)))),
			onTestCollection(col -> {
				assertThat(col.countDocuments()).isEqualTo(1);
				col.deleteMany(Document.parse("{}"));
				assertThat(col.countDocuments()).isEqualTo(0);
			}),
			onTestCollection(col -> {
				FindIterable<Document> documents = col.find(Document.parse("{}"));
				String docName = documents.map(doc -> doc.get("name", String.class)).first();
				assertThat(docName).isEqualTo(name);

				assertThat(col.countDocuments()).isEqualTo(1);
			}));
	}

	@Test
	public void restoreDump() throws UnknownHostException {
		final String dumpLocation = Thread.currentThread().getContextClassLoader().getResource("dump").getFile();

		Version.Main version = Version.Main.PRODUCTION;
		ImmutableMongoRestoreArguments mongoRestoreArguments = MongoRestoreArguments.builder()
			.verbose(true)
			.dropCollection(true)
			.dir(dumpLocation)
			.build();

		Transitions transitions = MongoRestore.instance().transitions(version)
			.replace(Start.to(MongoRestoreArguments.class).initializedWith(mongoRestoreArguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoRestoreProcess> executedRestore = runningMongoD.initState(
				StateID.of(ExecutedMongoRestoreProcess.class))) {

				System.out.println("-------------------");
				System.out.println("restore done: "+executedRestore.current().returnCode());
				System.out.println("-------------------");
			}

			com.mongodb.ServerAddress serverAddress = serverAddress(runningMongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("restoredb");
				MongoCollection<Document> col = db.getCollection("sample");

				ArrayList<Object> names = Lists.newArrayList(col.find().map(doc -> doc.get("name")));

				assertThat(names).containsExactlyInAnyOrder("Cassandra", "HBase", "MongoDB");
			}
		}
	}

	@Test
	@Disabled("invalid archive format")
	public void restoreArchiveFile() throws UnknownHostException {
		final String dumpLocation = Thread.currentThread().getContextClassLoader().getResource("dump").getFile();

		Version.Main version = Version.Main.PRODUCTION;
		ImmutableMongoRestoreArguments mongoRestoreArguments = MongoRestoreArguments.builder()
			.verbose(true)
			.dropCollection(true)
			.archive(dumpLocation + "/foo.archive.gz")
			.build();

		Transitions transitions = MongoRestore.instance().transitions(version)
			.replace(Start.to(MongoRestoreArguments.class).initializedWith(mongoRestoreArguments))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoRestoreProcess> executedRestore = runningMongoD.initState(
				StateID.of(ExecutedMongoRestoreProcess.class))) {

				System.out.println("-------------------");
				System.out.println("restore done: "+executedRestore.current().returnCode());
				System.out.println("-------------------");

				assertThat(executedRestore.current().returnCode())
					.describedAs("restore process must be successful")
					.isEqualTo(0);
			}

			com.mongodb.ServerAddress serverAddress = serverAddress(runningMongoD.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("restoredb");
				MongoCollection<Document> col = db.getCollection("sample");

				ArrayList<Object> names = Lists.newArrayList(col.find().map(doc -> doc.get("name")));

				assertThat(names).containsExactlyInAnyOrder("Cassandra", "HBase", "MongoDB");
			}
		}
	}

	private static void dumpAndRestore(
		Version.Main version,
		MongoDumpArguments mongoDumpArguments,
		MongoRestoreArguments mongoRestoreArguments,
		Consumer<ServerAddress> beforeDump,
		Consumer<ServerAddress> beforeRestore,
		Consumer<ServerAddress> afterRestore
	) throws UnknownHostException {

		Transitions transitions = MongoRestore.instance().transitions(version)
			.replace(Start.to(MongoRestoreArguments.class).initializedWith(mongoRestoreArguments))
			.addAll(MongoDump.instance().transitions(version)
				.replace(Start.to(MongoDumpArguments.class).initializedWith(mongoDumpArguments))
				.walker().asTransitionTo(TransitionMapping.builder("mongoDump", StateID.of(ExecutedMongoDumpProcess.class))
					.build()))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			ServerAddress serverAddress = runningMongoD.current().getServerAddress();

			beforeDump.accept(serverAddress);

			try (TransitionWalker.ReachedState<ExecutedMongoDumpProcess> executedDump = runningMongoD.initState(
				StateID.of(ExecutedMongoDumpProcess.class))) {
				System.out.println("dump return code: "+executedDump.current().returnCode());
			}

			beforeRestore.accept(serverAddress);

			try (TransitionWalker.ReachedState<ExecutedMongoRestoreProcess> executedRestore = runningMongoD.initState(
				StateID.of(ExecutedMongoRestoreProcess.class))) {
				System.out.println("restore return code: "+executedRestore.current().returnCode());
			}

			afterRestore.accept(serverAddress);
		}
	}

	private static Consumer<ServerAddress> onTestCollection(Consumer<MongoCollection<Document>> onCollection) {
		return serverAddress -> {
			com.mongodb.ServerAddress serverAddress1 = serverAddress(serverAddress);
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress1)) {
				MongoDatabase db = mongo.getDatabase("testdb");
				MongoCollection<Document> col = db.getCollection("testcol");

				onCollection.accept(col);
			}
		};
	}

}