### Usage

```java

try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().start(Version.Main.PRODUCTION)) {
  try (MongoClient mongo = MongoClientF.client(serverAddress(running.current().getServerAddress()))) {
    MongoDatabase db = mongo.getDatabase("test");
    MongoCollection<Document> col = db.getCollection("testCol");
    col.insertOne(new Document("testDoc", new Date()));
...

  }
}

```

#### Customize by Override

```java
Mongod mongod = new Mongod() {
  @Override
  public Transition<DistributionBaseUrl> distributionBaseUrl() {
    return Start.to(DistributionBaseUrl.class)
      .initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain"));
  }
};
```

#### Customize by Builder

```java
Mongod mongod = Mongod.builder()
  .distributionBaseUrl(Start.to(DistributionBaseUrl.class)
    .initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain")))
  .build();
```

#### Customize by Replacement

```java
Transitions mongod = Mongod.instance()
  .transitions(Version.Main.PRODUCTION)
  .replace(Start.to(DistributionBaseUrl.class)
    .initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain")));
```

### Main Versions
```java
IFeatureAwareVersion version = Version.V2_2_5;
// uses latest supported 2.2.x Version
version = Version.Main.V2_2;
// uses latest supported production version
version = Version.Main.PRODUCTION;
// uses latest supported development version
version = Version.Main.DEVELOPMENT;
```

### Command Line Post Processing
```java
// TODO change command line arguments before calling process start??
```

### Custom Command Line Options

We changed the syncDelay to 0 which turns off sync to disc. To turn on default value used defaultSyncDelay().
```java
new Mongod() {
  @Override
  public Transition<MongodArguments> mongodArguments() {
    return Start.to(MongodArguments.class)
      .initializedWith(MongodArguments.defaults().withSyncDelay(10)
        .withUseNoPrealloc(false)
        .withUseSmallFiles(false)
        .withUseNoJournal(false)
        .withEnableTextSearch(true));
  }
}.transitions(Version.Main.PRODUCTION);
```

### Snapshot database files from temp dir

We changed the syncDelay to 0 which turns off sync to disc. To get the files to create an snapshot you must turn on default value (use defaultSyncDelay()).
```java

Listener listener = Listener.typedBuilder()
  .onStateTearDown(StateID.of(DatabaseDir.class), databaseDir -> {
    Try.run(() -> FileUtils.copyDirectory(databaseDir.value(), destination));
  })
  .build();

try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().transitions(Version.Main.PRODUCTION).walker()
  .initState(StateID.of(RunningMongodProcess.class), listener)) {
}

assertThat(destination)
  .isDirectory()
  .isDirectoryContaining(path -> path.getFileName().toString().startsWith("WiredTiger.lock"));

```

### Start mongos with mongod instance

this is an very easy example to use mongos and mongod
```java
Version.Main version = Version.Main.PRODUCTION;

Mongod mongod = new Mongod() {
  @Override
  public Transition<MongodArguments> mongodArguments() {
    return Start.to(MongodArguments.class).initializedWith(MongodArguments.defaults()
      .withIsConfigServer(true)
      .withReplication(Storage.of("testRepSet", 5000)));
  }
};

try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongod = mongod.start(version)) {

  ServerAddress serverAddress = runningMongod.current().getServerAddress();

  try (MongoClient mongo = MongoClientF.client(serverAddress(serverAddress))) {
    mongo.getDatabase("admin").runCommand(new Document("replSetInitiate", new Document()));
  }

  Mongos mongos = new Mongos() {
    @Override public Start<MongosArguments> mongosArguments() {
      return Start.to(MongosArguments.class).initializedWith(MongosArguments.defaults()
        .withConfigDB(serverAddress.toString())
        .withReplicaSet("testRepSet")
      );
    }
  };

  try (TransitionWalker.ReachedState<RunningMongosProcess> runningMongos = mongos.start(version)) {
    try (MongoClient mongo = MongoClientF.client(serverAddress(runningMongos.current().getServerAddress()))) {
      assertThat(mongo.listDatabaseNames()).contains("admin", "config");
    }
  }
}
```

### Import JSON file with mongoimport command
```java

Version.Main version = Version.Main.PRODUCTION;

Transitions transitions = MongoImport.instance().transitions(version)
  .replace(Start.to(MongoImportArguments.class).initializedWith(MongoImportArguments.builder()
    .databaseName("importTestDB")
    .collectionName("importedCollection")
    .upsertDocuments(true)
    .dropCollection(true)
    .isJsonArray(true)
    .importFile(jsonFile)
    .build()))
  .addAll(Derive.given(RunningMongodProcess.class).state(ServerAddress.class)
    .deriveBy(Try.function(RunningMongodProcess::getServerAddress).mapToUncheckedException(RuntimeException::new)))
  .addAll(Mongod.instance().transitions(version).walker()
    .asTransitionTo(TransitionMapping.builder("mongod", StateID.of(RunningMongodProcess.class))
      .build()));

try (TransitionWalker.ReachedState<RunningMongodProcess> runningMongoD = transitions.walker()
  .initState(StateID.of(RunningMongodProcess.class))) {

  try (TransitionWalker.ReachedState<ExecutedMongoImportProcess> executedImport = runningMongoD.initState(
    StateID.of(ExecutedMongoImportProcess.class))) {

    assertThat(executedImport.current().returnCode())
      .describedAs("import successful")
      .isEqualTo(0);
  }
}

```
                      
### User/Roles setup

```java

Listener withRunningMongod = EnableAuthentication.of("i-am-admin", "admin-password")
  .withEntries(
    EnableAuthentication.role("test-db", "test-collection", "can-list-collections")
      .withActions("listCollections"),
    EnableAuthentication.user("test-db", "read-only", "user-password")
      .withRoles("can-list-collections", "read")
  ).withRunningMongod();

try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance()
  .withMongodArguments(
    Start.to(MongodArguments.class)
      .initializedWith(MongodArguments.defaults().withAuth(true)))
  .start(Version.Main.PRODUCTION, withRunningMongod)) {

  try (MongoClient mongo = MongoClientF.client(
    serverAddress(running.current().getServerAddress()),
    MongoCredential.createCredential("i-am-admin", "admin", "admin-password".toCharArray()))) {

    MongoDatabase db = mongo.getDatabase("test-db");
    MongoCollection<Document> col = db.getCollection("test-collection");
    col.insertOne(new Document("testDoc", new Date()));
  }

  try (MongoClient mongo = MongoClientF.client(
    serverAddress(running.current().getServerAddress()),
    MongoCredential.createCredential("read-only", "test-db", "user-password".toCharArray()))) {

    MongoDatabase db = mongo.getDatabase("test-db");
    MongoCollection<Document> col = db.getCollection("test-collection");
    assertThat(col.countDocuments()).isEqualTo(1L);

    assertThatThrownBy(() -> col.insertOne(new Document("testDoc", new Date())))
      .isInstanceOf(MongoCommandException.class)
      .message().contains("not authorized on test-db");
  }
}

```

```java
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
```