package com.googlecode.cmakemavenproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

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
	 * Overrides environment variables.
	 *
	 * @param source new environment variables
	 * @param target existing environment variables
	 */
	public static void overrideEnvironmentVariables(Map<String, String> source,
	                                                Map<String, String> target)
	{
		assert (source != null);
		assert (target != null);
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
			target.put(entry.getKey(), value);
		}
	}

	/**
	 * Prevent construction.
	 */
	private Mojos()
	{
	}
}
