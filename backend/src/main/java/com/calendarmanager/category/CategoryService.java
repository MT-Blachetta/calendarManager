package com.calendarmanager.category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.calendarmanager.category.CategoryDto.InheritedTagDto;
import com.calendarmanager.common.BusinessRuleException;
import com.calendarmanager.common.ConflictException;
import com.calendarmanager.common.NotFoundException;
import com.calendarmanager.event.EventRepository;
import com.calendarmanager.tag.TagService;

@Service
@Transactional
public class CategoryService {

	/** Default calendar colors, assigned round-robin when a request does not specify one. */
	static final List<String> PALETTE = List.of("#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6",
			"#EC4899", "#14B8A6", "#F97316", "#84CC16", "#06B6D4", "#A855F7");

	static final String ABSTRACT_COLOR = "#64748B";

	private final CategoryRepository categories;

	private final EventRepository events;

	private final TagService tags;

	CategoryService(CategoryRepository categories, EventRepository events, TagService tags) {
		this.categories = categories;
		this.events = events;
		this.tags = tags;
	}

	/** Snapshot of the whole tree, including own tags of every category. */
	@Transactional(readOnly = true)
	public CategoryTree loadTree() {
		return CategoryTree.fromEntities(categories.findAllWithTags());
	}

	/** All categories in depth-first order. */
	@Transactional(readOnly = true)
	public List<CategoryDto> findAll() {
		List<Category> all = categories.findAllWithTags();
		Map<Long, Category> byId = new HashMap<>();
		all.forEach(c -> byId.put(c.getId(), c));
		CategoryTree tree = CategoryTree.fromEntities(all);
		Map<Long, Long> eventCounts = eventCounts();
		return tree.depthFirst().stream().map(n -> toDto(byId.get(n.id()), tree, eventCounts)).toList();
	}

	@Transactional(readOnly = true)
	public List<CategoryTreeNodeDto> findTree() {
		CategoryTree tree = loadTree();
		return tree.roots().stream().map(n -> toTreeNode(n, tree)).toList();
	}

	@Transactional(readOnly = true)
	public CategoryDto findById(Long id) {
		CategoryTree tree = loadTree();
		if (!tree.contains(id)) {
			throw new NotFoundException("category", id);
		}
		return toDto(get(id), tree, Map.of(id, events.countByCategory_Id(id)));
	}

	@Transactional(readOnly = true)
	public CategoryUsageDto usage(Long id) {
		CategoryTree tree = loadTree();
		if (!tree.contains(id)) {
			throw new NotFoundException("category", id);
		}
		Set<Long> subtree = tree.subtreeIds(id);
		return new CategoryUsageDto(id, subtree.size() - 1, events.countByCategory_IdIn(subtree),
				categories.countViewsReferencing(subtree));
	}

	/** Returns the category if it exists and is a CALENDAR (i.e. may own events). */
	public Category requireCalendar(Long id) {
		Category category = get(id);
		if (category.getKind() != CategoryKind.CALENDAR) {
			throw new BusinessRuleException("error.category.abstractNoEvents", category.getName());
		}
		return category;
	}

	public CategoryDto create(CategoryRequest request) {
		CategoryTree tree = loadTree();
		String name = request.name().trim();
		Category parent = resolveParent(request.parentId());
		checkSiblingName(tree, request.parentId(), name, null);
		Category category = new Category(name, request.kind(), colorOrDefault(request));
		category.setParent(parent);
		category.setDescription(blankToNull(request.description()));
		category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
		category.replaceTags(tags.getAll(request.tagIds()));
		Long id = categories.saveAndFlush(category).getId();
		return findById(id);
	}

	public CategoryDto update(Long id, CategoryRequest request) {
		CategoryTree tree = loadTree();
		Category category = get(id);
		category.checkVersion(request.version());
		String name = request.name().trim();

		if (request.kind() != category.getKind()) {
			if (request.kind() == CategoryKind.CALENDAR && !tree.children(id).isEmpty()) {
				throw new BusinessRuleException("error.category.hasChildren", category.getName());
			}
			if (request.kind() == CategoryKind.ABSTRACT && events.countByCategory_Id(id) > 0) {
				throw new BusinessRuleException("error.category.hasEvents", category.getName());
			}
			category.setKind(request.kind());
		}

		Long newParentId = request.parentId();
		if (!Objects.equals(newParentId, category.getParentId())) {
			if (tree.wouldCreateCycle(id, newParentId)) {
				throw new BusinessRuleException("error.category.cycle");
			}
			category.setParent(resolveParent(newParentId));
		}
		checkSiblingName(tree, newParentId, name, id);

		category.setName(name);
		category.setDescription(blankToNull(request.description()));
		if (request.color() != null && !request.color().isBlank()) {
			category.setColor(request.color().toUpperCase());
		}
		category.setSortOrder(request.sortOrder() == null ? category.getSortOrder() : request.sortOrder());
		category.replaceTags(tags.getAll(request.tagIds()));
		categories.flush();
		return findById(id);
	}

	/**
	 * Deletes a category. Without {@code cascade} only categories without sub-categories and events can be
	 * deleted; with {@code cascade} the whole subtree and all its events are removed.
	 */
	public void delete(Long id, boolean cascade) {
		CategoryTree tree = loadTree();
		if (!tree.contains(id)) {
			throw new NotFoundException("category", id);
		}
		Set<Long> subtree = tree.subtreeIds(id);
		long eventCount = events.countByCategory_IdIn(subtree);
		if (!cascade && (subtree.size() > 1 || eventCount > 0)) {
			throw new ConflictException("error.category.notEmpty", subtree.size() - 1, eventCount);
		}
		events.deleteByCategoryIds(subtree);
		categories.deleteAllByIdIn(subtree);
	}

	/** Loads all given categories or fails with 404 naming the first missing id. */
	public Set<Category> getAll(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new HashSet<>();
		}
		Set<Long> wanted = new HashSet<>(ids);
		List<Category> found = categories.findAllById(wanted);
		if (found.size() != wanted.size()) {
			Set<Long> missing = new HashSet<>(wanted);
			found.forEach(c -> missing.remove(c.getId()));
			throw new NotFoundException("category", missing.iterator().next());
		}
		return new HashSet<>(found);
	}

	private Category get(Long id) {
		return categories.findById(id).orElseThrow(() -> new NotFoundException("category", id));
	}

	private Category resolveParent(Long parentId) {
		if (parentId == null) {
			return null;
		}
		Category parent = categories.findById(parentId)
			.orElseThrow(() -> new NotFoundException("parentCategory", parentId));
		if (parent.getKind() != CategoryKind.ABSTRACT) {
			throw new BusinessRuleException("error.category.parentIsCalendar", parent.getName());
		}
		return parent;
	}

	private static void checkSiblingName(CategoryTree tree, Long parentId, String name, Long selfId) {
		boolean taken = tree.children(parentId)
			.stream()
			.anyMatch(n -> n.name().equalsIgnoreCase(name) && !Long.valueOf(n.id()).equals(selfId));
		if (taken) {
			throw new ConflictException("error.category.duplicate", name);
		}
	}

	private String colorOrDefault(CategoryRequest request) {
		if (request.color() != null && !request.color().isBlank()) {
			return request.color().toUpperCase();
		}
		if (request.kind() == CategoryKind.ABSTRACT) {
			return ABSTRACT_COLOR;
		}
		return PALETTE.get((int) (categories.countByKind(CategoryKind.CALENDAR) % PALETTE.size()));
	}

	private Map<Long, Long> eventCounts() {
		Map<Long, Long> result = new HashMap<>();
		for (Object[] row : events.countPerCategory()) {
			result.put((Long) row[0], (Long) row[1]);
		}
		return result;
	}

	private static CategoryDto toDto(Category category, CategoryTree tree, Map<Long, Long> eventCounts) {
		long id = category.getId();
		List<InheritedTagDto> inherited = new ArrayList<>();
		tree.inheritedTags(id).forEach((tagId, source) -> inherited.add(new InheritedTagDto(tagId, source)));
		List<Long> ownTagIds = category.getTags().stream().map(t -> t.getId()).sorted().toList();
		return new CategoryDto(id, category.getParentId(), category.getName(), category.getDescription(),
				category.getKind(), category.getColor(), category.getSortOrder(), tree.depth(id), ownTagIds, inherited,
				tree.children(id).size(), eventCounts.getOrDefault(id, 0L), category.getVersion());
	}

	private static CategoryTreeNodeDto toTreeNode(CategoryTree.Node node, CategoryTree tree) {
		return new CategoryTreeNodeDto(node.id(), node.name(), node.kind(), node.color(),
				node.ownTagIds().stream().sorted().toList(),
				tree.effectiveTagIds(node.id()).stream().sorted().toList(),
				tree.children(node.id()).stream().map(c -> toTreeNode(c, tree)).toList());
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
