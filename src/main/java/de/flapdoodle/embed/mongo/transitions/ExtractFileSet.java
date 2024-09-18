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

import de.flapdoodle.embed.mongo.types.SystemEnv;
import de.flapdoodle.embed.mongo.types.SystemProperties;
import de.flapdoodle.embed.process.archives.ExtractedFileSet;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.io.directories.PersistentDir;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.StandardConsoleProgressListener;
import de.flapdoodle.embed.process.store.ContentHashExtractedFileSetStore;
import de.flapdoodle.embed.process.store.DownloadCache;
import de.flapdoodle.embed.process.store.ExtractedFileSetStore;
import de.flapdoodle.embed.process.store.LocalDownloadCache;
import de.flapdoodle.embed.process.transitions.DownloadPackage;
import de.flapdoodle.embed.process.transitions.ExtractPackage;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.Transitions;
import de.flapdoodle.reverse.transitions.Derive;
import de.flapdoodle.reverse.transitions.Join;
import de.flapdoodle.reverse.transitions.Start;
import org.immutables.value.Value;

import java.nio.file.Paths;
import java.util.Optional;

public interface ExtractFileSet {
	String ARTIFACT_STORE_PROPERTY_NAME = "de.flapdoodle.embed.mongo.artifacts";
	String ARTIFACT_STORE_ENV_NAME = "EMBEDDED_MONGO_ARTIFACTS";

	@Value.Default
	default Transition<PersistentDir> persistentBaseDir() {
		return Join.given(SystemEnv.class).and(SystemProperties.class)
			.state(PersistentDir.class)
			.deriveBy((systemEnv, systemProperties) -> Optional.ofNullable(
					systemProperties.value().getOrDefault(ARTIFACT_STORE_PROPERTY_NAME,
						systemEnv.value().get(ARTIFACT_STORE_ENV_NAME)))
				.map(Paths::get)
				.map(PersistentDir::of)
				.orElseGet(PersistentDir.inUserHome(".embedmongo")
					.mapToUncheckedException(RuntimeException::new)));
	}

	@Value.Default
	default Transition<DownloadCache> downloadCache() {
		return Derive.given(PersistentDir.class)
			.state(DownloadCache.class)
			.deriveBy(storeBaseDir -> new LocalDownloadCache(storeBaseDir.value().resolve("archives")))
			.withTransitionLabel("downloadCache");
	}

	@Value.Default
	default Transition<ExtractedFileSetStore> extractedFileSetStore() {
		return Derive.given(PersistentDir.class)
			.state(ExtractedFileSetStore.class)
			.deriveBy(baseDir -> new ContentHashExtractedFileSetStore(baseDir.value().resolve("fileSets")))
			.withTransitionLabel("extractedFileSetStore");
	}

	@Value.Default
	default DownloadPackage downloadPackage() {
		return DownloadPackage.withDefaults();
	}

	@Value.Default
	default Transition<ProgressListener> progressListener() {
		return Start.to(ProgressListener.class)
			.providedBy(StandardConsoleProgressListener::new);
	}

	@Value.Default
	default Transition<ExtractedFileSet> extractPackage() {
		return ExtractPackage.withDefaults()
			.withExtractedFileSetStore(StateID.of(ExtractedFileSetStore.class));
	}

	@Value.Default
	default Transition<Package> packageOfDistribution() {
		return PackageOfCommandDistribution.withDefaults();
	}

	@Value.Auxiliary
	default Transitions extractFileSet() {
		return Transitions.from(
			persistentBaseDir(),
			downloadCache(),
			packageOfDistribution(),
			progressListener(),
			downloadPackage(),
			extractedFileSetStore(),
			extractPackage()
		);
	}
}
