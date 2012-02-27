package com.googlecode.cmakemavenproject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Enumeration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

/**
 * Downloads and installs the CMake binaries into the local Maven repository.
 *
 * @goal get-binaries
 * @phase generate-resources
 * @author Gili Tzabari
 */
public class GetBinariesMojo
	extends AbstractMojo
{
	/**
	 * The release platform.
	 *
	 * @parameter expression="${classifier}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String classifier;
	/**
	 * The project version.
	 *
	 * @parameter expression="${project.version}"
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private String version;
	/**
	 * The maven plugin manager.
	 *
	 * @component
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private BuildPluginManager pluginManager;
	/**
	 * The local maven repository.
	 *
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private ArtifactRepository localRepository;
	/**
	 * @component
	 */
	private RepositorySystem repositorySystem;
	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private MavenProject project;
	/**
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private MavenSession session;

	@Override
	@SuppressWarnings("NP_UNWRITTEN_FIELD")
	public void execute()
		throws MojoExecutionException
	{
		final String groupId = "com.googlecode.cmake-maven-project";
		final String artifactId = "cmake-binaries";
		String suffix;
		switch (classifier)
		{
			case "windows":
			{
				suffix = "win32-x86.zip";
				break;
			}
			case "linux":
			{
				suffix = "Linux-i386.tar.gz";
				break;
			}
			case "mac":
			{
				suffix = "Darwin64-universal.tar.gz";
				break;
			}
			default:
				throw new MojoExecutionException("Unsupported classifier: " + classifier);
		}

		Artifact artifact = getArtifact(groupId, artifactId, version, classifier);
		if (artifact != null)
			return;

		String cmakeVersion = getCMakeVersion(version);
		final Path target = Paths.get(project.getBuild().getDirectory(), "dependency/cmake");
		try
		{
			deleteRecursively(target);
			String majorVersion = getMajorVersion(cmakeVersion);
			Path result = download(new URL("http://www.cmake.org/files/v" + majorVersion + "/cmake-"
																		 + cmakeVersion + "-" + suffix));
			extract(result, target);
			normalizeDirectories(target);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}

	/**
	 * Returns the cmake version associated with the project.
	 *
	 * @param version the project version
	 * @return the cmake version
	 * @throws NullPointerException if version is null
	 * @throws IllegalArgumentException if version is empty or has an unexpected format
	 */
	private String getCMakeVersion(String version)
	{
		Preconditions.checkNotNull(version, "version may not be null");
		Preconditions.checkArgument(!version.isEmpty(), "version may not be empty");

		Pattern pattern = Pattern.compile("^(.*?)-.*");
		Matcher matcher = pattern.matcher(version);
		if (!matcher.find())
			throw new IllegalArgumentException("Unexpected version format: " + version);
		return matcher.group(1);
	}

	/**
	 * Returns the major version number of a version.
	 *
	 * @param version the full version number
	 * @return the major version number
	 * @throws NullPointerException if version is null
	 * @throws IllegalArgumentException if version is empty or has an unexpected format
	 */
	private String getMajorVersion(String version)
	{
		Preconditions.checkNotNull(version, "version may not be null");
		Preconditions.checkArgument(!version.isEmpty(), "version may not be empty");

		Pattern pattern = Pattern.compile("^[\\d]*\\.[\\d]*");
		Matcher matcher = pattern.matcher(version);
		if (!matcher.find())
			throw new IllegalArgumentException("Unexpected version format: " + version);
		return matcher.group();
	}

	/**
	 * Returns a local artifact.
	 *
	 * @param groupId the artifact group id
	 * @param artifactId the artifact id
	 * @param version the artifact version
	 * @param classifier the artifact classifier, empty string if there is none
	 * @return null if the artifact is not installed
	 * @throws MojoExecutionException if an error occurs while resolving the artifact
	 */
	private Artifact getArtifact(String groupId, String artifactId, String version,
															 String classifier)
		throws MojoExecutionException
	{
		Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version,
			"jar", classifier);
		artifact.setFile(new File(localRepository.getBasedir(), localRepository.pathOf(artifact)));
		if (!artifact.getFile().exists())
			return null;

		Log log = getLog();
		if (log.isDebugEnabled())
			log.debug("Artifact already installed: " + artifact.getFile().getAbsolutePath());
		return artifact;
	}

	/**
	 * Downloads a file.
	 *
	 * @param url the file to download
	 * @return the path of the downloaded file
	 * @throws MojoExecutionException if an error occurs downloading the file
	 */
	private Path download(URL url) throws MojoExecutionException
	{
		String filename = new File(url.getPath()).getName();
		Path result = Paths.get(project.getBuild().getDirectory(), filename);
		try
		{
			if (Files.notExists(result))
			{
				Log log = getLog();
				if (log.isInfoEnabled())
					log.info("Downloading: " + url.toString());
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				try
				{
					BufferedInputStream in = new BufferedInputStream(connection.getInputStream());

					Files.createDirectories(Paths.get(project.getBuild().getDirectory()));

					BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(result));
					byte[] buffer = new byte[10 * 1024];
					try
					{
						while (true)
						{
							int count = in.read(buffer);
							if (count == -1)
								break;
							out.write(buffer, 0, count);
						}
					}
					finally
					{
						in.close();
						out.close();
					}
				}
				finally
				{
					connection.disconnect();
				}
			}
			return result;
		}
		catch (IOException e)
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
		try
		{
			final byte[] buffer = new byte[10 * 1024];
			Enumeration<ZipArchiveEntry> entries = zipFile.getEntriesInPhysicalOrder();
			while (entries.hasMoreElements())
			{
				ZipArchiveEntry entry = entries.nextElement();
				try (InputStream in = zipFile.getInputStream(entry))
				{
					try (OutputStream out = Files.newOutputStream(target.resolve(entry.getName())))
					{
						while (true)
						{
							int count = in.read(buffer);
							if (count == -1)
								break;
							out.write(buffer, 0, count);
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
																								PosixFilePermissions.asFileAttribute(getPosixPermissions(entry.
					getMode()));
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

	/**
	 * Normalize the directory structure across all platforms.
	 *
	 * @param source the binary path
	 * @throws IOException if an I/O error occurs
	 */
	private void normalizeDirectories(final Path source) throws IOException
	{
		final Path[] topDirectory = new Path[1];
		Files.walkFileTree(source, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws
				IOException
			{
				if (dir.getFileName().toString().equals("bin"))
				{
					topDirectory[0] = dir.getParent().toAbsolutePath();
					return FileVisitResult.TERMINATE;
				}
				return FileVisitResult.CONTINUE;
			}
		});
		if (topDirectory[0] == null)
			throw new IOException("Could not find \"bin\" in: " + source);

		Files.walkFileTree(source, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				if (topDirectory[0].startsWith(file.getParent()))
				{
					// Skip paths outside topDirectory
					return FileVisitResult.CONTINUE;
				}
				Files.move(file, source.resolve(topDirectory[0].relativize(file)),
					StandardCopyOption.ATOMIC_MOVE);
				return super.visitFile(file, attrs);
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws
				IOException
			{
				if (topDirectory[0].startsWith(dir))
				{
					// Skip paths outside topDirectory
					return FileVisitResult.CONTINUE;
				}
				Files.move(dir, source.resolve(topDirectory[0].relativize(dir)),
					StandardCopyOption.ATOMIC_MOVE);
				return FileVisitResult.SKIP_SUBTREE;
			}
		});
		deleteRecursively(topDirectory[0]);
	}

	/**
	 * Deletes a path recursively.
	 *
	 * @param path the path to delete
	 * @throws IOException if an I/O error occurs
	 */
	private void deleteRecursively(Path path) throws IOException
	{
		// This method is vulnerable to race-conditions but it's the best we can do.
		//
		// BUG: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7148952
		if (Files.notExists(path))
			return;
		Files.walkFileTree(path, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Files.deleteIfExists(file);
				return super.visitFile(file, attrs);
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException t) throws IOException
			{
				if (t == null)
					Files.deleteIfExists(dir);
				return super.postVisitDirectory(dir, t);
			}
		});
	}
}
