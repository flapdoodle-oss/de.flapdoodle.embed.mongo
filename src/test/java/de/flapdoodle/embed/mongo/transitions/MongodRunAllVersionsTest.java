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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.Versions;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.os.CPUType;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.OSType;
import de.flapdoodle.reverse.TransitionWalker;
import org.bson.Document;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Test whether a race condition occurs between setup and tear down of setting
 * up and closing a mongo process.
 * <p/>
 * This test will run a long time based on the download process for all mongodb versions.
 *
 * @author m.joehren
 */
@RunWith(value = Parameterized.class)
public class MongodRunAllVersionsTest {

	@Parameters(name = "{0}")
	public static java.util.Collection<Object[]> data() {
		final Collection<Object[]> result = new ArrayList<>();
		int unknownId = 0;
		for (final de.flapdoodle.embed.process.distribution.Version version : Versions.testableVersions(Version.Main.class)) {
			if (version instanceof Enum) {
				result.add(new Object[]{((Enum<?>) version).name(), version});
			} else {
				result.add(new Object[]{"unknown version " + (unknownId++), version});
			}
		}
		return result;
	}

	@Parameter
	public String mongoVersionName;

	@Parameter(value = 1)
	public IFeatureAwareVersion mongoVersion;

	private TransitionWalker.ReachedState<RunningMongodProcess> running;

	@Before
	public void setUp() {

		final Distribution distribution = Distribution.detectFor(CommonOS.list(), mongoVersion);
		if (distribution.platform().operatingSystem().type() == OSType.Linux && distribution.platform().architecture().cpuType() == CPUType.ARM) {
			Assume.assumeTrue("Mongodb supports Linux ARM64 since 3.4.0", mongoVersion.numericVersion().isNewerOrEqual(3, 4, 0));
		}

		running = Mongod.instance().start(mongoVersion);
	}

	@After
	public void tearDown() {
		running.close();
	}

	@Test
	public void testInsert1() throws UnknownHostException {
		com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
		try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
			MongoDatabase db = mongo.getDatabase("test");
			db.createCollection("testCol");
			MongoCollection<Document> col = db.getCollection("testCol");
			col.insertOne(new Document("testDoc", new Date()));
		}
	}

	@Test
	public void testInsert2() throws UnknownHostException {
		com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
		try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
			MongoDatabase db = mongo.getDatabase("test");
			db.createCollection("testCol");
			MongoCollection<Document> col = db.getCollection("testCol");
			col.insertOne(new Document("testDoc", new Date()));
		}
	}

	private com.mongodb.ServerAddress serverAddress(ServerAddress serverAddress) {
		return new com.mongodb.ServerAddress(serverAddress.getHost(), serverAddress.getPort());
	}

}
