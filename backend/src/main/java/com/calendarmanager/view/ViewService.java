package com.calendarmanager.view;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.calendarmanager.category.CategoryService;
import com.calendarmanager.category.CategoryTree;
import com.calendarmanager.category.CategoryTree.Node;
import com.calendarmanager.common.BusinessRuleException;
import com.calendarmanager.common.ConflictException;
import com.calendarmanager.common.NotFoundException;
import com.calendarmanager.event.EventDto;
import com.calendarmanager.event.EventService;
import com.calendarmanager.tag.TagService;
import com.calendarmanager.view.ResolvedCalendarDto.PathElement;

@Service
@Transactional
public class ViewService {

	static final Duration MAX_SCHEDULE_RANGE = Duration.ofDays(62);

	private final CalendarViewRepository views;

	private final CategoryService categories;

	private final TagService tags;

	private final EventService events;

	ViewService(CalendarViewRepository views, CategoryService categories, TagService tags, EventService events) {
		this.views = views;
		this.categories = categories;
		this.tags = tags;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public List<ViewDto> findAll() {
		return views.findAllByOrderBySystemDescSortOrderAscNameAsc().stream().map(ViewDto::of).toList();
	}

	@Transactional(readOnly = true)
	public ViewDto findById(Long id) {
		return ViewDto.of(get(id));
	}

	public ViewDto create(ViewRequest request) {
		String name = request.name().trim();
		if (views.existsByNameIgnoreCase(name)) {
			throw new ConflictException("error.view.duplicate", name);
		}
		CalendarView view = new CalendarView(name);
		apply(view, request);
		return ViewDto.of(views.saveAndFlush(view));
	}

	public ViewDto update(Long id, ViewRequest request) {
		CalendarView view = get(id);
		view.checkVersion(request.version());
		String name = request.name().trim();
		if (views.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("error.view.duplicate", name);
		}
		view.setName(name);
		apply(view, request);
		views.flush();
		return ViewDto.of(view);
	}

	public void delete(Long id) {
		CalendarView view = get(id);
		if (view.isSystem()) {
			throw new ConflictException("error.view.systemDelete", view.getName());
		}
		views.delete(view);
	}

	/** Resolves an unsaved filter (live preview). Unknown ids are ignored. */
	@Transactional(readOnly = true)
	public List<ResolvedCalendarDto> resolve(ViewFilter filter) {
		CategoryTree tree = categories.loadTree();
		return toResolved(tree, ViewFilterEvaluator.visibleCalendars(tree, filter));
	}

	@Transactional(readOnly = true)
	public ScheduleDto schedule(Long id, OffsetDateTime from, OffsetDateTime to) {
		if (!to.isAfter(from)) {
			throw new BusinessRuleException("error.range.toAfterFrom");
		}
		if (Duration.between(from, to).compareTo(MAX_SCHEDULE_RANGE) > 0) {
			throw new BusinessRuleException("error.range.maxDays", MAX_SCHEDULE_RANGE.toDays());
		}
		CalendarView view = get(id);
		CategoryTree tree = categories.loadTree();
		List<Node> visible = ViewFilterEvaluator.visibleCalendars(tree, view.toFilter());
		List<Long> ids = visible.stream().map(Node::id).toList();
		List<EventDto> eventDtos = events.findOverlapping(ids, from.toInstant(), to.toInstant());
		return new ScheduleDto(ViewDto.of(view), from, to, toResolved(tree, visible), eventDtos);
	}

	private void apply(CalendarView view, ViewRequest request) {
		ViewFilter filter = request.toFilter();
		if (view.isSystem() && (filter.mode() != FilterMode.ALL || !filter.categoryIds().isEmpty()
				|| !filter.matchTagIds().isEmpty() || !filter.exceptTagIds().isEmpty())) {
			throw new BusinessRuleException("error.view.systemFilter");
		}
		validate(filter);
		view.setDescription(request.description() == null || request.description().isBlank() ? null
				: request.description().trim());
		view.setSortOrder(request.sortOrder() == null ? view.getSortOrder() : request.sortOrder());
		if (filter.mode() == FilterMode.ALL) {
			view.replaceFilter(FilterMode.ALL, Set.of(), Set.of(), Set.of());
			return;
		}
		tags.getAll(filter.matchTagIds());
		tags.getAll(filter.exceptTagIds());
		view.replaceFilter(filter.mode(), categories.getAll(filter.categoryIds()), filter.matchTagIds(),
				filter.exceptTagIds());
	}

	private static void validate(ViewFilter filter) {
		if (filter.mode() == FilterMode.INCLUDE) {
			if (filter.categoryIds().isEmpty() && filter.matchTagIds().isEmpty()) {
				throw new BusinessRuleException("error.view.includeEmpty");
			}
			if (!filter.exceptTagIds().isEmpty()) {
				throw new BusinessRuleException("error.view.exceptOnlyInExclude");
			}
		}
		Set<Long> overlap = new HashSet<>(filter.matchTagIds());
		overlap.retainAll(filter.exceptTagIds());
		if (!overlap.isEmpty()) {
			throw new BusinessRuleException("error.view.tagHiddenAndExcept");
		}
	}

	private static List<ResolvedCalendarDto> toResolved(CategoryTree tree, List<Node> calendars) {
		return calendars.stream()
			.map(n -> new ResolvedCalendarDto(n.id(), n.name(), n.color(),
					tree.ancestors(n.id()).stream().map(a -> new PathElement(a.id(), a.name())).toList(),
					tree.effectiveTagIds(n.id()).stream().sorted().toList()))
			.toList();
	}

	private CalendarView get(Long id) {
		return views.findById(id).orElseThrow(() -> new NotFoundException("view", id));
	}

}
