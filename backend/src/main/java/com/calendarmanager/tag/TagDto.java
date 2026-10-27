package com.calendarmanager.tag;

import io.swagger.v3.oas.annotations.media.Schema;

public record TagDto(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "#0EA5E9") String color, String description,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Number of categories the tag is directly attached to") long usageCount,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version) {

	static TagDto of(Tag tag, long usageCount) {
		return new TagDto(tag.getId(), tag.getName(), tag.getColor(), tag.getDescription(), usageCount,
				tag.getVersion());
	}

}
