package com.calendarmanager.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DaysCoveredTest {

	static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

	static Instant at(String local) {
		return java.time.LocalDateTime.parse(local).atZone(BERLIN).toInstant();
	}

	@Test
	void singleDayEvent() {
		assertThat(EventService.daysCovered(at("2026-10-18T20:15"), at("2026-10-18T21:45"), BERLIN))
			.containsExactly(LocalDate.of(2026, 10, 18));
	}

	@Test
	void eventCrossingMidnightCoversBothDays() {
		assertThat(EventService.daysCovered(at("2026-10-18T23:00"), at("2026-10-19T01:00"), BERLIN))
			.containsExactly(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19));
	}

	@Test
	void eventEndingAtMidnightDoesNotTouchNextDay() {
		assertThat(EventService.daysCovered(at("2026-10-18T22:00"), at("2026-10-19T00:00"), BERLIN))
			.containsExactly(LocalDate.of(2026, 10, 18));
	}

	@Test
	void multiDayEventAcrossDstChange() {
		// DST ends in Germany on 2026-10-25
		assertThat(EventService.daysCovered(at("2026-10-24T10:00"), at("2026-10-26T10:00"), BERLIN))
			.containsExactly(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26));
	}

	@Test
	void dayDependsOnTimeZone() {
		Instant lateEvening = at("2026-10-18T23:30");
		assertThat(EventService.daysCovered(lateEvening, lateEvening.plusSeconds(600), ZoneId.of("UTC")))
			.containsExactly(LocalDate.of(2026, 10, 18));
		assertThat(EventService.daysCovered(lateEvening, lateEvening.plusSeconds(600), ZoneId.of("Asia/Tokyo")))
			.containsExactly(LocalDate.of(2026, 10, 19));
	}

}
