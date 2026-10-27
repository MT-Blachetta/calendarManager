package com.calendarmanager.category;

import java.util.HashSet;
import java.util.Set;

import com.calendarmanager.common.BaseEntity;
import com.calendarmanager.tag.Tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A node of the category tree. ABSTRACT categories group children; CALENDAR categories own events.
 */
@Entity
@Table(name = "category")
public class Category extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private CategoryKind kind;

	@Column(nullable = false, length = 7)
	private String color;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/** Own tags only; inherited tags are derived via {@link CategoryTree}. */
	@ManyToMany
	@JoinTable(name = "category_tag", joinColumns = @JoinColumn(name = "category_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();

	protected Category() {
	}

	public Category(String name, CategoryKind kind, String color) {
		this.name = name;
		this.kind = kind;
		this.color = color;
	}

	public Long getParentId() {
		return parent == null ? null : parent.getId();
	}

	public Category getParent() {
		return parent;
	}

	public void setParent(Category parent) {
		this.parent = parent;
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

	public CategoryKind getKind() {
		return kind;
	}

	public void setKind(CategoryKind kind) {
		this.kind = kind;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void replaceTags(Set<Tag> newTags) {
		tags.clear();
		tags.addAll(newTags);
	}

}
