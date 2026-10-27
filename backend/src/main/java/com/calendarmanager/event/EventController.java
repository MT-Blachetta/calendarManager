package com.calendarmanager.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.calendarmanager.common.BusinessRuleException;
import com.calendarmanager.common.PageDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events")
class EventController {

	private static final Set<String> SORTABLE = Set.of("start", "end", "title", "place");

	private final EventService service;

	EventController(EventService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Search events",
			description = "All filters are optional. from/to select events overlapping [from, to).")
	PageDto<EventDto> search(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
			@RequestParam(name = "categoryId", required = false) List<Long> categoryIds,
			@RequestParam(name = "q", required = false) @Parameter(
					description = "Case-insensitive text search in title, place and description") String query,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size,
			@RequestParam(defaultValue = "start") @Parameter(
					description = "One of start, end, title, place") String sort,
			@RequestParam(defaultValue = "asc") @Parameter(description = "asc or desc") String direction) {
		if (!SORTABLE.contains(sort)) {
			throw new BusinessRuleException("error.sort", "start, end, title, place");
		}
		String property = switch (sort) {
			case "start" -> "startTime";
			case "end" -> "endTime";
			default -> sort;
		};
		Sort order = Sort.by("desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, property)
			.and(Sort.by("id"));
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), order);
		return service.search(from, to, categoryIds, query, pageable);
	}

	@GetMapping("/day-summary")
	@Operation(summary = "Days with events (month view)",
			description = "For a calendar, or a group including all calendars below it (all calendars if "
					+ "categoryId is omitted): the local days within [from, to] that have at least one event, with "
					+ "event counts and the calendars involved. Max range: 62 days.")
	EventDaySummaryDto daySummary(
			@RequestParam(required = false) @Parameter(description = "Calendar or group id") Long categoryId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(
					example = "2026-09-28") LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(
					example = "2026-11-08") LocalDate to,
			@RequestParam(defaultValue = "UTC") @Parameter(description = "IANA time zone of the client",
					example = "Europe/Berlin") String zone) {
		return service.daySummary(categoryId, from, to, zone);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an event")
	EventDto get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create an event in a CALENDAR category")
	EventDto create(@Valid @RequestBody EventRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an event")
	EventDto update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete an event")
	void delete(@PathVariable Long id) {
		service.delete(id);
	}

}
