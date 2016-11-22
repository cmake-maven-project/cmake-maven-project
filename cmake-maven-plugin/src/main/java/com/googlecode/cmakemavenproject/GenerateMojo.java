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
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private static final Set<String> VALID_CLASSIFIERS = ImmutableSet.of("windows-i386",
		"windows-amd64", "linux-i386", "linux-amd64", "linux-arm", "mac-amd64");
	/**
	 * The classifier of the current platform.
	 * <p>
	 * One of [windows-i386, windows-amd64, linux-i386, linux-amd64, linux-arm, mac-amd64].
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(property = "classifier", readonly = true)
	private String classifier;
	/**
	 * /**
	 * The directory containing CMakeLists.txt.
	 */
	@SuppressFBWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(required = true)
	private File sourcePath;
	/**
	 * The output directory.
	 */
	@SuppressFBWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(required = true)
	private File targetPath;
	/**
	 * The makefile generator to use.
	 */
	@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(required = true)
	private String generator;
	/**
	 * The environment variables.
	 */
	@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to cmake.
	 */
	@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter
	private List<String> options;
	@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
	@Component
	private BuildPluginManager pluginManager;
	@SuppressFBWarnings(
		{
			"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
		})
	@Parameter(property = "project", required = true, readonly = true)
	private MavenProject project;
	@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
	@Parameter(property = "session", required = true, readonly = true)
	private MavenSession session;

	@Parameter(property = "download.cmake", defaultValue = "true", required = false)
	private boolean downloadBinaries;

	@Parameter(property = "cmake.root.dir", defaultValue = "/usr", required = false)
	private String cmakeRootDir;

	@Parameter(property = "cmake.child.dir", defaultValue = "bin/cmake", required = false)
	private String cmakeChildDir;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		try
		{
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

			File cmakeDir;
			if (downloadBinaries)
			{
				getLog().info("Downloading binaries");
				cmakeDir = new File(project.getBuild().getDirectory(), "dependency/cmake");
				if (classifier == null)
					throw new MojoExecutionException("\"classifier\" must be set");
				if (!VALID_CLASSIFIERS.contains(classifier))
				{
					throw new MojoExecutionException("\"classifier\" must be one of " + VALID_CLASSIFIERS +
						"\nActual: " + classifier);
				}
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

			ProcessBuilder processBuilder = new ProcessBuilder(
				new File(cmakeDir, cmakeChildDir).getAbsolutePath(),
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
