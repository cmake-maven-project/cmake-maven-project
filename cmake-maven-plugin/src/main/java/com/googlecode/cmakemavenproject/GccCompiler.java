package com.googlecode.cmakemavenproject;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.logging.Log;

/**
 * A source-code compiler.
 *
 * @author Gili Tzabari
 */
public final class GccCompiler extends Compiler
{
	/**
	 * Creates a new GCC compiler.
	 *
	 * @param log Maven log
	 */
	public GccCompiler(Log log)
	{
		super(Type.GNU_MAKE, log);
	}

	/**
	 * Creates a Compiler from a makefile generator name.
	 *
	 * @param name the makefile generator
	 * @param log the Maven log
	 * @return null if the generator does not map to a VisualStudio compiler
	 */
	public static GccCompiler fromGenerator(String name, Log log)
	{
		if (!name.equals("Unix Makefiles"))
			return null;
		return new GccCompiler(log);
	}

	@Override
	public boolean compile(File projectPath, String platform, String buildType)
		throws IOException, InterruptedException
	{
		List<String> commandLine = Lists.newArrayList("make");

		ProcessBuilder processBuilder = new ProcessBuilder(commandLine).directory(projectPath.
			getParentFile());
		Log log = getLog();
		if (log.isDebugEnabled())
		{
			log.debug("buildType: " + buildType);
			log.debug("platform: " + platform);
			log.debug("projectPath: " + projectPath);
			log.debug("command-line: " + processBuilder.command());
		}
		return Mojos.waitFor(processBuilder) == 0;
	}

	@Override
	public String toString()
	{
		return getClass().getName();
	}
}
