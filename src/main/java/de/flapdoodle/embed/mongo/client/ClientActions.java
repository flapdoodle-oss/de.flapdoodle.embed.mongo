package de.flapdoodle.embed.mongo.client;

import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Feature;
import de.flapdoodle.embed.mongo.transitions.RunningMongoProcess;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.Listener;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.types.Try;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ClientActions {
	private static Logger logger = LoggerFactory.getLogger(ClientActions.class);

	private ClientActions() {
		// no instance
	}

	public static Listener setupAuthentication(ExecuteMongoClientAction<?> executeAction, String databaseName, AuthenticationSetup setup) {
		Listener.TypedListener.Builder typedBuilder = Listener.typedBuilder();

		StateID<RunningMongodProcess> expectedState = StateID.of(RunningMongodProcess.class);

		UsernamePassword admin = setup.admin();

		// client action without credentials
		MongoClientAction createAdminUser = MongoClientAction.runCommand("admin",
			MongoClientAction.createUser(admin.name(), admin.passwordAsString(), Arrays.asList("root")));

		List<MongoClientAction> setupRoles;

		if (setup.entries().isEmpty()) {
			setupRoles = Arrays.asList(
				// test list collections to fail fast if something went wrong
				MongoClientAction.runCommand(databaseName, MongoClientAction.listCollections())
					.withCredentials(MongoClientAction.credentials(databaseName, admin.name(), admin.password()))
			);
		} else {
			MongoClientAction.Credentials adminCredentials = MongoClientAction.credentials("admin", admin.name(), admin.password());

			setupRoles = setup.entries()
				.stream().map(entry -> {
					if (entry instanceof AuthenticationSetup.Role) {
						AuthenticationSetup.Role role = (AuthenticationSetup.Role) entry;
						return MongoClientAction.runCommand(role.database(),
								MongoClientAction.createRole(role.name(),
									MongoClientAction.privilege(role.database(), role.collection(), role.actions())))
							.withCredentials(adminCredentials);
					}
					if (entry instanceof AuthenticationSetup.User) {
						AuthenticationSetup.User user = (AuthenticationSetup.User) entry;
						return MongoClientAction.runCommand(user.database(),
								MongoClientAction.createUser(user.user().name(), user.user().passwordAsString(), user.roles()))
							.withCredentials(adminCredentials);
					}
					throw new IllegalArgumentException("not supported: " + entry);
				})
				.collect(Collectors.toList());
		}

		typedBuilder.onStateReached(expectedState,
			executeClientActions(executeAction, createAdminUser)
				.andThen(executeClientActions(executeAction, setupRoles)));

		typedBuilder.onStateTearDown(StateID.of(RunningMongodProcess.class),
			executeClientActions(executeAction, shutdown(admin.name(), admin.password()))
				.andThen(RunningMongoProcess::shutDownCommandAlreadyExecuted));

		return typedBuilder.build();
	}

	public static Listener initReplicaSet(
		ExecuteMongoClientAction<?> executeAction,
		IFeatureAwareVersion version,
		Storage replication
	) {
		return initReplicaSet(executeAction, version, replication, Optional.empty());
	}
	
	public static Listener initReplicaSet(
		ExecuteMongoClientAction<?> executeAction,
		IFeatureAwareVersion version,
		Storage replication,
		Optional<UsernamePassword> adminUser
	) {
		Listener.TypedListener.Builder builder = Listener.typedBuilder();

		Optional<MongoClientAction.Credentials> credentials = adminUser
			.map(it -> MongoClientAction.credentials("admin", it.name(), it.password()));

		if (version.enabled(Feature.RS_INITIATE)) {
			Consumer<RunningMongodProcess> initReplicaSet = runningMongodProcess -> {
				ServerAddress serverAddress = runningMongodProcess.getServerAddress();
				executeAction.execute(runningMongodProcess,
					MongoClientAction.runCommand("admin",
							new Document("replSetInitiate",
								new Document("_id", replication.getReplSetName())
									.append("members", Collections.singletonList(
										new Document("_id", 0)
											.append("host", serverAddress.getHost() + ":" + serverAddress.getPort())
									))))
						.withCredentials(credentials)
				);
			};

			builder.onStateReached(StateID.of(RunningMongodProcess.class), initReplicaSet.andThen(runningMongodProcess -> {
				AtomicBoolean isMaster = new AtomicBoolean();
				MongoClientAction checkIfMaster = MongoClientAction.runCommand("admin", new Document("isMaster", 1))
					.withOnResult(doc -> isMaster.set(doc.getBoolean("ismaster")))
					.withCredentials(credentials);

				long started = System.currentTimeMillis();
				long diff;
				do {
					executeAction.execute(runningMongodProcess, checkIfMaster);
					diff = System.currentTimeMillis() - started;
					logger.info("check if server is elected as master: {} (after {} ms)", isMaster.get(), diff);
					Try.run(() -> Thread.sleep(100));
				} while (!isMaster.get() && diff < 1000);

				if (!isMaster.get()) {
					throw new IllegalArgumentException(
						"initReplicaSet failed to elect " + runningMongodProcess.getServerAddress() + " as master after " + Duration.ofMillis(diff));
				}

			}));
		}

		return builder.build();
	}

	private static Consumer<RunningMongodProcess> executeClientActions(ExecuteMongoClientAction<?> executeAction, MongoClientAction... actions) {
		return runningMongodProcess -> executeClientActions(executeAction, runningMongodProcess, Arrays.asList(actions));
	}

	private static Consumer<RunningMongodProcess> executeClientActions(ExecuteMongoClientAction<?> executeAction, List<? extends MongoClientAction> actions) {
		return runningMongodProcess -> executeClientActions(executeAction, runningMongodProcess, actions);
	}

	private static void executeClientActions(ExecuteMongoClientAction<?> executeAction, RunningMongodProcess runningMongodProcess,
		List<? extends MongoClientAction> actions) {
		for (MongoClientAction action : actions) {
			executeAction.execute(runningMongodProcess, action);
		}
	}

	private static MongoClientAction shutdown(String username, char[] password) {
		return MongoClientAction.shutdown("admin")
			.withCredentials(MongoClientAction.credentials("admin", username, password))
			.withOnError(ex -> logger.debug("expected send shutdown exception", ex));
	}
}
