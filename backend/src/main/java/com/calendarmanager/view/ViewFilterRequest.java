package com.calendarmanager.view;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** A view filter to resolve without saving (live preview). */
public record ViewFilterRequest(@NotNull FilterMode filterMode,
		@Schema(description = "Category subtrees (INCLUDE: shown, EXCLUDE: hidden)") List<Long> categoryIds,
		@Schema(description = "INCLUDE: any of these tags required; EXCLUDE: hidden") List<Long> matchTagIds,
		@Schema(description = "EXCLUDE only: never hide calendars carrying these tags") List<Long> exceptTagIds) {

	ViewFilter toFilter() {
		return new ViewFilter(filterMode, ViewRequest.toSet(categoryIds), ViewRequest.toSet(matchTagIds),
				ViewRequest.toSet(exceptTagIds));
	}

}
