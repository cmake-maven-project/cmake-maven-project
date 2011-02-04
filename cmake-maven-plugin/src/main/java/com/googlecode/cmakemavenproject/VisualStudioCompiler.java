package com.googlecode.cmakemavenproject;

import java.io.File;
import java.io.IOException;
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
		 * Visual Studio 2005.
		 */
		V8,
		/**
		 * Visual Studio 2008.
		 */
		V9,
		/**
		 * Visual Studio 2010.
		 */
		V10
	}
	private final Version version;

	/**
	 * Creates a new Visual Studio compiler.
	 *
	 * @param version the compiler version
	 * @param log Maven log
	 */
	public VisualStudioCompiler(Version version, Log log)
	{
		super(Type.VISUAL_STUDIO, getPath(version), log);
		this.version = version;
	}

	/**
	 * Returns the compiler path.
	 *
	 * @param version the compiler version
	 * @return the compiler path
	 */
	private static File getPath(Version version)
	{
		switch (version)
		{
			case V6:
				return new File(System.getenv("VS60COMNTOOLS") + "/../IDE/devenv.com");
			case V7_0:
				return new File(System.getenv("VS70COMNTOOLS") + "/../IDE/devenv.com");
			case V7_1:
				return new File(System.getenv("VS71COMNTOOLS") + "/../IDE/devenv.com");
			case V8:
				return new File(System.getenv("VS80COMNTOOLS") + "/../IDE/devenv.com");
			case V9:
				return new File(System.getenv("VS90COMNTOOLS") + "/../IDE/devenv.com");
			case V10:
				return new File(System.getenv("VS100COMNTOOLS") + "/../IDE/devenv.com");
			default:
				throw new AssertionError(version);
		}
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

	/**
	 * Indicates if the JVM is 32-bit.
	 *
	 * @return true if the JVM is 32-bit
	 */
	private static boolean is32Bit()
	{
		String architecture = System.getProperty("os.arch");
		return architecture.equals("x86") || architecture.equals("i386");
	}

	@Override
	public String getName()
	{
		StringBuilder result = new StringBuilder("Visual Studio ");
		switch (version)
		{
			case V6:
			{
				result.append("6");
				break;
			}
			case V7_0:
			{
				result.append("7");
				break;
			}
			case V7_1:
			{
				result.append("7 .NET 2003");
				break;
			}
			case V8:
			{
				result.append("8 2005");
				if (!is32Bit())
					result.append(" Win64");
				break;
			}
			case V9:
			{
				result.append("9 2008");
				if (!is32Bit())
					result.append(" Win64");
				break;
			}
			case V10:
			{
				result.append("10 2010");
				if (!is32Bit())
					result.append(" Win64");
				break;
			}
			default:
				throw new AssertionError(version);
		}
		return result.toString();
	}

	@Override
	public boolean compile(File projectPath, String platform, String buildType)
		throws IOException, InterruptedException
	{
		ProcessBuilder processBuilder = new ProcessBuilder(getPath().getAbsolutePath(),
			"/build", "\"" + buildType + "|" + platform + "\"", projectPath.toString()).directory(projectPath.
			getParentFile());
		Log log = getLog();
		if (log.isDebugEnabled())
		{
			log.debug("compilerPath: " + getPath());
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
		return getClass().getName() + "[path=" + getPath() + ", version=" + version + "]";
	}
}
