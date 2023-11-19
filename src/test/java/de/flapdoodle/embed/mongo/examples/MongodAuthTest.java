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
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.client.AuthenticationSetup;
import de.flapdoodle.embed.mongo.client.ClientActions;
import de.flapdoodle.embed.mongo.client.SyncClientAdapter;
import de.flapdoodle.embed.mongo.client.UsernamePassword;
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
import java.util.List;

import static de.flapdoodle.embed.mongo.MongoClientUtil.mongoClient;
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

			try (final MongoClient clientWithoutCredentials = MongoClients.create("mongodb://" + address)) {
				// do nothing
			}
		}
	}

	@Test
	public void customRole() {
		String roleName = "listColls";

		Listener withRunningMongod = ClientActions.setupAuthentication(new SyncClientAdapter(), DB_ADMIN, AuthenticationSetup.of(UsernamePassword.of(USERNAME_ADMIN, PASSWORD_ADMIN))
			.withEntries(
				AuthenticationSetup.role(DB_TEST, COLL_TEST, roleName)
					.withActions("listCollections"),
				AuthenticationSetup.user(DB_TEST, UsernamePassword.of(USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER)).withRoles(roleName, "readWrite")
			));


		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(true, withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = mongoClient(address, credentialAdmin)) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
				db.getCollection(COLL_TEST)
					.insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());

			try (final MongoClient clientNormalUser =mongoClient(address, credentialNormalUser)) {
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				assertThat(actual).containsExactly(COLL_TEST);
			}
		}
	}

	@Test
	@Disabled("readAnyDatabase is not assignable")
	public void readAnyDatabaseRole() {
		Listener withRunningMongod = ClientActions.setupAuthentication(new SyncClientAdapter(), DB_ADMIN, AuthenticationSetup.of(UsernamePassword.of(USERNAME_ADMIN, PASSWORD_ADMIN))
			.withEntries(
				AuthenticationSetup.user(DB_TEST, UsernamePassword.of(USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER)).withRoles("readAnyDatabase")
			));

		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);
//			try (final MongoClient clientWithoutCredentials = new MongoClient(address)) {
//				// Create admin user.
//				clientWithoutCredentials.getDatabase(DB_ADMIN)
//					.runCommand(commandCreateUser(USERNAME_ADMIN, PASSWORD_ADMIN, "root"));
//			}

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = mongoClient(address, credentialAdmin)) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
//				// Create normal user and grant them the builtin "readAnyDatabase" role.
//				// FIXME This unexpectedly fails with "No role named readAnyDatabase@test-db".
//				db.runCommand(commandCreateUser(USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER, "readAnyDatabase"));
				// Create collection.
				db.getCollection(COLL_TEST).insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());

			try (final MongoClient clientNormalUser = mongoClient(address, credentialNormalUser)) {
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				assertThat(actual)
					.containsExactly(COLL_TEST);
			}
		}
	}

	@Test
	public void readRole() {
		Listener withRunningMongod = ClientActions.setupAuthentication(new SyncClientAdapter(), DB_ADMIN, AuthenticationSetup.of(UsernamePassword.of(USERNAME_ADMIN, PASSWORD_ADMIN))
			.withEntries(
				AuthenticationSetup.user(DB_TEST, UsernamePassword.of(USERNAME_NORMAL_USER, PASSWORD_NORMAL_USER)).withRoles("read")
			));

		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = mongoClient(address, credentialAdmin)) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
				db.getCollection(COLL_TEST).insertOne(new Document(ImmutableMap.of("key", "value")));
			}

			final MongoCredential credentialNormalUser =
				MongoCredential.createCredential(USERNAME_NORMAL_USER, DB_TEST, PASSWORD_NORMAL_USER.toCharArray());
			
			try (final MongoClient clientNormalUser = mongoClient(address, credentialNormalUser)) {
				final List<String> expected = Lists.newArrayList(COLL_TEST);
				final ArrayList<String> actual = clientNormalUser.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
				Assertions.assertIterableEquals(expected, actual);
			}
		}
	}

	@Test
	public void withoutAnyAditionalConfig() {
		Listener withRunningMongod = ClientActions.setupAuthentication(new SyncClientAdapter(), DB_ADMIN,
			AuthenticationSetup.of(UsernamePassword.of(USERNAME_ADMIN, PASSWORD_ADMIN))
		);

		try (final TransitionWalker.ReachedState<RunningMongodProcess> running = startMongod(true, withRunningMongod)) {
			final ServerAddress address = getServerAddress(running);

			final MongoCredential credentialAdmin =
				MongoCredential.createCredential(USERNAME_ADMIN, DB_ADMIN, PASSWORD_ADMIN.toCharArray());

			try (final MongoClient clientAdmin = mongoClient(address, credentialAdmin)) {
				final MongoDatabase db = clientAdmin.getDatabase(DB_TEST);
				db.getCollection(COLL_TEST).insertOne(new Document(ImmutableMap.of("key", "value")));

				final List<String> expected = Lists.newArrayList(COLL_TEST);
				final ArrayList<String> actual = clientAdmin.getDatabase(DB_TEST).listCollectionNames().into(new ArrayList<>());
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
