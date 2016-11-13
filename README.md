# CMake-Maven-Project [![Build Status](https://travis-ci.org/cmake-maven-project/cmake-maven-project.png?branch=master)](https://travis-ci.org/cmake-maven-project/cmake-maven-project)

## Introduction

A Maven project for the CMake build system. It can be used by including it as a plugin within your Maven project's pom.xml file.

This repository [originally lived]((https://code.google.com/p/cmake-maven-project/)) on Google Code and was migrated to GitHub (and Git) after Google Code shut down.

## Sample Usage

### Generate Goal

    <plugin>
      <groupId>com.googlecode.cmake-maven-project</groupId>
      <artifactId>cmake-maven-plugin</artifactId>
      <version>3.7.0-b1</version>
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

https://bitbucket.org/cowwoc/requirements/src/238f37a385057a640acb8bec5452a425ab8e9ce0/native/pom.xml?at=dev-3.0.0&fileviewer=file-view-default

### Building instructions

To build the plugin, run:

    mvn -P<profile> install

To clean an old build, run:

    mvn -P<profile> clean

The following profiles are supported:

* windows32
* windows64
* linux64
* linux32
* mac64

For instance, when building for 64-bit Windows machines, use:

    mvn -Pwindows64 install

### Building Your Project on a Raspberry Pi

CMake doesn't offer a pre-built version of their software for ARM machines, but Raspberry Pi distributions like Raspbian offer CMake as a part of the system's software packages. Simply set the `download.cmake` system property to `false` and the plugin will use the local installation:

    mvn -Ddownload.cmake=false clean install

The plugin looks for the cmake under `${cmake.root.dir}/${cmake.child.dir}` and ctest under `${cmake.root.dir}/${cmake.ctest.dir}`. By default, `${cmake.root.dir}` resolves to `/usr`, `${cmake.child.dir}` to `/bin/cmake` and `${cmake.test.dir}` to `/`.

That's it! To learn more about CMake itself, consult the [CMake.org](https://cmake.org/) website.

### License

CMake-Maven-Project is released under an [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)