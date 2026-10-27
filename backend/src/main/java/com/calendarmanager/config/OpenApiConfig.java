package com.calendarmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

	@Bean
	OpenAPI calendarManagerOpenApi() {
		return new OpenAPI()
			.info(new Info().title("calendarManager API")
				.version("v1")
				.description("""
						Manage multiple independent calendars organised in a hierarchical category tree.
						ABSTRACT categories are containers (no events) whose tags are inherited by all
						descendants; CALENDAR categories own events. Calendar views are saved filters
						(ALL / INCLUDE / EXCLUDE) over categories and tags."""))
			.addTagsItem(new Tag().name("Tags").description("Labels attached to categories"))
			.addTagsItem(new Tag().name("Categories").description("Hierarchical category tree"))
			.addTagsItem(new Tag().name("Events").description("Events / appointments of calendar categories"))
			.addTagsItem(new Tag().name("Views").description("Saved calendar views and day schedules"));
	}

}
