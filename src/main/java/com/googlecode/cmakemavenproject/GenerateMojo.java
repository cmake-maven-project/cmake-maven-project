package com.googlecode.cmakemavenproject;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;

/**
 * Goal which generates project files.
 *
 * @goal generate
 * @phase process-sources
 * @author Gili Tzabari
 */
public class GenerateMojo
	extends AbstractMojo
{
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

	@Override
	public void execute()
		throws MojoExecutionException
	{
		try
		{
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());
			Log log = getLog();
			@java.lang.SuppressWarnings("unchecked")
			ProcessBuilder processBuilder = new ProcessBuilder("cmake", sourcePath.getAbsolutePath(), "-G",
				generator).directory(targetPath);
			if (environmentVariables != null)
				processBuilder.environment().putAll(environmentVariables);
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
}
