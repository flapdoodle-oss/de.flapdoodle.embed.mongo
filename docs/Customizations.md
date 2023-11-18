# Customizations

## Customize Server Port

Warning: maybe not as stable, as expected.

### ... by hand
```java
int port = Network.getFreeServerPort();
```

### ... or with fixed value
```java
Mongod mongod = Mongod.builder()
  .net(Start.to(Net.class).initializedWith(Net.defaults()
    .withPort(12345)))
  .build();
```


## Customize Download URL

```java
Mongod mongod = new Mongod() {
  @Override
  public Transition<DistributionBaseUrl> distributionBaseUrl() {
    return Start.to(DistributionBaseUrl.class)
      .initializedWith(DistributionBaseUrl.of("http://my.custom.download.domain"));
  }
};
```
    
You can provide basic auth information if needed:

```java
Mongod mongod = new Mongod() {
  @Override
  public Transition<DistributionBaseUrl> distributionBaseUrl() {
    return Start.to(DistributionBaseUrl.class)
      .initializedWith(DistributionBaseUrl.of("http://user:password@my.custom.download.domain"));
  }
};
``` 

## Customize Proxy for Download
```java
Mongod mongod = new Mongod() {
  @Override
  public DownloadPackage downloadPackage() {
    return DownloadPackage.withDefaults()
      .withDownloadConfig(DownloadConfig.defaults()
        .withProxyFactory(new HttpProxyFactory("fooo", 1234)));
  }
};
```

## Customize Downloader Implementation
```java
DownloadToPath custom = new DownloadToPath() {
  @Override
  public void download(URL url, Path destination,
    Optional<Proxy> proxy, String userAgent, TimeoutConfig timeoutConfig,
    DownloadCopyListener copyListener) throws IOException {
    // download url to destination
  }
};

Mongod mongod = Mongod.instance()
  .withDownloadPackage(DownloadPackage.withDefaults()
    .withDownloadToPath(custom));
```

## Customize Artifact Storage
```java
Mongod mongod = new Mongod() {
  @Override
  public Transition<PersistentDir> persistentBaseDir() {
    return Start.to(PersistentDir.class)
      .providedBy(PersistentDir.inUserHome(".embeddedMongodbCustomPath")
        .mapToUncheckedException(RuntimeException::new));
  }
};
```

## Custom database directory

If you set a custom database directory, it will not be deleted after shutdown
```java
Mongod mongod = new Mongod() {
  @Override public Transition<DatabaseDir> databaseDir() {
    return Start.to(DatabaseDir.class).initializedWith(DatabaseDir.of(customDatabaseDir));
  }
};
```

## Usage - custom mongod process output

### ... to console with line prefix
```java
Mongod mongod = new Mongod() {
  @Override
  public Transition<ProcessOutput> processOutput() {
    return Start.to(ProcessOutput.class)
      .initializedWith(ProcessOutput.builder()
        .output(Processors.namedConsole("[mongod>]"))
        .error(Processors.namedConsole("[MONGOD>]"))
        .commands(Processors.namedConsole("[console>]"))
        .build()
      )
      .withTransitionLabel("create named console");
  }
};
```

### ... to file
```java
...
Mongod mongod = new Mongod() {
  @Override
  public Transition<ProcessOutput> processOutput() {
    return Start.to(ProcessOutput.class)
      .providedBy(Try.<ProcessOutput, IOException>supplier(() -> ProcessOutput.builder()
          .output(Processors.named("[mongod>]",
            new FileStreamProcessor(File.createTempFile("mongod", "log"))))
          .error(new FileStreamProcessor(File.createTempFile("mongod-error", "log")))
          .commands(Processors.namedConsole("[console>]"))
          .build())
        .mapToUncheckedException(RuntimeException::new))
      .withTransitionLabel("create named console");
  }
};
...
```

```java
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
```

### ... to null device
```java
Mongod mongod = new Mongod() {
  @Override public Transition<ProcessOutput> processOutput() {
    return Start.to(ProcessOutput.class)
      .initializedWith(ProcessOutput.silent())
      .withTransitionLabel("no output");
  }
};

try (TransitionWalker.ReachedState<RunningMongodProcess> running = mongod.start(Version.Main.PRODUCTION)) {
  ServerAddress serverAddress = serverAddress(running.current().getServerAddress());
  try (MongoClient mongo = MongoClients.create("mongodb://" + serverAddress)) {
    MongoDatabase db = mongo.getDatabase("test");
    MongoCollection<Document> col = db.getCollection("testCol");
    col.insertOne(new Document("testDoc", new Date()));
...

  }
}
```

## customize package resolver
                                      
You can just create your own way to provide a mongodb package...

```java
Mongod customizedInstance = Mongod.instance()
  .withPackageOfDistribution(Start.to(Package.class).providedBy(() -> Package.builder()
    .fileSet(FileSet.builder()
      .addEntry(FileType.Executable, "mongod")
      .build())
    .archiveType(ArchiveType.TGZ)
    .url("http://some-local-server/mongod-to-download")
    .build()));
```

... or you use some utility classes to create a more complex ruleset for different versions and platforms:

```java

PackageFinderRules mongodPackageRules = PackageFinderRules.builder()
  .addRules(PackageFinderRule.builder()
    .match(PlatformMatch.withOs(CommonOS.Linux).withBitSize(BitSize.B64).withCpuType(CPUType.X86).withVersion(UbuntuVersion.Ubuntu_22_04)
      .andThen(DistributionMatch.any(VersionRange.of("7.0.0", "7.1.0"))
      ))
    .finder(UrlTemplatePackageFinder.builder()
      .fileSet(FileSet.builder()
        .addEntry(FileType.Executable, "mongod")
        .build())
      .archiveType(ArchiveType.TGZ)
      .urlTemplate("/relativePath-{version}.tgz")
      .build())
    .build())
  .addRules(PackageFinderRule.builder()
    .match(PlatformMatch.withOs(CommonOS.Windows)
      .andThen(DistributionMatch.any(VersionRange.of("5.0.0", "10.0.0"))))
    .finder(PackageFinder.failWithMessage(distribution -> "not supported: " + distribution))
    .build())
  .build();

Mongod customizedInstance = Mongod.instance()
  .withDistributionBaseUrl(Start.to(DistributionBaseUrl.class)
    .initializedWith(DistributionBaseUrl.of("http://some-local-server")))
  .withPackageOfDistribution(PackageOfCommandDistribution.withDefaults()
    .withCommandPackageResolver(command -> distribution -> {
      switch (command) {
        case MongoD:
          return mongodPackageRules.packageFor(distribution)
            .orElseThrow(() -> new IllegalArgumentException("could not find package for " + distribution));
        default:
          throw new IllegalArgumentException("not implemented");
      }
    }));

```