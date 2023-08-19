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
package de.flapdoodle.embed.mongo.examples;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MongodAuthTest {
	private static final int PORT = 27018;
	private static final String USERNAME_ADMIN = "admin-user";
	private static final String PASSWORD_ADMIN = "admin-password";
	private static final String USERNAME_NORMAL_USER = "test-db-user";
	private static final String PASSWORD_NORMAL_USER = "test-db-user-password";
	private static final String DB_ADMIN = "admin";
	private static final String DB_TEST = "test-db";
	private static final String COLL_TEST = "test-coll";

	@Test
	@Disabled("with --auth shutdown on tearDown does not work")
	public void noRole() {
		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(true)) {
			final ServerAddress address = getServerAddress(running);

			try (final MongoClient clientWithoutCredentials = new MongoClient(address)) {
				// do nothing
			}
		}
	}

	@Test
	public void customRole() {
		String roleName = "listColls";
		Listener withRunningMongod = EnableAuthentication.of(USERNAME_ADMIN, PASSWORD_ADMIN)
			.withEntries(
				EnableAuthentication.role(DB_TEST, COLL_TEST, roleName).withActions("listCollections"),
				EnableAuthentication.user(DB_TEST, USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER).withRoles(roleName, "readWrite")
			)
			.withRunningMongod();


		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(true, withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = new MongoClient(address, credentialAdmin, MongoClientOptions.builder().build())) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
				db.getCollection(COLL_TEST)
					.insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());

			try (final MongoClient clientNormalUser =
				new MongoClient(address, credentialNormalUser, MongoClientOptions.builder().build())) {
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				assertThat(actual).containsExactly(COLL_TEST);
			}
		}
	}

	@Test
	@Disabled("readAnyDatabase is not assignable")
	public void readAnyDatabaseRole() {
		Listener withRunningMongod = EnableAuthentication.of(USERNAME_ADMIN, PASSWORD_ADMIN)
			.withEntries(
				EnableAuthentication.user(DB_TEST, USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER).withRoles("readAnyDatabase")
			)
			.withRunningMongod();

		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);
//			try (final MongoClient clientWithoutCredentials = new MongoClient(address)) {
//				// Create admin user.
//				clientWithoutCredentials.getDatabase(DB_ADMIN)
//					.runCommand(commandCreateUser(USERNAME_ADMIN, PASSWORD_ADMIN, "root"));
//			}

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = new MongoClient(address, credentialAdmin, MongoClientOptions.builder().build())) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
//				// Create normal user and grant them the builtin "readAnyDatabase" role.
//				// FIXME This unexpectedly fails with "No role named readAnyDatabase@test-db".
//				db.runCommand(commandCreateUser(USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER, "readAnyDatabase"));
				// Create collection.
				db.getCollection(COLL_TEST).insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());

			try (final MongoClient clientNormalUser =
				new MongoClient(address, credentialNormalUser, MongoClientOptions.builder().build())) {
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				assertThat(actual)
					.containsExactly(COLL_TEST);
			}
		}
	}

	@Test
	public void readRole() {
		Listener withRunningMongod = EnableAuthentication.of(USERNAME_ADMIN, PASSWORD_ADMIN)
			.withEntries(
				EnableAuthentication.user(DB_TEST, USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER).withRoles("read")
			)
			.withRunningMongod();

		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = new MongoClient(address, credentialAdmin, MongoClientOptions.builder().build())) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
				db.getCollection(COLL_TEST).insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());
			
			try (final MongoClient clientNormalUser =
				new MongoClient(address, credentialNormalUser, MongoClientOptions.builder().build())) {
				final List<String> expected = Lists.newArrayList(COLL_TEST);
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				Assertions.assertIterableEquals(expected, actual);
			}
		}
	}

	private static TransitionWalker.ReachedState<RunningMongodProcess> startMongod(Listener... listener) {
		return startMongod(false, listener);
	}

	private static TransitionWalker.ReachedState<RunningMongodProcess> startMongod(boolean withAuth, Listener... listener) {
		return Mongod.builder()
			.net(Start.to(Net.class)
				.initializedWith(Net.defaults().withPort( PORT))
			)
			.mongodArguments(Start.to(MongodArguments.class)
				.initializedWith(MongodArguments.defaults()
					.withAuth(withAuth)))
			.build()
			.start(Version.Main.V4_4, listener);
	}

	private static ServerAddress getServerAddress(
		final TransitionWalker.ReachedState<RunningMongodProcess> running
	) {
		final de.flapdoodle.embed.mongo.commands.ServerAddress address = running.current().getServerAddress();
		return new ServerAddress(address.getHost(), address.getPort());
	}

}
