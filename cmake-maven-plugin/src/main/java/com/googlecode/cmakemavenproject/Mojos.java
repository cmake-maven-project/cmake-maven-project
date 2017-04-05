package com.googlecode.cmakemavenproject;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Mojo helper functions.
 * <p>
 * @author Gili Tzabari
 */
public class Mojos
{
	/**
	 * The set of valid classifiers.
	 */
	public static final Set<String> VALID_CLASSIFIERS = ImmutableSet.of("windows-x86_32",
		"windows-x86_64", "linux-x86_32", "linux-x86_64", "linux-arm_32", "mac-x86_64");

	/**
	 * Launches and waits for a process to complete.
	 * <p>
	 * @param processBuilder the process builder
	 * @return the process exit code
	 * @throws IOException          if an I/O error occurs while running the process
	 * @throws InterruptedException if the thread was interrupted
	 */
	@SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification
		= "the process output encoding corresponds to the default platform encoding")
	public static int waitFor(ProcessBuilder processBuilder)
		throws IOException, InterruptedException
	{
		Process process = processBuilder.redirectErrorStream(true).start();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
		{
			while (true)
			{
				String line = in.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
		}
		return process.waitFor();
	}

	/**
	 * Prevent construction.
	 */
	private Mojos()
	{
	}
}
