# Use Cases

## start mongod                                       

```java
${startMongoD}
```

![start mongod](${startMongoD.graph.svg})

## start mongod with persistent database

```java
${startMongoDWithPersistentDatabase}
```

![start mongod](${startMongoDWithPersistentDatabase.graph.svg})
               
## json import with mongoimport into mongod

```java
${startMongoImport}
```

![start mongod](${startMongoImport.graph.svg})

## json import with mongoimport into mongod - compact version

```java
${startMongoImportAsOneTransition}
```

![start mongod](${startMongoImportAsOneTransition.graph.svg})


## execute mongo shell with running mongod server

```java
${startMongoShell}
```

![start mongod](${startMongoShell.graph.svg})

Because mongo shell binary is missing since version >= 6.x.x you may emulate this with a
listener called after server start:

```java
${emulateMongoShell}
```
