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
package de.flapdoodle.embed.mongo.runtime;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.flapdoodle.embed.mongo.config.IMongoImportConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;

/**
 * Created by canyaman on 10/04/14.
 */
public class MongoImport extends AbstractMongo {

    public static List<String> getCommandLine(IMongoImportConfig config, IExtractedFileSet files)
            throws UnknownHostException {
        List<String> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(files.executable().getAbsolutePath()));
        if (config.cmdOptions().isVerbose()) {
            ret.add("-v");
        }
        Net net = config.net();
        ret.add("--port");
        ret.add("" + net.getPort());
        if (net.isIpv6()) {
            ret.add("--ipv6");
        }
        if (net.getBindIp()!=null) {
            ret.add("--host");
            ret.add(net.getBindIp());
        }

        if (config.getDatabaseName()!=null) {
            ret.add("--db");
            ret.add(config.getDatabaseName());
        }
        if (config.getCollectionName()!=null) {
            ret.add("--collection");
            ret.add(config.getCollectionName());
        }
        if (config.isJsonArray()) {
            ret.add("--jsonArray");
        }
        if (config.isDropCollection()) {
            ret.add("--drop");
        }
        if (config.isUpsertDocuments()) {
            ret.add("--upsert");
        }
        if (config.getImportFile()!=null) {
            ret.add("--file");
            ret.add(config.getImportFile());
        }
		if (config.isHeaderline()) {
			ret.add("--headerline");
		}
		if (config.getType() != null) {
			ret.add("--type");
			ret.add(config.getType());
		}

        return ret;
    }
}
