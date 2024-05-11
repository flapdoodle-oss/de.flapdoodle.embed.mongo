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

## Customize Downloader Implementation
```java
${testCustomDownloader}
```

## Customize Artifact Storage
```java
${testCustomizeArtifactStorage}
```

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
