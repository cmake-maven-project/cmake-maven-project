# CMake-Maven-Project

## Introduction

A Maven project for the CMake build system. It can be used by including it as a plugin within your Maven project's pom.xml file.

This repository [originally lived]((https://code.google.com/p/cmake-maven-project/)) on Google Code and was migrated to GitHub (and Git) after Google Code shut down.

## Sample Usage

### Generate Goal

    <plugin>
      <groupId>com.googlecode.cmake-maven-project</groupId>
      <artifactId>cmake-maven-plugin</artifactId>
      <version>3.3.2-b1</version>
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
              <!-- One of the generators defined at http://www.cmake.org/cmake/help/v2.8.10/cmake.html#section_Generators -->
            </generator>
            <environmentVariables>
              <key>value</key>
            </environmentVariables>
            <options>
              <!--
                Optional: One or more options found at http://www.cmake.org/cmake/help/v2.8.10/cmake.html#section_Options
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
      <version>3.3.2-b1</version>
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
      <version>3.3.2-b1</version>
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

### Building Your Project Using Version 2.8.11-b4 and Later

Since version 2.8.11-b4, CMake-Maven-Project will use OS activation to determine which profile to use. If you're building on a Linux system, it will automatically select the 'linux' profile; if you're running on a Windows system, it will automatically select the 'windows' profile. You simply have to build your project in the standard Maven way:

    mvn install

OS profile activation can be overridden (if you have a Windows cross-compiling setup on a Linux machine, for instance). To select the Windows profile on a Linux machine, use:

    mvn -Pwindows,-linux install

This removes the 'linux' profile and adds the 'windows' one.

The following profiles are supported:

* windows
* linux
* mac

To clean an old build, run:

    mvn -P<profile> clean

### Building Your Project Using Version 2.8.11-b3 and Earlier

To build your project using an earlier version of CMake-Maven-Project, you will need to supply the OS profile you want to use. It will not be auto-detected by the build system.Instead, you'll need to supply the OS profile at build time:

    mvn -P<profile> install

So, for example, to build on Linux:

    mvn -Plinux install

The following profiles are supported:

* windows
* linux
* mac

That's it!  To learn more about CMake itself, consult the [CMake.org website](https://cmake.org/).

### License

CMake-Maven-Project is released under an [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
