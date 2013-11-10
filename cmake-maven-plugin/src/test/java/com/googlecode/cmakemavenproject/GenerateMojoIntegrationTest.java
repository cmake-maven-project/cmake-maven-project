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

import org.apache.maven.it.Verifier;

import org.junit.Test;

/**
 * An integration test that runs tests related to generating code using CMake.
 * <p/>
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 */
public class GenerateMojoIntegrationTest extends CMakeMojoIntegrationTest
{

    /**
     * Tests the generation of a simple Hello-World-Test project.
     * 
     * @throws Exception If the test fails as a result of an exception
     */
    @Test
    public void testGenerateHelloWorldTest() throws Exception
    {
        Verifier verifier = getVerifier("hello-world-test");
        verifier.displayStreamBuffers();
        verifier.executeGoal("process-sources");
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

    /**
     * Tests the generation of a simple Dashboard-Test project.
     * 
     * @throws Exception If the test fails as a result of an exception
     */
    @Test
    public void testGenerateDashboardTest() throws Exception
    {
        Verifier verifier = getVerifier("dashboard-test");
        verifier.displayStreamBuffers();
        verifier.executeGoal("process-sources");
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

}
