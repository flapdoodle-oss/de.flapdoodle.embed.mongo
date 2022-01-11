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
package de.flapdoodle.embed.mongo.packageresolver.linux;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.packageresolver.*;
import de.flapdoodle.embed.process.config.store.DistributionPackage;
import de.flapdoodle.embed.process.config.store.FileSet;
import de.flapdoodle.embed.process.config.store.FileType;
import de.flapdoodle.embed.process.config.store.ImmutableFileSet;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.os.BitSize;
import de.flapdoodle.os.ImmutablePlatform;
import de.flapdoodle.os.OS;
import de.flapdoodle.os.linux.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class LinuxPackageFinder implements PackageFinder {

	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxPackageFinder.class);

	private final Command command;
	private final ImmutablePlatformMatchRules rules;

	public LinuxPackageFinder(Command command) {
		this.command = command;
		this.rules = rules(command);
	}

	@Override
	public Optional<DistributionPackage> packageFor(Distribution distribution) {
		return rules.packageFor(distribution);
	}

	private static ImmutablePlatformMatchRules rules(Command command) {
		ImmutableFileSet fileSet = FileSet.builder().addEntry(FileType.Executable, command.commandName()).build();

    UbuntuPackageResolver ubuntuPackageResolver = new UbuntuPackageResolver(command);

    final ImmutablePlatformMatchRule ubuntuRule = PlatformMatchRule.builder()
			.match(PlatformMatch.withOs(OS.Linux)
				.withVersion(UbuntuVersion.values()))
			.finder(ubuntuPackageResolver)
			.build();

    final ImmutablePlatformMatchRule linuxMintRule = PlatformMatchRule.builder()
      .match(PlatformMatch.withOs(OS.Linux)
        .withVersion(LinuxMintVersion.values()))
      .finder(new LinuxMintPackageResolver(ubuntuPackageResolver))
      .build();

    final ImmutablePlatformMatchRule debianRule = PlatformMatchRule.builder()
			.match(PlatformMatch.withOs(OS.Linux).withVersion(DebianVersion.values()))
			.finder(new DebianPackageResolver(command))
			.build();

		ImmutablePlatformMatchRule centosRule = PlatformMatchRule.builder()
			.match(PlatformMatch.withOs(OS.Linux)
				.withVersion(CentosVersion.values()))
			.finder(new CentosRedhatPackageResolver(command))
			.build();

		ImmutablePlatformMatchRule amazonRule = PlatformMatchRule.builder()
			.match(PlatformMatch.withOs(OS.Linux)
				.withVersion(AmazonVersion.values()))
			.finder(new AmazonPackageResolver(command))
			.build();

    /*
      Linux (legacy) undefined
      https://fastdl.mongodb.org/linux/mongodb-linux-i686-{}.tgz
      3.2.21 - 3.2.0, 3.0.14 - 3.0.0, 2.6.12 - 2.6.0
    */
		PlatformMatchRule legacy32 = PlatformMatchRule.builder()
			.match(DistributionMatch.any(
					VersionRange.of("3.2.0", "3.2.21"),
					VersionRange.of("3.0.0", "3.0.14"),
					VersionRange.of("2.6.0", "2.6.12")
				)
				.andThen(PlatformMatch.withOs(OS.Linux).withBitSize(BitSize.B32)))
			.finder(UrlTemplatePackageResolver.builder()
				.fileSet(fileSet)
				.archiveType(ArchiveType.TGZ)
				.urlTemplate("/linux/mongodb-linux-i686-{version}.tgz")
				.build())
			.build();

  /*
    Linux (legacy) x64
    https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-{}.tgz
    4.0.26 - 4.0.0, 3.6.22 - 3.6.0, 3.4.23 - 3.4.9, 3.4.7 - 3.4.0, 3.2.21 - 3.2.0, 3.0.14 - 3.0.0, 2.6.12 - 2.6.0
   */
		PlatformMatchRule legacy64 = PlatformMatchRule.builder()
			.match(DistributionMatch.any(
					VersionRange.of("4.0.0", "4.0.26"),
					VersionRange.of("3.6.0", "3.6.22"),
					VersionRange.of("3.4.9", "3.4.23"),
					VersionRange.of("3.4.0", "3.4.7"),
					VersionRange.of("3.2.0", "3.2.21"),
					VersionRange.of("3.0.0", "3.0.14"),
					VersionRange.of("2.6.0", "2.6.12")
				)
				.andThen(PlatformMatch.withOs(OS.Linux).withBitSize(BitSize.B64)))
			.finder(UrlTemplatePackageResolver.builder()
				.fileSet(fileSet)
				.archiveType(ArchiveType.TGZ)
				.urlTemplate("/linux/mongodb-linux-x86_64-{version}.tgz")
				.build())
			.build();

		PlatformMatchRule hiddenLegacy64 = PlatformMatchRule.builder()
			.match(DistributionMatch.any(
					VersionRange.of("3.3.1", "3.3.1"),
					VersionRange.of("3.5.5", "3.5.5")
				)
				.andThen(PlatformMatch.withOs(OS.Linux).withBitSize(BitSize.B64)))
			.finder(UrlTemplatePackageResolver.builder()
				.fileSet(fileSet)
				.archiveType(ArchiveType.TGZ)
				.urlTemplate("/linux/mongodb-linux-x86_64-{version}.tgz")
				.build())
			.build();

		PlatformMatchRule hiddenLegacy32 = PlatformMatchRule.builder()
			.match(DistributionMatch.any(
					VersionRange.of("3.3.1", "3.3.1"),
					VersionRange.of("3.5.5", "3.5.5")
				)
				.andThen(PlatformMatch.withOs(OS.Linux).withBitSize(BitSize.B32)))
			.finder(UrlTemplatePackageResolver.builder()
				.fileSet(fileSet)
				.archiveType(ArchiveType.TGZ)
				.urlTemplate("/linux/mongodb-linux-i686-{version}.tgz")
				.build())
			.build();

		PlatformMatchRule failIfNothingMatches = PlatformMatchRule.builder()
			.match(PlatformMatch.withOs(OS.Linux))
			.finder(distribution -> {
				Distribution ubuntuLTSFallback = Distribution.of(distribution.version(),
					ImmutablePlatform.copyOf(distribution.platform())
						.withVersion(UbuntuVersion.Ubuntu_20_04));

				LOGGER.warn("because there is no package for "+distribution+" we fall back to "+ubuntuLTSFallback);

				Optional<DistributionPackage> resolvedPackage = ubuntuPackageResolver.packageFor(ubuntuLTSFallback);
				if (!resolvedPackage.isPresent()) {
					throw new IllegalArgumentException("linux distribution not supported: "+distribution+"(with fallback to "+ubuntuLTSFallback+")");
				}
				return resolvedPackage;
			})
			.build();

		return PlatformMatchRules.empty()
			.withRules(
				ubuntuRule,
				linuxMintRule,
				debianRule,
				centosRule,
				amazonRule,
				legacy32,
				legacy64,
				hiddenLegacy64,
				hiddenLegacy32,
				failIfNothingMatches
			);
	}

}
