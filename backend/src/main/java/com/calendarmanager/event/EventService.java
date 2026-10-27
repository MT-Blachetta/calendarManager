package com.calendarmanager.event;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.calendarmanager.category.Category;
import com.calendarmanager.category.CategoryService;
import com.calendarmanager.category.CategoryTree;
import com.calendarmanager.category.CategoryTree.Node;
import com.calendarmanager.common.BusinessRuleException;
import com.calendarmanager.common.NotFoundException;
import com.calendarmanager.common.PageDto;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class EventService {

	private final EventRepository events;

	private final CategoryService categories;

	EventService(EventRepository events, CategoryService categories) {
		this.events = events;
		this.categories = categories;
	}

	@Transactional(readOnly = true)
	public PageDto<EventDto> search(OffsetDateTime from, OffsetDateTime to, List<Long> categoryIds, String query,
			Pageable pageable) {
		Specification<Event> spec = (root, cq, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (from != null) {
				predicates.add(cb.greaterThan(root.get("endTime"), from.toInstant()));
			}
			if (to != null) {
				predicates.add(cb.lessThan(root.get("startTime"), to.toInstant()));
			}
			if (categoryIds != null && !categoryIds.isEmpty()) {
				predicates.add(root.get("category").get("id").in(categoryIds));
			}
			if (query != null && !query.isBlank()) {
				String like = "%" + query.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("title")), like, '\\'),
						cb.like(cb.lower(cb.coalesce(root.get("place"), "")), like, '\\'),
						cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like, '\\')));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		return PageDto.of(events.findAll(spec, pageable), EventDto::of);
	}

	@Transactional(readOnly = true)
	public EventDto findById(Long id) {
		return EventDto.of(get(id));
	}

	/** Events of the given calendars overlapping [from, to), ordered by start. */
	@Transactional(readOnly = true)
	public List<EventDto> findOverlapping(Collection<Long> calendarIds, Instant from, Instant to) {
		if (calendarIds.isEmpty()) {
			return List.of();
		}
		return events.findOverlapping(calendarIds, from, to).stream().map(EventDto::of).toList();
	}

	/** Maximum length of a day-summary range (a month grid shows at most 42 days). */
	static final int MAX_SUMMARY_DAYS = 62;

	/**
	 * Days within [from, to] (inclusive, local dates in {@code zone}) on which the calendars of the category
	 * (a calendar itself or all calendars below a group; all calendars if null) have events.
	 */
	@Transactional(readOnly = true)
	public EventDaySummaryDto daySummary(Long categoryId, LocalDate from, LocalDate to, String zoneId) {
		ZoneId zone = parseZone(zoneId);
		if (to.isBefore(from)) {
			throw new BusinessRuleException("error.range.toNotBeforeFrom");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_SUMMARY_DAYS) {
			throw new BusinessRuleException("error.range.maxDays", MAX_SUMMARY_DAYS);
		}
		CategoryTree tree = categories.loadTree();
		if (categoryId != null && !tree.contains(categoryId)) {
			throw new NotFoundException("category", categoryId);
		}
		Set<Long> scope = categoryId == null ? null : tree.subtreeIds(categoryId);
		List<Node> calendars = tree.depthFirst()
			.stream()
			.filter(Node::isCalendar)
			.filter(n -> scope == null || scope.contains(n.id()))
			.toList();
		List<Long> calendarIds = calendars.stream().map(Node::id).toList();

		Instant start = from.atStartOfDay(zone).toInstant();
		Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
		Map<LocalDate, Integer> counts = new TreeMap<>();
		Map<LocalDate, Set<Long>> calendarsPerDay = new TreeMap<>();
		if (!calendarIds.isEmpty()) {
			for (Event event : events.findOverlapping(calendarIds, start, end)) {
				daysCovered(event.getStartTime(), event.getEndTime(), zone)
					.filter(d -> !d.isBefore(from) && !d.isAfter(to))
					.forEach(d -> {
						counts.merge(d, 1, Integer::sum);
						calendarsPerDay.computeIfAbsent(d, k -> new LinkedHashSet<>()).add(event.getCategoryId());
					});
			}
		}
		List<EventDaySummaryDto.Day> days = counts.entrySet()
			.stream()
			.map(e -> new EventDaySummaryDto.Day(e.getKey(), e.getValue(),
					calendarIds.stream().filter(calendarsPerDay.get(e.getKey())::contains).toList()))
			.toList();
		return new EventDaySummaryDto(categoryId, zone.getId(), from, to,
				calendars.stream().map(n -> new EventDaySummaryDto.CalendarRef(n.id(), n.name(), n.color())).toList(),
				days);
	}

	/**
	 * Local dates touched by the half-open interval [start, end): an event ending exactly at midnight does not
	 * count for the following day.
	 */
	static Stream<LocalDate> daysCovered(Instant start, Instant end, ZoneId zone) {
		LocalDate first = start.atZone(zone).toLocalDate();
		LocalDate last = end.minusNanos(1).atZone(zone).toLocalDate();
		if (last.isBefore(first)) {
			last = first;
		}
		return first.datesUntil(last.plusDays(1));
	}

	private static ZoneId parseZone(String zoneId) {
		try {
			return ZoneId.of(zoneId);
		}
		catch (DateTimeException ex) {
			throw new BusinessRuleException("error.zone", zoneId);
		}
	}

	public EventDto create(EventRequest request) {
		Category calendar = categories.requireCalendar(request.categoryId());
		validateRange(request);
		Event event = new Event(calendar, request.title().trim(), request.start().toInstant(),
				request.end().toInstant());
		apply(event, request);
		return EventDto.of(events.save(event));
	}

	public EventDto update(Long id, EventRequest request) {
		Event event = get(id);
		event.checkVersion(request.version());
		validateRange(request);
		if (!event.getCategoryId().equals(request.categoryId())) {
			event.setCategory(categories.requireCalendar(request.categoryId()));
		}
		event.setTitle(request.title().trim());
		event.setStartTime(request.start().toInstant());
		event.setEndTime(request.end().toInstant());
		apply(event, request);
		events.flush();
		return EventDto.of(event);
	}

	public void delete(Long id) {
		events.delete(get(id));
	}

	private Event get(Long id) {
		return events.findById(id).orElseThrow(() -> new NotFoundException("event", id));
	}

	private static void apply(Event event, EventRequest request) {
		event.setPlace(blankToNull(request.place()));
		event.setDescription(blankToNull(request.description()));
	}

	private static void validateRange(EventRequest request) {
		if (!request.end().isAfter(request.start())) {
			throw new BusinessRuleException("error.event.endAfterStart");
		}
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
