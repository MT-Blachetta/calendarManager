package com.calendarmanager.common;

/** The request is well-formed but violates a domain rule (HTTP 422). */
public class BusinessRuleException extends LocalizedException {

	/**
	 * @param code message key in {@code messages*.properties}
	 * @param arguments message arguments ({@code {0}}, {@code {1}}, ...)
	 */
	public BusinessRuleException(String code, Object... arguments) {
		super(code, arguments);
	}

}
