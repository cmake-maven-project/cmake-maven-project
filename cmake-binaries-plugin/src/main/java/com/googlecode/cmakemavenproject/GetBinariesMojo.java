package com.googlecode.cmakemavenproject;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

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
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	@SuppressWarnings("UWF_UNWRITTEN_FIELD")
	private MavenProject project;
	/**
	 * To look up Archiver/UnArchiver implementations.
	 *
	 * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
	 * @required
	 */
	@Component
	private ArchiverManager archiverManager;

	@Override
	@SuppressWarnings("NP_UNWRITTEN_FIELD")
	public void execute()
		throws MojoExecutionException
	{
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

		String cmakeVersion = getCMakeVersion(version);
		final Path target = Paths.get(project.getBuild().getDirectory(), "dependency/cmake");
		try
		{
			String majorVersion = getMajorVersion(cmakeVersion);
			Path result = download(new URL("http://www.cmake.org/files/v" + majorVersion + "/cmake-"
																		 + cmakeVersion + "-" + suffix));
			if (Files.notExists(target.resolve("bin")))
			{
				Files.createDirectories(target);
				// Directories not normalized, begin by unpacking the binaries
				try
				{
					// Based on AbstractDependencyMojo.java in maven-dependency-plugin revision 1403449
					UnArchiver unArchiver;
					try
					{
						unArchiver = archiverManager.getUnArchiver(result.toFile());
						getLog().debug("Found unArchiver by type: " + unArchiver);
					}
					catch (NoSuchArchiverException e)
					{
						getLog().debug("Unknown archiver type", e);
						return;
					}

					unArchiver.setUseJvmChmod(true);
					unArchiver.setSourceFile(result.toFile());
					unArchiver.setDestDirectory(target.toFile());
					unArchiver.extract();
				}
				catch (ArchiverException e)
				{
					throw new MojoExecutionException("Error unpacking file: " + result + " to: " + target
																					 + "\r\n" + e.toString(), e);
				}
				normalizeDirectories(target);
			}
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
