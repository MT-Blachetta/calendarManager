package com.calendarmanager.category;

import java.util.List;

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
@RequestMapping("/api/categories")
@Tag(name = "Categories")
class CategoryController {

	private final CategoryService service;

	CategoryController(CategoryService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List all categories (flat, depth-first order)",
			description = "Each entry carries its own tag ids and the tags inherited from its ancestors.")
	List<CategoryDto> list() {
		return service.findAll();
	}

	@GetMapping("/tree")
	@Operation(summary = "Get the category hierarchy as nested tree")
	List<CategoryTreeNodeDto> tree() {
		return service.findTree();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a category")
	CategoryDto get(@PathVariable Long id) {
		return service.findById(id);
	}

	@GetMapping("/{id}/usage")
	@Operation(summary = "What deleting the category would affect (sub-categories, events, views)")
	CategoryUsageDto usage(@PathVariable Long id) {
		return service.usage(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a category",
			description = "The parent must be ABSTRACT. Sibling names must be unique.")
	CategoryDto create(@Valid @RequestBody CategoryRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a category (rename, move, change kind, set tags)",
			description = "Moving below its own subtree is rejected. ABSTRACT→CALENDAR requires no children, "
					+ "CALENDAR→ABSTRACT requires no events.")
	CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a category")
	void delete(@PathVariable Long id, @RequestParam(defaultValue = "false") @Parameter(
			description = "Also delete all sub-categories and events") boolean cascade) {
		service.delete(id, cascade);
	}

}
