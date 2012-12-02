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
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

/**
 * Goal which generates project files.
 *
 * @goal generate
 * @phase process-sources
 *
 * @author Gili Tzabari
 */
public class GenerateMojo
	extends AbstractMojo
{
	/**
	 * The release platform.
	 *
	 * @parameter expression="${classifier}"
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String classifier;
	/**
	 * The directory containing CMakeLists.txt
	 *
	 * @parameter
	 * @required
	 */
	@SuppressWarnings(
	{
		"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
	})
	private File sourcePath;
	/**
	 * The output directory.
	 *
	 * @parameter
	 * @required
	 */
	@SuppressWarnings(
	{
		"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
	})
	private File targetPath;
	/**
	 * The makefile generator to use.
	 *
	 * @parameter
	 * @required
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String generator;
	/**
	 * The environment variables.
	 *
	 * @parameter
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to cmake.
	 *
	 * @parameter
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String options;
	/**
	 * @component
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private BuildPluginManager pluginManager;
	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings(
	{
		"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
	})
	private MavenProject project;
	/**
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private MavenSession session;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().
			get("pluginDescriptor");
		String version = pluginDescriptor.getVersion();
		try
		{
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

			final String groupId = "com.googlecode.cmake-maven-project";
			final String artifactId = "cmake-binaries";

			Plugin dependencyPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
				"maven-dependency-plugin", "2.6");

			if (classifier == null)
			{
				String os = System.getProperty("os.name");
				if (os.toLowerCase().startsWith("windows"))
					classifier = "windows";
				else if (os.toLowerCase().startsWith("linux"))
					classifier = "linux";
				else if (os.toLowerCase().startsWith("mac"))
					classifier = "mac";
				else
					throw new MojoExecutionException("Unsupported os.name: " + os);
			}
			Path cmakeDir = Paths.get(project.getBuild().getDirectory(), "dependency/cmake").
				toAbsolutePath();

			Element groupIdElement = new Element("groupId", groupId);
			Element artifactIdElement = new Element("artifactId", artifactId);
			Element versionElement = new Element("version", version);
			Element classifierElement = new Element("classifier", classifier);
			Element outputDirectoryElement = new Element("outputDirectory", cmakeDir.toString());
			Element artifactItemElement = new Element("artifactItem", groupIdElement, artifactIdElement,
				versionElement, classifierElement, outputDirectoryElement);
			Element artifactItemsItem = new Element("artifactItems", artifactItemElement);
			Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
			ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project, session,
				pluginManager);
			MojoExecutor.executeMojo(dependencyPlugin, "unpack", configuration, environment);

			ProcessBuilder processBuilder = new ProcessBuilder(cmakeDir.resolve("bin/cmake").toString(),
				"-G", generator).directory(targetPath);
			if (options != null)
				processBuilder.command().add(options);
			processBuilder.command().add(sourcePath.getAbsolutePath());
			Map<String, String> env = processBuilder.environment();

			if (environmentVariables != null)
				env.putAll(environmentVariables);
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
