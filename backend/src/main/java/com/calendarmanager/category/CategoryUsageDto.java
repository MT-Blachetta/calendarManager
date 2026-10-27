package com.calendarmanager.category;

import io.swagger.v3.oas.annotations.media.Schema;

/** What a (cascading) delete of a category would remove. */
public record CategoryUsageDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long categoryId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int descendantCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Events of the category and all its descendants") long eventCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Views whose filter references the category or a descendant") long viewCount) {
}
