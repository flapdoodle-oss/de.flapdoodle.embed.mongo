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

import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.examples.FileStreamProcessor;
import de.flapdoodle.embed.mongo.packageresolver.*;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.PackageOfCommandDistribution;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.mongo.types.DatabaseDir;
import de.flapdoodle.embed.mongo.types.DistributionBaseUrl;
import de.flapdoodle.embed.process.config.DownloadConfig;
import de.flapdoodle.embed.process.config.TimeoutConfig;
import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.io.ProcessOutput;
import de.flapdoodle.embed.process.io.Processors;
import de.flapdoodle.embed.process.io.directories.PersistentDir;
import de.flapdoodle.embed.process.net.DownloadToPath;
import de.flapdoodle.embed.process.net.HttpProxyFactory;
import de.flapdoodle.embed.process.runtime.Network;
import de.flapdoodle.embed.process.transitions.DownloadPackage;
import de.flapdoodle.os.BitSize;
import de.flapdoodle.os.CPUType;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.linux.UbuntuVersion;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.TransitionWalker;
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
import static de.flapdoodle.embed.mongo.doc.HowToDocTest.assertRunningMongoDB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CustomizationsTest {
	@RegisterExtension
	public static final Recording recording = Recorder.with("Customizations.md", TabSize.spaces(2));

	@Test
	public void testFreeServerPort() throws IOException {
		recording.begin();
		int port = Network.getFreeServerPort();
		recording.end();
	}

	@Test
	public void customizeNetworkPort() {
		recording.begin();
		Mongod mongod = Mongod.builder()
			.net(Start.to(Net.class).initializedWith(Net.defaults()
				.withPort(12345)))
			.build();
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testCustomizeDownloadURL() {
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
	public void useBasicAuthInDownloadUrl() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public Transition<DistributionBaseUrl> distributionBaseUrl() {
				return Start.to(DistributionBaseUrl.class)
					.initializedWith(DistributionBaseUrl.of("http://user:password@my.custom.download.domain"));
			}
		};
		recording.end();

		assertThatThrownBy(() -> mongod.start(Version.Main.PRODUCTION))
			.isInstanceOf(RuntimeException.class);
	}

	@Test
	public void testCustomProxy() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public DownloadPackage downloadPackage() {
				return DownloadPackage.withDefaults()
					.withDownloadConfig(DownloadConfig.defaults()
						.withProxyFactory(new HttpProxyFactory("fooo", 1234)));
			}
		};
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testCustomDownloader() {
		recording.begin();
		DownloadToPath custom = new DownloadToPath() {
			@Override
			public void download(URL url, Path destination,
				Optional<Proxy> proxy, String userAgent, TimeoutConfig timeoutConfig,
				DownloadCopyListener copyListener) throws IOException {
				// download url to destination
			}
		};

		Mongod mongod = Mongod.instance()
			.withDownloadPackage(DownloadPackage.withDefaults()
				.withDownloadToPath(custom));
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testCustomizeArtifactStorage() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public Transition<PersistentDir> persistentBaseDir() {
				return Start.to(PersistentDir.class)
					.providedBy(PersistentDir.inUserHome(".embeddedMongodbCustomPath")
						.mapToUncheckedException(RuntimeException::new));
			}
		};
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testCustomDatabaseDirectory(@TempDir Path customDatabaseDir) {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override public Transition<DatabaseDir> databaseDir() {
				return Start.to(DatabaseDir.class).initializedWith(DatabaseDir.of(customDatabaseDir));
			}
		};
		recording.end();
	}

	@Test
	public void testCustomOutputToConsolePrefix() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public Transition<ProcessOutput> processOutput() {
				return Start.to(ProcessOutput.class)
					.initializedWith(ProcessOutput.builder()
						.output(Processors.namedConsole("[mongod>]"))
						.error(Processors.namedConsole("[MONGOD>]"))
						.commands(Processors.namedConsole("[console>]"))
						.build()
					)
					.withTransitionLabel("create named console");
			}
		};
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testCustomOutputToFile() {
		recording.include(FileStreamProcessor.class, Includes.WithoutImports, Includes.WithoutPackage, Includes.Trim);
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override
			public Transition<ProcessOutput> processOutput() {
				return Start.to(ProcessOutput.class)
					.providedBy(Try.<ProcessOutput, IOException>supplier(() -> ProcessOutput.builder()
							.output(Processors.named("[mongod>]",
								new FileStreamProcessor(File.createTempFile("mongod", "log"))))
							.error(new FileStreamProcessor(File.createTempFile("mongod-error", "log")))
							.commands(Processors.namedConsole("[console>]"))
							.build())
						.mapToUncheckedException(RuntimeException::new))
					.withTransitionLabel("create named console");
			}
		};
		recording.end();
		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			assertRunningMongoDB(running);
		}
	}

	@Test
	public void testDefaultOutputToNone() {
		recording.begin();
		Mongod mongod = new Mongod() {
			@Override public Transition<ProcessOutput> processOutput() {
				return Start.to(ProcessOutput.class)
					.initializedWith(ProcessOutput.silent())
					.withTransitionLabel("no output");
			}
		};

		try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
			ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
			try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
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
	public void customPackageResolver() {
		recording.begin();
		Mongod customizedInstance = Mongod.instance()
			.withPackageOfDistribution(Start.to(Package.class).providedBy(() -> Package.builder()
				.fileSet(FileSet.builder()
					.addEntry(FileType.Executable, "mongod")
					.build())
				.archiveType(ArchiveType.TGZ)
				.url("http://some-local-server/mongod-to-download")
				.build()));
		recording.end();

		try (TransitionWalker.ReachedState<Package> reachedState = customizedInstance.transitions(Version.Main.V7_0)
			.walker().initState(StateID.of(Package.class))) {
			Package resolvedPackage = reachedState.current();

			assertThat(resolvedPackage.url())
				.isEqualTo("http://some-local-server/mongod-to-download");
		}
	}
	
	@Test
	public void customPackageResolverRules() {
		recording.begin();

		PackageFinderRules mongodPackageRules = PackageFinderRules.builder()
			.addRules(PackageFinderRule.builder()
				.match(PlatformMatch.withOs(CommonOS.Linux).withBitSize(BitSize.B64).withCpuType(CPUType.X86).withVersion(UbuntuVersion.Ubuntu_22_04)
					.andThen(DistributionMatch.any(VersionRange.of("7.0.0", "7.1.0"))
					))
				.finder(UrlTemplatePackageFinder.builder()
					.fileSet(FileSet.builder()
						.addEntry(FileType.Executable, "mongod")
						.build())
					.archiveType(ArchiveType.TGZ)
					.urlTemplate("/relativePath-{version}.tgz")
					.build())
				.build())
			.addRules(PackageFinderRule.builder()
				.match(PlatformMatch.withOs(CommonOS.Windows)
					.andThen(DistributionMatch.any(VersionRange.of("5.0.0", "10.0.0"))))
				.finder(PackageFinder.failWithMessage(distribution -> "not supported: " + distribution))
				.build())
			.build();

		Mongod customizedInstance = Mongod.instance()
			.withDistributionBaseUrl(Start.to(DistributionBaseUrl.class)
				.initializedWith(DistributionBaseUrl.of("http://some-local-server")))
			.withPackageOfDistribution(PackageOfCommandDistribution.withDefaults()
				.withCommandPackageResolver(command -> distribution -> {
					switch (command) {
						case MongoD:
							return mongodPackageRules.packageFor(distribution)
								.orElseThrow(() -> new IllegalArgumentException("could not find package for " + distribution));
						default:
							throw new IllegalArgumentException("not implemented");
					}
				}));

		recording.end();

		try (TransitionWalker.ReachedState<Package> reachedState = customizedInstance.transitions(Version.Main.V7_0)
			.walker().initState(StateID.of(Package.class))) {
			Package resolvedPackage = reachedState.current();

			assertThat(resolvedPackage.url())
				.isEqualTo("http://some-local-server/relativePath-7.0.2.tgz");
		}
	}

}
