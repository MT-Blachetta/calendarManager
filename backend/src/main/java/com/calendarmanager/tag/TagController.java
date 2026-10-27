package com.calendarmanager.tag;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tags")
@Tag(name = "Tags")
class TagController {

	private final TagService service;

	TagController(TagService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List all tags (alphabetically)")
	List<TagDto> list() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a tag")
	TagDto get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a tag")
	TagDto create(@Valid @RequestBody TagRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a tag")
	TagDto update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a tag", description = "Detaches it from all categories and view filters.")
	void delete(@PathVariable Long id) {
		service.delete(id);
	}

}
