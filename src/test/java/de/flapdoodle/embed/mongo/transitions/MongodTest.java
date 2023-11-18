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
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.Versions;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.types.SystemEnv;
import de.flapdoodle.embed.process.archives.ExtractedFileSet;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.io.directories.PersistentDir;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.store.ExtractedFileSetStore;
import de.flapdoodle.os.*;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import de.flapdoodle.reverse.types.TypeNames;
import de.flapdoodle.types.Pair;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Assume;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static de.flapdoodle.embed.mongo.ServerAddressMapping.serverAddress;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

class MongodTest {
	private static final Logger logger = LoggerFactory.getLogger(MongodTest.class.getName());

	@Test
	public void useEnvVariableForFileSetStore(@TempDir Path tempDir) throws IOException {
		Path persistentDirInEnv = tempDir.resolve(".embedmongoENV");
		Files.createDirectory(persistentDirInEnv);

		ImmutableMongod mongodInstance = Mongod.instance()
			.withSystemEnv(Start.to(SystemEnv.class)
				.initializedWith(SystemEnv.of(ImmutableMap.of("EMBEDDED_MONGO_ARTIFACTS", persistentDirInEnv.toString()))));

		try (TransitionWalker.ReachedState<PersistentDir> withPersistenDir = mongodInstance
			.transitions(Version.Main.V5_0).walker().initState(StateID.of(PersistentDir.class))) {

			Path expected = tempDir.resolve(".embedmongoENV");

			assertThat(withPersistenDir.current().value())
				.isEqualTo(expected);
			assertThat(expected)
				.exists()
				.isDirectory();
		}
	}

	@Test
	public void mustCreateBaseDirToInitFileSetStore(@TempDir Path tempDir) throws IOException {
		Path baseDir = tempDir.resolve(".embedmongo");
		Files.createDirectories(baseDir);
		
		try (TransitionWalker.ReachedState<ExtractedFileSetStore> withfileSetStore = Mongod.instance()
			.withPersistentBaseDir(Start.to(PersistentDir.class)
				.initializedWith(PersistentDir.of(baseDir)))
			.transitions(Version.Main.V5_0).walker().initState(StateID.of(ExtractedFileSetStore.class))) {

			assertThat(baseDir.resolve("fileSets"))
				.exists()
				.isDirectory();
		}
	}

	@Test
	public void testStartStopTenTimesWithNewMongoExecutable() throws IOException {
		int loops = 10;

		Mongod mongod = new Mongod() {
			@Override public Transition<MongodArguments> mongodArguments() {
				return Start.to(MongodArguments.class)
					.initializedWith(MongodArguments.defaults()
						.withUseNoPrealloc(true)
						.withUseSmallFiles(true));
			}
		};

		for (int i = 0; i < loops; i++) {
			logger.info("Loop: {}", i);
			try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongod = mongod.start(Version.Main.PRODUCTION)) {
				ServerAddress serverAddress = serverAddress(runningMongod.current().getServerAddress());
				try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
					MongoDatabase db = mongo.getDatabase("test");
					MongoCollection<Document> col = db.getCollection("testCol");
					col.insertOne(new Document("testDoc", new Date()));
				}
			}
		}
	}

	@Test
	public void perfTest() throws IOException {
		int loops = 100;

		Mongod mongod = new Mongod() {
			@Override public Transition<MongodArguments> mongodArguments() {
				return Start.to(MongodArguments.class)
					.initializedWith(MongodArguments.defaults()
						.withUseNoPrealloc(true)
						.withUseSmallFiles(true));
			}
		};

		for (int i = 0; i < loops; i++) {
//			logger.info("Loop: {}", i);
			try (TransitionWalker.ReachedState<ExtractedFileSet> fileSet = mongod.transitions(Version.Main.PRODUCTION)
				.walker().initState(StateID.of(ExtractedFileSet.class))) {
				
			}
		}
	}

	@Test
	public void measureTimeSpend() throws IOException {
		Mongod mongod = Mongod.instance()
			.withMongodArguments(Start.to(MongodArguments.class)
				.initializedWith(MongodArguments.defaults()
					.withUseNoPrealloc(true)
					.withUseSmallFiles(true)))
//			.withProcessOutput(Start.to(ProcessOutput.class)
//				.initializedWith(ProcessOutput.silent()))
			;

		Supplier<Long> timeStamp=System::currentTimeMillis;
		List<Pair<StateID<?>, Long>> timeline=new ArrayList<>();

		Listener listener=new Listener() {
			@Override public <T> void onStateReached(StateID<T> stateID, T value) {
				timeline.add(Pair.of(stateID, timeStamp.get()));
			}

			@Override public <T> void onStateTearDown(StateID<T> stateID, T value) {
				timeline.add(Pair.of(stateID, timeStamp.get()));
			}
		};

		timeline.add(Pair.of(StateID.of(Void.class),timeStamp.get()));

		try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongod = mongod.transitions(Version.Main.PRODUCTION)
			.walker()
			.initState(StateID.of(RunningMongodProcess.class), listener)) {
			ServerAddress serverAddress = serverAddress(runningMongod.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));
			}
		}

		timeline.add(Pair.of(StateID.of(Void.class),timeStamp.get()));

		int lastEntryOffset=timeline.size()-1;

		long start=timeline.get(0).second();
		long end=timeline.get(lastEntryOffset).second();

		String columnFormat="| %-25s | %4s | %4s | %5s |%n";
		System.out.println("---------------------------------------------------");
		System.out.printf(columnFormat,"State","up","down","<->");
		System.out.println("---------------------------------------------------");
		for (int i = 0; i < timeline.size()/2; i++) {
			Pair<StateID<?>, Long> startEntry = timeline.get(i);
			Pair<StateID<?>, Long> endEntry = timeline.get(lastEntryOffset-i);
			long timeSpendOnStart=startEntry.second()-start;
			long timeSpendOnTeardown=end-endEntry.second();
			long timeStateIsActive=endEntry.second()-startEntry.second();

			System.out.printf(columnFormat, TypeNames.typeName(startEntry.first().type()), timeSpendOnStart, timeSpendOnTeardown, timeStateIsActive);

			end=endEntry.second();
			start=startEntry.second();
		}
		System.out.println("---------------------------------------------------");
	}

	@Test
	public void startTwoMongodInstancesUsingDifferentPorts() throws UnknownHostException {
		try (TransitionWalker.ReachedState<RunningMongodProcess> outerMongod = Mongod.instance().start(Version.Main.PRODUCTION)) {
			try (TransitionWalker.ReachedState<RunningMongodProcess> innerMongod = Mongod.instance().start(Version.Main.PRODUCTION)) {

				ServerAddress serverAddress1 = serverAddress(innerMongod.current().getServerAddress());
				try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress1)) {
					MongoDatabase db = mongo.getDatabase("test");
					db.createCollection("testCol");
					MongoCollection<Document> col = db.getCollection("testColl");
					col.insertOne(new Document("testDoc", new Date()));
				}

				ServerAddress serverAddress = serverAddress(outerMongod.current().getServerAddress());
				try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
					MongoDatabase db = mongo.getDatabase("test");
					db.createCollection("testCol");
					MongoCollection<Document> col = db.getCollection("testColl");
					col.insertOne(new Document("testDoc", new Date()));
				}
			}
		}
	}

	@Test
	public void startMongodOnNonFreePort() {
		Net net = Net.defaults();

		Mongod mongod = new Mongod() {
			@Override public Transition<Net> net() {
				return Start.to(Net.class)
					.initializedWith(net);
			}
		};

		try (TransitionWalker.ReachedState<RunningMongodProcess> outerMongod = mongod.start(Version.Main.PRODUCTION)) {
			Assertions.assertThatThrownBy(() -> mongod.start(Version.Main.PRODUCTION))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("rollback after error on transition to State(RunningMongodProcess)")
				.hasCauseInstanceOf(RuntimeException.class)
				.cause()
				.hasMessage("Could not start process: Address already in use");
		}
	}

	@Test
	@Disabled("does not run on ubuntu>=22.x because 3.6 uses libssl.so.1.0.0")
	public void startLegacyMongodWichDontSupportBindIpArgument() throws IOException {
		Net net = Net.of("localhost",
			Network.freeServerPort(Network.getLocalHost()),
			Network.localhostIsIPv6());

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = new Mongod() {
			@Override
			public Transition<Net> net() {
				return Start.to(Net.class).initializedWith(net);
			}
		}.start(Version.V3_6_0)) {
			ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));
			}
		}
	}

	@Test
	public void shutdownShouldWorkWithMongodbVersion6() throws UnknownHostException {
		Version.Main version = Version.Main.V6_0;

		try (TransitionWalker.ReachedState<RunningMongodProcess> outerMongod = Mongod.instance()
			.withNet(Start.to(Net.class).initializedWith(Net.of("localhost", 23456, Network.localhostIsIPv6())))
			.start(version)) {
			ServerAddress serverAddress = serverAddress(outerMongod.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
				MongoDatabase db = mongo.getDatabase("test");
				MongoCollection<Document> col = db.getCollection("testCol");
				col.insertOne(new Document("testDoc", new Date()));

				MongoDatabase adminDB = mongo.getDatabase("admin");
				System.out.println(outerMongod.current().getServerAddress());

				boolean asThisIsHowShutdownIsSendToTheDBAndWeShouldDoIt = false;

				if (asThisIsHowShutdownIsSendToTheDBAndWeShouldDoIt) {
					try {
						Document result = adminDB.runCommand(new Document()
							.append("shutdown", 1)
						);
					}
					catch (Exception x) {
						x.printStackTrace();
					}
				}
			}
		}
	}

	private static List<Arguments> testableDistributions() {
		ArrayList<Arguments> ret = new ArrayList<>();
		for (OS os : CommonOS.values()) {
			// there is no generic linux version of mongodb after 3.6
			// so we should use linux dists instead
			for (CommonArchitecture arc : CommonArchitecture.values()) {
				for (Version.Main version : Versions.testableVersions(Version.Main.class)) {
					ret.add(Arguments.of(os,arc,version));
				}
			}
		}
		return ret;
	}

	@ParameterizedTest
	@MethodSource("testableDistributions")
	public void extractArtifact(OS os, CommonArchitecture arch, Version.Main version) {
		if (skipThisVersion(os, version, arch.cpuType(), arch.bitSize())) {
			Assume.assumeTrue(true);
		} else {
			assertCanExtractArtifact(distributionOf(version, os, arch));
		}
	}

	private static void assertCanExtractArtifact(Distribution distribution) {
		try (TransitionWalker.ReachedState<de.flapdoodle.embed.process.archives.ExtractedFileSet> extractedFileSet = new Mongod() {
			@Override
			public Transition<Distribution> distribution() {
				return Start.to(Distribution.class).initializedWith(distribution);
			}
		}.transitions(distribution.version())
			.walker()
			.initState(StateID.of(de.flapdoodle.embed.process.archives.ExtractedFileSet.class))) {
			assertNotNull(extractedFileSet.current().executable());
		}
	}

	private static boolean skipThisVersion(OS os, IFeatureAwareVersion version, CPUType cpuType, BitSize bitsize) {
		if (version.enabled(Feature.ONLY_64BIT) && bitsize==BitSize.B32) {
			return true;
		}

		if ((os.type() == OSType.OS_X) && (bitsize == BitSize.B32)) {
			// there is no osx 32bit version for v2.2.1 and above, so we dont check
			return true;
		}
		if (os.type() == OSType.OS_X && cpuType==CPUType.ARM) {
			return true;
		}
		if ((os.type() == OSType.Solaris)  && ((bitsize == BitSize.B32) || version.enabled(Feature.NO_SOLARIS_SUPPORT))) {
			return true;
		}
		if (os.type() == OSType.FreeBSD) {
			return true;
		}
		if (os.type() == OSType.Windows) {
			if (cpuType==CPUType.ARM) return true;
			// there is no windows 2008 version for 1.8.5
			return version.asInDownloadPath().equals(Version.V1_8_5.asInDownloadPath());
		}
		return false;
	}

	private static Distribution distributionOf(IFeatureAwareVersion version, OS os, Architecture architecture) {
		return Distribution.of(version, ImmutablePlatform.builder()
			.operatingSystem(os)
			.architecture(architecture)
			.build());
	}
}