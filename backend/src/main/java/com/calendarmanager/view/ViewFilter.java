package com.calendarmanager.view;

import java.util.Set;

/**
 * The filter part of a calendar view.
 *
 * @param mode ALL, INCLUDE or EXCLUDE
 * @param categoryIds selected category subtrees (INCLUDE: shown, EXCLUDE: hidden)
 * @param matchTagIds selected tags (INCLUDE: required (any), EXCLUDE: hidden)
 * @param exceptTagIds EXCLUDE only: calendars carrying any of these tags are never hidden
 */
public record ViewFilter(FilterMode mode, Set<Long> categoryIds, Set<Long> matchTagIds, Set<Long> exceptTagIds) {

	public ViewFilter {
		mode = mode == null ? FilterMode.ALL : mode;
		categoryIds = categoryIds == null ? Set.of() : Set.copyOf(categoryIds);
		matchTagIds = matchTagIds == null ? Set.of() : Set.copyOf(matchTagIds);
		exceptTagIds = exceptTagIds == null ? Set.of() : Set.copyOf(exceptTagIds);
	}

	public static ViewFilter all() {
		return new ViewFilter(FilterMode.ALL, Set.of(), Set.of(), Set.of());
	}

}
