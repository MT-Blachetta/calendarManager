package com.calendarmanager.view;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ViewDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String description,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) FilterMode filterMode,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "System views cannot be deleted and always show all calendars") boolean system,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> categoryIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> matchTagIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Long> exceptTagIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version) {

	static ViewDto of(CalendarView view) {
		ViewFilter filter = view.toFilter();
		return new ViewDto(view.getId(), view.getName(), view.getDescription(), view.getFilterMode(), view.isSystem(),
				view.getSortOrder(), filter.categoryIds().stream().sorted().toList(),
				filter.matchTagIds().stream().sorted().toList(), filter.exceptTagIds().stream().sorted().toList(),
				view.getVersion());
	}

}
