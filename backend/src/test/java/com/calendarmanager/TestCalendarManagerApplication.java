package com.calendarmanager;

import org.springframework.boot.SpringApplication;

/** Runs the app locally against a Testcontainers PostgreSQL (no docker compose needed). */
public class TestCalendarManagerApplication {

	public static void main(String[] args) {
		SpringApplication.from(CalendarManagerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
