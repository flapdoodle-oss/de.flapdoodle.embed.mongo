# Make Peace, No War!


# Organization Flapdoodle OSS
[![Build Status](https://travis-ci.org/flapdoodle-oss/de.flapdoodle.embed.mongo.svg?branch=master)](https://travis-ci.org/flapdoodle-oss/de.flapdoodle.embed.mongo)
[![Maven Central](https://img.shields.io/maven-central/v/de.flapdoodle.embed/de.flapdoodle.embed.mongo.svg)](https://maven-badges.herokuapp.com/maven-central/de.flapdoodle.embed/de.flapdoodle.embed.mongo)

We are now a github organization. You are invited to participate.
Starting with version 2 we are going to support only java 8 or higher. If you are looking for the older version you can find it in the 1.7 branch.

# Embedded MongoDB

Embedded MongoDB will provide a platform neutral way for running mongodb in unittests.

## Why?

- dropping databases causing some pains (often you have to wait long time after each test)
- its easy, much easier as installing right version by hand
- you can change version per test

## How?

- download mongodb (and cache it)
- extract it (and cache it)
- java uses its process api to start and monitor the mongo process
- you run your tests
- java kills the mongo process


## License

We use http://www.apache.org/licenses/LICENSE-2.0

## We need your help?

Poll: [Which MongoDB version should stay supported?](https://docs.google.com/forms/d/1Iu8Gy4W0dPfwsE2czoPJAGtYijjmfcZISgb7pU-dZ9U/viewform?usp=send_form)

## Dependencies

### Build on top of

- Embed Process Util [de.flapdoodle.embed.process](https://github.com/flapdoodle-oss/de.flapdoodle.embed.process)

### Other ways to use Embedded MongoDB

- in a Maven build using [maven-mongodb-plugin](https://github.com/Syncleus/maven-mongodb-plugin) or [embedmongo-maven-plugin](https://github.com/joelittlejohn/embedmongo-maven-plugin)
- in a Clojure/Leiningen project using [lein-embongo](https://github.com/joelittlejohn/lein-embongo)
- in a Gradle build using [gradle-mongo-plugin](https://github.com/sourcemuse/GradleMongoPlugin)
- in a Scala/specs2 specification using [specs2-embedmongo](https://github.com/athieriot/specs2-embedmongo)
- in Scala tests using [scalatest-embedmongo](https://github.com/SimplyScala/scalatest-embedmongo)

### Comments about Embedded MongoDB in the Wild

- http://stackoverflow.com/questions/6437226/embedded-mongodb-when-running-integration-tests
- http://blog.diabol.se/?p=390

### Other MongoDB Stuff

- https://github.com/thiloplanz/jmockmongo - mongodb mocking
- https://github.com/lordofthejars/nosql-unit - extended nosql unit testing
- https://github.com/jirutka/embedmongo-spring - Spring Factory Bean for EmbedMongo

### Backward binary compatibility and API changes

There is a report on backward binary compatibility and API changes for the library: https://abi-laboratory.pro/java/tracker/timeline/de.flapdoodle.embed.mongo/ -> thanks @lvc

## Howto

### Maven

Snapshots (Repository http://oss.sonatype.org/content/repositories/snapshots)

	<dependency>
		<groupId>de.flapdoodle.embed</groupId>
		<artifactId>de.flapdoodle.embed.mongo</artifactId>
		<version>3.4.12-SNAPSHOT</version>
	</dependency>

### Gradle

Make sure you have mavenCentral() in your repositories or that your enterprise/local server proxies the maven central repository.

	dependencies {
		testCompile group: "de.flapdoodle.embed", name: "de.flapdoodle.embed.mongo", version: "3.4.11-SNAPSHOT"
	}

### Build from source

When you fork or clone our branch you should always be able to build the library by running

	mvn package

### Changelog

[Changelog](Changelog.md)

### Supported Versions

Versions: some older, a stable and a development version
Support for Linux, Windows and MacOSX.

### Spring Integration

As the spring projects
[removed the embed mongo support in 2.7.0](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.7-Release-Notes#springmongodbembeddedfeatures-configuration-property-removed)
you should consider to use one of these integration projects.
It should behave mostly like the original spring integration, but there are some minor differences:
- version in 'spring.mongodb.embedded.version' is used in package resolver and is not matched against version enum.
- 'spring.mongodb.embedded.features' is not supported (not the way to change the config of mongodb)

If you have any trouble in using them fell free to create an issue.

- [Spring 2.5.x](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.spring/tree/spring-2.5.x)
- [Spring 2.6.x](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.spring/tree/spring-2.6.x)
- [Spring 2.7.x](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo.spring/tree/spring-2.7.x)

### Usage
```java
MongodStarter starter = MongodStarter.getDefaultInstance();

int port = Network.getFreeServerPort();
MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(port, Network.localhostIsIPv6()))
    .build();

MongodExecutable mongodExecutable = null;
try {
  mongodExecutable = starter.prepare(mongodConfig);
  MongodProcess mongod = mongodExecutable.start();

  try (MongoClient mongo = new MongoClient("localhost", port)) {
    DB db = mongo.getDB("test");
    DBCollection col = db.createCollection("testCol", new BasicDBObject());
    col.save(new BasicDBObject("testDoc", new Date()));
  }

} finally {
  if (mongodExecutable != null)
    mongodExecutable.stop();
}
```

### Usage - Optimization

You should make the MongodStarter instance or the RuntimeConfig instance static (per Class or per JVM).
The main purpose of that is the caching of extracted executables and library files. This is done by the ArtifactStore instance
configured with the RuntimeConfig instance. Each instance uses its own cache so multiple RuntimeConfig instances will use multiple
ArtifactStores an multiple caches with much less cache hits:)  

### Usage - custom mongod filename

If you do not restrict `bindId` to `localhost` you get windows firewall dialog popups.
To avoid them you can choose a stable executable name with UserTempNaming.
This way the firewall dialog only popups once. See [Executable Collision](#executable-collision)

```java
int port = Network.getFreeServerPort();

Command command = Command.MongoD;

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(command)
.artifactStore(Defaults.extractedArtifactStoreFor(command)
    .withDownloadConfig(Defaults.downloadConfigFor(command).build())
    .executableNaming(new UserTempNaming()))
.build();

MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(port, Network.localhostIsIPv6()))
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);

MongodExecutable mongodExecutable = null;
try {
  mongodExecutable = runtime.prepare(mongodConfig);
  MongodProcess mongod = mongodExecutable.start();

  try (MongoClient mongo = new MongoClient("localhost", port)) {
    DB db = mongo.getDB("test");
    DBCollection col = db.createCollection("testCol", new BasicDBObject());
    col.save(new BasicDBObject("testDoc", new Date()));
  }

} finally {
  if (mongodExecutable != null)
    mongodExecutable.stop();
}
```

### Unit Tests

```java
public abstract class AbstractMongoDBTest extends TestCase {


  /**
   * please store Starter or RuntimeConfig in a static final field
   * if you want to use artifact store caching (or else disable caching) 
   */
  private static final MongodStarter starter = MongodStarter.getDefaultInstance();

  private MongodExecutable _mongodExe;
  private MongodProcess _mongod;

  private MongoClient _mongo;
  private int port;
  
  @Override
  protected void setUp() throws Exception {
    port = Network.getFreeServerPort();
    _mongodExe = starter.prepare(createMongodConfig());
    _mongod = _mongodExe.start();

    super.setUp();

    _mongo = new MongoClient("localhost", port);
  }
  
  public int port() {
    return port;
  }

  protected IMongodConfig createMongodConfig() throws UnknownHostException, IOException {
    return createMongodConfigBuilder().build();
  }

  protected MongodConfigBuilder createMongodConfigBuilder() throws UnknownHostException, IOException {
    return new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    _mongod.stop();
    _mongodExe.stop();
  }

  public Mongo getMongo() {
    return _mongo;
  }

}
```

#### ... with some more help

```java
MongodForTestsFactory factory = null;
try {
  factory = MongodForTestsFactory.with(Version.Main.PRODUCTION);

  try (MongoClient mongo = factory.newMongo()) {
    DB db = mongo.getDB("test-" + UUID.randomUUID());
    DBCollection col = db.createCollection("testCol", new BasicDBObject());
    col.save(new BasicDBObject("testDoc", new Date()));
  }

} finally {
  if (factory != null)
    factory.shutdown();
}
```

### Customize Download URL

```java
Command command = Command.MongoD;

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(command)
    .artifactStore(Defaults.extractedArtifactStoreFor(command)
        .withDownloadConfig(Defaults.downloadConfigFor(command)
            .downloadPath((__) -> "http://my.custom.download.domain/")
            .build()))
    .build();
```

### Customize Proxy for Download
```java
Command command = Command.MongoD;

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(command)
    .artifactStore(Defaults.extractedArtifactStoreFor(command)
        .withDownloadConfig(Defaults.downloadConfigFor(command)
            .proxyFactory(new HttpProxyFactory("fooo", 1234))
            .build()))
    .build();
```

### Customize Artifact Storage
```java
Directory artifactStorePath = new FixedPath(System.getProperty("user.home") + "/.embeddedMongodbCustomPath");
TempNaming executableNaming = new UUIDTempNaming();

Command command = Command.MongoD;    

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(command)
    .artifactStore(Defaults.extractedArtifactStoreFor(command)
        .withDownloadConfig(Defaults.downloadConfigFor(command)
            .artifactStorePath(artifactStorePath)
            .build())
        .executableNaming(executableNaming))
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
MongodExecutable mongodExe = runtime.prepare(mongodConfig);
```

### Usage - custom mongod process output

#### ... to console with line prefix
```java
ProcessOutput processOutput = ProcessOutput.builder()
        .output(Processors.namedConsole("[mongod>]"))
        .error(Processors.namedConsole("[MONGOD>]"))
        .commands(Processors.namedConsole("[console>]"))
        .build();

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
    .processOutput(processOutput)
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
```

#### ... to file
```java
...
StreamProcessor mongodOutput = Processors.named("[mongod>]",
    new FileStreamProcessor(File.createTempFile("mongod", "log")));
StreamProcessor mongodError = new FileStreamProcessor(File.createTempFile("mongod-error", "log"));
StreamProcessor commandsOutput = Processors.namedConsole("[console>]");

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
    .processOutput(ProcessOutput.builder().output(mongodOutput).error(mongodError).commands(commandsOutput).build())
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
...

...
// ...
public class FileStreamProcessor implements StreamProcessor {

  private final FileOutputStream outputStream;

  public FileStreamProcessor(File file) throws FileNotFoundException {
    outputStream = new FileOutputStream(file);
  }

  @Override
  public void process(String block) {
    try {
      outputStream.write(block.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onProcessed() {
    try {
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
// ...
// <-
...
```

#### ... to java logging
```java
Logger logger = LoggerFactory.getLogger(getClass().getName());

ProcessOutput processOutput = ProcessOutput.builder()
        .output(Processors.logTo(logger, Slf4jLevel.INFO))
        .error(Processors.logTo(logger,Slf4jLevel.ERROR))
        .commands(Processors.named("[console>]", Processors.logTo(logger, Slf4jLevel.DEBUG)))
        .build();


RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD, logger)
    .processOutput(processOutput)
    .artifactStore(Defaults.extractedArtifactStoreFor(Command.MongoD)
        .download(Defaults.downloadConfigFor(Command.MongoD)
            .progressListener(new Slf4jProgressListener(logger))))
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
```

#### ... to default java logging (the easy way)
```java
Logger logger = LoggerFactory.getLogger(getClass().getName());

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD, logger)
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
```

#### ... to null device
```java
Logger logger = LoggerFactory.getLogger(getClass().getName());

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD, logger)
    .processOutput(ProcessOutput.silent())
    .build();

MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
```

### Custom Version
```java
int port = 12345;
MongodConfig mongodConfig = MongodConfig.builder()
    .version(Versions.withFeatures(de.flapdoodle.embed.process.distribution.Version.of("2.7.1"), Feature.SYNC_DELAY))
    .net(new Net(port, Network.localhostIsIPv6()))
    .build();

MongodStarter runtime = MongodStarter.getDefaultInstance();
MongodProcess mongod = null;

MongodExecutable mongodExecutable = null;
try {
  mongodExecutable = runtime.prepare(mongodConfig);
  mongod = mongodExecutable.start();

...


} finally {
  if (mongod != null) {
    mongod.stop();
  }
  if (mongodExecutable != null)
    mongodExecutable.stop();
}
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

### Use Free Server Port

  Warning: maybe not as stable, as expected.

#### ... by hand
```java
int port = Network.getFreeServerPort();
```

#### ... automagic
```java
MongodConfig mongodConfig = MongodConfig.builder().version(Version.Main.PRODUCTION).build();

MongodStarter runtime = MongodStarter.getDefaultInstance();

MongodExecutable mongodExecutable = null;
MongodProcess mongod = null;
try {
  mongodExecutable = runtime.prepare(mongodConfig);
  mongod = mongodExecutable.start();

  try (MongoClient mongo = new MongoClient(
      new ServerAddress(mongodConfig.net().getServerAddress(), mongodConfig.net().getPort()))) {
...

  }

} finally {
  if (mongod != null) {
    mongod.stop();
  }
  if (mongodExecutable != null)
    mongodExecutable.stop();
}
```

### ... custom timeouts
```java
MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .timeout(new Timeout(30000))
    .build();
```

### Command Line Post Processing
```java
CommandLinePostProcessor postProcessor = new CommandLinePostProcessor() {
      @Override
      public List<String> process(Distribution distribution, List<String> args) {
        return null;
      }
    };
...

RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD)
    .commandLinePostProcessor(postProcessor)
    .build();
```

### Custom Command Line Options

We changed the syncDelay to 0 which turns off sync to disc. To turn on default value used defaultSyncDelay().
```java
MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .cmdOptions(MongoCmdOptions.builder()
        .syncDelay(10)
        .useNoPrealloc(false)
        .useSmallFiles(false)
        .useNoJournal(false)
        .enableTextSearch(true)
        .build())
    .build();
```

### Snapshot database files from temp dir

We changed the syncDelay to 0 which turns off sync to disc. To get the files to create an snapshot you must turn on default value (use defaultSyncDelay()).
```java
MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .processListener(new CopyDbFilesFromDirBeforeProcessStop(destination))
    .cmdOptions(MongoCmdOptions.builder()
        .useDefaultSyncDelay(true)
        .build())
    .build();
```

### Custom database directory  

If you set a custom database directory, it will not be deleted after shutdown
```java
Storage replication = new Storage("/custom/databaseDir",null,0);

MongodConfig mongodConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .replication(replication)
    .build();
```

### Start mongos with mongod instance

this is an very easy example to use mongos and mongod
```java
  int port = 12121;
  int defaultConfigPort = 12345;
  String defaultHost = "localhost";

  MongodProcess mongod = startMongod(defaultConfigPort);

  try {
    MongosProcess mongos = startMongos(port, defaultConfigPort, defaultHost);
    try {
      MongoClient mongoClient = new MongoClient(defaultHost, defaultConfigPort);
      System.out.println("DB Names: " + mongoClient.getDatabaseNames());
    } finally {
      mongos.stop();
    }
  } finally {
    mongod.stop();
  }

  private MongosProcess startMongos(int port, int defaultConfigPort, String defaultHost) throws UnknownHostException,
      IOException {
    IMongosConfig mongosConfig = new MongosConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(port, Network.localhostIsIPv6()))
      .configDB(defaultHost + ":" + defaultConfigPort)
      .build();

    MongosExecutable mongosExecutable = MongosStarter.getDefaultInstance().prepare(mongosConfig);
    MongosProcess mongos = mongosExecutable.start();
    return mongos;
  }

  private MongodProcess startMongod(int defaultConfigPort) throws UnknownHostException, IOException {
    IMongodConfig mongoConfigConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(defaultConfigPort, Network.localhostIsIPv6()))
      .configServer(true)
      .build();

    MongodExecutable mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongoConfigConfig);
    MongodProcess mongod = mongodExecutable.start();
    return mongod;
  }
```

### Import JSON file with mongoimport command
```java
int defaultConfigPort = Network.getFreeServerPort();
String database = "importTestDB";
String collection = "importedCollection";

MongodConfig mongoConfigConfig = MongodConfig.builder()
    .version(Version.Main.PRODUCTION)
    .net(new Net(defaultConfigPort, Network.localhostIsIPv6()))
    .build();

MongodExecutable mongodExecutable = MongodStarter.getDefaultInstance().prepare(mongoConfigConfig);
MongodProcess mongod = mongodExecutable.start();

try {
  MongoImportConfig mongoImportConfig = MongoImportConfig.builder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(defaultConfigPort, Network.localhostIsIPv6()))
      .databaseName(database)
      .collectionName(collection)
      .isUpsertDocuments(true)
      .isDropCollection(true)
      .isJsonArray(true)
      .importFile(jsonFile)
      .build();

  MongoImportExecutable mongoImportExecutable = MongoImportStarter.getDefaultInstance().prepare(mongoImportConfig);
  MongoImportProcess mongoImport = mongoImportExecutable.start();
  try {
...

  }
  finally {
    mongoImport.stop();
  }
}
finally {
  mongod.stop();
}
```

### Executable Collision

There is a good chance of filename collisions if you use a custom naming schema for the executable (see [Usage - custom mongod filename](#usage---custom-mongod-filename)). If you got an exception, then you should make your RuntimeConfig or MongoStarter class or jvm static (static final in your test class or singleton class for all tests).

----

YourKit is kindly supporting open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
