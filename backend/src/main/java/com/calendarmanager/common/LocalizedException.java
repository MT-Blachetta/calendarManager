package com.calendarmanager.common;

import java.util.Arrays;

import org.springframework.context.MessageSourceResolvable;

/**
 * An exception whose client-facing message is a key in {@code messages*.properties}; the
 * {@link GlobalExceptionHandler} resolves it in the language of the request ({@code Accept-Language}).
 * Arguments may themselves be {@link MessageSourceResolvable}s (e.g. translated resource names).
 */
public abstract class LocalizedException extends RuntimeException implements MessageSourceResolvable {

	private final String code;

	private final Object[] arguments;

	protected LocalizedException(String code, Object... arguments) {
		super(code + (arguments.length > 0 ? " " + Arrays.toString(arguments) : ""));
		this.code = code;
		this.arguments = arguments;
	}

	@Override
	public String[] getCodes() {
		return new String[] { this.code };
	}

	@Override
	public Object[] getArguments() {
		return this.arguments;
	}

	@Override
	public String getDefaultMessage() {
		return getMessage();
	}

}
