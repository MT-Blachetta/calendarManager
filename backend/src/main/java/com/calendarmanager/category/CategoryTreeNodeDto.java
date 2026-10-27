package com.calendarmanager.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Nested representation of the category hierarchy. */
public record CategoryTreeNodeDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CategoryKind kind,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String color,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> tagIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> effectiveTagIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CategoryTreeNodeDto> children) {
}
