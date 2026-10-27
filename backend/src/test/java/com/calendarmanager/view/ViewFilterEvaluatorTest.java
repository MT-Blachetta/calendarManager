package com.calendarmanager.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.calendarmanager.category.CategoryKind;
import com.calendarmanager.category.CategoryTree;
import com.calendarmanager.category.CategoryTree.Node;

class ViewFilterEvaluatorTest {

	static final long LEISURE = 10, TV = 20, PUBLIC = 30, SPORTS = 40, MEDICAL = 50;

	// Entertainment(1)#Leisure ─ TV(2)#TV ─ ARD(3)#Public, ZDF(4)#Public, Sport1(5)#Sports
	// Health(6)#Medical ─ Dentist(7)
	// Club(8)#Sports#Leisure
	final CategoryTree tree = CategoryTree.of(List.of(
			new Node(1, null, "Entertainment", CategoryKind.ABSTRACT, "#000000", 0, Set.of(LEISURE)),
			new Node(2, 1L, "TV", CategoryKind.ABSTRACT, "#000000", 0, Set.of(TV)),
			new Node(3, 2L, "ARD", CategoryKind.CALENDAR, "#000000", 0, Set.of(PUBLIC)),
			new Node(4, 2L, "ZDF", CategoryKind.CALENDAR, "#000000", 1, Set.of(PUBLIC)),
			new Node(5, 2L, "Sport1", CategoryKind.CALENDAR, "#000000", 2, Set.of(SPORTS)),
			new Node(6, null, "Health", CategoryKind.ABSTRACT, "#000000", 1, Set.of(MEDICAL)),
			new Node(7, 6L, "Dentist", CategoryKind.CALENDAR, "#000000", 0, Set.of()),
			new Node(8, null, "Club", CategoryKind.CALENDAR, "#000000", 2, Set.of(SPORTS, LEISURE))));

	List<Long> visible(FilterMode mode, Set<Long> categories, Set<Long> match, Set<Long> except) {
		return ViewFilterEvaluator.visibleCalendars(tree, new ViewFilter(mode, categories, match, except))
			.stream()
			.map(Node::id)
			.toList();
	}

	@Test
	void allShowsEveryCalendarInTreeOrder() {
		assertThat(visible(FilterMode.ALL, Set.of(), Set.of(), Set.of())).containsExactly(3L, 4L, 5L, 7L, 8L);
	}

	@Test
	void includeByCategoryCoversWholeSubtree() {
		assertThat(visible(FilterMode.INCLUDE, Set.of(1L), Set.of(), Set.of())).containsExactly(3L, 4L, 5L);
		assertThat(visible(FilterMode.INCLUDE, Set.of(7L, 8L), Set.of(), Set.of())).containsExactly(7L, 8L);
	}

	@Test
	void includeByTagUsesInheritedTags() {
		assertThat(visible(FilterMode.INCLUDE, Set.of(), Set.of(LEISURE), Set.of())).containsExactly(3L, 4L, 5L, 8L);
		assertThat(visible(FilterMode.INCLUDE, Set.of(), Set.of(MEDICAL), Set.of())).containsExactly(7L);
	}

	@Test
	void includeCombinesCategoriesAndTagsWithAnd() {
		assertThat(visible(FilterMode.INCLUDE, Set.of(2L), Set.of(SPORTS), Set.of())).containsExactly(5L);
	}

	@Test
	void includeWithoutSelectionShowsNothing() {
		assertThat(visible(FilterMode.INCLUDE, Set.of(), Set.of(), Set.of())).isEmpty();
	}

	@Test
	void excludeHidesSubtreesAndTags() {
		assertThat(visible(FilterMode.EXCLUDE, Set.of(2L), Set.of(), Set.of())).containsExactly(7L, 8L);
		assertThat(visible(FilterMode.EXCLUDE, Set.of(), Set.of(SPORTS), Set.of())).containsExactly(3L, 4L, 7L);
		assertThat(visible(FilterMode.EXCLUDE, Set.of(6L), Set.of(SPORTS), Set.of())).containsExactly(3L, 4L);
	}

	@Test
	void excludeKeepsCalendarsWithExceptTags() {
		assertThat(visible(FilterMode.EXCLUDE, Set.of(2L), Set.of(), Set.of(PUBLIC))).containsExactly(3L, 4L, 7L,
				8L);
		// Inherited except tag rescues the whole TV subtree
		assertThat(visible(FilterMode.EXCLUDE, Set.of(1L), Set.of(SPORTS), Set.of(TV))).containsExactly(3L, 4L, 5L,
				7L);
	}

	@Test
	void excludeWithoutSelectionShowsEverything() {
		assertThat(visible(FilterMode.EXCLUDE, Set.of(), Set.of(), Set.of())).containsExactly(3L, 4L, 5L, 7L, 8L);
	}

}
