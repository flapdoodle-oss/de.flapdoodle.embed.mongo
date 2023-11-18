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

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.embed.mongo.MongoClientF;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import org.bson.Document;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Value.Immutable
public abstract class EnableAuthentication {
	private static Logger LOGGER= LoggerFactory.getLogger(EnableAuthentication.class);

	@Value.Parameter
	protected abstract String adminUser();
	@Value.Parameter
	protected abstract String adminPassword();

	@Value.Default
	protected List<Entry> entries() {
		return Collections.emptyList();
	}

	public interface Entry {

	}

	@Value.Immutable
	public interface Role extends Entry {
		@Value.Parameter
		String database();
		@Value.Parameter
		String collection();
		@Value.Parameter
		String name();
		List<String> actions();
	}

	@Value.Immutable
	public interface User extends Entry {
		@Value.Parameter
		String database();
		@Value.Parameter
		String username();
		@Value.Parameter
		String password();
		List<String> roles();
	}

	@Value.Auxiliary
	public Listener withRunningMongod() {
		StateID<RunningMongodProcess> expectedState = StateID.of(RunningMongodProcess.class);

		return Listener.typedBuilder()
			.onStateReached(expectedState, running -> {
					final ServerAddress address = serverAddress(running);

				// Create admin user.
				try (final MongoClient clientWithoutCredentials = MongoClientF.client(address)) {
					runCommand(
						clientWithoutCredentials.getDatabase("admin"),
						commandCreateUser(adminUser(), adminPassword(), Arrays.asList("root"))
					);
				}

				final MongoCredential credentialAdmin =
					MongoCredential.createCredential(adminUser(), "admin", adminPassword().toCharArray());

				// create roles and users
				try (final MongoClient clientAdmin = MongoClientF.client(address, credentialAdmin)) {
					entries().forEach(entry -> {
						if (entry instanceof Role) {
							Role role = (Role) entry;
							MongoDatabase db = clientAdmin.getDatabase(role.database());
							runCommand(db, commandCreateRole(role.database(), role.collection(), role.name(), role.actions()));
						}
						if (entry instanceof User) {
							User user = (User) entry;
							MongoDatabase db = clientAdmin.getDatabase(user.database());
							runCommand(db, commandCreateUser(user.username(), user.password(), user.roles()));
						}
					});
				}

			})
			.onStateTearDown(expectedState, running -> {
				final ServerAddress address = serverAddress(running);

				final MongoCredential credentialAdmin =
					MongoCredential.createCredential(adminUser(), "admin", adminPassword().toCharArray());

				try (final MongoClient clientAdmin = MongoClientF.client(address, credentialAdmin)) {
					try {
						// if success there will be no answer, the connection just closes..
						runCommand(
							clientAdmin.getDatabase("admin"),
							new Document("shutdown", 1).append("force", true)
						);
					} catch (MongoSocketReadException mx) {
						LOGGER.debug("shutdown completed by closing stream");
					}

					running.shutDownCommandAlreadyExecuted();
				}
			})
			.build();
	}

	private static void runCommand(MongoDatabase db, Document document) {
		Document result = db.runCommand(document);
		boolean success = result.get("ok", Double.class) == 1.0d;
		Preconditions.checkArgument(success, "runCommand %s failed: %s", document, result);
	}

	private static Document commandCreateRole(
		String database,
		String collection,
		String roleName,
		List<String> actions
	) {
		return new Document("createRole", roleName)
			.append("privileges", Collections.singletonList(
					new Document("resource",
						new Document("db", database)
							.append("collection", collection))
						.append("actions", actions)
				)
			).append("roles", Collections.emptyList());
	}

	static Document commandCreateUser(
		final String username,
		final String password,
		final List<String> roles
	) {
		return new Document("createUser", username)
			.append("pwd", password)
			.append("roles", roles);
	}

	private static ServerAddress serverAddress(RunningMongodProcess running) {
		de.flapdoodle.embed.mongo.commands.ServerAddress serverAddress = running.getServerAddress();
		return new ServerAddress(serverAddress.getHost(), serverAddress.getPort());
	}

	public static ImmutableRole role(String database, String collection, String name) {
		return ImmutableRole.of(database, collection, name);
	}

	public static ImmutableUser user(String database, String username, String password) {
		return ImmutableUser.of(database, username, password);
	}

	public static ImmutableEnableAuthentication of(String adminUser, String adminPassword) {
		return ImmutableEnableAuthentication.of(adminUser,adminPassword);
	}
}
