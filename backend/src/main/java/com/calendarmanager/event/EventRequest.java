package com.calendarmanager.event;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventRequest(@NotNull @Schema(description = "Id of a CALENDAR category") Long categoryId,
		@NotBlank @Size(max = 200) String title, @Size(max = 200) String place,
		@Size(max = 1000) String description,
		@NotNull @Schema(example = "2026-10-18T20:15:00+02:00") OffsetDateTime start,
		@NotNull @Schema(example = "2026-10-18T21:45:00+02:00") OffsetDateTime end,
		@Schema(description = "Version last seen by the client (optimistic locking); omit on create") Long version) {
}
