package com.calendarmanager.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.calendarmanager.IntegrationTest;
import com.calendarmanager.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;

@IntegrationTest
class ApiIntegrationTest {

	@DynamicPropertySource
	static void db(DynamicPropertyRegistry registry) {
		IntegrationTestSupport.registerExternalDb(registry);
	}

	@Autowired
	MockMvc mvc;

	long leisure;

	long tv;

	long publicTv;

	long entertainment;

	long tvProgram;

	long ard;

	long sport1;

	long club;

	@BeforeEach
	void setUp() throws Exception {
		leisure = create("/api/tags", "{\"name\":\"T-Leisure\",\"color\":\"#10b981\"}");
		tv = create("/api/tags", "{\"name\":\"T-TV\"}");
		publicTv = create("/api/tags", "{\"name\":\"T-Public\"}");
		entertainment = create("/api/categories",
				"{\"name\":\"T-Entertainment\",\"kind\":\"ABSTRACT\",\"tagIds\":[%d]}".formatted(leisure));
		tvProgram = create("/api/categories",
				"{\"name\":\"TV\",\"kind\":\"ABSTRACT\",\"parentId\":%d,\"tagIds\":[%d]}".formatted(entertainment, tv));
		ard = create("/api/categories", "{\"name\":\"ARD\",\"kind\":\"CALENDAR\",\"parentId\":%d,\"tagIds\":[%d]}"
			.formatted(tvProgram, publicTv));
		sport1 = create("/api/categories",
				"{\"name\":\"Sport1\",\"kind\":\"CALENDAR\",\"parentId\":%d,\"sortOrder\":1}".formatted(tvProgram));
		club = create("/api/categories", "{\"name\":\"T-Club\",\"kind\":\"CALENDAR\",\"sortOrder\":99}");
	}

	@Test
	void tagCrudAndValidation() throws Exception {
		mvc.perform(get("/api/tags/{id}", leisure))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.color").value("#10B981"))
			.andExpect(jsonPath("$.usageCount").value(1));
		json(put("/api/tags/{id}", leisure), "{\"name\":\"T-Free time\",\"version\":0}").andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(1));
		json(put("/api/tags/{id}", leisure), "{\"name\":\"T-Free time\",\"version\":0}")
			.andExpect(status().isConflict());
		json(post("/api/tags"), "{\"name\":\"t-tv\"}").andExpect(status().isConflict());
		json(post("/api/tags"), "{\"name\":\"\",\"color\":\"red\"}").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.color").value(containsString("hex color")));
		mvc.perform(delete("/api/tags/{id}", tv)).andExpect(status().isNoContent());
		mvc.perform(get("/api/tags/{id}", tv)).andExpect(status().isNotFound());
		mvc.perform(get("/api/categories/{id}", ard)).andExpect(jsonPath("$.inheritedTags", hasSize(1)));
	}

	@Test
	void messagesFollowAcceptLanguage() throws Exception {
		// English by default and for unsupported languages
		json(post("/api/tags"), "{\"name\":\"t-tv\"}").andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Conflict"))
			.andExpect(jsonPath("$.detail").value("A tag named 't-tv' already exists"));
		mvc.perform(get("/api/tags/{id}", 987654).header("Accept-Language", "fr-FR"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Tag 987654 not found"));
		json(put("/api/categories/{id}", ard),
				"{\"name\":\"ARD\",\"kind\":\"CALENDAR\",\"parentId\":%d,\"version\":7}".formatted(tvProgram))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("Category %d was modified by someone else".formatted(ard))));

		// German
		json(post("/api/tags").header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8"), "{\"name\":\"t-tv\"}")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Konflikt"))
			.andExpect(jsonPath("$.detail").value("Ein Tag namens „t-tv“ existiert bereits"));
		mvc.perform(get("/api/categories/{id}", 987654).header("Accept-Language", "de"))
			.andExpect(jsonPath("$.detail").value("Kategorie 987654 wurde nicht gefunden"));
		json(post("/api/tags").header("Accept-Language", "de"), "{\"name\":\"\",\"color\":\"red\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Ein oder mehrere Felder sind ungültig."))
			.andExpect(jsonPath("$.errors.color").value("muss eine Hex-Farbe wie #4F46E5 sein"))
			.andExpect(jsonPath("$.errors.name").value("darf nicht leer sein"));
		json(post("/api/tags"), "{\"name\":\"\"}").andExpect(jsonPath("$.errors.name").value("must not be blank"));
		mvc.perform(get("/api/events/day-summary").param("from", "2026-10-01")
			.param("to", "2026-09-01")
			.header("Accept-Language", "de")).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.title").value("Geschäftsregel verletzt"))
			.andExpect(jsonPath("$.detail").value("„to“ darf nicht vor „from“ liegen"));
	}

	@Test
	void categoriesExposeInheritedTagsAndEnforceTreeRules() throws Exception {
		mvc.perform(get("/api/categories/{id}", ard))
			.andExpect(jsonPath("$.depth").value(2))
			.andExpect(jsonPath("$.tagIds", contains((int) publicTv)))
			.andExpect(jsonPath("$.inheritedTags[?(@.tagId == %d)].sourceCategoryId".formatted(tv))
				.value(contains((int) tvProgram)))
			.andExpect(jsonPath("$.inheritedTags[?(@.tagId == %d)].sourceCategoryId".formatted(leisure))
				.value(contains((int) entertainment)));

		// a CALENDAR cannot be a parent
		json(post("/api/categories"), "{\"name\":\"Sub\",\"kind\":\"CALENDAR\",\"parentId\":%d}".formatted(ard))
			.andExpect(status().isUnprocessableContent());
		// no cycles
		json(put("/api/categories/{id}", entertainment),
				"{\"name\":\"T-Entertainment\",\"kind\":\"ABSTRACT\",\"parentId\":%d}".formatted(tvProgram))
			.andExpect(status().isUnprocessableContent());
		// sibling names are unique
		json(post("/api/categories"), "{\"name\":\"ard\",\"kind\":\"CALENDAR\",\"parentId\":%d}".formatted(tvProgram))
			.andExpect(status().isConflict());
		// ABSTRACT with children cannot become a CALENDAR
		json(put("/api/categories/{id}", tvProgram),
				"{\"name\":\"TV\",\"kind\":\"CALENDAR\",\"parentId\":%d}".formatted(entertainment))
			.andExpect(status().isUnprocessableContent());
		// moving a calendar to the root works
		json(put("/api/categories/{id}", sport1), "{\"name\":\"Sport1\",\"kind\":\"CALENDAR\",\"version\":0}")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.parentId").doesNotExist())
			.andExpect(jsonPath("$.inheritedTags", hasSize(0)));
	}

	@Test
	void eventsRequireCalendarCategoryAndValidRange() throws Exception {
		json(post("/api/events"), event(tvProgram, "10:00", "11:00")).andExpect(status().isUnprocessableContent());
		json(post("/api/events"), event(ard, "11:00", "10:00")).andExpect(status().isUnprocessableContent());
		long id = create("/api/events", event(ard, "20:15", "21:45"));
		json(put("/api/events/{id}", id),
				"{\"categoryId\":%d,\"title\":\"Moved\",\"place\":\"Studio\",\"start\":\"2030-01-01T20:00:00+01:00\",\"end\":\"2030-01-01T21:00:00+01:00\",\"version\":0}"
					.formatted(sport1))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.categoryId").value(sport1))
			.andExpect(jsonPath("$.start").value("2030-01-01T19:00:00Z"));
		mvc.perform(get("/api/events").param("q", "MOV").param("categoryId", String.valueOf(sport1)))
			.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(delete("/api/events/{id}", id)).andExpect(status().isNoContent());
		mvc.perform(get("/api/events/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void scheduleResolvesViewAndReturnsOverlappingEvents() throws Exception {
		create("/api/events", event(ard, "20:15", "21:45"));
		create("/api/events", event(sport1, "20:00", "22:00"));
		create("/api/events", event(club, "23:00", "23:59"));
		// ends exactly at 'from' of the next query window -> excluded there
		create("/api/events", event(ard, "05:00", "06:00"));

		long view = create("/api/views",
				"{\"name\":\"T-Public TV\",\"filterMode\":\"EXCLUDE\",\"categoryIds\":[%d],\"exceptTagIds\":[%d]}"
					.formatted(tvProgram, publicTv));
		mvc.perform(get("/api/views/{id}/schedule", view).param("from", "2030-01-01T06:00:00+01:00")
			.param("to", "2030-01-02T00:00:00+01:00"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.calendars[*].id", contains((int) ard, (int) club)))
			.andExpect(jsonPath("$.calendars[0].path[*].name", contains("T-Entertainment", "TV")))
			.andExpect(jsonPath("$.events[*].categoryId", contains((int) ard, (int) club)));

		json(post("/api/views/resolve"), "{\"filterMode\":\"INCLUDE\",\"matchTagIds\":[%d]}".formatted(tv))
			.andExpect(jsonPath("$[*].id", contains((int) ard, (int) sport1)));

		mvc.perform(get("/api/views/{id}/schedule", view).param("from", "2030-01-02T00:00:00Z")
			.param("to", "2030-01-01T00:00:00Z")).andExpect(status().isUnprocessableContent());
	}

	@Test
	void viewRulesAndSystemViewProtection() throws Exception {
		json(post("/api/views"), "{\"name\":\"T-Empty\",\"filterMode\":\"INCLUDE\"}")
			.andExpect(status().isUnprocessableContent());
		json(post("/api/views"),
				"{\"name\":\"T-Bad\",\"filterMode\":\"INCLUDE\",\"categoryIds\":[%d],\"exceptTagIds\":[%d]}"
					.formatted(ard, tv))
			.andExpect(status().isUnprocessableContent());
		json(post("/api/views"), "{\"name\":\"T-Missing\",\"filterMode\":\"INCLUDE\",\"categoryIds\":[999999]}")
			.andExpect(status().isNotFound());

		String views = mvc.perform(get("/api/views")).andReturn().getResponse().getContentAsString();
		int systemId = JsonPath.read(views, "$[0].id");
		mvc.perform(delete("/api/views/{id}", systemId)).andExpect(status().isConflict());
		json(put("/api/views/{id}", systemId),
				"{\"name\":\"All calendars\",\"filterMode\":\"INCLUDE\",\"categoryIds\":[%d]}".formatted(ard))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void deletingCategoriesRestrictsOrCascades() throws Exception {
		create("/api/events", event(ard, "20:15", "21:45"));
		long view = create("/api/views",
				"{\"name\":\"T-TV\",\"filterMode\":\"INCLUDE\",\"categoryIds\":[%d]}".formatted(tvProgram));
		mvc.perform(get("/api/categories/{id}/usage", entertainment))
			.andExpect(jsonPath("$.descendantCount").value(3))
			.andExpect(jsonPath("$.eventCount").value(1))
			.andExpect(jsonPath("$.viewCount").value(1));
		mvc.perform(delete("/api/categories/{id}", entertainment)).andExpect(status().isConflict());
		mvc.perform(delete("/api/categories/{id}", entertainment).param("cascade", "true"))
			.andExpect(status().isNoContent());
		mvc.perform(get("/api/categories/{id}", ard)).andExpect(status().isNotFound());
		mvc.perform(get("/api/views/{id}", view)).andExpect(jsonPath("$.categoryIds", hasSize(0)));
		mvc.perform(delete("/api/categories/{id}", club)).andExpect(status().isNoContent());
	}

	@Test
	void daySummaryHighlightsDaysOfACalendarOrGroup() throws Exception {
		create("/api/events", event(ard, "20:15", "21:45")); // 2030-01-01
		create("/api/events", event(ard, "22:00", "23:00")); // 2030-01-01, second event
		create("/api/events", event(sport1, "10:00", "11:00")); // 2030-01-01
		// Sport1: 2030-01-03 23:00 – 2030-01-05 01:00 (+01:00) → touches the 3rd, 4th and 5th
		create("/api/events",
				"{\"categoryId\":%d,\"title\":\"Marathon\",\"start\":\"2030-01-03T23:00:00+01:00\",\"end\":\"2030-01-05T01:00:00+01:00\"}"
					.formatted(sport1));
		// club: ends exactly at midnight of the 7th → only the 6th
		create("/api/events",
				"{\"categoryId\":%d,\"title\":\"Party\",\"start\":\"2030-01-06T22:00:00+01:00\",\"end\":\"2030-01-07T00:00:00+01:00\"}"
					.formatted(club));

		// group: all calendars below "TV"
		mvc.perform(get("/api/events/day-summary").param("categoryId", String.valueOf(tvProgram))
			.param("from", "2030-01-01")
			.param("to", "2030-01-31")
			.param("zone", "Europe/Berlin"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.calendars[*].id", contains((int) ard, (int) sport1)))
			.andExpect(jsonPath("$.days[*].date", contains("2030-01-01", "2030-01-03", "2030-01-04", "2030-01-05")))
			.andExpect(jsonPath("$.days[0].eventCount").value(3))
			.andExpect(jsonPath("$.days[0].calendarIds", contains((int) ard, (int) sport1)))
			.andExpect(jsonPath("$.days[1].calendarIds", contains((int) sport1)));

		// single calendar
		mvc.perform(get("/api/events/day-summary").param("categoryId", String.valueOf(club))
			.param("from", "2030-01-01")
			.param("to", "2030-01-31")
			.param("zone", "Europe/Berlin"))
			.andExpect(jsonPath("$.calendars[*].id", contains((int) club)))
			.andExpect(jsonPath("$.days[*].date", contains("2030-01-06")));

		// range is clipped to [from, to]
		mvc.perform(get("/api/events/day-summary").param("categoryId", String.valueOf(sport1))
			.param("from", "2030-01-04")
			.param("to", "2030-01-04")
			.param("zone", "Europe/Berlin")).andExpect(jsonPath("$.days[*].date", contains("2030-01-04")));

		// validation
		mvc.perform(get("/api/events/day-summary").param("from", "2030-01-01").param("to", "2030-06-01"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/api/events/day-summary").param("from", "2030-01-02").param("to", "2030-01-01"))
			.andExpect(status().isUnprocessableContent());
		mvc.perform(get("/api/events/day-summary").param("from", "2030-01-01")
			.param("to", "2030-01-02")
			.param("zone", "Mars/Olympus")).andExpect(status().isUnprocessableContent());
		mvc.perform(get("/api/events/day-summary").param("categoryId", "999999")
			.param("from", "2030-01-01")
			.param("to", "2030-01-02")).andExpect(status().isNotFound());
	}

	private static String event(long categoryId, String start, String end) {
		return "{\"categoryId\":%d,\"title\":\"Event %s\",\"start\":\"2030-01-01T%s:00+01:00\",\"end\":\"2030-01-01T%s:00+01:00\"}"
			.formatted(categoryId, start, start, end);
	}

	private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
			String body) throws Exception {
		return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private long create(String path, String body) throws Exception {
		String response = json(post(path), body).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

}
