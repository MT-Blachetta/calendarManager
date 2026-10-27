package com.calendarmanager.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long categoryId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title, String place, String description,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime start,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime end,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version) {

	public static EventDto of(Event e) {
		return new EventDto(e.getId(), e.getCategoryId(), e.getTitle(), e.getPlace(), e.getDescription(),
				e.getStartTime().atOffset(ZoneOffset.UTC), e.getEndTime().atOffset(ZoneOffset.UTC), e.getVersion());
	}

}
