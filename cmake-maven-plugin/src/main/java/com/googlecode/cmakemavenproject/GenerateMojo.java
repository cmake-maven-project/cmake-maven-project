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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * Goal which generates project files.
 *
 * @goal generate
 * @phase process-sources
 *
 * @author Gili Tzabari
 */
public class GenerateMojo
	extends AbstractMojo
{
	/**
	 * The release platform.
	 *
	 * @parameter expression="${classifier}"
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String classifier;
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
	/**
	 * @component
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private BuildPluginManager pluginManager;
	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings(
	{
		"UWF_UNWRITTEN_FIELD", "NP_UNWRITTEN_FIELD"
	})
	private MavenProject project;
	/**
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private MavenSession session;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().
			get("pluginDescriptor");
		String version = pluginDescriptor.getVersion();
		try
		{
			if (!targetPath.exists() && !targetPath.mkdirs())
				throw new MojoExecutionException("Cannot create " + targetPath.getAbsolutePath());

			final String groupId = "com.googlecode.cmake-maven-project";
			final String artifactId = "cmake-binaries";

			Plugin dependencyPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
				"maven-dependency-plugin", "2.4");

			if (classifier == null)
			{
				String os = System.getProperty("os.name");
				if (os.toLowerCase().startsWith("windows"))
					classifier = "windows";
				else if (os.toLowerCase().startsWith("linux"))
					classifier = "linux";
				else if (os.toLowerCase().startsWith("mac"))
					classifier = "mac";
				else
					throw new MojoExecutionException("Unsupported os.name: " + os);
			}
			Path cmakeDir = Paths.get(project.getBuild().getDirectory(), "dependency/cmake").
				toAbsolutePath();

			MojoExecutor.Element groupIdElement = new MojoExecutor.Element("groupId", groupId);
			MojoExecutor.Element artifactIdElement = new MojoExecutor.Element("artifactId", artifactId);
			MojoExecutor.Element versionElement = new MojoExecutor.Element("version", version);
			MojoExecutor.Element classifierElement = new MojoExecutor.Element("classifier", classifier);
			MojoExecutor.Element outputDirectoryElement = new MojoExecutor.Element("outputDirectory",
				cmakeDir.toString());
			MojoExecutor.Element artifactItemElement = new MojoExecutor.Element("artifactItem",
				groupIdElement, artifactIdElement, versionElement, classifierElement,
				outputDirectoryElement);
			MojoExecutor.Element artifactItemsItem = new MojoExecutor.Element("artifactItems",
				artifactItemElement);
			Xpp3Dom configuration = MojoExecutor.configuration(artifactItemsItem);
			MojoExecutor.ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project,
				session, pluginManager);
			MojoExecutor.executeMojo(dependencyPlugin, "copy", configuration, environment);
			Path binariesArchive = null;
			for (Path path: Files.newDirectoryStream(cmakeDir))
			{
				if (Files.isRegularFile(path))
				{
					binariesArchive = path;
					break;
				}
			}
			if (binariesArchive == null)
				throw new IOException("Could not find cmake-binaries archive at: " + cmakeDir);
			extract(binariesArchive, cmakeDir);

			ProcessBuilder processBuilder = new ProcessBuilder(cmakeDir.resolve("bin/cmake").toString(),
				sourcePath.getAbsolutePath(), "-G", generator).directory(targetPath);

			Map<String, String> env = processBuilder.environment();

			if (environmentVariables != null)
				env.putAll(environmentVariables);
			Log log = getLog();
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
		catch (InterruptedException | IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}

	/**
	 * Extracts the contents of an archive.
	 *
	 * @param source the file to extract
	 * @param target the directory to extract to
	 * @throws IOException if an I/O error occurs
	 */
	private void extract(Path source, Path target) throws IOException
	{
		Files.createDirectories(target);
		String filename = source.getFileName().toString();
		String extension = getFileExtension(filename);
		String nameWithoutExtension = filename.substring(0, filename.length() - extension.length());
		String nextExtension = getFileExtension(nameWithoutExtension);
		switch (extension)
		{
			case ".jar":
			case ".zip":
			{
				if (!nextExtension.isEmpty())
					throw new UnsupportedOperationException("Unsupported file type: " + source);

				extractZip(source, target);
				break;
			}
			case ".gz":
			{
				if (!nextExtension.isEmpty())
				{
					Path outputDir = Files.createTempDirectory("cmake");
					Path result = extractGzip(source, outputDir);
					extract(result, target);
					Files.deleteIfExists(result);
					Files.deleteIfExists(outputDir);
				}
				else
					extractGzip(source, target);
				break;
			}
			case ".tar":
			{
				if (!nextExtension.isEmpty())
					throw new UnsupportedOperationException("Unsupported file type: " + source);
				extractTar(source, target);
				break;
			}
			default:
				throw new UnsupportedOperationException("Unsupported file type: " + source);
		}
	}

	/**
	 * Extracts a zip file.
	 *
	 * @param source the source file
	 * @param target the target directory
	 * @throws IOException if an I/O error occurs
	 */
	private void extractZip(Path source, Path target) throws IOException
	{
		ZipFile zipFile = new ZipFile(source.toFile());
		ByteBuffer buffer = ByteBuffer.allocate(10 * 1024);
		try
		{
			Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
			while (entries.hasMoreElements())
			{
				ZipArchiveEntry entry = entries.nextElement();
				FileAttribute<Set<PosixFilePermission>> attribute =
																								PosixFilePermissions.
					asFileAttribute(getPosixPermissions(entry.getUnixMode()));
				if (entry.isDirectory())
				{
					Path directory = target.resolve(entry.getName());
					Files.createDirectories(directory);

					Files.setPosixFilePermissions(directory, attribute.value());
					continue;
				}
				try (ReadableByteChannel reader = Channels.newChannel(zipFile.getInputStream(entry)))
				{
					Path targetFile = target.resolve(entry.getName());

					// Omitted directories are created using the default permissions
					Files.createDirectories(targetFile.getParent());

					try (SeekableByteChannel out = Files.newByteChannel(targetFile,
							ImmutableSet.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.WRITE), attribute))
					{
						long bytesLeft = entry.getSize();
						while (bytesLeft > 0)
						{
							if (bytesLeft < buffer.limit())
								buffer.limit((int) bytesLeft);
							int count = reader.read(buffer);
							if (count == -1)
								break;
							buffer.flip();
							do
							{
								out.write(buffer);
							}
							while (buffer.hasRemaining());
							buffer.clear();
							bytesLeft -= count;
						}
					}
				}
			}
		}
		finally
		{
			zipFile.close();
		}
	}

	/**
	 * Extracts a tar file.
	 *
	 * @param source the source file
	 * @param target the target directory
	 * @throws IOException if an I/O error occurs
	 */
	private void extractTar(Path source, Path target) throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(10 * 1024);
		try (TarArchiveInputStream in = new TarArchiveInputStream(Files.newInputStream(source)))
		{
			while (true)
			{
				TarArchiveEntry entry = in.getNextTarEntry();
				if (entry == null)
					break;
				FileAttribute<Set<PosixFilePermission>> attribute =
																								PosixFilePermissions.
					asFileAttribute(getPosixPermissions(entry.getMode()));
				if (entry.isDirectory())
				{
					Path directory = target.resolve(entry.getName());
					Files.createDirectories(directory);

					Files.setPosixFilePermissions(directory, attribute.value());
					continue;
				}
				ReadableByteChannel reader = Channels.newChannel(in);
				Path targetFile = target.resolve(entry.getName());

				// Omitted directories are created using the default permissions
				Files.createDirectories(targetFile.getParent());

				try (SeekableByteChannel out = Files.newByteChannel(targetFile,
						ImmutableSet.of(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.WRITE), attribute))
				{
					long bytesLeft = entry.getSize();
					while (bytesLeft > 0)
					{
						if (bytesLeft < buffer.limit())
							buffer.limit((int) bytesLeft);
						int count = reader.read(buffer);
						if (count == -1)
							break;
						buffer.flip();
						do
						{
							out.write(buffer);
						}
						while (buffer.hasRemaining());
						buffer.clear();
						bytesLeft -= count;
					}
				}
			}
		}
	}

	/**
	 * Converts an integer mode to a set of PosixFilePermissions.
	 *
	 * @param mode the integer mode
	 * @return the PosixFilePermissions
	 * @see http://stackoverflow.com/a/9445853/14731
	 */
	private Set<PosixFilePermission> getPosixPermissions(int mode)
	{
		StringBuilder result = new StringBuilder(9);

		// Extract digits from left to right
		//
		// REFERENCE: http://stackoverflow.com/questions/203854/how-to-get-the-nth-digit-of-an-integer-with-bit-wise-operations
		for (int i = 3; i >= 1; --i)
		{
			// Octal is base-8
			mode %= Math.pow(8, i);
			int digit = (int) (mode / Math.pow(8, i - 1));
			if ((digit & 0b0000_0100) != 0)
				result.append("r");
			else
				result.append("-");
			if ((digit & 0b0000_0010) != 0)
				result.append("w");
			else
				result.append("-");
			if ((digit & 0b0000_0001) != 0)
				result.append("x");
			else
				result.append("-");
		}
		return PosixFilePermissions.fromString(result.toString());
	}

	/**
	 * Extracts a Gzip file.
	 *
	 * @param source the source file
	 * @param target the target directory
	 * @return the output file
	 * @throws IOException if an I/O error occurs
	 */
	private Path extractGzip(Path source, Path target) throws IOException
	{
		String filename = source.getFileName().toString();
		String extension = getFileExtension(filename);
		String nameWithoutExtension = filename.substring(0, filename.length() - extension.length());
		Path outPath = target.resolve(nameWithoutExtension);
		try (GzipCompressorInputStream in = new GzipCompressorInputStream(Files.newInputStream(
				source)))
		{
			try (OutputStream out = Files.newOutputStream(outPath))
			{
				final byte[] buffer = new byte[10 * 1024];
				while (true)
				{
					int count = in.read(buffer);
					if (count == -1)
						break;
					out.write(buffer, 0, count);
				}
			}
		}
		return outPath;
	}

	/**
	 * Returns a filename extension. For example, {@code getFileExtension("foo.tar.gz")} returns
	 * {@code .gz}. Unix hidden files (e.g. ".hidden") have no extension.
	 *
	 * @param filename the filename
	 * @return an empty string if no extension is found
	 * @throws NullArgumentException if filename is null
	 */
	private String getFileExtension(String filename)
	{
		Preconditions.checkNotNull(filename, "filename may not be null");

		Pattern pattern = Pattern.compile("[^\\.]+(\\.[\\p{Alnum}]+)$");
		Matcher matcher = pattern.matcher(filename);
		if (!matcher.find())
			return "";
		return matcher.group(1);
	}
}
