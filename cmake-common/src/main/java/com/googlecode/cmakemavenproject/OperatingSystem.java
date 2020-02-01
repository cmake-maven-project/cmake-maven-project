package com.googlecode.cmakemavenproject;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.bitbucket.cowwoc.pouch.ConcurrentLazyReference;
import org.bitbucket.cowwoc.pouch.Reference;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * An operating system.
 */
public final class OperatingSystem
{
	private static final Reference<OperatingSystem> DETECTED = ConcurrentLazyReference.create(() ->
	{
		Type type = Type.detected();
		Architecture architecture = Architecture.detected();
		return new OperatingSystem(type, architecture);
	});

	/**
	 * @return the detected operating system
	 * @throws AssertionError if the operating system is unsupported
	 */
	public static OperatingSystem detected()
	{
		return DETECTED.getValue();
	}

	public final Type type;
	public final Architecture architecture;

	/**
	 * @return the classifier associated with this operating system
	 * @throws UnsupportedOperationException if the operating system was unsupported
	 */
	public String getClassifier()
	{
		switch (type)
		{
			case LINUX:
				switch (architecture)
				{
					case X86_32:
						return "linux-arm_32";
					case X86_64:
						return "linux-x86_64";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			case MAC:
				if (architecture == Architecture.X86_64)
					return "mac-x86_64";
				throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
			case WINDOWS:
				switch (architecture)
				{
					case X86_64:
						return "windows-x86_64";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * @param filename the filename of a binary
	 * @return the command-line arguments for running the binary on the path
	 */
	public List<String> getCommandLineForRunningBinaryOnPath(String filename)
	{
		switch (type)
		{
			case LINUX:
			case MAC:
				return Collections.singletonList(filename);
			case WINDOWS:
				return Arrays.asList("cmd.exe", "/c", filename + ".exe");
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * @return the suffix to append to the cmake executables
	 * @throws UnsupportedOperationException if the operating system was unsupported
	 */
	public String getExecutableSuffix()
	{
		switch (type)
		{
			case LINUX:
			case MAC:
				return "";
			case WINDOWS:
				return ".exe";
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * @return the suffix to append to the cmake download filename
	 * @throws UnsupportedOperationException if the operating system was unsupported
	 */
	public String getDownloadSuffix()
	{
		switch (type)
		{
			case LINUX:
				switch (architecture)
				{
					case X86_64:
						return "Linux-x86_64.tar.gz";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			case MAC:
				if (architecture == Architecture.X86_64)
					return "Darwin-x86_64.tar.gz";
				throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
			case WINDOWS:
				switch (architecture)
				{
					case X86_64:
						return "win64-x64.zip";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * @param in the InputStream associated with the archive
	 * @return true if the operating system supports POSIX attributes
	 * @throws UnsupportedOperationException if the operating system was unsupported
	 */
	public boolean supportsPosix(InputStream in)
	{
		switch (type)
		{
			case LINUX:
			case MAC:
				return in instanceof ArchiveInputStream;
			case WINDOWS:
				return false;
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * @param type         the type of the operating system
	 * @param architecture the architecture of the operating system
	 * @throws AssertionError if any of the arguments are null
	 */
	OperatingSystem(Type type, Architecture architecture)
	{
		assert (type != null) : "type may not be null";
		assert (architecture != null) : "architecture may not be null";
		this.type = type;
		this.architecture = architecture;
	}

	@Override
	public String toString()
	{
		return type + " " + architecture;
	}

	/**
	 * The architecture of an operating system.
	 * <p>
	 * Naming convention based on https://github.com/trustin/os-maven-plugin.
	 */
	public enum Architecture
	{
		X86_32,
		X86_64;

		private static final Reference<Architecture> DETECTED = ConcurrentLazyReference.create(() ->
		{
			String osArch = System.getProperty("os.arch");
			osArch = osArch.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
			switch (osArch)
			{
				case "x8632":
				case "x86":
				case "i386":
				case "i486":
				case "i586":
				case "i686":
				case "ia32":
				case "x32":
					return X86_32;
				case "x8664":
				case "amd64":
				case "ia32e":
				case "em64t":
				case "x64":
					return X86_64;
				default:
					throw new AssertionError("Unsupported architecture: " + osArch + "\n" +
						"properties: " + System.getProperties());
			}
		});

		/**
		 * @return the architecture of the detected operating system
		 */
		public static Architecture detected()
		{
			return DETECTED.getValue();
		}
	}

	/**
	 * Operating system types.
	 */
	public enum Type
	{
		WINDOWS,
		LINUX,
		MAC;

		private static final Reference<Type> DETECTED = ConcurrentLazyReference.create(() ->
		{
			String osName = System.getProperty("os.name");
			if (startsWith(osName, "windows", true))
				return WINDOWS;
			if (startsWith(osName, "linux", true))
				return LINUX;
			if (startsWith(osName, "mac", true))
				return MAC;
			throw new AssertionError("Unsupported operating system: " + osName + "\n" +
				"properties: " + System.getProperties());
		});

		/**
		 * @return the type of the detected operating system
		 */
		public static Type detected()
		{
			return DETECTED.getValue();
		}

		/**
		 * @param str        a string
		 * @param prefix     a prefix
		 * @param ignoreCase {@code true} if case should be ignored when comparing characters
		 * @return true if {@code start} starts with {@code prefix}, disregarding case sensitivity
		 * @throws NullPointerException if any of the arguments are null
		 */
		public static boolean startsWith(String str, String prefix, boolean ignoreCase)
		{
			return str.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
		}
	}
}
