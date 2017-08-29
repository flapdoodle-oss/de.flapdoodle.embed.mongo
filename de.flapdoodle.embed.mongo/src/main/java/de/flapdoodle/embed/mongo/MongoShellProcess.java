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
package de.flapdoodle.embed.mongo;

import java.io.IOException;
import java.util.List;

import de.flapdoodle.embed.mongo.config.IMongoShellConfig;
import de.flapdoodle.embed.mongo.runtime.MongoShell;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;

/**
 *
 */
public class MongoShellProcess extends AbstractMongoProcess<IMongoShellConfig, MongoShellExecutable, MongoShellProcess> {

	public MongoShellProcess(Distribution distribution, IMongoShellConfig config, IRuntimeConfig runtimeConfig,
			MongoShellExecutable mongoShellExecutable) throws IOException {
		super(distribution, config, runtimeConfig, mongoShellExecutable);
	}
	
	@Override
	protected String successMessage() {
		return "connecting to:";
	}
	
	@Override
	protected List<String> getCommandLine(Distribution distribution, IMongoShellConfig config, IExtractedFileSet files) throws IOException {
		return MongoShell.getCommandLine(getConfig(), files);
	}

}
