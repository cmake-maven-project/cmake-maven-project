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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * An abstract test class that handles <code>Verifier</code> configuration.
 *
 * @author <a href="mailto:ksclarke@gmail.com">Kevin S. Clarke</a>
 */
public abstract class CMakeMojoIntegrationTest
{
	// Maven settings.xml file to be used for the test projects
	private static final String SETTINGS = "/settings.xml";

	// Get our group id
	private static final String PLUGIN_GROUPID = "cmake.plugin.groupid";

	// CMake-Maven-Plugin version (so we don't have to manually keep in sync)
	private static final String PLUGIN_VERSION = "cmake.plugin.version";

	// Get the platform configured by our build process
	private static final String BUILD_PLATFORM = "platform";

	private static final String DOWNLOAD_CMAKE = "download.cmake";

	/**
	 * Returns a <code>Verifier</code> that has been configured to use the test repository along with
	 * the test project that was passed in as a variable.
	 *
	 * @param testName The CMake Maven project to test
	 * @return A configured <code>Verifier</code>
	 * @throws IOException           if there is a problem with the configuration
	 * @throws VerificationException if there is a problem with the verification
	 */
	protected Verifier getVerifier(String testName) throws IOException, VerificationException
	{
		Class<? extends CMakeMojoIntegrationTest> cls = getClass();
		String name = testName.startsWith("/") ? testName : "/" + testName;
		File config = ResourceExtractor.simpleExtractResources(cls, SETTINGS);
		File test = ResourceExtractor.simpleExtractResources(cls, name);
		String settings = config.getAbsolutePath();

		// Construct a verifier that will run our integration tests
		Verifier verifier = new Verifier(test.getAbsolutePath(), settings, true);
		Properties verProperties = verifier.getVerifierProperties();
		Properties sysProperties = verifier.getSystemProperties();

		// Pass along properties from our parent project
		sysProperties.setProperty(PLUGIN_GROUPID, System.getProperty(PLUGIN_GROUPID));
		sysProperties.setProperty(PLUGIN_VERSION, System.getProperty(PLUGIN_VERSION));
		sysProperties.setProperty(BUILD_PLATFORM, System.getProperty(BUILD_PLATFORM));

		if (System.getProperty(DOWNLOAD_CMAKE, "true").equals("false"))
			sysProperties.setProperty(DOWNLOAD_CMAKE, "false");

		// use.mavenRepoLocal instructs forked tests to use the local repo
		verProperties.setProperty("use.mavenRepoLocal", "true");

		// Clean before each test
		verifier.setAutoclean(true);
		return verifier;
	}
}
