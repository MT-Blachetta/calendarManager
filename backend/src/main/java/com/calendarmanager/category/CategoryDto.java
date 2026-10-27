package com.calendarmanager.category;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(description = "Parent (always an ABSTRACT category); null for root categories") Long parentId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name, String description,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) CategoryKind kind,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "#4F46E5") String color,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int sortOrder,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "0 for root categories") int depth,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Ids of the directly attached tags") List<Long> tagIds,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Tags inherited from ancestors (excluding own tags)") List<InheritedTagDto> inheritedTags,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int childCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long eventCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version) {

	public record InheritedTagDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long tagId,
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
					description = "Nearest ancestor the tag is attached to") Long sourceCategoryId) {
	}

}
