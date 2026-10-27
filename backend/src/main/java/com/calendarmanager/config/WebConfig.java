package com.calendarmanager.config;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Web settings: language of API messages and CORS for browser clients served from another origin (the Vite dev
 * server normally proxies /api).
 */
@Configuration(proxyBeanMethods = false)
class WebConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	WebConfig(@Value("${calendar-manager.cors.allowed-origins:}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	/**
	 * API messages (problem details, bean-validation errors) follow the request's {@code Accept-Language}:
	 * English or German, English for everything else and when the header is missing, independent of the host
	 * locale.
	 */
	@Bean
	LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.GERMAN));
		resolver.setDefaultLocale(Locale.ENGLISH);
		return resolver;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (allowedOrigins.length > 0) {
			registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
		}
	}

}
