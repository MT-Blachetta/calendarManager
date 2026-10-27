package com.calendarmanager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full-stack test against PostgreSQL: Testcontainers when Docker is available, otherwise the external database
 * given by {@code -DCM_TEST_DB_URL}. Skipped when neither is available. Each test runs in a rolled-back
 * transaction.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@EnabledIf("com.calendarmanager.IntegrationTestSupport#databaseAvailable")
public @interface IntegrationTest {

}
