package com.calendarmanager.category;

/** Kind of a category node in the hierarchy. */
public enum CategoryKind {

	/** Container that groups sub-categories; has tags but no calendar/events. */
	ABSTRACT,

	/** Leaf that owns a calendar of events. */
	CALENDAR

}
