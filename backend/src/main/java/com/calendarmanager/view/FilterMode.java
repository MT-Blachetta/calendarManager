package com.calendarmanager.view;

/** How a calendar view selects calendars. */
public enum FilterMode {

	/** Every calendar (the default view). */
	ALL,

	/** Positive filter: only calendars in the selected category subtrees and/or carrying a selected tag. */
	INCLUDE,

	/** Negative filter: hide calendars in the selected subtrees or with a selected tag, except EXCEPT-tagged ones. */
	EXCLUDE

}
