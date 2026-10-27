package com.calendarmanager.view;

import java.time.OffsetDateTime;
import java.util.List;

import com.calendarmanager.event.EventDto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Everything the day grid needs: the visible calendars (in column order) and their events in the range. */
public record ScheduleDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) ViewDto view,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime from,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime to,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ResolvedCalendarDto> calendars,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<EventDto> events) {
}
