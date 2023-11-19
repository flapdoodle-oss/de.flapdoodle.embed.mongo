### Usage

```java
${testStandard}
```

#### Customize by Override

```java
${customizeMongodByOverride}
```

#### Customize by Builder

```java
${customizeMongodByBuilder}
```

#### Customize by Replacement

```java
${customizeMongodByReplacement}
```

### Main Versions
```java
${testMainVersions}
```

### Command Line Post Processing
```java
${testCommandLinePostProcessing}
```

### Custom Command Line Options

We changed the syncDelay to 0 which turns off sync to disc. To turn on default value used defaultSyncDelay().
```java
${testCommandLineOptions}
```

### Snapshot database files from temp dir

We changed the syncDelay to 0 which turns off sync to disc. To get the files to create an snapshot you must turn on default value (use defaultSyncDelay()).
```java
${testSnapshotDbFiles}
```

### Start mongos with mongod instance

this is an very easy example to use mongos and mongod
```java
${testMongosAndMongod}
```

### Import JSON file with mongoimport command
```java
${importJsonIntoMongoDB}
```
                      
### User/Roles setup

```java
${setupUserAndRoles}
```
