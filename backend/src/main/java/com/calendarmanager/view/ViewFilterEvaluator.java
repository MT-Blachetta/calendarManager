package com.calendarmanager.view;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.calendarmanager.category.CategoryTree;
import com.calendarmanager.category.CategoryTree.Node;

/**
 * Decides which CALENDAR categories a view shows.
 * <ul>
 * <li><b>ALL</b>: every calendar.</li>
 * <li><b>INCLUDE</b>: shown if (no categories selected OR within a selected subtree) AND (no tags selected OR
 * carries any selected tag). With nothing selected nothing is shown.</li>
 * <li><b>EXCLUDE</b>: hidden if (within an excluded subtree OR carries an excluded tag) AND NOT carries an
 * EXCEPT tag.</li>
 * </ul>
 * "Carries" refers to effective tags, i.e. own tags plus tags inherited from all ancestors. The result is in
 * depth-first tree order.
 */
public final class ViewFilterEvaluator {

	private ViewFilterEvaluator() {
	}

	public static List<Node> visibleCalendars(CategoryTree tree, ViewFilter filter) {
		return tree.depthFirst().stream().filter(Node::isCalendar).filter(n -> isVisible(tree, n, filter)).toList();
	}

	static boolean isVisible(CategoryTree tree, Node calendar, ViewFilter filter) {
		return switch (filter.mode()) {
			case ALL -> true;
			case INCLUDE -> {
				if (filter.categoryIds().isEmpty() && filter.matchTagIds().isEmpty()) {
					yield false;
				}
				boolean categoryOk = filter.categoryIds().isEmpty() || inSelectedSubtree(tree, calendar, filter);
				boolean tagOk = filter.matchTagIds().isEmpty()
						|| intersects(tree.effectiveTagIds(calendar.id()), filter.matchTagIds());
				yield categoryOk && tagOk;
			}
			case EXCLUDE -> {
				Set<Long> tags = tree.effectiveTagIds(calendar.id());
				boolean excluded = inSelectedSubtree(tree, calendar, filter) || intersects(tags, filter.matchTagIds());
				boolean excepted = intersects(tags, filter.exceptTagIds());
				yield !excluded || excepted;
			}
		};
	}

	private static boolean inSelectedSubtree(CategoryTree tree, Node calendar, ViewFilter filter) {
		if (filter.categoryIds().contains(calendar.id())) {
			return true;
		}
		return tree.ancestors(calendar.id()).stream().anyMatch(a -> filter.categoryIds().contains(a.id()));
	}

	private static boolean intersects(Set<Long> a, Set<Long> b) {
		return !a.isEmpty() && !b.isEmpty() && !Collections.disjoint(a, b);
	}

}
