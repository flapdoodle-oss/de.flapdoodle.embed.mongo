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
package de.flapdoodle.embed.mongo.config;

import java.io.IOException;
import java.net.UnknownHostException;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.process.builder.TypedProperty;

/**
 * Created by canyaman on 10/04/14.
 */
public class MongoImportConfigBuilder extends AbstractMongoConfigBuilder<IMongoImportConfig>  {

    protected static final TypedProperty<String> DB_NAME = TypedProperty.with("db", String.class);
    protected static final TypedProperty<String> IMPORT_FILE = TypedProperty.with("file", String.class);
    protected static final TypedProperty<String> COLLECTION = TypedProperty.with("collection", String.class);
	protected static final TypedProperty<String> TYPE = TypedProperty.with("type", String.class);
    protected static final TypedProperty<Boolean> HEADERLINE = TypedProperty.with("headerline", Boolean.class);
    protected static final TypedProperty<Boolean> JSON_ARRAY = TypedProperty.with("jsonArray", Boolean.class);
    protected static final TypedProperty<Boolean> UPSERT = TypedProperty.with("upsert", Boolean.class);
    protected static final TypedProperty<Boolean> DROP = TypedProperty.with("drop", Boolean.class);


    public MongoImportConfigBuilder() throws IOException {
        super();
        property(PID_FILE).setDefault("mongoimport.pid");
		property(TYPE).setDefault("json");
		property(HEADERLINE).setDefault(Boolean.FALSE);
    }

    public MongoImportConfigBuilder version(IFeatureAwareVersion version) {
        version().set(version);
        return this;
    }

    public MongoImportConfigBuilder timeout(Timeout timeout) {
        timeout().set(timeout);
        return this;
    }

    public MongoImportConfigBuilder net(Net net) {
        net().set(net);
        return this;
    }

    public MongoImportConfigBuilder cmdOptions(IMongoCmdOptions cmdOptions) {
        cmdOptions().set(cmdOptions);
        return this;
    }

    public MongoImportConfigBuilder importFile(String importFile) {
        set(IMPORT_FILE, importFile);
        return this;
    }

    public MongoImportConfigBuilder db(String dbName) {
        set(DB_NAME, dbName);
        return this;
    }

    public MongoImportConfigBuilder collection(String collection) {
        set(COLLECTION, collection);
        return this;
    }
	
	public MongoImportConfigBuilder type(String type) {
		set(TYPE, type);
		return this;
	}
	
	public MongoImportConfigBuilder headerline(boolean headerline) {
		set(HEADERLINE, headerline);
		return this;
	}

    public MongoImportConfigBuilder jsonArray(boolean jsonArray) {
        set(JSON_ARRAY, jsonArray);
        return this;
    }

    public MongoImportConfigBuilder upsert(boolean upsert) {
        set(UPSERT, upsert);
        return this;
    }

    public MongoImportConfigBuilder dropCollection(boolean dropCollection) {
        set(DROP, dropCollection);
        return this;
    }

    @Override
    public IMongoImportConfig build() {
        IFeatureAwareVersion version = version().get();
        Net net = net().get();
        Timeout timeout = timeout().get();
        String database = get(DB_NAME);
        String collection = get(COLLECTION);
        String importFile = get(IMPORT_FILE);
		String type = get(TYPE);
        Boolean headerline = get(HEADERLINE);
        Boolean jsonArray= get(JSON_ARRAY);
        Boolean upsert = get(UPSERT);
        Boolean drop = get(DROP);
        IMongoCmdOptions cmdOptions=get(CMD_OPTIONS);
        String pidFile = get(PID_FILE);

		return new ImmutableMongoImportConfig(version, net, timeout, cmdOptions, pidFile,
				database, collection, importFile, type, headerline, jsonArray, upsert, drop);
    }

    static class ImmutableMongoImportConfig extends ImmutableMongoConfig implements IMongoImportConfig {
        private final String _databaseName;
        private final String _getImportFile;
        private final String _collectionName;
		private final String _type;
		private final boolean _headerline;
        private final boolean _jsonArray;
        private final boolean _dropCollection;
        private final boolean _upsetDocuments;

		public ImmutableMongoImportConfig(IFeatureAwareVersion version, Net net, Timeout timeout, IMongoCmdOptions cmdOptions, String pidFile,
											String database, String collection, String importFile, String type, boolean headerline,
											boolean jsonArray, boolean upsert, boolean drop) {
			super(new SupportConfig(Command.MongoImport), version, net, null, null, timeout, cmdOptions, pidFile);
            _databaseName=database;
            _collectionName=collection;
            _getImportFile=importFile;
            _jsonArray=jsonArray;
            _dropCollection=drop;
            _upsetDocuments=upsert;
			_type=type;
			_headerline=headerline;
        }

        public String getDatabaseName(){
            return _databaseName;
        }
        public String getCollectionName(){
            return _collectionName;
        }
        public String getImportFile(){
            return _getImportFile;
        }
		public String getType(){
			return _type;
		}
		public boolean isHeaderline(){
			return _headerline;
		}
        public boolean isJsonArray() {
            return _jsonArray;
        }
        public boolean isDropCollection(){
            return _dropCollection;
        }
        public boolean isUpsertDocuments(){
            return _upsetDocuments;
        }
    }
}