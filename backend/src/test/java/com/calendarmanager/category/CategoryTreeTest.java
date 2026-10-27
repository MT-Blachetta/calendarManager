package com.calendarmanager.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.calendarmanager.category.CategoryTree.Node;

class CategoryTreeTest {

	// Entertainment(1)#10 ─ TV(2)#20 ─ ARD(3)#30, Sport1(4)#40
	// Health(5)#50 ─ Dentist(6)
	// Club(7)#40 (root calendar)
	static CategoryTree sample() {
		return CategoryTree.of(List.of(
				new Node(1, null, "Entertainment", CategoryKind.ABSTRACT, "#000000", 0, Set.of(10L)),
				new Node(2, 1L, "TV", CategoryKind.ABSTRACT, "#000000", 0, Set.of(20L)),
				new Node(4, 2L, "Sport1", CategoryKind.CALENDAR, "#000000", 2, Set.of(40L)),
				new Node(3, 2L, "ARD", CategoryKind.CALENDAR, "#000000", 1, Set.of(30L, 20L)),
				new Node(5, null, "Health", CategoryKind.ABSTRACT, "#000000", 1, Set.of(50L)),
				new Node(6, 5L, "Dentist", CategoryKind.CALENDAR, "#000000", 0, Set.of()),
				new Node(7, null, "Club", CategoryKind.CALENDAR, "#000000", 2, Set.of(40L))));
	}

	@Test
	void ancestorsAreOrderedFromRootToParent() {
		assertThat(sample().ancestors(3)).extracting(Node::id).containsExactly(1L, 2L);
		assertThat(sample().ancestors(1)).isEmpty();
		assertThat(sample().depth(4)).isEqualTo(2);
	}

	@Test
	void effectiveTagsIncludeAllAncestorTags() {
		CategoryTree tree = sample();
		assertThat(tree.effectiveTagIds(4)).containsExactlyInAnyOrder(10L, 20L, 40L);
		assertThat(tree.effectiveTagIds(6)).containsExactly(50L);
		assertThat(tree.effectiveTagIds(7)).containsExactly(40L);
	}

	@Test
	void inheritedTagsExcludeOwnTagsAndNameNearestSource() {
		CategoryTree tree = sample();
		// ARD has tag 20 itself, so only 10 (from Entertainment) is inherited
		assertThat(tree.inheritedTags(3)).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(10L, 1L));
		assertThat(tree.inheritedTags(4)).containsEntry(20L, 2L).containsEntry(10L, 1L).hasSize(2);
	}

	@Test
	void depthFirstOrdersSiblingsBySortOrderThenName() {
		assertThat(sample().depthFirst()).extracting(Node::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
	}

	@Test
	void subtreeContainsSelfAndDescendants() {
		assertThat(sample().subtreeIds(1)).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
		assertThat(sample().subtreeIds(7)).containsExactly(7L);
		assertThat(sample().subtreeIds(99)).isEmpty();
	}

	@Test
	void detectsCycles() {
		CategoryTree tree = sample();
		assertThat(tree.wouldCreateCycle(1, 2L)).isTrue();
		assertThat(tree.wouldCreateCycle(1, 1L)).isTrue();
		assertThat(tree.wouldCreateCycle(2, 5L)).isFalse();
		assertThat(tree.wouldCreateCycle(2, null)).isFalse();
	}

}
