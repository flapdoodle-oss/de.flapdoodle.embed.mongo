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
package de.flapdoodle.embed.mongo.distribution;

import de.flapdoodle.embed.mongo.packageresolver.*;

import java.util.Optional;

/**
 * MongoDB Version enum
 */
public enum Version implements IFeatureAwareVersion, HasMongotoolsPackage {

	@Deprecated
	V1_6_5("1.6.5"),
	@Deprecated
	V1_7_6("1.7.6"),
	@Deprecated
	V1_8_0_rc0("1.8.0-rc0"),
	@Deprecated
	V1_8_0("1.8.0"),
	@Deprecated
	V1_8_1("1.8.1"),
	@Deprecated
	V1_8_2_rc0("1.8.2-rc0"),
	@Deprecated
	V1_8_2("1.8.2"),
	@Deprecated
	V1_8_4("1.8.4"),
	@Deprecated
	V1_8_5("1.8.5"),

	@Deprecated
	V1_9_0("1.9.0"),
	@Deprecated
	V2_0_1("2.0.1"),
	@Deprecated
	V2_0_4("2.0.4"),
	@Deprecated
	V2_0_5("2.0.5"),
	@Deprecated
	V2_0_6("2.0.6"),
	@Deprecated
	V2_0_7_RC1("2.0.7-rc1"),
	@Deprecated
	V2_0_7("2.0.7"),
	@Deprecated
	V2_0_8_RC0("2.0.8-rc0"),
	@Deprecated
	V2_0_9("2.0.9"),

	@Deprecated
	V2_1_0("2.1.0"),
	@Deprecated
	V2_1_1("2.1.1"),
  @Deprecated
	V2_1_2("2.1.2"),

	@Deprecated
	V2_2_0_RC0("2.2.0-rc0"),
	@Deprecated
	V2_2_0("2.2.0"),
	@Deprecated
	V2_2_1("2.2.1"),
  @Deprecated
  V2_2_3("2.2.3"),
  @Deprecated
	V2_2_4("2.2.4"),
	@Deprecated
	V2_2_5("2.2.5"),
	@Deprecated
  V2_2_6("2.2.6"),
	@Deprecated
  V2_2_7("2.2.7"),

	@Deprecated
	V2_3_0("2.3.0"),

	@Deprecated
	V2_4_0_RC3("2.4.0-rc3"),
	@Deprecated
	V2_4_0("2.4.0"),
  @Deprecated
  V2_4_1("2.4.1"),
  @Deprecated
 	V2_4_2("2.4.2"),
  @Deprecated
	V2_4_3("2.4.3"),
  @Deprecated
  V2_4_5("2.4.5"),
  @Deprecated
	V2_4_6("2.4.6"),
  @Deprecated
	V2_4_7("2.4.7"),
	@Deprecated
	V2_4_8("2.4.8"),
  @Deprecated
  V2_4_9("2.4.9"),
  @Deprecated
  V2_4_10("2.4.10"),

  @Deprecated
  V2_5_0("2.5.0"),
  @Deprecated
  V2_5_1("2.5.1"),
  @Deprecated
  V2_5_3("2.5.3"),
  @Deprecated
  V2_5_4("2.5.4"),

  /**
	 * 2.6 series production releases --------------
	 */
  @Deprecated
  V2_6_0("2.6.0"),
  @Deprecated
  V2_6_1("2.6.1"),
  @Deprecated
  V2_6_8("2.6.8"),
  @Deprecated
  V2_6_10("2.6.10"),

  /**
   * Latest 2.6 production release
   */
  @Deprecated
  V2_6_11("2.6.11"),

  /**
   * Latest 2.7 series development release
   */
  @Deprecated
  V2_7_0("2.7.0"),
  @Deprecated
  V2_7_1("2.7.1"),

  /**
   * Latest 3.0 series production release
   */
  @Deprecated
  V3_0_0("3.0.0"),
  @Deprecated
  V3_0_1("3.0.1"),
  @Deprecated
  V3_0_2("3.0.2"),
  @Deprecated
  V3_0_4("3.0.4"),
  @Deprecated
  V3_0_5("3.0.5"),
  @Deprecated
  V3_0_6("3.0.6"),
  @Deprecated
  V3_0_7("3.0.7"),
  @Deprecated
  V3_0_8("3.0.8"),

  /**
   * Latest 3.1 series development release
   */
  @Deprecated
  V3_1_0("3.1.0"),
  @Deprecated
  V3_1_5("3.1.5"),
  @Deprecated
  V3_1_6("3.1.6"),
  @Deprecated
  V3_2_0("3.2.0"),

  /**
   * Latest 3.2 series development release
   */
  @Deprecated
  V3_2_1_RC3("3.2.1-rc3"),

  /**
   * Latest 3.2 series production release
   */
  @Deprecated
  V3_2_1("3.2.1"),
  V3_2_20("3.2.20"),

  /**
   * Latest 3 series production release
   */
  @Deprecated
  V3_3_1("3.3.1"),
  @Deprecated
  V3_4_3("3.4.3"),
  /**
   * last version with solaris support
   */
  V3_4_5("3.4.5"),
  V3_4_15("3.4.15"),
  V3_5_5("3.5.5"),
  @Deprecated
  V3_6_0("3.6.0"),
  @Deprecated
  V3_6_2("3.6.2"),
  @Deprecated
  V3_6_3("3.6.3"),

	V3_6_5("3.6.5"),
	V3_6_22("3.6.22"),
  @Deprecated
  V4_0_2("4.0.2"),
  V4_0_12("4.0.12"),

	V4_2_13("4.2.13"),
	V4_2_22("4.2.22"),
	V4_2_23("4.2.23"),

  @Deprecated
  V4_4_1("4.4.1", MongotoolsVersion.Main.V100_5),
  V4_4_5("4.4.5", MongotoolsVersion.Main.V100_5),
	V4_4_9("4.4.9", MongotoolsVersion.Main.V100_5),
	V4_4_11("4.4.11", MongotoolsVersion.Main.V100_5),
	V4_4_13("4.4.13", MongotoolsVersion.Main.V100_5),
	V4_4_16("4.4.16", MongotoolsVersion.Main.V100_5),
	V4_4_17("4.4.17", MongotoolsVersion.Main.V100_5),
	V4_4_18("4.4.18", MongotoolsVersion.Main.V100_5),
	V5_0_2("5.0.2", MongotoolsVersion.Main.V100_5),
	V5_0_5("5.0.5", MongotoolsVersion.Main.V100_5),
	V5_0_6("5.0.6", MongotoolsVersion.Main.V100_5),
	V5_0_12("5.0.12", MongotoolsVersion.Main.V100_5),
	V5_0_13("5.0.13", MongotoolsVersion.Main.V100_5),
	V5_0_14("5.0.14", MongotoolsVersion.Main.V100_5),
	V5_0_26("5.0.26", MongotoolsVersion.Main.V100_5),
	V6_0_1("6.0.1", MongotoolsVersion.Main.V100_6),
	V6_0_2("6.0.2", MongotoolsVersion.Main.V100_6),
	V6_0_3("6.0.3", MongotoolsVersion.Main.V100_6),
	V6_0_4("6.0.4", MongotoolsVersion.Main.V100_6),
	V6_0_5("6.0.5", MongotoolsVersion.Main.V100_7),
	V6_0_6("6.0.6", MongotoolsVersion.Main.V100_7),
	V6_0_8("6.0.8", MongotoolsVersion.Main.V100_7),
	V6_0_12("6.0.12", MongotoolsVersion.Main.V100_7),
	V6_0_14("6.0.14", MongotoolsVersion.Main.V100_7),

	V6_3_1_DEV("6.3.1", MongotoolsVersion.Main.V100_7),
	V6_3_2_DEV("6.3.2", MongotoolsVersion.Main.V100_7),

	V7_0_0_RC1("7.0.0-rc1", MongotoolsVersion.Main.V100_7),
	V7_0_0_RC2("7.0.0-rc2", MongotoolsVersion.Main.V100_7),
	V7_0_0_RC8("7.0.0-rc8", MongotoolsVersion.Main.V100_7),
	V7_0_0_RC10("7.0.0-rc10", MongotoolsVersion.Main.V100_7),
	V7_0_0("7.0.0", MongotoolsVersion.Main.V100_7),
	V7_0_1("7.0.1", MongotoolsVersion.Main.V100_7),
	V7_0_2("7.0.2", MongotoolsVersion.Main.V100_7),
	V7_0_4("7.0.4", MongotoolsVersion.Main.V100_8),
	V7_0_7("7.0.7", MongotoolsVersion.Main.V100_8),
	V7_0_8("7.0.8", MongotoolsVersion.Main.V100_8),

	@Deprecated
  LATEST_NIGHTLY("latest"),
  ;

	private final String specificVersion;
	private final FeatureSet features;
	private final NumericVersion numericVersion;
	private final MongotoolsVersion.Main mongoToolsVersion;

		Version(String vName) {
				this.specificVersion = vName;
				this.features = FeatureSetResolver.defaultInstance()
					.featuresOf(de.flapdoodle.embed.process.distribution.Version.of(specificVersion));
				this.numericVersion = NumericVersion.of(vName);
				this.mongoToolsVersion = null;
		}

		Version(String vName, MongotoolsVersion.Main mongoToolsVersion) {
				this.specificVersion = vName;
				this.features = FeatureSetResolver.defaultInstance()
					.featuresOf(de.flapdoodle.embed.process.distribution.Version.of(specificVersion));
				this.numericVersion = NumericVersion.of(vName);
				this.mongoToolsVersion = mongoToolsVersion;
		}

		@Override
		public Optional<MongotoolsVersion.Main> mongotoolsVersion() {
				return Optional.ofNullable(mongoToolsVersion);
		}
		@Override
	public String asInDownloadPath() {
		return specificVersion;
	}

	@Override
	public NumericVersion numericVersion() {
		return numericVersion;
	}

	@Override
	public FeatureSet features() {
		return features;
	}
	@Override
	public String toString() {
		return "Version{" + specificVersion + '}';
	}

	public enum Main implements IFeatureAwareVersion, HasMongotoolsPackage {
		@Deprecated
		V1_8(V1_8_5),

		@Deprecated
		V2_0(V2_0_9),
		@Deprecated
		V2_1(V2_1_2),
		@Deprecated
		V2_2(V2_2_7),
		@Deprecated
		V2_3(V2_3_0),
		@Deprecated
		V2_4(V2_4_10),
        @Deprecated
		V2_5(V2_5_4),
        @Deprecated
		V2_6(V2_6_11),
        @Deprecated
        V2_7(V2_7_1),

		// Older releases
		@Deprecated
		V3_0(V3_0_8),
		@Deprecated
		V3_1(V3_1_6),

		@Deprecated
		V3_2(V3_2_20),
		@Deprecated
		V3_3(V3_3_1),
		@Deprecated
		V3_4(V3_4_15),
		@Deprecated
		V3_5(V3_5_5),
		V3_6(V3_6_22),
		V4_0(V4_0_12),
		V4_2(V4_2_23),
		V4_4(V4_4_18),
		V5_0(V5_0_26),
		V6_0(V6_0_14),
		V7_0(V7_0_7),

		V6_3_DEV(V6_3_2_DEV),
		@Deprecated
		V7_0_RC(V7_0_0_RC10),

		@Deprecated
		LEGACY(V3_6),
		/**
		 * @deprecated use V4_4 instead
		 */
		@Deprecated
		PRODUCTION(V4_4),
		/**
		 * @deprecated use V5_0 instead
		 */
		@Deprecated
		DEVELOPMENT(V5_0);

		private final IFeatureAwareVersion _latest;

		Main(IFeatureAwareVersion latest) {
			_latest = latest;
		}

		@Override
		public String asInDownloadPath() {
			return _latest.asInDownloadPath();
		}

		@Override
		@Deprecated
		public NumericVersion numericVersion() {
			return _latest.numericVersion();
		}

		@Override
		public FeatureSet features() {
			return _latest.features();
		}

		@Override
		public Optional<? extends de.flapdoodle.embed.process.distribution.Version> mongotoolsVersion() {
					return _latest instanceof HasMongotoolsPackage
							? ((HasMongotoolsPackage) _latest).mongotoolsVersion()
							: Optional.empty();
			}
	}
}
