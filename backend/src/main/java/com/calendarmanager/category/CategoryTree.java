package com.calendarmanager.category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable in-memory snapshot of the whole category tree with tag inheritance.
 * <p>
 * Built from a single query; used for tree navigation, inherited/effective tags, cycle detection and the
 * depth-first ordering of calendars. Pure Java (no persistence), so it is easily unit-tested.
 */
public final class CategoryTree {

	/** One category of the snapshot. {@code ownTagIds} are the directly attached tags. */
	public record Node(long id, Long parentId, String name, CategoryKind kind, String color, int sortOrder,
			Set<Long> ownTagIds) {

		public Node {
			ownTagIds = Set.copyOf(ownTagIds);
		}

		public boolean isCalendar() {
			return kind == CategoryKind.CALENDAR;
		}

	}

	static final Comparator<Node> SIBLING_ORDER = Comparator.comparingInt(Node::sortOrder)
		.thenComparing(Node::name, String.CASE_INSENSITIVE_ORDER)
		.thenComparingLong(Node::id);

	private final Map<Long, Node> nodes;

	private final Map<Long, List<Node>> children;

	private final List<Node> roots;

	private CategoryTree(Map<Long, Node> nodes) {
		this.nodes = nodes;
		Map<Long, List<Node>> byParent = new HashMap<>();
		List<Node> rootList = new ArrayList<>();
		for (Node node : nodes.values()) {
			if (node.parentId() == null || !nodes.containsKey(node.parentId())) {
				rootList.add(node);
			}
			else {
				byParent.computeIfAbsent(node.parentId(), k -> new ArrayList<>()).add(node);
			}
		}
		byParent.values().forEach(list -> list.sort(SIBLING_ORDER));
		rootList.sort(SIBLING_ORDER);
		this.children = byParent;
		this.roots = List.copyOf(rootList);
	}

	public static CategoryTree of(Collection<Node> nodes) {
		Map<Long, Node> map = new LinkedHashMap<>();
		nodes.forEach(n -> map.put(n.id(), n));
		return new CategoryTree(map);
	}

	public static CategoryTree fromEntities(Collection<Category> categories) {
		List<Node> list = new ArrayList<>(categories.size());
		for (Category c : categories) {
			Set<Long> tagIds = new HashSet<>();
			c.getTags().forEach(t -> tagIds.add(t.getId()));
			list.add(new Node(c.getId(), c.getParentId(), c.getName(), c.getKind(), c.getColor(), c.getSortOrder(),
					tagIds));
		}
		return of(list);
	}

	public Optional<Node> find(Long id) {
		return Optional.ofNullable(id == null ? null : nodes.get(id));
	}

	public boolean contains(Long id) {
		return id != null && nodes.containsKey(id);
	}

	public int size() {
		return nodes.size();
	}

	public List<Node> roots() {
		return roots;
	}

	public List<Node> children(Long parentId) {
		if (parentId == null) {
			return roots;
		}
		return children.getOrDefault(parentId, Collections.emptyList());
	}

	/** Ancestors ordered from the root down to the direct parent (excluding the node itself). */
	public List<Node> ancestors(long id) {
		List<Node> result = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		Node current = nodes.get(id);
		while (current != null && current.parentId() != null && seen.add(current.id())) {
			current = nodes.get(current.parentId());
			if (current != null) {
				result.add(current);
			}
		}
		Collections.reverse(result);
		return result;
	}

	public int depth(long id) {
		return ancestors(id).size();
	}

	/** Ids of the node and all its descendants. */
	public Set<Long> subtreeIds(long id) {
		Set<Long> result = new LinkedHashSet<>();
		if (!nodes.containsKey(id)) {
			return result;
		}
		collectSubtree(id, result);
		return result;
	}

	private void collectSubtree(long id, Set<Long> into) {
		if (into.add(id)) {
			for (Node child : children(id)) {
				collectSubtree(child.id(), into);
			}
		}
	}

	/** True if {@code candidateId} equals {@code ancestorId} or lies below it. */
	public boolean isSelfOrDescendant(long candidateId, long ancestorId) {
		if (candidateId == ancestorId) {
			return true;
		}
		return ancestors(candidateId).stream().anyMatch(a -> a.id() == ancestorId);
	}

	/** Whether re-parenting {@code id} below {@code newParentId} would create a cycle. */
	public boolean wouldCreateCycle(long id, Long newParentId) {
		return newParentId != null && isSelfOrDescendant(newParentId, id);
	}

	/**
	 * Tags inherited from ancestors that are not attached to the node itself, mapped to the nearest ancestor
	 * carrying them.
	 */
	public Map<Long, Long> inheritedTags(long id) {
		Node node = nodes.get(id);
		Map<Long, Long> result = new LinkedHashMap<>();
		if (node == null) {
			return result;
		}
		List<Node> ancestors = ancestors(id);
		for (int i = ancestors.size() - 1; i >= 0; i--) {
			Node ancestor = ancestors.get(i);
			for (Long tagId : ancestor.ownTagIds()) {
				if (!node.ownTagIds().contains(tagId)) {
					result.putIfAbsent(tagId, ancestor.id());
				}
			}
		}
		return result;
	}

	/** Own tags plus all tags inherited from ancestors. */
	public Set<Long> effectiveTagIds(long id) {
		Node node = nodes.get(id);
		if (node == null) {
			return Set.of();
		}
		Set<Long> result = new HashSet<>(node.ownTagIds());
		ancestors(id).forEach(a -> result.addAll(a.ownTagIds()));
		return result;
	}

	/** All nodes in depth-first pre-order, siblings ordered by sort order then name. */
	public List<Node> depthFirst() {
		List<Node> result = new ArrayList<>(nodes.size());
		Set<Long> seen = new HashSet<>();
		for (Node root : roots) {
			visit(root, result, seen);
		}
		return result;
	}

	private void visit(Node node, List<Node> into, Set<Long> seen) {
		if (!seen.add(node.id())) {
			return;
		}
		into.add(node);
		for (Node child : children(node.id())) {
			visit(child, into, seen);
		}
	}

}
