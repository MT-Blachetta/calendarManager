package com.calendarmanager.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/** Stable JSON representation of a page of results. */
public record PageDto<T>(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> content,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Zero-based page index") int page,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages) {

	public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> mapper) {
		return new PageDto<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
