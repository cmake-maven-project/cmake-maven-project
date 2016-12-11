# CMake-Maven-Project [![Build Status](https://travis-ci.org/cmake-maven-project/cmake-maven-project.png?branch=master)](https://travis-ci.org/cmake-maven-project/cmake-maven-project)

## Introduction

A Maven project for the CMake build system. It can be used by including it as a plugin within your Maven project's pom.xml file.

This repository [originally lived](https://code.google.com/p/cmake-maven-project/) on Google Code and was migrated to GitHub (and Git) after Google Code shut down.

## Sample Usage

### Generate Goal

    <plugin>
      <groupId>com.googlecode.cmake-maven-project</groupId>
      <artifactId>cmake-maven-plugin</artifactId>
      <version>3.7.0-b2</version>
      <executions>
        <execution>
          <id>cmake-generate</id>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <sourcePath>
              <!-- The directory containing CMakeLists -->
            </sourcePath>
            <targetPath>
              <!-- The directory write the project files to -->
            </targetPath>
            <generator>
              <!-- One of the generators defined at https://cmake.org/cmake/help/v3.7/manual/cmake-generators.7.html -->
            </generator>
            <classifier>
              <!-- The classifier of the current platform. One of [windows-i386, windows-amd64, linux-i386, linux-amd64, linux-arm, mac-amd64]. -->
            </classifier>
            <environmentVariables>
              <key>value</key>
            </environmentVariables>
            <options>
              <!--
                Optional: One or more options found at https://cmake.org/cmake/help/v3.7/manual/cmake.1.html
                For example:
              -->
              <option>-DBUILD_THIRDPARTY:bool=on</option>
            </options>
          </configuration>
        </execution>
      </executions>
    </plugin>

### Compile Goal

    <plugin>
      <groupId>com.googlecode.cmake-maven-project</groupId>
      <artifactId>cmake-maven-plugin</artifactId>
      <version>3.7.0-b1</version>
      <executions>
        <execution>
          <id>cmake-compile</id>
          <goals>
            <goal>compile</goal>
          </goals>
          <configuration>
            <config>
              <!-- Optional: the build configuration (e.g. "Release|x64") -->
            </config>
            <target>
              <!-- Optional: the build "target" -->
            </target>
            <projectDirectory>
              <!-- "targetPath" from the "generate" goal -->
            </projectDirectory>
            <classifier>
              <!-- The classifier of the current platform. One of [windows-i386, windows-amd64, linux-i386, linux-amd64, linux-arm, mac-amd64]. -->
            </classifier>
            <environmentVariables>
              <key>value</key>
            </environmentVariables>
          </configuration>
        </execution>
      </executions>
    </plugin>

### Test Goal

    <plugin>
      <groupId>com.googlecode.cmake-maven-project</groupId>
      <artifactId>cmake-maven-plugin</artifactId>
      <version>3.7.0-b1</version>
      <executions>
        <execution>
          <id>cmake-test</id>
          <goals>
            <goal>test</goal>
          </goals>
          <configuration>
            <!-- "buildDirectory" is "targetPath" from the "generate" goal -->
            <buildDirectory>${project.build.directory}</buildDirectory>
            <!-- Optional way to not fail the build on test failures -->
            <!-- <testFailureIgnore>true</testFailureIgnore> -->
            <!-- Optional way to skip just the ctest tests -->
            <!-- <ctest.skip.tests>true</ctest.skip.tests> -->
            <!-- Optional/standard way to skip all Maven tests -->
            <!-- <maven.test.skip>true</maven.test.skip> -->
            <!-- Optional way to configure number of threads tests should use -->
            <!-- <threadCount>2</threadCount> -->
            <!-- Optional dashboard configuration; used with CTestConfig.cmake -->
            <!-- <dashboard>Experimental</dashboard> -->
          </configuration>
        </execution>
      </executions>
    </plugin>

### Examples

The following projects contain examples of how to use this plugin:

[Requirements API](https://bitbucket.org/cowwoc/requirements/src/1d6416782875b6d412903c5b7d8fd3686e63927b/native/pom.xml?at=dev-3.0.0&fileviewer=file-view-default#pom.xml-166)

### Building instructions

To build the plugin, run:

    mvn install

To clean an old build, run:

    mvn clean

By default, Maven will activate the right profile based on your JVM:

* windows-i386
* windows-amd64
* linux-i386
* linux-amd64
* linux-arm
* mac-amd64

If detection does not work, or you wish to override it (e.g. you're running linux-amd64 but want to cross-compile for linux-i386) then set `-DosDetection=false -P<profile>`.

For instance, when building for 32-bit Linux machines, use:

    mvn -DosDetection=false -Plinux-i386 install

### Using a local CMake installation

cmake.org doesn't provide binaries for some platforms, such as 32-bit Linux and Raspberry Pi. In such cases, users can install the binaries themselves (typically using package managers like `apt-get`) and point the plugin at them.

The following Maven profiles use local CMake installations:

* `linux-i386` corresponds to the 32-bit Linux platform.
* `linux-arm` corresponds to the Rasbian (Raspberry Pi) platform.

but you can configure this behavior for any platform by setting `${download.cmake}` to `false`. The plugin looks for cmake under `${cmake.root.dir}/${cmake.child.dir}` and ctest under `${cmake.root.dir}/${cmake.ctest.dir}`. By default, `${cmake.root.dir}` resolves to `/usr`, `${cmake.child.dir}` to `/bin/cmake` and `${cmake.test.dir}` to `/`.

That's it! To learn more about CMake itself, consult the [CMake.org](https://cmake.org/) website.

### License

CMake-Maven-Project is released under an [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
