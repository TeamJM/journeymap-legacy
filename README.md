# [JourneyMap for Minecraft][1]

Source code and build resources for [JourneyMap][2] ([http://journeymap.info][2])

## Requirements

* Java 1.7 or 1.8 JDK
* IntelliJ IDEA

## Environment Setup

### 1. Git the JourneyMap source

Check out a branch of the JourneyMap GIT repo to a directory called journeymap.  For example:

```sh
    git clone git@github.com:TeamJM/journeymap-private.git   
    cd journeymap
    git fetch && git checkout (branchname)
```

### 2. Setup JourneyMap with Forge for IntelliJ IDEA

* In a command window, go into the journeymap directory and invoke the Gradle build to setup the workspace:

```
    gradlew.bat setupDecompWorkspace idea
```

* Open journeymap.ipr in IDEA
* Import Project from Gradle when prompted
* Edit the Minecraft Client run configuration, add your credentials to `Program Arguments`: 

```
    --username name@email.address --password foo
```

### 3. Build the jars

* Update `project.properties` version info
* Build using Gradle (build.gradle) > build
* The end result will be in `build/libs/journeymap*.jar`

[1]: https://github.com/TeamJM/journeymap-legacy
[2]: http://journeymap.info
