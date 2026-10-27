package com.calendarmanager.event;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Which days of a date range have events in the calendars of a category (month view highlighting).
 */
public record EventDaySummaryDto(
		@Schema(description = "Selected calendar or group; null means all calendars") Long categoryId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Europe/Berlin") String zone,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate from,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate to,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Calendars covered by the selection, in tree order") List<CalendarRef> calendars,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Only days with at least one event, ascending") List<Day> days) {

	public record CalendarRef(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String color) {
	}

	public record Day(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate date,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
					description = "Events touching this day (multi-day events count on every day)") int eventCount,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
					description = "Calendars with events on this day, in tree order") List<Long> calendarIds) {
	}

}
