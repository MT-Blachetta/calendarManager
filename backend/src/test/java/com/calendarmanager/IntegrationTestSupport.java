package com.calendarmanager;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;

public final class IntegrationTestSupport {

	private IntegrationTestSupport() {
	}

	public static boolean databaseAvailable() {
		if (TestcontainersConfiguration.externalDbConfigured()) {
			return true;
		}
		try {
			return DockerClientFactory.instance().isDockerAvailable();
		}
		catch (RuntimeException ex) {
			return false;
		}
	}

	/** Points the datasource at the external test DB if one is configured. */
	public static void registerExternalDb(DynamicPropertyRegistry registry) {
		if (TestcontainersConfiguration.externalDbConfigured()) {
			String url = System.getProperty(TestcontainersConfiguration.EXTERNAL_DB_PROPERTY,
					System.getenv(TestcontainersConfiguration.EXTERNAL_DB_PROPERTY));
			registry.add("spring.datasource.url", () -> url);
		}
	}

}
