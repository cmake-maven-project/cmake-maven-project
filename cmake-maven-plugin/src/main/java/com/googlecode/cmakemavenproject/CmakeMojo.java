package com.googlecode.cmakemavenproject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class CmakeMojo extends AbstractMojo
{
	@Component
	private BuildPluginManager pluginManager;
	@Parameter(property = "project", required = true, readonly = true)
	protected MavenProject project;
	@Parameter(property = "session", required = true, readonly = true)
	private MavenSession session;
	@Parameter(property = "cmake.download", defaultValue = "true")
	private boolean downloadBinaries;
	/**
	 * The directory containing the cmake executable. By default, it is assumed that the executable is on
	 * the PATH. This parameter is ignored if {@link #downloadBinaries} is set.
	 */
	@Parameter(property = "cmake.dir")
	private String cmakeDir;
	/**
	 * The environment variables.
	 */
	@Parameter
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to cmake or ctest.
	 */
	@Parameter
	private List<String> options;

	/**
	 * Downloads cmake if necessary.
	 *
	 * @throws MojoExecutionException if the download fails
	 */
	protected void downloadBinariesIfNecessary() throws MojoExecutionException
	{
		if (!downloadBinaries)
			return;
		Path outputDirectory = Paths.get(project.getBuild().getDirectory(), "dependency/cmake");
		downloadBinaries(outputDirectory);
	}

	/**
	 * Downloads cmake.
	 *
	 * @param outputDirectory the directory to download into
	 * @throws MojoExecutionException if the download fails
	 */
	private void downloadBinaries(Path outputDirectory)
		throws MojoExecutionException
	{
		getLog().info("Downloading binaries to " + outputDirectory);
		PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
		String groupId = pluginDescriptor.getGroupId();
		String version = pluginDescriptor.getVersion();
		String binariesArtifact = "cmake-binaries";
		Element groupIdElement = new Element("groupId", groupId);
		Element artifactIdElement = new Element("artifactId", binariesArtifact);
		Element versionElement = new Element("version", version);
		OperatingSystem os = OperatingSystem.detected();
		Element classifierElement = new Element("classifier", os.getClassifier());
		Element outputDirectoryElement = new Element("outputDirectory", outputDirectory.toString());
		Element artifactItemElement = new Element("artifactItem", groupIdElement, artifactIdElement,
			versionElement, classifierElement, outputDirectoryElement);
		Element artifactItemsItem = new Element("artifactItems", artifactItemElement);
		Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
		ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project, session,
			pluginManager);
		Plugin dependencyPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
			"maven-dependency-plugin", "3.1.1");
		MojoExecutor.executeMojo(dependencyPlugin, "unpack", configuration, environment);
	}

	/**
	 * @param filename       the filename of the binary
	 * @param processBuilder the {@code ProcessBuilder}
	 * @return the command-line arguments for running the binary
	 * @throws FileNotFoundException if the binary was not found
	 */
	public Path getBinaryPath(String filename, ProcessBuilder processBuilder) throws FileNotFoundException
	{
		OperatingSystem os = OperatingSystem.detected();
		Path cmakeDir = getCmakeDir();
		if (cmakeDir == null)
		{
			getLog().info("Executing " + filename + " on PATH");
			return os.getExecutableOnPath(filename, processBuilder.environment().get("PATH"));
		}
		Path result = cmakeDir.resolve(filename + os.getExecutableSuffix());
		getLog().info("Executing " + result);
		return result;
	}

	/**
	 * Returns the directory containing the cmake binaries.
	 *
	 * @return {@code null} if cmake should be executed from the PATH
	 */
	private Path getCmakeDir()
	{
		if (downloadBinaries)
		{
			Path outputDirectory = Paths.get(project.getBuild().getDirectory(), "dependency/cmake");
			return outputDirectory.resolve("bin");
		}
		if (cmakeDir == null)
			return null;
		return Paths.get(cmakeDir);
	}

	/**
	 * Adds command-line options to the processBuilder.
	 *
	 * @param processBuilder the {@code ProcessBuilder}
	 */
	public void addOptions(ProcessBuilder processBuilder)
	{
		if (options == null)
			return;
		// Skip undefined Maven properties:
		// <options>
		//   <option>${optional.property}</option>
		// </options>
		List<String> nonEmptyOptions = options.stream().filter(option -> !option.isEmpty()).
			collect(Collectors.toList());
		processBuilder.command().addAll(nonEmptyOptions);
	}

	/**
	 * Overrides environment variables in the {@code ProcessBuilder}.
	 *
	 * @param processBuilder the {@code ProcessBuilder}
	 */
	public void overrideEnvironmentVariables(ProcessBuilder processBuilder)
	{
		if (environmentVariables == null)
			return;
		Map<String, String> env = processBuilder.environment();
		Mojos.overrideEnvironmentVariables(environmentVariables, env);
	}
}
