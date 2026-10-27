package com.calendarmanager.common;

/** The request conflicts with the current state, e.g. a duplicate name or a stale version (HTTP 409). */
public class ConflictException extends LocalizedException {

	/**
	 * @param code message key in {@code messages*.properties}
	 * @param arguments message arguments ({@code {0}}, {@code {1}}, ...)
	 */
	public ConflictException(String code, Object... arguments) {
		super(code, arguments);
	}

}
