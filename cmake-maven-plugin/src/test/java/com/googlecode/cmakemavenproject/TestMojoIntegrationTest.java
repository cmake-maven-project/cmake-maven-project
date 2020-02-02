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
import org.junit.Test;

import java.io.IOException;

/**
 * An integration test that runs tests related to testing CMake projects.
 *
 * @author <a href="mailto:ksclarke@gmail.com">Kevin S. Clarke</a>
 */
public class TestMojoIntegrationTest extends CMakeMojoIntegrationTest
{
	/**
	 * Runs the hello-world-test.
	 *
	 * @throws Exception If the test fails as a result of an exception
	 */
	@Test
	public void testGenerateHelloTest() throws IOException, VerificationException
	{
		Verifier verifier = getVerifier("hello-world-test");
		verifier.displayStreamBuffers();
		verifier.executeGoal("verify");
		verifier.resetStreams();
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Runs the generate-dashboard-test.
	 *
	 * @throws IOException           if there is a problem with the test configuration
	 * @throws VerificationException if the test fails as a result of an exception
	 */
	@Test
	public void testGenerateDashboardTest() throws IOException, VerificationException
	{
		Verifier verifier = getVerifier("dashboard-test");
		verifier.displayStreamBuffers();
		verifier.executeGoal("verify");
		verifier.resetStreams();
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Runs the explicit-generator-test.
	 *
	 * @throws IOException           if there is a problem with the test configuration
	 * @throws VerificationException if the test fails as a result of an exception
	 */
	@Test
	public void testExplicitGeneratorTest() throws IOException, VerificationException
	{
		Verifier verifier = getVerifier("explicit-generator-test");
		verifier.displayStreamBuffers();
		verifier.executeGoal("verify");
		verifier.resetStreams();
		verifier.verifyErrorFreeLog();
	}

	/**
	 * Runs the binaries-on-path-test.
	 *
	 * @throws IOException           if there is a problem with the test configuration
	 * @throws VerificationException if the test fails as a result of an exception
	 */
	@Test
	public void testBinariesOnPathTest() throws IOException, VerificationException
	{
		Verifier verifier = getVerifier("binaries-on-path-test");
		verifier.displayStreamBuffers();
		verifier.executeGoal("verify");
		verifier.resetStreams();
		verifier.verifyErrorFreeLog();
	}
}
