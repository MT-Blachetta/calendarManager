package com.calendarmanager.demo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.calendarmanager.category.CategoryDto;
import com.calendarmanager.category.CategoryKind;
import com.calendarmanager.category.CategoryRepository;
import com.calendarmanager.category.CategoryRequest;
import com.calendarmanager.category.CategoryService;
import com.calendarmanager.event.EventRequest;
import com.calendarmanager.event.EventService;
import com.calendarmanager.tag.TagDto;
import com.calendarmanager.tag.TagRequest;
import com.calendarmanager.tag.TagService;
import com.calendarmanager.view.FilterMode;
import com.calendarmanager.view.ViewRequest;
import com.calendarmanager.view.ViewService;

/**
 * Seeds an empty database with a demo category tree, tags, views and overlapping events around today.
 * Active only with the {@code demo} profile.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	private final CategoryRepository categoryRepository;

	private final TagService tags;

	private final CategoryService categories;

	private final EventService events;

	private final ViewService views;

	private final ZoneId zone = ZoneId.systemDefault();

	DemoDataSeeder(CategoryRepository categoryRepository, TagService tags, CategoryService categories,
			EventService events, ViewService views) {
		this.categoryRepository = categoryRepository;
		this.tags = tags;
		this.categories = categories;
		this.events = events;
		this.views = views;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (categoryRepository.count() > 0) {
			log.info("Demo data not seeded: database already contains categories");
			return;
		}
		TagDto leisure = tag("Leisure", "#10B981", "Free-time activities");
		TagDto tv = tag("TV", "#0EA5E9", "Television program");
		TagDto publicTv = tag("Public broadcaster", "#4F46E5", "Publicly funded channels");
		TagDto sports = tag("Sports", "#F97316", "Sport related");
		TagDto movies = tag("Movies", "#A855F7", "Feature films");
		TagDto appointments = tag("Appointments", "#EF4444", "Personal appointments");
		TagDto medical = tag("Medical", "#EC4899", "Doctors and health care");

		CategoryDto entertainment = group("Entertainment", null, 0, leisure);
		CategoryDto tvProgram = group("TV Program", entertainment, 0, tv);
		CategoryDto ard = calendar("Das Erste", tvProgram, "#2563EB", 0, publicTv);
		CategoryDto zdf = calendar("ZDF", tvProgram, "#F59E0B", 1, publicTv);
		CategoryDto sport1 = calendar("Sport1", tvProgram, "#EF4444", 2, sports);
		CategoryDto cinema = group("Cinema", entertainment, 1, movies);
		CategoryDto cinemaCity = calendar("Cinema City", cinema, "#8B5CF6", 0);
		CategoryDto health = group("Health", null, 1, appointments);
		CategoryDto doctors = group("Doctors", health, 0, medical);
		CategoryDto dentist = calendar("Dr. Müller – Dentist", doctors, "#14B8A6", 0);
		CategoryDto gp = calendar("Dr. Schmidt – GP", doctors, "#06B6D4", 1);
		CategoryDto club = calendar("Sports Club", null, "#84CC16", 2, sports, leisure);

		LocalDate today = LocalDate.now(zone);
		for (int offset = -1; offset <= 3; offset++) {
			LocalDate day = today.plusDays(offset);
			int v = Math.floorMod(offset, 3);
			event(ard, day, "06:00", "09:00", "ARD-Morgenmagazin", null, "News and service in the morning");
			event(ard, day, "12:00", "14:00", "ARD-Mittagsmagazin", null, null);
			event(ard, day, "20:00", "20:15", "Tagesschau", null, "News");
			event(ard, day, "20:15", "21:45", List.of("Tatort", "Polizeiruf 110", "Der Usedom-Krimi").get(v), null,
					"Crime series");
			event(ard, day, "21:45", "22:15", "Tagesthemen", null, "News magazine");
			event(zdf, day, "09:05", "11:30", "Volle Kanne", null, "Service magazine");
			event(zdf, day, "19:00", "19:20", "heute", null, "News");
			event(zdf, day, "19:25", "20:15", "Die Rosenheim-Cops", null, "Crime comedy");
			event(zdf, day, "20:15", "21:45", List.of("Der Bergdoktor", "Ein starkes Team", "Terra X").get(v), null,
					null);
			event(zdf, day, "21:45", "22:15", "heute journal", null, "News magazine");
			event(sport1, day, "18:30", "20:00", "Bundesliga Pur", null, "Highlights");
			event(sport1, day, "20:15", "22:30", "Fußball live: 2. Bundesliga", "Volksparkstadion",
					"Live football broadcast");
			event(sport1, day, "22:30", "23:59", "Darts – Premier League", null, null);
			event(cinemaCity, day, "15:00", "17:15", "The Wild Robot", "Screen 2", "Animated");
			event(cinemaCity, day, "20:00", "22:45", "Dune: Part Three", "Screen 1", "Science fiction");
			event(cinemaCity, day, "20:30", "22:30", "Arrival (re-release)", "Screen 3", "Science fiction classic");
			if (offset == 0) {
				event(dentist, day, "09:00", "09:30", "Check-up", "Hauptstraße 12", "Annual dental check-up");
				event(gp, day, "08:30", "08:50", "Blood test", "Praxis Dr. Schmidt", "Fasting required");
				event(gp, day, "16:00", "16:30", "Results discussion", "Praxis Dr. Schmidt", null);
			}
			if (offset == 1) {
				event(dentist, day, "14:00", "14:45", "Professional teeth cleaning", "Hauptstraße 12", null);
			}
			event(club, day, "18:00", "19:30", "Training", "Sports hall North", "Bring indoor shoes");
			if (offset == 2) {
				event(club, day, "20:00", "21:30", "Members' meeting", "Clubhouse", null);
			}
		}

		views.create(new ViewRequest("TV only", "Just the television program", FilterMode.INCLUDE,
				List.of(tvProgram.id()), List.of(), List.of(), 1, null));
		views.create(new ViewRequest("Sports", "Everything tagged Sports", FilterMode.INCLUDE, List.of(),
				List.of(sports.id()), List.of(), 2, null));
		views.create(new ViewRequest("No private TV", "Hide the TV program except public broadcasters",
				FilterMode.EXCLUDE, List.of(tvProgram.id()), List.of(), List.of(publicTv.id()), 3, null));
		log.info("Demo data seeded around {}", today);
	}

	private TagDto tag(String name, String color, String description) {
		return tags.create(new TagRequest(name, color, description, null));
	}

	private CategoryDto group(String name, CategoryDto parent, int sortOrder, TagDto... tagList) {
		return categories.create(new CategoryRequest(name, null, CategoryKind.ABSTRACT, null,
				parent == null ? null : parent.id(), sortOrder, ids(tagList), null));
	}

	private CategoryDto calendar(String name, CategoryDto parent, String color, int sortOrder, TagDto... tagList) {
		return categories.create(new CategoryRequest(name, null, CategoryKind.CALENDAR, color,
				parent == null ? null : parent.id(), sortOrder, ids(tagList), null));
	}

	private void event(CategoryDto calendar, LocalDate day, String start, String end, String title, String place,
			String description) {
		OffsetDateTime from = day.atTime(LocalTime.parse(start)).atZone(zone).toOffsetDateTime();
		OffsetDateTime to = day.atTime(LocalTime.parse(end)).atZone(zone).toOffsetDateTime();
		events.create(new EventRequest(calendar.id(), title, place, description, from, to, null));
	}

	private static List<Long> ids(TagDto... tagList) {
		return java.util.Arrays.stream(tagList).map(TagDto::id).toList();
	}

}
