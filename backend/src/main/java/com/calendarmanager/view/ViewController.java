package com.calendarmanager.view;

import java.time.OffsetDateTime;
import java.util.List;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/views")
@Tag(name = "Views")
class ViewController {

	private final ViewService service;

	ViewController(ViewService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List all views (system view first)")
	List<ViewDto> list() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a view")
	ViewDto get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a view")
	ViewDto create(@Valid @RequestBody ViewRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a view", description = "The filter of the system view cannot be changed.")
	ViewDto update(@PathVariable Long id, @Valid @RequestBody ViewRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a view", description = "The system view cannot be deleted.")
	void delete(@PathVariable Long id) {
		service.delete(id);
	}

	@PostMapping("/resolve")
	@Operation(summary = "Preview which calendars a filter shows (without saving)")
	List<ResolvedCalendarDto> resolve(@Valid @RequestBody ViewFilterRequest request) {
		return service.resolve(request.toFilter());
	}

	@GetMapping("/{id}/schedule")
	@Operation(summary = "Visible calendars of a view and their events overlapping [from, to)",
			description = "Calendars are returned in column order (depth-first through the category tree) with "
					+ "their ancestor path for the hierarchical header. Max range: 62 days.")
	ScheduleDto schedule(@PathVariable Long id,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(
					example = "2026-10-18T00:00:00+02:00") OffsetDateTime from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(
					example = "2026-10-19T00:00:00+02:00") OffsetDateTime to) {
		return service.schedule(id, from, to);
	}

}
