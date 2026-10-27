package com.calendarmanager.view;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.calendarmanager.category.Category;
import com.calendarmanager.common.BaseEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/** A saved calendar view: a filter over categories and tags that decides which calendars are shown. */
@Entity
@Table(name = "calendar_view")
public class CalendarView extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "filter_mode", nullable = false, length = 10)
	private FilterMode filterMode = FilterMode.ALL;

	/** System views (the default "All calendars") cannot be deleted and always show everything. */
	@Column(name = "is_system", nullable = false)
	private boolean system;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@ManyToMany
	@JoinTable(name = "view_category_filter", joinColumns = @JoinColumn(name = "view_id"),
			inverseJoinColumns = @JoinColumn(name = "category_id"))
	private Set<Category> categories = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "view_tag_filter", joinColumns = @JoinColumn(name = "view_id"))
	private Set<TagFilter> tagFilters = new HashSet<>();

	protected CalendarView() {
	}

	public CalendarView(String name) {
		this.name = name;
	}

	public ViewFilter toFilter() {
		return new ViewFilter(filterMode, categories.stream().map(Category::getId).collect(Collectors.toSet()),
				tagIds(TagFilterRole.MATCH), tagIds(TagFilterRole.EXCEPT));
	}

	public Set<Long> tagIds(TagFilterRole role) {
		return tagFilters.stream().filter(f -> f.getRole() == role).map(TagFilter::getTagId).collect(Collectors.toSet());
	}

	public void replaceFilter(FilterMode mode, Set<Category> newCategories, Set<Long> matchTagIds,
			Set<Long> exceptTagIds) {
		this.filterMode = mode;
		categories.clear();
		categories.addAll(newCategories);
		tagFilters.clear();
		matchTagIds.forEach(id -> tagFilters.add(new TagFilter(id, TagFilterRole.MATCH)));
		exceptTagIds.forEach(id -> tagFilters.add(new TagFilter(id, TagFilterRole.EXCEPT)));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public FilterMode getFilterMode() {
		return filterMode;
	}

	public boolean isSystem() {
		return system;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

}
