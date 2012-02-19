package com.googlecode.cmakemavenproject;

import com.google.common.base.Preconditions;
import de.schlichtherle.truezip.fs.FsSyncOptions;
import de.schlichtherle.truezip.nio.file.TPath;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;
import org.twdata.maven.mojoexecutor.MojoExecutor.ExecutionEnvironment;

/**
 * Downloads and installs the CMake binaries into the local Maven repository.
 *
 * @goal get-binaries
 * @phase generate-resources
 *
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
		throws MojoExecutionException, MojoFailureException
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

		File file;
		String cmakeVersion = getCMakeVersion(version);
		try
		{
			String majorVersion = getMajorVersion(cmakeVersion);
			file = download(new URL("http://www.cmake.org/files/v" + majorVersion + "/cmake-"
															+ cmakeVersion + "-" + suffix));
		}
		catch (MalformedURLException e)
		{
			throw new MojoExecutionException("", e);
		}
		Plugin installPlugin = MojoExecutor.plugin("org.apache.maven.plugins",
			"maven-install-plugin", "2.3.1");
		Element fileElement = new Element("file", file.getAbsolutePath());
		Element groupIdElement = new Element("groupId", groupId);
		Element artifactIdElement = new Element("artifactId", artifactId);
		Element versionElement = new Element("version", version);
		Element classifierElement = new Element("classifier", classifier);
		Element packagingElement = new Element("packaging", "jar");
		Xpp3Dom configuration = MojoExecutor.configuration(fileElement, groupIdElement,
			artifactIdElement, versionElement, classifierElement, packagingElement);
		ExecutionEnvironment environment = MojoExecutor.executionEnvironment(project, session,
			pluginManager);
		MojoExecutor.executeMojo(installPlugin, "install-file", configuration, environment);
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
	 * Returns a filename extension.
	 *
	 * @param path the path
	 * @return an empty string if no extension is found
	 */
	private String getFileExtension(String path)
	{
		Preconditions.checkNotNull(path, "path may not be null");

		Pattern pattern = Pattern.compile(".*?[^\\.]([\\p{Alnum}]+[\\.]?)+$");
		Matcher matcher = pattern.matcher(path);
		if (!matcher.find())
			return "";
		return matcher.group(1);
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
	 * Converts a compressed file to JAR format, removing the top-level directory at the same
	 * time.
	 *
	 * @param file the file to convert
	 * @return the JAR file
	 * @throws IOException if an I/O error occurs
	 */
	private File convertToJar(File file) throws IOException
	{
		final TPath sourceFile = new TPath(file);
		String extension = getFileExtension(file.getName());
		String nameWithoutExtension = file.getName().substring(0,
			file.getName().length() - extension.length());
		File result = new File(project.getBuild().getDirectory(), nameWithoutExtension + "jar");
		Files.deleteIfExists(result.toPath());
		final TPath targetFile = new TPath(result);		
		final Path[] rootPath = new Path[1];
		
		// Locate path relative-to-which binaries are available for all platforms
		Files.walkFileTree(sourceFile, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
			{
				if (dir.getFileName().toString().equals("bin"))
				{
					rootPath[0] = dir.getParent();
					return FileVisitResult.TERMINATE;
				}
				return super.preVisitDirectory(dir, attrs);
			}			
		});
		if (rootPath[0] == null)
			throw new IOException("Could not locate bin directory: " + file);
		
		Files.walkFileTree(sourceFile, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
			{
				Files.copy(file, targetFile.resolve(rootPath[0].relativize(file)), 
					StandardCopyOption.COPY_ATTRIBUTES);
				return super.visitFile(file, attrs);
			}
		});
		targetFile.getFileSystem().sync(FsSyncOptions.UMOUNT);
		return result;
	}

	/**
	 * Downloads a file.
	 *
	 * @param url the file to download
	 * @return the downloaded File
	 * @throws MojoExecutionException if an error occurs downloading the file
	 */
	private File download(URL url) throws MojoExecutionException
	{
		Log log = getLog();
		if (log.isInfoEnabled())
			log.info("Downloading: " + url.toString());
		try
		{
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			File result;
			try
			{
				BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
				String filename = new File(url.getPath()).getName();
				result = new File(project.getBuild().getDirectory(), filename);
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(result));
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
			return convertToJar(result);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("", e);
		}
	}
}
