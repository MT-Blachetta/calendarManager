package com.calendarmanager.view;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** A tag reference with its role inside a view filter (row of {@code view_tag_filter}). */
@Embeddable
public class TagFilter {

	@Column(name = "tag_id", nullable = false)
	private Long tagId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TagFilterRole role;

	protected TagFilter() {
	}

	public TagFilter(Long tagId, TagFilterRole role) {
		this.tagId = tagId;
		this.role = role;
	}

	public Long getTagId() {
		return tagId;
	}

	public TagFilterRole getRole() {
		return role;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TagFilter other && Objects.equals(tagId, other.tagId) && role == other.role;
	}

	@Override
	public int hashCode() {
		return Objects.hash(tagId, role);
	}

}
