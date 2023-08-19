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

import de.flapdoodle.embed.mongo.commands.MongoRestoreArguments;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.naming.HasLabel;
import org.immutables.value.Value;

@Value.Immutable
public abstract class MongoRestoreProcessArguments extends MongoToolsProcessArguments<MongoRestoreArguments> implements HasLabel {

	@Override
	@Value.Auxiliary
	public String transitionLabel() {
		return "Create mongoRestore arguments";
	}

	@Override
	@Value.Default
	public StateID<MongoRestoreArguments> arguments() {
		return StateID.of(MongoRestoreArguments.class);
	}

	public static ImmutableMongoRestoreProcessArguments withDefaults() {
		return builder().build();
	}

	public static ImmutableMongoRestoreProcessArguments.Builder builder() {
		return ImmutableMongoRestoreProcessArguments.builder();
	}
}
