package com.calendarmanager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts a throw-away PostgreSQL (same major version as docker-compose.yml) for integration tests, unless an
 * external test database is given via {@code -DCM_TEST_DB_URL=jdbc:postgresql://...}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	public static final String EXTERNAL_DB_PROPERTY = "CM_TEST_DB_URL";

	@Bean
	@ServiceConnection
	@Conditional(NoExternalDb.class)
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	public static boolean externalDbConfigured() {
		String url = System.getProperty(EXTERNAL_DB_PROPERTY, System.getenv(EXTERNAL_DB_PROPERTY));
		return url != null && !url.isBlank();
	}

	static class NoExternalDb implements org.springframework.context.annotation.Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return !externalDbConfigured();
		}

	}

}
