### Changelog

### 3.5.2

- dep updates

### 3.5.0

- dep updates, minor changes

### 3.4.11

- mongodb shutdown fix

### 3.4.10

- mongodb 6 support

### 3.4.9

- dep updates

### 3.4.6

- platform detection override, see [de.flapdoodle.os](https://github.com/flapdoodle-oss/de.flapdoodle.os)

### 3.4.5

- Process.waitFor timeout added

### 3.4.4

- mongodb dist upgrade

### 3.4.3

- new spring integration sub project
- fixed spring compat after refactoring

### 3.4.2

- restored spring compat after refactoring (still broken)

### 3.4.1

- moved feature detection to packageresolver repo

### 3.4.0

- moved packageresolver to extra repo

### 3.3.1

- ubuntu 21.x support

### 3.3.0

- mongodb version updates

### 3.2.8

- redhat support fixed thanks to @infoShare

### 3.2.7

- redhat support

### 3.2.6

- centos bugfix

### 3.2.5

- amazon linux support

### 3.2.4

- linux mint support
- linux fallback to ubuntu 20.04 LTS

### 3.2.3

- spring boot api fix

### 3.2.2

- debian support added

### 3.2.1

- centos support added

### 3.2.0

- major rework, now supporting mongodb 4.4.x and 5.x

### 3.0.0

- big refactoring

#### 2.1.1 / 2.1.0

- implemented getFeatures of an IFeatureAwareVersion to create your own GenericVersion with Features of Version X
- fixes gh-255 
- fixes gh-244 updating mongodb driver
- use random port in test
- use process lib 2.0.5
- Fix transisent dependency on Apache Commons-IO
- removed version 3.7
- Name the tests by version to help diagnose problems with specific versions
- added toString in GenericFeatureAwareVersion
- fixes gh-240
- gh-237 Unable to use '--archive' with mongodump

#### 2.0.4
- mongodump with archive option

#### 2.0.2 & 2.0.3
- jdk 9 compatibility issue thanks to https://github.com/flapdoodle-oss/de.flapdoodle.embed.process/pull/66

#### 2.0.1 

- mongodb 3.6
- mongodb nightly builds
- `--shardsvr`did not get honored when starting mongod, thanks to https://github.com/faizan-talena


#### 2.0.0

- java8, immutables.org, bugfixes

#### 1.50.0

- major refactoring in de.flapdoodle.embed.process thanks to https://github.com/julien-may
- extracted files are keept, which should give some faster tests :)

#### 1.48.2

- re-release of 1.48.1

#### 1.48.1

- mongo version upgrade (2.6.10, 3.0.4, 3.1.5)

#### 1.47.3

- mongo client dependency changed to provided, now you can use version 2.x.x or 3.0.0

#### 1.47.2

#### 1.47.1

- many fixes by https://github.com/jochenberger
- mongo version upgrade to (3.0.2)

#### 1.47.0

- changed logging to slf4j
- mongo version upgrade (2.6.8, 3.0.0)

#### 1.46.4

- Implement equals and hashCode on GenericFeatureAwareVersion so that CachingArtifactStore can work. Version is a component of Distribution, which is used as the cache key of CachingArtifactStore

#### 1.46.3
- skipped due to release 'problems'

#### 1.46.2
- skipped due to release 'problems' 

#### 1.46.1 

- support for OSGi (thanks to Niels Betram)
- support for FreeBSD (thanks to Alex Sanchez)
- updated gradle build
- switch to enable textsearch in mongodb before 2.6

#### 1.46.0

- changed to semantic versions
- custom database dir supported
- upgrade mongo versions
- changed some log levels, fixed some error logs
- proxy configuration added

#### 1.45

- embed.process upgrade (better error logging)

#### 1.44

- mongo version upgrade (2.6.0, ...)
- mongo import command support

#### 1.43

#### 1.42

- massive performance improvment (removed option -v (can be enabled by config))
- mongo version upgrade (2.5.4, 2.4.9, 2.2.7)

#### 1.41

- mongo env command line bug fix (now working)
- mongo version upgrade (2.5.4, 2.4.8, 2.2.6)

#### 1.40

- mongo env command line bug fix

#### 1.39

- embed process dep update

#### 1.38

- embed process dep update
- bug fixes
- update examples

#### 1.37

- new mongo version supported

#### 1.36

- dependencies updated 

#### 1.35

- syncDelay set to 0 (no sync)
- process listener added (can copy db files after test into custom directory)

#### 1.34

- added builder for mongod and mongos config
- dependencies updated

#### 1.33 (not public)

#### 1.32

- added 2.5.0 as new development version
- added example for custom download path

#### 1.31

- minor bugfixes

#### 1.30

- deprecate old versions (left current production(2.4.1) and previous version left(2.2.3))
- coming versions will change use the 2.4 branch of mongodb as production version
- no release time check for deprecated versions, so use with care

#### 1.29

- major api changes, speed improvement, easier configuration, need to update documentation

#### 1.28

- mongod config refactor
- configurable startup timeout
- added windows2008 support

#### 1.27

- dep version change

#### 1.26

- bind_ip configuration parameter support (MongodConfig constructor)

#### 1.25

- dep version change

#### 1.24

- dep version change

#### 1.23

- added 2.0.7, 2.2.0
- mongodb java driver update to 2.9.0 (test dependency)

#### 1.22

- maven dep version range does not work as expected -> disabled

#### 1.21

- dependency version range for de.flapdoodle.embed.process

#### 1.20

- NPE fix with custom database directory
- custom database directory will not be deleted

#### 1.19

- **massive refactoring, some api breaks**
- **project split**
- some relevant process.stop() improvements

#### 1.18

- added some unit test support (thanx to trajano)
- added some logging only runtime config option
- added 2.0.7-rc1, 2.2.0-rc0
- command line post processor hock

#### 1.17

- added version 2.0.6 and 2.1.2
- version refactoring
- you can now have a custom version, so you do not depend on a new release of this project

#### 1.16

- added version 2.0.5 (main version 2.0 now points to it)
- changed http user agent
- customizeable mongod process output 
- better loopback device detection for mongod process shutdown via command

#### 1.15

- now we send ctrl+c on linux and osx, then send shutdown to server, then taskkill on windows (may the force be with us)
- disable journal for faster turnaround times
- noauth added
- customize artifact storage path
- detection if localhost is not loopback (command shutdown on mongod does not work for remote access)
- formated process output
- much better windows support

#### 1.14

- changed back to send ctrl+c and then send shutdown

#### 1.13

- mongod process management improvement 
 (windows mongod shutdown improvement (alpha) (some trouble stopping process on windows - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4770092))
 - send shutdown to server
 - send ctrl+c to mongod will perform clean shutdown (untested on windows using taskkill)
- now with 2.1.1

#### 1.12

- NUMA support (alpha) - http://www.mongodb.org/display/DOCS/NUMA

#### 1.11

- timeout fix on slow systems
- stability on win plattforms (hopefully)

#### 1.10

- race condition and cleanup of mongod process

#### 1.9

- fixed 64Bit detection - amd64
- now with main versions 1.6, 1.8, 2.0, 2.1
