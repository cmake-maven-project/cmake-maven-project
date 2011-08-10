package com.googlecode.cmakemavenproject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;

/**
 * A source-code compiler.
 *
 * @author Gili Tzabari
 */
public final class VisualStudioCompiler extends Compiler
{
	/**
	 * Visual Studio versions.
	 */
	public enum Version
	{
		/**
		 * Visual Studio 6.0
		 */
		V6,
		/**
		 * Visual Studio .NET 2002
		 */
		V7_0,
		/**
		 * Visual Studio .NET 2003
		 */
		V7_1,
		/**
		 * Visual Studio 2005, 32-bit.
		 */
		V8_I386,
		/**
		 * Visual Studio 2005, 64-bit.
		 */
		V8_AMD64,
		/**
		 * Visual Studio 2008, 32-bit.
		 */
		V9_I386,
		/**
		 * Visual Studio 2008, 64-bit.
		 */
		V9_AMD64,
		/**
		 * Visual Studio 2010, 32-bit.
		 */
		V10_I386,
		/**
		 * Visual Studio 2010, 64-bit.
		 */
		V10_AMD64
	}
	private static Map<String, Version> compilers =
																			new ImmutableMap.Builder<String, Version>().put(
		"Visual Studio 6", Version.V6).
		put("Visual Studio 7", Version.V7_0).
		put("Visual Studio 7 .NET 2003", Version.V7_1).
		put("Visual Studio 8", Version.V8_I386).
		put("Visual Studio 8 Win64", Version.V8_AMD64).
		put("Visual Studio 9", Version.V9_I386).
		put("Visual Studio 9 Win64", Version.V9_AMD64).
		put("Visual Studio 10", Version.V10_I386).
		put("Visual Studio 10 Win64", Version.V10_AMD64).build();
	private final Version version;

	/**
	 * Creates a new Visual Studio compiler.
	 *
	 * @param version the compiler version
	 * @param log Maven log
	 */
	public VisualStudioCompiler(Version version, Log log)
	{
		super(Type.VISUAL_STUDIO, log);
		this.version = version;
	}

	/**
	 * Creates a Compiler from a makefile generator name.
	 *
	 * @param name the makefile generator
	 * @param log the Maven log
	 * @return null if the generator does not map to a VisualStudio compiler
	 */
	public static VisualStudioCompiler fromGenerator(String name, Log log)
	{
		Version version = compilers.get(name);
		if (version == null)
			return null;
		return new VisualStudioCompiler(version, log);
	}

	/**
	 * Returns the compiler version.
	 *
	 * @return the compiler version
	 */
	public Version getVersion()
	{
		return version;
	}

	@Override
	public boolean compile(File projectPath, String platform, String buildType)
		throws IOException, InterruptedException
	{
		List<String> commandLine = Lists.newArrayList("cmd.exe", "/c");
		String compilerPath;
		switch (version)
		{
			case V6:
			{
				compilerPath = System.getenv("VS60COMNTOOLS");
				break;
			}
			case V7_0:
			{
				compilerPath = System.getenv("VS70COMNTOOLS");
				break;
			}
			case V7_1:
			{
				compilerPath = System.getenv("VS71COMNTOOLS");
				break;
			}
			case V8_I386:
			case V8_AMD64:
			{
				compilerPath = System.getenv("VS80COMNTOOLS");
				break;
			}
			case V9_I386:
			case V9_AMD64:
			{
				compilerPath = System.getenv("VS90COMNTOOLS");
				break;
			}
			case V10_I386:
			case V10_AMD64:
			{
				compilerPath = System.getenv("VS100COMNTOOLS");
				break;
			}
			default:
				throw new AssertionError(version);
		}
		switch (version)
		{
			case V6:
			case V7_0:
			case V7_1:
			{
				commandLine.add(new File(compilerPath + "../IDE/devenv.com").getAbsolutePath());
				commandLine.add("/build");
				commandLine.add("\"" + buildType + "|" + platform + "\"");
				commandLine.add(projectPath.toString());
				break;
			}
			case V8_I386:
			case V8_AMD64:
			case V9_I386:
			case V9_AMD64:
			case V10_I386:
			case V10_AMD64:
			{
				commandLine.add(new File(compilerPath + "../../VC/vcvarsall.bat").getAbsolutePath());
				commandLine.add("&&");
				commandLine.add("msbuild");
				commandLine.add("/p:Configuration=" + buildType);
				commandLine.add("/p:Platform=" + platform);
				commandLine.add(projectPath.toString());
				break;
			}
			default:
				throw new AssertionError(version);
		}

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
		return getClass().getName() + "[version=" + version + "]";
	}
}
