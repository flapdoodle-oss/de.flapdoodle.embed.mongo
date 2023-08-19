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

import de.flapdoodle.embed.process.transitions.Executer;
import de.flapdoodle.embed.process.types.ExecutedProcess;
import de.flapdoodle.embed.process.types.RunningProcess;
import de.flapdoodle.embed.process.types.RunningProcessImpl;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.Transition;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ExecutedMongoDumpProcess implements ExecutedProcess {

	public static ExecutedMongoDumpProcess stop(RunningProcess process) {
		return ImmutableExecutedMongoDumpProcess.builder()
			.returnCode(process.stop())
			.build();
	}

	public static Transition<ExecutedMongoDumpProcess> withDefaults() {
		return Executer.with(RunningProcessImpl::new, ExecutedMongoDumpProcess::stop)
			.destination(StateID.of(ExecutedMongoDumpProcess.class))
			.build();
	}
}
