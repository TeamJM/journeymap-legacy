# [JourneyMap for Minecraft][1]

Source code and build resources for [JourneyMap][2] ([http://journeymap.info][2])

## Requirements

* Java 17 JDK
* IntelliJ IDEA

## Contributing

We're huge fans of the open source community, and we're happy to accept pull requests for patches and improvements. That said, we would prefer that you join the discord server and have a chat with us about it first. This allows us to cooperate with you and ensure that your PR makes sense, and isn't stepping on anyone else's toes.

If you would like to contribute, please fork this repository and submit a pull request.

When submitting a pull request, please follow these guidelines:

- Make sure to describe your changes in the pull request description.
- Make sure to run your code locally to ensure that your changes are functioning correctly and have not broken anything. See the section below for instructions on how to do this.


## Environment Setup

### 1. Git the JourneyMap source

Check out a branch of the JourneyMap GIT repo to a directory called journeymap.  For example:

```sh
    git clone git@github.com:TeamJM/journeymap-legacy.git
    cd journeymap-legacy
    git fetch && git checkout (branchname)
```

### 2. Setup JourneyMap with Forge for IntelliJ IDEA

* In a command window, go into the journeymap directory and invoke the Gradle build to setup the workspace:

```
    gradlew.bat build
```

* Open the journeymap-legacy folder in IDEA
* Import Project from Gradle if prompted

### 3. Build the jars

* For releases, ensure all your changes are commited and create a git tag named after the version you want to build, e.g. `5.1.4p8`.
* If you don't make a dedicated tag, metadata about your current git branch will be automatically appended to create a unique jar version, e.g. `5.1.4p8-my-branch.1+c5c1d4797c`
* You can also set the environment variable `VERSION` when running Gradle to any version to fully override the automatic version assignment, e.g. `VERSION=5.1.4p8`
* Build using Gradle (build.gradle) > build
* The end result will be in `build/libs/journeymap*.jar`

[1]: https://github.com/TeamJM/journeymap-legacy
[2]: http://journeymap.info
