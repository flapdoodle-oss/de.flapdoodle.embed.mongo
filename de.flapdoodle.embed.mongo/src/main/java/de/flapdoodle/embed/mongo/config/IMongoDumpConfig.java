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
package de.flapdoodle.embed.mongo.config;

public interface IMongoDumpConfig extends IMongoConfig {
   public boolean isVerbose();
   public String getDatabaseName();
   public String getCollectionName();

   public String getQuery();
   public String getQueryFile();
   public String getReadPreference();
   public boolean isForceTableScan();

   public String getArchive();
   public boolean isDumpDbUsersAndRoles();
   public boolean isGzip();
   public boolean isRepair();
   public String getOut();
   public boolean isOplog();
   public String getExcludeCollection();
   public String getExcludeCollectionWithPrefix();
   public Integer getNumberOfParallelCollections();
}
