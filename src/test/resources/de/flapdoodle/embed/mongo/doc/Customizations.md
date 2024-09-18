# Customizations

## Customize Server Port

Warning: maybe not as stable, as expected.

### ... by hand
```java
${testFreeServerPort}
```

### ... or with fixed value
```java
${customizeNetworkPort}
```

## Customize StartTimeout

```java
${increaseStartTimeout}
```

## Customize Download URL

```java
${testCustomizeDownloadURL}
```
    
You can provide basic auth information if needed:

```java
${useBasicAuthInDownloadUrl}
``` 

## Customize Proxy for Download

```java
${testCustomProxy}
```

... or use system properties as described in [JDK Networking Properties](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html).
There is also an experimental [environment variable support](https://github.com/flapdoodle-oss/de.flapdoodle.java8/blob/master/docs/URLConnections.md#enable-env-variable-httpproxy-detection).

## Customize Downloader Implementation
```java
${testCustomDownloader}
```

## Customize Artifact Storage
```java
${testCustomizeArtifactStorage}
```

.. or just by setting system env variable '${testCustomizeArtifactStorageENV.name}' or system property '${testCustomizeArtifactStorageSystemProperty.name}'. 

## Custom database directory

If you set a custom database directory, it will not be deleted after shutdown
```java
${testCustomDatabaseDirectory}
```

## Usage - custom mongod process output

### ... to console with line prefix
```java
${testCustomOutputToConsolePrefix}
```

### ... to file
```java
...
${testCustomOutputToFile}
...
```

```java
${testCustomOutputToFile.FileStreamProcessor}
```

### ... to null device
```java
${testDefaultOutputToNone}
```

## customize package resolver
                                      
You can just create your own way to provide a mongodb package...

```java
${customPackageResolver}
```

... or you use some utility classes to create a more complex ruleset for different versions and platforms:

```java
${customPackageResolverRules}
```
