package com.calendarmanager.common;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 9457 problem details ({@code application/problem+json}). Titles and details are
 * resolved from {@code messages*.properties} in the language of the request ({@code Accept-Language}, English
 * by default).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private final MessageSource messages;

	GlobalExceptionHandler(MessageSource messages) {
		this.messages = messages;
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "error.title.notFound", ex);
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "error.title.conflict", ex);
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, "error.title.businessRule", ex);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
		return problem(HttpStatus.CONFLICT, "error.title.conflict", text("error.optimisticLock"));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
		return problem(HttpStatus.CONFLICT, "error.title.integrity", text("error.integrity"));
	}

	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail handleBadSort(PropertyReferenceException ex) {
		return problem(HttpStatus.BAD_REQUEST, "error.title.badRequest", ex.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// Field messages are already interpolated in the request locale by the validator.
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		ex.getBindingResult().getGlobalErrors().forEach(e -> errors.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "error.title.validation", text("error.validation"));
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	private ProblemDetail problem(HttpStatus status, String titleKey, MessageSourceResolvable detail) {
		return problem(status, titleKey, this.messages.getMessage(detail, locale()));
	}

	private ProblemDetail problem(HttpStatus status, String titleKey, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(text(titleKey));
		return problem;
	}

	private String text(String key) {
		return this.messages.getMessage(key, null, key, locale());
	}

	private static Locale locale() {
		return LocaleContextHolder.getLocale();
	}

}
