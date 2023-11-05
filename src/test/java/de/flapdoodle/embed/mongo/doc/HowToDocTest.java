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
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.MongoImportArguments;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.commands.MongosArguments;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.examples.EnableAuthentication;
import de.flapdoodle.embed.mongo.examples.FileStreamProcessor;
import de.flapdoodle.embed.mongo.transitions.*;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.embed.mongo.types.DistributionBaseUrl;
import de.flapdoodle.embed.mongo.util.FileUtils;
import de.flapdoodle.embed.process.config.DownloadConfig;
import de.flapdoodle.embed.process.config.TimeoutConfig;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.directories.PersistentDir;
import de.flapdoodle.embed.process.net.DownloadToPath;
import de.flapdoodle.embed.process.net.HttpProxyFactory;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.transitions.DownloadPackage;
import de.flapdoodle.reverse.*;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.testdoc.Includes;
import de.flapdoodle.testdoc.Recorder;
import de.flapdoodle.testdoc.Recording;
import de.flapdoodle.testdoc.TabSize;
import de.flapdoodle.types.Try;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HowToDocTest {

	@RegisterExtension
	public static final Recording recording = Recorder.with("Howto.md", TabSize.spaces(2));

	@Test
	public void testStandard() {
		recording.begin();

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().start(Version.Main.PRODUCTION)) {
			try (MongoClient mongo = new MongoClient(serverAddress(running.current().getServerAddress()))) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));
				recording.end();
				assertThat(col.countDocuments()).isEqualTo(1L);
				recording.begin();
			}
		}

		recording.end();
	}

	@Test
	public void customizeMongodByOverride() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public Transition<DistributionBaseUrl> distributionBaseUrl() {
				return Start.to(DistributionBaseUrl.class)
					.initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain"));
			}
		};
		recording.end();

		assertThatThrownBy(() -> mongod.start(Version.Main.PRODUCTION))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void customizeMongodByBuilder() {
		recording.begin();
		Mongod mongod = Mongod.builder()
			.distributionBaseUrl(Start.to(DistributionBaseUrl.class)
				.initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain")))
			.build();
		recording.end();

		assertThatThrownBy(() -> mongod.start(Version.Main.PRODUCTION))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void customizeMongodByReplacement() {
		recording.begin();
		Transitions mongod = Mongod.instance()
			.transitions(Version.Main.PRODUCTION)
			.replace(Start.to(DistributionBaseUrl.class)
				.initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain")));
		recording.end();

		assertThatThrownBy(() -> mongod.walker().initState(StateID.of(RunningMongodProcess.class)))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testMainVersions() {
		recording.begin();
		IFeatureAwareVersion version = Version.V2_2_5;
		// uses latest supported 2.2.x Version
		version = Version.Main.V2_2;
		// uses latest supported production version
		version = Version.Main.PRODUCTION;
		// uses latest supported development version
		version = Version.Main.DEVELOPMENT;
		recording.end();
	}

	@Test
	public void testCommandLinePostProcessing() {
		recording.begin();
		// TODO change command line arguments before calling process start??
		recording.end();
	}

	@Test
	public void testCommandLineOptions() {
		recording.begin();
		new Mongod() {
			@Override
			public Transition<MongodArguments> mongodArguments() {
				return Start.to(MongodArguments.class)
					.initializedWith(MongodArguments.defaults().withSyncDelay(10)
						.withUseNoPrealloc(false)
						.withUseSmallFiles(false)
						.withUseNoJournal(false)
						.withEnableTextSearch(true));
			}
		}.transitions(Version.Main.PRODUCTION);
		recording.end();
	}

	@Test
	public void testSnapshotDbFiles(@TempDir Path destination) {
		recording.begin();

		Listener listener = Listener.typedBuilder()
			.onStateTearDown(StateID.of(DatabaseDir.class), databaseDir -> {
				Try.run(() -> FileUtils.copyDirectory(databaseDir.value(), destination));
			})
			.build();

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().transitions(Version.Main.PRODUCTION).walker()
			.initState(StateID.of(RunningMongodProcess.class), listener)) {
		}

		assertThat(destination)
			.isDirectory()
			.isDirectoryContaining(path -> path.getFileName().toString().startsWith("WiredTiger.lock"));

		recording.end();
	}

	@Test
	public void testMongosAndMongod() {
		recording.begin();
		Version.Main version = Version.Main.PRODUCTION;

		Mongod mongod = new Mongod() {
			@Override
			public Transition<MongodArguments> mongodArguments() {
				return Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults()
					.withIsConfigServer(true)
					.withReplication(Storage.of("testRepSet", 5000)));
			}
		};

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongod = mongod.start(version)) {

			ServerAddress serverAddress = runningMongod.current().getServerAddress();

			try (MongoClient mongo = new MongoClient(serverAddress(serverAddress))) {
				mongo.getDatabase("admin").runCommand(new Document("replSetInitiate", new Document()));
			}

			Mongos mongos = new Mongos() {
				@Override public Start<MongosArguments> mongosArguments() {
					return Start.to(MongosArguments.class).initializedWith(MongosArguments.defaults()
						.withConfigDB(serverAddress.toString())
						.withReplicaSet("testRepSet")
					);
				}
			};

			try (TransitionWalker.ReachedState<RunningMongosProcess> runningMongos = mongos.start(version)) {
				try (MongoClient mongo = new MongoClient(serverAddress(runningMongos.current().getServerAddress()))) {
					assertThat(mongo.listDatabaseNames()).contains("admin", "config");
				}
			}
		}
		recording.end();
	}

	@Test
	public void importJsonIntoMongoDB() {
		String jsonFile = Resources.getResource("sample.json").getFile();

		recording.begin();

		Version.Main version = Version.Main.PRODUCTION;

		Transitions transitions = MongoImport.instance().transitions(version)
			.replace(Start.to(MongoImportArguments.class).initializedWith(MongoImportArguments.builder()
				.databaseName("importTestDB")
				.collectionName("importedCollection")
				.upsertDocuments(true)
				.dropCollection(true)
				.isJsonArray(true)
				.importFile(jsonFile)
				.build()))
			.addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
				.deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
			.addAll(Mongod.instance().transitions(version).walker()
				.asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
					.build()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
			.initState(StateID.of(RunningMongodProcess.class))) {

			try (TransitionWalker.ReachedState<ExecutedMongoImportProcess> executedImport = runningMongoD.initState(
				StateID.of(ExecutedMongoImportProcess.class))) {

				assertThat(executedImport.current().returnCode())
					.describedAs("import successful")
					.isEqualTo(0);
			}
		}

		recording.end();
	}

	@Test
	public void setupUserAndRoles() {
		recording.include(EnableAuthentication.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim);
		recording.begin();

		Listener withRunningMongod = EnableAuthentication.of("i-am-admin", "admin-password")
			.withEntries(
				EnableAuthentication.role("test-db", "test-collection", "can-list-collections")
					.withActions("listCollections"),
				EnableAuthentication.user("test-db", "read-only", "user-password")
					.withRoles("can-list-collections", "read")
			).withRunningMongod();

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance()
			.withMongodArguments(
				Start.to(MongodArguments.class)
					.initializedWith(MongodArguments.defaults().withAuth(true)))
			.start(Version.Main.PRODUCTION, withRunningMongod)) {

			try (MongoClient mongo = new MongoClient(
				serverAddress(running.current().getServerAddress()),
				MongoCredential.createCredential("i-am-admin", "admin", "admin-password".toCharArray()),
				MongoClientOptions.builder().build())) {

				MongoDatabase db = mongo.getDatabase("test-db");
				MongoCollection<Document> col = db.getCollection("test-collection");
				col.insertOne(new Document("testDoc", new Date()));
			}

			try (MongoClient mongo = new MongoClient(
				serverAddress(running.current().getServerAddress()),
				MongoCredential.createCredential("read-only", "test-db", "user-password".toCharArray()),
				MongoClientOptions.builder().build())) {

				MongoDatabase db = mongo.getDatabase("test-db");
				MongoCollection<Document> col = db.getCollection("test-collection");
				assertThat(col.countDocuments()).isEqualTo(1L);

				assertThatThrownBy(() -> col.insertOne(new Document("testDoc", new Date())))
					.isInstanceOf(MongoCommandException.class)
					.message().contains("not authorized on test-db");
			}
		}

		recording.end();
	}


	protected static void assertRunningMongoDB(TransitionWalker.ReachedState<RunningMongodProcess> running) {
		try (MongoClient mongo = new MongoClient(serverAddress(running.current().getServerAddress()))) {
			MongoDatabase db = mongo.getDatabase("test");
			MongoCollection<Document> col = db.getCollection("testCol");
			col.insertOne(new Document("testDoc", new Date()));
			assertThat(col.countDocuments()).isEqualTo(1L);
		}
	}
}
