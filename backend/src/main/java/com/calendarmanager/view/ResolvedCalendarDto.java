package com.calendarmanager.view;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** A calendar visible in a view, with its ancestor path for the hierarchical column header. */
public record ResolvedCalendarDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String color,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Ancestors from the root down to the direct parent") List<PathElement> path,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Own plus inherited tags") List<Long> effectiveTagIds) {

	public record PathElement(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {
	}

}
