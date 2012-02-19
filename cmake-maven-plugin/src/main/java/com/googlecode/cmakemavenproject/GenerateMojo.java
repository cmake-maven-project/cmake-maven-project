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
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
				"maven-dependency-plugin", "2.4");

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

			MojoExecutor.Element groupIdElement = new MojoExecutor.Element("groupId", groupId);
			MojoExecutor.Element artifactIdElement = new MojoExecutor.Element("artifactId", artifactId);
			MojoExecutor.Element versionElement = new MojoExecutor.Element("version", version);
			MojoExecutor.Element classifierElement = new MojoExecutor.Element("classifier", classifier);
			MojoExecutor.Element outputDirectoryElement = new MojoExecutor.Element("outputDirectory",
				cmakeDir.toString());
			MojoExecutor.Element artifactItemElement = new MojoExecutor.Element("artifactItem",
				groupIdElement, artifactIdElement, versionElement, classifierElement,
				outputDirectoryElement);
			MojoExecutor.Element artifactItemsItem = new MojoExecutor.Element("artifactItems",
				artifactItemElement);
			Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
			MojoExecutor.ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project,
				session, pluginManager);
			MojoExecutor.executeMojo(dependencyPlugin, "unpack", configuration, environment);
			ProcessBuilder processBuilder = new ProcessBuilder(cmakeDir.resolve("bin/cmake").toString(),
				sourcePath.getAbsolutePath(), "-G", generator).directory(targetPath);

			Map<String, String> env = processBuilder.environment();

			if (environmentVariables != null)
				env.putAll(environmentVariables);
			Log log = getLog();
			if (log.isDebugEnabled())
			{
				log.debug("sourcePath: " + sourcePath.getPath());
				log.debug("targetPath: " + targetPath.getPath());
				log.debug("command-line: " + processBuilder.command());
				log.debug("environment: " + processBuilder.environment());
			}
			int returnCode = Mojos.waitFor(processBuilder);
			if (returnCode != 0)
				throw new MojoExecutionException("Return code: " + returnCode);
		}
		catch (InterruptedException e)
		{
			throw new MojoExecutionException("", e);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}

	/**
	 * Unpacks a zip file into a directory.
	 *
	 * @param file the file
	 * @param dir the directory
	 * @throws IOException if an I/O error occurs
	 */
	private void unpack(File file, File dir) throws IOException
	{
		ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)));
		try
		{
			byte[] buffer = new byte[10 * 1024];
			while (true)
			{
				ZipEntry entry = in.getNextEntry();
				if (entry == null)
					break;

				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(dir,
					entry.getName())));
				try
				{
					long bytesLeft = entry.getSize();
					while (bytesLeft > 0)
					{
						int count = in.read(buffer, 0, (int) Math.min(buffer.length, bytesLeft));
						out.write(buffer, 0, count);
						bytesLeft -= count;
					}
				}
				finally
				{
					out.close();
				}
			}
		}
		finally
		{
			in.close();
		}
	}
}
