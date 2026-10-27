package com.calendarmanager.view;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record ViewRequest(@NotBlank @Size(max = 100) String name, @Size(max = 500) String description,
		@NotNull FilterMode filterMode,
		@Schema(description = "Category subtrees (INCLUDE: shown, EXCLUDE: hidden)") List<Long> categoryIds,
		@Schema(description = "INCLUDE: any of these tags required; EXCLUDE: hidden") List<Long> matchTagIds,
		@Schema(description = "EXCLUDE only: never hide calendars carrying these tags") List<Long> exceptTagIds,
		Integer sortOrder,
		@Schema(description = "Version last seen by the client (optimistic locking); omit on create") Long version) {

	ViewFilter toFilter() {
		return new ViewFilter(filterMode, toSet(categoryIds), toSet(matchTagIds), toSet(exceptTagIds));
	}

	static Set<Long> toSet(List<Long> ids) {
		return ids == null ? Set.of() : Set.copyOf(ids);
	}

}
