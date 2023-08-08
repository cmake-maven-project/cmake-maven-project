package com.googlecode.cmakemavenproject;

import com.github.cowwoc.pouch.core.ConcurrentLazyReference;
import com.github.cowwoc.pouch.core.Reference;
import org.apache.commons.compress.archivers.ArchiveInputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

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
	 * Returns the detected operating system.
	 *
	 * @return the detected operating system
	 * @throws AssertionError if the operating system is unsupported
	 */
	public static OperatingSystem detected()
	{
		return DETECTED.getValue();
	}

	/**
	 * The type of the operating system.
	 */
	public final Type type;
	/**
	 * The architecture of the operating system.
	 */
	public final Architecture architecture;

	/**
	 * Returns the classifier associated with this operating system.
	 *
	 * @return the classifier associated with this operating system
	 * @throws UnsupportedOperationException if the operating system was unsupported
	 */
	public String getClassifier()
	{
		switch (type)
		{
			case LINUX:
				return "linux-" + architecture.name().toLowerCase(Locale.ENGLISH);
			case MAC:
				switch (architecture)
				{
					case X86_64:
					case ARM_32:
					case ARM_64:
						return "mac-universal";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			case WINDOWS:
				switch (architecture)
				{
					case X86_64:
						return "windows-x86_64";
					case ARM_64:
						return "windows-arm64";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * Returns the fully-qualified path of the executable.
	 *
	 * @param filename the filename of a binary
	 * @param path     the {@code PATH} environment variable
	 * @return the fully-qualified path of the executable
	 * @throws NullPointerException  if any of the arguments are null
	 * @throws FileNotFoundException if the binary could not be found
	 */
	public Path getExecutableOnPath(String filename, String path) throws FileNotFoundException
	{
		if (filename == null)
			throw new NullPointerException("filename may not be null");
		if (path == null)
			throw new NullPointerException("path may not be null");
		// Per https://stackoverflow.com/a/34061154/14731 it's easier to invoke a fully-qualified path
		// than trying to quote command-line arguments properly.
		// https://stackoverflow.com/a/32827512/14731 shows how this can be done.
		String suffix = getExecutableSuffix();
		for (String dirname : path.split(File.pathSeparator))
		{
			// Strip leading/trailing quotes
			dirname = dirname.trim();
			if (dirname.length() >= 2 &&
				(dirname.startsWith("\"") && dirname.endsWith("\"")) ||
				(dirname.startsWith("'") && dirname.endsWith("'")))
			{
				dirname = dirname.substring(1, dirname.length() - 1);
			}
			Path result = Paths.get(dirname, filename + suffix);
			if (Files.isRegularFile(result) && Files.isExecutable(result))
				return result;
		}
		throw new FileNotFoundException(filename + " not found on PATH: " + path);
	}

	/**
	 * Returns the suffix to append to the cmake executables.
	 *
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
	 * Returns the suffix to append to the cmake download filename.
	 *
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
						return "linux-x86_64.tar.gz";
					case ARM_64:
						return "linux-aarch64.tar.gz";
					case ARM_32:
						// cmake is assumed to ship with the operating system
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			case MAC:
				switch (architecture)
				{
					case X86_64:
					case ARM_32:
					case ARM_64:
						return "macos-universal.tar.gz";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			case WINDOWS:
				switch (architecture)
				{
					case X86_64:
						return "windows-x86_64.zip";
					case ARM_64:
						return "windows-arm64.zip";
					default:
						throw new UnsupportedOperationException("Unsupported architecture: " + architecture);
				}
			default:
				throw new UnsupportedOperationException("Unsupported operating system: " + type);
		}
	}

	/**
	 * Indicates if the operating system supports POSIX attributes.
	 *
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
	 * Returns the value of a {@code ProcessBuilder}'s environment variable.
	 *
	 * @param processBuilder a {@code ProcessBuilder}
	 * @param name           an environment variable
	 * @return the value of the environment variable
	 */
	public String getEnvironment(ProcessBuilder processBuilder, String name)
	{
		Map<String, String> environment = processBuilder.environment();
		return environment.get(type.getEnvironmentCanonicalName(environment, name));
	}

	/**
	 * Overrides the environment variables of a process builder.
	 *
	 * @param source new environment variables
	 * @param target existing environment variables
	 * @throws NullPointerException if any of the arguments are null
	 */
	public void overrideEnvironmentVariables(Map<String, String> source, ProcessBuilder target)
	{
		assert (source != null);
		assert (target != null);
		if (source.isEmpty())
			return;

		Map<String, String> environment = target.environment();
		for (Entry<String, String> entry : source.entrySet())
		{
			String value = entry.getValue();
			if (value == null)
			{
				// Maven converts empty properties to null and Linux does not support null values,
				// so we convert them back to empty strings:
				// https://github.com/cmake-maven-project/cmake-maven-project/issues/11
				value = "";
			}
			String name = entry.getKey();
			name = type.getEnvironmentCanonicalName(environment, name);
			environment.put(name, value);
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
	 * Naming convention based on <a href="https://github.com/trustin/os-maven-plugin">os-maven-plugin</a>.
	 */
	public enum Architecture
	{
		/**
		 * x86, 32-bit.
		 */
		X86_32,
		/**
		 * x86, 64-bit.
		 */
		X86_64,
		/**
		 * ARM, 32-bit.
		 */
		ARM_32,
		/**
		 * ARM, 64-bit.
		 */
		ARM_64;

		private static final Reference<Architecture> DETECTED = ConcurrentLazyReference.create(() ->
		{
			String osArch = System.getProperty("os.arch");
			osArch = osArch.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "");
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
				case "arm":
				case "arm32":
					return ARM_32;
				case "arm64":
				case "aarch64":
					return ARM_64;
				default:
					throw new AssertionError("Unsupported architecture: " + osArch + "\n" +
						"properties: " + System.getProperties());
			}
		});

		/**
		 * Returns the architecture of the detected operating system.
		 *
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
		/**
		 * Windows.
		 */
		WINDOWS
			{
				@Override
				String getEnvironmentCanonicalName(Map<String, String> environment, String name)
				{
					// WORKAROUND: https://bugs.openjdk.java.net/browse/JDK-8245431
					for (String canonicalName : environment.keySet())
						if (canonicalName.equalsIgnoreCase(name))
							return canonicalName;
					return name;
				}
			},
		/**
		 * Linux.
		 */
		LINUX
			{
				@Override
				String getEnvironmentCanonicalName(Map<String, String> environment, String name)
				{
					return name;
				}
			},
		/**
		 * macOS.
		 */
		MAC
			{
				@Override
				String getEnvironmentCanonicalName(Map<String, String> environment, String name)
				{
					return name;
				}
			};

		private static final Reference<Type> DETECTED = ConcurrentLazyReference.create(() ->
		{
			String osName = System.getProperty("os.name");
			if (startsWithIgnoreCase(osName, "windows"))
				return WINDOWS;
			if (startsWithIgnoreCase(osName, "linux"))
				return LINUX;
			if (startsWithIgnoreCase(osName, "mac"))
				return MAC;
			throw new AssertionError("Unsupported operating system: " + osName + "\n" +
				"properties: " + System.getProperties());
		});

		/**
		 * Returns the type of the detected operating system.
		 *
		 * @return the type of the detected operating system
		 */
		public static Type detected()
		{
			return DETECTED.getValue();
		}

		/**
		 * @param str    a string
		 * @param prefix a prefix
		 * @return true if {@code start} starts with {@code prefix}, disregarding case sensitivity
		 * @throws NullPointerException if any of the arguments are null
		 */
		private static boolean startsWithIgnoreCase(String str, String prefix)
		{
			return str.regionMatches(true, 0, prefix, 0, prefix.length());
		}

		/**
		 * @param environment a {@code ProcessBuilder}'s environment variables
		 * @param name        an environment variable
		 * @return the case-sensitive form of the variable
		 */
		abstract String getEnvironmentCanonicalName(Map<String, String> environment, String name);
	}
}
