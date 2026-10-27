package com.calendarmanager.view;

/** Role of a tag inside a view filter. */
public enum TagFilterRole {

	/** INCLUDE mode: calendar must carry any MATCH tag. EXCLUDE mode: calendars with such a tag are hidden. */
	MATCH,

	/** EXCLUDE mode only: calendars carrying such a tag are never hidden. */
	EXCEPT

}
