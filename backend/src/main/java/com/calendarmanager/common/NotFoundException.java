package com.calendarmanager.common;

import org.springframework.context.support.DefaultMessageSourceResolvable;

/** A requested resource does not exist (HTTP 404). */
public class NotFoundException extends LocalizedException {

	/**
	 * @param resource key suffix of the resource name ({@code resource.<resource>} in the message bundle)
	 * @param id the id that was not found
	 */
	public NotFoundException(String resource, Object id) {
		super("error.notFound", resourceName(resource), String.valueOf(id));
	}

	/** Translated name of a resource such as {@code "category"}. */
	public static DefaultMessageSourceResolvable resourceName(String resource) {
		return new DefaultMessageSourceResolvable(new String[] { "resource." + resource }, resource);
	}

}
