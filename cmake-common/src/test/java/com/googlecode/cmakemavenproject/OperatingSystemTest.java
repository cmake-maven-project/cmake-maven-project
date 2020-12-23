package com.googlecode.cmakemavenproject;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public final class OperatingSystemTest
{
	/**
	 * OperatingSystem.overrideEnvironmentVariables() used to throws a NullPointerException if the key did not
	 * already exist.
	 */
	@Test
	public void overrideEnvironmentVariablesWithNonExistentKey()
	{
		OperatingSystem os = OperatingSystem.detected();
		ProcessBuilder processBuilder = new ProcessBuilder();
		Map<String, String> source = new HashMap<>();
		source.put("doesn't-exist", "value");
		os.overrideEnvironmentVariables(source, processBuilder);
	}
}