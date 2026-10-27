package com.calendarmanager.category;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryRequest(@NotBlank @Size(max = 100) String name, @Size(max = 1000) String description,
		@NotNull CategoryKind kind,
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.hexColor}") @Schema(
				example = "#4F46E5") String color,
		@Schema(description = "Id of an ABSTRACT parent category; null for a root category") Long parentId,
		Integer sortOrder, @Schema(description = "Ids of the directly attached tags") List<Long> tagIds,
		@Schema(description = "Version last seen by the client (optimistic locking); omit on create") Long version) {
}
