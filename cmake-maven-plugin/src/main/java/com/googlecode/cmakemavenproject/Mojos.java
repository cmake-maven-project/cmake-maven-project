package com.googlecode.cmakemavenproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Mojo helper functions.
 * <p>
 * @author Gili Tzabari
 */
public class Mojos
{
	/**
	 * Launches and waits for a process to complete.
	 * <p>
	 * @param processBuilder the process builder
	 * @return the process exit code
	 * @throws IOException          if an I/O error occurs while running the process
	 * @throws InterruptedException if the thread was interrupted
	 */
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
