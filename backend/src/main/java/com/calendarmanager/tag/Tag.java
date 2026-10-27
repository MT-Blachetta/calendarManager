package com.calendarmanager.tag;

import com.calendarmanager.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A label that can be attached to categories; inherited by all sub-categories. */
@Entity
@Table(name = "tag")
public class Tag extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, length = 7)
	private String color;

	@Column(length = 500)
	private String description;

	protected Tag() {
	}

	public Tag(String name, String color, String description) {
		this.name = name;
		this.color = color;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
