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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Goal which generates project files.
 *
 * @author Gili Tzabari
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class GenerateMojo extends CmakeMojo
{
	/**
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
	 * Extra command-line options to pass to cmake.
	 */
	@Parameter
	private List<String> options;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		try
		{
			if (!sourcePath.exists())
				throw new MojoExecutionException("sourcePath does not exist: " + sourcePath.getAbsolutePath());
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

			downloadBinariesIfNecessary();

			ProcessBuilder processBuilder = new ProcessBuilder().directory(targetPath);
			overrideEnvironmentVariables(processBuilder);

			String cmakePath = getBinaryPath("cmake", processBuilder).toString();
			processBuilder.command().add(cmakePath);

			if (generator != null && !generator.trim().isEmpty())
				Collections.addAll(processBuilder.command(), "-G", generator);

			addOptions(processBuilder);
			processBuilder.command().add(sourcePath.getAbsolutePath());

			Log log = getLog();
			if (log.isDebugEnabled())
			{
				log.debug("sourcePath: " + sourcePath);
				log.debug("targetPath: " + targetPath);
				log.debug("environment: " + processBuilder.environment());
				log.debug("command-line: " + processBuilder.command());
			}
			int returnCode = Mojos.waitFor(processBuilder, getLog());
			if (returnCode != 0)
				throw new MojoExecutionException("Return code: " + returnCode);
		}
		catch (InterruptedException | IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}
}
