package com.googlecode.cmakemavenproject;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Goal which generates project files.
 *
 * @author Gili Tzabari
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateMojo
	extends AbstractMojo
{
	/**
	 * The classifier of the current platform.
	 * <p>
	 * One of [windows-x86_64, linux-x86_64, linux-arm_32, mac-x86_64].
	 */
	@Parameter(property = "classifier", readonly = true, required = true)
	private String classifier;
	/**
	 * /**
	 * The directory containing CMakeLists.txt.
	 */
	@Parameter(required = true)
	private File sourcePath;
	/**
	 * The output directory.
	 */
	@Parameter(required = true)
	private File targetPath;
	/**
	 * The makefile generator to use.
	 */
	@Parameter
	private String generator;
	/**
	 * The environment variables.
	 */
	@Parameter
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to cmake.
	 */
	@Parameter
	private List<String> options;
	@Component
	private BuildPluginManager pluginManager;
	@Parameter(property = "project", required = true, readonly = true)
	private MavenProject project;
	@Parameter(property = "session", required = true, readonly = true)
	private MavenSession session;
	@Parameter(property = "download.cmake", defaultValue = "true")
	private boolean downloadBinaries;
	@Parameter(property = "cmake.root.dir", defaultValue = "/usr")
	private String cmakeRootDir;
	@Parameter(property = "cmake.child.dir")
	private String cmakeChildDir;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		if (cmakeChildDir == null)
			cmakeChildDir = Mojos.getCmakePath();
		try
		{
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

			File cmakeDir;
			if (downloadBinaries)
			{
				getLog().info("Downloading binaries");
				cmakeDir = new File(project.getBuild().getDirectory(), "dependency/cmake");
				PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().
					get("pluginDescriptor");
				String groupId = pluginDescriptor.getGroupId();
				String version = pluginDescriptor.getVersion();
				String binariesArtifact = "cmake-binaries";
				Element groupIdElement = new Element("groupId", groupId);
				Element artifactIdElement = new Element("artifactId", binariesArtifact);
				Element versionElement = new Element("version", version);
				Element classifierElement = new Element("classifier", classifier);
				Element outputDirectoryElement = new Element("outputDirectory", cmakeDir.getAbsolutePath());
				Element artifactItemElement = new Element("artifactItem", groupIdElement, artifactIdElement,
					versionElement, classifierElement, outputDirectoryElement);
				Element artifactItemsItem = new Element("artifactItems", artifactItemElement);
				Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
				ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project, session,
					pluginManager);
				Plugin dependencyPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
					"maven-dependency-plugin", "2.8");
				MojoExecutor.executeMojo(dependencyPlugin, "unpack", configuration, environment);
			}
			else
			{
				getLog().info("Using local binaries");
				cmakeDir = new File(cmakeRootDir);
			}

			List<String> command = new ArrayList<>();
			command.add(new File(cmakeDir, cmakeChildDir).getAbsolutePath());
			if (generator != null && !generator.trim().isEmpty())
			{
				command.add("-G");
				command.add(generator);
			}
			ProcessBuilder processBuilder = new ProcessBuilder(command).directory(targetPath);
			if (options != null)
			{
				// Skip undefined Maven properties:
				// <options>
				//   <option>${optional.property}</option>
				// </options>
				List<String> nonEmptyOptions = options.stream().filter(option -> !option.isEmpty()).
					collect(Collectors.toList());
				processBuilder.command().addAll(nonEmptyOptions);
			}
			processBuilder.command().add(sourcePath.getAbsolutePath());

			Map<String, String> env = processBuilder.environment();
			if (environmentVariables != null)
				Mojos.overrideEnvironmentVariables(environmentVariables, env);

			Log log = getLog();
			if (log.isDebugEnabled())
			{
				log.debug("sourcePath: " + sourcePath);
				log.debug("targetPath: " + targetPath);
				log.debug("environment: " + processBuilder.environment());
				log.debug("command-line: " + processBuilder.command());
			}
			int returnCode = Mojos.waitFor(processBuilder);
			if (returnCode != 0)
				throw new MojoExecutionException("Return code: " + returnCode);
		}
		catch (InterruptedException | IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}
}
