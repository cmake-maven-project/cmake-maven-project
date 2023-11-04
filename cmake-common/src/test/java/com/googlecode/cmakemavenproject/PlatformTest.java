package com.googlecode.cmakemavenproject;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public final class PlatformTest
{
	/**
	 * OperatingSystem.overrideEnvironmentVariables() used to throw a NullPointerException if the key did not
	 * already exist.
	 */
	@Test
	public void overrideEnvironmentVariablesWithNonExistentKey()
	{
		Platform platform = Platform.detected();
		ProcessBuilder processBuilder = new ProcessBuilder();
		Map<String, String> source = new HashMap<>();
		source.put("doesn't-exist", "value");
		platform.overrideEnvironmentVariables(source, processBuilder);
	}
}