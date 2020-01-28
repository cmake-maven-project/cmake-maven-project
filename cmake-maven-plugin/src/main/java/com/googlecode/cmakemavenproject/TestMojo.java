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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Goal which runs CMake/CTest tests.
 *
 * @author <a href="mailto:ksclarke@gmail.com">Kevin S. Clarke</a>
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class TestMojo extends AbstractMojo
{
	/**
	 * The directory containing the DartConfiguration.tcl file.
	 */
	@Parameter(property = "ctest.build.dir", required = true)
	private File buildDirectory;
	/**
	 * The Maven project directory.
	 */
	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;
	/**
	 * Value that lets Maven tests fail without causing the build to fail.
	 */
	@Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
	private boolean testFailureIgnore;
	/**
	 * Maven tests value that indicates just the ctest tests are to be skipped.
	 */
	@Parameter(property = "ctest.skip.tests", defaultValue = "false")
	private boolean ctestSkip;
	/**
	 * Standard Maven tests value that indicates all tests are to be skipped.
	 */
	@Parameter(property = "maven.test.skip", defaultValue = "false")
	private boolean skipTests;
	/**
	 * Number of threads to use; if not specified, uses
	 * <code>Runtime.getRuntime().availableProcessors()</code>.
	 */
	@Parameter(property = "threadCount", defaultValue = "0")
	private int threadCount;
	/**
	 * The dashboard to which results should be submitted. This is configured through the optional
	 * CTestConfig.cmake file.
	 */
	@Parameter(property = "dashboard")
	private String dashboard;
	/**
	 * The environment variables.
	 */
	@Parameter
	private Map<String, String> environmentVariables;
	/**
	 * Extra command-line options to pass to ctest.
	 */
	@Parameter
	private List<String> options;

	@Parameter(property = "download.cmake", defaultValue = "true")
	private boolean downloadBinaries;

	@Parameter(property = "cmake.root.dir", defaultValue = "/usr")
	private String cmakeRootDir;

	@Parameter(property = "cmake.ctest.dir")
	private String ctestChildDir;

	/**
	 * Executes the CTest run.
	 *
	 * @throws MojoExecutionException if an unexpected problem occurs
	 * @throws MojoFailureException   if the test(s) fail
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		Log log = getLog();

		// Surefire skips tests with properties so we'll do it this way too
		if (skipTests || ctestSkip)
		{
			if (log.isInfoEnabled())
				log.info("Tests are skipped.");
			return;
		}
		if (ctestChildDir == null)
			ctestChildDir = Mojos.getCtestPath();

		if (threadCount == 0)
			threadCount = Runtime.getRuntime().availableProcessors();

		try
		{
			String threadCountString = Integer.toString(threadCount);
			String projBuildDir = project.getBuild().getDirectory();
			String buildDir = buildDirectory.getAbsolutePath();
			List<String> args;

			if (!buildDirectory.exists())
				throw new MojoExecutionException(buildDir + " does not exist");
			if (!buildDirectory.isDirectory())
				throw new MojoExecutionException(buildDir + " isn't directory");

			if (downloadBinaries)
			{
				args = new ArrayList<>(Arrays.asList(
					new File(new File(projBuildDir, "dependency/cmake").getAbsoluteFile(), ctestChildDir)
						.getAbsolutePath(), "-T", "Test", "-j", threadCountString));
			}
			else
			{
				args = new ArrayList<>(Arrays.asList(
					new File(cmakeRootDir, ctestChildDir)
						.getAbsolutePath(), "-T", "Test", "-j", threadCountString));
			}

			// If set, this will post results to a pre-configured dashboard
			if (dashboard != null)
				args.addAll(Arrays.asList("-D", dashboard));

			ProcessBuilder processBuilder = new ProcessBuilder(args);

			// Set the directory with the DartConfiguration.tcl config file
			processBuilder.directory(buildDirectory);

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

			Map<String, String> env = processBuilder.environment();

			if (environmentVariables != null)
			{
				for (Entry<String, String> entry : environmentVariables.entrySet())
				{
					String value = entry.getValue();
					if (value == null)
					{
						// Maven converts empty properties to null and Linux does not support null values,
						// so we convert them back to empty strings:
						// https://github.com/cmake-maven-project/cmake-maven-project/issues/11
						value = "";
					}
					env.put(entry.getKey(), value);
				}
			}

			if (log.isDebugEnabled())
			{
				log.debug("CTest build directory: " + buildDir);
				log.debug("Number of threads used: " + threadCount);
				log.debug("Environment: " + processBuilder.environment());
				log.debug("Command-line: " + processBuilder.command());
			}

			// Run the ctest suite of tests
			int returnCode = Mojos.waitFor(processBuilder);

			// Convert ctest xml output to junit xml for better integration
			InputStream stream = TestMojo.class.getResourceAsStream("/ct2ju.xslt");
			TransformerFactory tf = TransformerFactory.newInstance();
			StreamSource xsltSource = new StreamSource(stream);
			Transformer transformer = tf.newTransformer(xsltSource);

			// Read the ctest TAG file to find out what current run was called
			File tagFile = new File(buildDirectory, "/Testing/TAG");
			Charset charset = Charset.defaultCharset();
			FileInputStream fis = new FileInputStream(tagFile);
			InputStreamReader isr = new InputStreamReader(fis, charset);
			BufferedReader tagReader = new BufferedReader(isr);
			String tag;
			try
			{
				tag = tagReader.readLine();
			}
			finally
			{
				tagReader.close();
			}

			if (tag == null || tag.trim().length() == 0)
				throw new IOException("Couldn't read ctest TAG file");

			// Get the current run's test data for reformatting
			String xmlTestFilePath = "/Testing/" + tag + "/Test.xml";
			File xmlSource = new File(buildDirectory, xmlTestFilePath);
			StreamSource source = new StreamSource(xmlSource);
			File reportsDir = new File(projBuildDir, "surefire-reports");
			File xmlReport = new File(reportsDir, "CTestResults.xml");
			StreamResult result = new StreamResult(xmlReport);

			// We have to create if there aren't other Surefire tests
			if (!reportsDir.exists())
				if (!reportsDir.mkdirs())
					throw new IOException("Couldn't create " + reportsDir);

			// Transform CTest output into Surefire style test output
			transformer.transform(source, result);

			if (returnCode != 0 && !testFailureIgnore)
				throw new MojoExecutionException("Return code: " + returnCode);
		}
		catch (InterruptedException | IOException | TransformerException e)
		{
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

}
