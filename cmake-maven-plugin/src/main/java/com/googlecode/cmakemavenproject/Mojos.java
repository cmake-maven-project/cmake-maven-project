package com.googlecode.cmakemavenproject;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Mojo helper functions.
 *
 * @author Gili Tzabari
 */
public class Mojos
{
	/**
	 * Launches and waits for a process to complete.
	 *
	 * @param processBuilder the process builder
	 * @param log            the Maven log
	 * @return the process exit code
	 * @throws IOException          if an I/O error occurs while running the process
	 * @throws InterruptedException if the thread was interrupted
	 */
	public static int waitFor(ProcessBuilder processBuilder, Log log)
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
		int result = process.waitFor();
		if (result != 0)
		{
			log.warn("Command: " + processBuilder.command());
			log.warn("Directory: " + processBuilder.directory());
			log.warn("Environment: " + processBuilder.environment());
			log.warn("Exit code: " + result);
		}
		return result;
	}

	/**
	 * Prevent construction.
	 */
	private Mojos()
	{
	}
}
