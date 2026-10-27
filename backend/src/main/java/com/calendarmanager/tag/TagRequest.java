package com.calendarmanager.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record TagRequest(@NotBlank @Size(max = 50) String name,
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "{validation.hexColor}") @Schema(
				example = "#0EA5E9") String color,
		@Size(max = 500) String description,
		@Schema(description = "Version last seen by the client (optimistic locking); omit on create") Long version) {
}
