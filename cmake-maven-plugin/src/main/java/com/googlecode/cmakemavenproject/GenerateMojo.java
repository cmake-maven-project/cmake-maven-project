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
import java.util.List;
import java.util.Map;
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

/**
 * Goal which generates project files.
 * <p>
 * @author Gili Tzabari
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateMojo
	extends AbstractMojo
{
	/**
	 * The release platform.
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(property = "classifier", readonly = true)
	private String classifier;
	/**
	 * The directory containing CMakeLists.txt.
	 */
	@SuppressWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(required = true)
	private File sourcePath;
	/**
	 * The output directory.
	 */
	@SuppressWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(required = true)
	private File targetPath;
	/**
	 * The makefile generator to use.
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(required = true)
	private String generator;
	/**
	 * The environment variables.
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to cmake.
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter
	private List<String> options;
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Component
	private BuildPluginManager pluginManager;
	@SuppressWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(property = "project", required = true, readonly = true)
	private MavenProject project;
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(property = "session", required = true, readonly = true)
	private MavenSession session;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		try
		{
			PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().
				get("pluginDescriptor");
			String groupId = pluginDescriptor.getGroupId();
			String version = pluginDescriptor.getVersion();
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

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
			String binariesArtifact = "cmake-binaries";

			Element groupIdElement = new Element("groupId", groupId);
			Element artifactIdElement = new Element("artifactId", binariesArtifact);
			Element versionElement = new Element("version", version);
			Element classifierElement = new Element("classifier", classifier);
			Element outputDirectoryElement = new Element("outputDirectory", cmakeDir.toString());
			Element artifactItemElement = new Element("artifactItem", groupIdElement, artifactIdElement,
				versionElement, classifierElement, outputDirectoryElement);
			Element artifactItemsItem = new Element("artifactItems", artifactItemElement);
			Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
			ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project, session,
				pluginManager);
			Plugin dependencyPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
				"maven-dependency-plugin", "2.8");
			MojoExecutor.executeMojo(dependencyPlugin, "unpack", configuration, environment);

			ProcessBuilder processBuilder = new ProcessBuilder(cmakeDir.resolve("bin/cmake").toString(),
				"-G", generator).directory(targetPath);
			if (options != null)
				processBuilder.command().addAll(options);
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
