### Usage

```java

try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().start(Version.Main.PRODUCTION)) {
  com.mongodb.ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
  try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
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

  com.mongodb.ServerAddress serverAddress2 = serverAddress(serverAddress);
  try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress2)) {
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
    com.mongodb.ServerAddress serverAddress1 = serverAddress(runningMongos.current().getServerAddress());
    try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress1)) {
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

    AuthenticationSetup setup = AuthenticationSetup.of(UsernamePassword.of("i-am-admin", "admin-password".toCharArray()))
      .withEntries(
        AuthenticationSetup.role("test-db", "test-collection", "can-list-collections")
          .withActions("listCollections"),
        ImmutableUser.of("test-db", UsernamePassword.of("read-only", "user-password".toCharArray()))
          .withRoles("can-list-collections", "read")
      );

    Listener withRunningMongod = ClientActions.authSetup(new SyncClientAdapter(), "admin", setup);

//    SyncClientAdapter.rolesAndReplication()

    try (TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance()
      .withMongodArguments(
        Start.to(MongodArguments.class)
          .initializedWith(MongodArguments.defaults().withAuth(true)))
      .start(Version.Main.PRODUCTION, withRunningMongod)) {

      try (MongoClient mongo = mongoClient(
        serverAddress(running.current().getServerAddress()),
        MongoCredential.createCredential("i-am-admin", "admin", "admin-password".toCharArray()))) {

        MongoDatabase db = mongo.getDatabase("test-db");
        MongoCollection<Document> col = db.getCollection("test-collection");
        col.insertOne(new Document("testDoc", new Date()));
      }

      try (MongoClient mongo = mongoClient(
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