/**
 * Copyright (C) 2011
 *   Can Yaman <can@yaman.me>
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
import java.util.Arrays;
import java.util.List;

import de.flapdoodle.embed.mongo.config.MongoDumpConfig;
import de.flapdoodle.embed.mongo.runtime.MongoDump;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.ExtractedFileSet;

public class MongoDumpProcess extends AbstractMongoProcess<MongoDumpConfig, MongoDumpExecutable, MongoDumpProcess> {

    public MongoDumpProcess(Distribution distribution, MongoDumpConfig config, RuntimeConfig runtimeConfig,
                            MongoDumpExecutable mongosExecutable) throws IOException {
        super(distribution, config, runtimeConfig, mongosExecutable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, MongoDumpConfig config, ExtractedFileSet files)
            throws IOException {
        return MongoDump.getCommandLine(getConfig(), files);
    }
    @Override
    protected List<String> successMessage() {
        return Arrays.asList("dumped");
    }

    @Override public void stopInternal() {
        // Nothing to stop since we are just running mongo restore and don't want to kill the mongo instance
    }
}
