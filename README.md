# calendarManager

A calendar app for **comparing many independent calendars side by side**: TV programs, cinemas, doctors'
appointments, club schedules and so on.

- **Category tree**: calendars are organised in a hierarchical category tree. Abstract categories (groups)
  carry tags that every sub-category inherits.
- **Views**: a calendar view is a saved filter (all / include / exclude over categories and tags). It decides
  which calendars appear as columns of the day view.
- **Day view**: the time axis runs from 6:00 to 0:00 in 30-minute steps, below a hierarchical header of
  category names. Events at the same time are easy to compare before you choose.
- **Month view**: pick one calendar or a whole group from a side list. Every day of the month that has at
  least one event in those calendars is highlighted. You can step back and forth through the months.
- **English and German**: the language is chosen on the *Settings* page (English by default) and saved in the
  browser. Dates, PrimeVue components and the backend's error messages follow it.
- **Public holidays**: German holidays (nationwide by default, or those of one federal state) appear in the month
  view, the day view title and the mini calendar of the sidebar.

![Day view](docs/images/day-view.png)

| | |
|---|---|
| Data model (ER + UML), schema, filter semantics | [docs/data-model.md](docs/data-model.md) |
| UI/UX design | [docs/ui-design.md](docs/ui-design.md) |
| REST API (Swagger UI) | <http://localhost:8080/swagger-ui.html> |

## Architecture

```
 Browser ── :5173 (Vite dev server, proxies /api) ──┐
                                                    ▼
             Spring Boot backend (runs natively on the host, :8080)
                                                    │ JDBC  127.0.0.1:15432
                                                    ▼
 Docker container "calendar-manager-db" (postgres:18-alpine, volume cm-pgdata)

 The host's default PostgreSQL instance(s) on :5432 (and 5433, 5434 …) are NOT used.
```

| Part | Stack |
|---|---|
| Database | PostgreSQL 18 in Docker (`docker-compose.yml`), Flyway migrations |
| Backend | Java 17, Spring Boot 4.1 (Web MVC, Data JPA / Hibernate 7, Validation, Actuator), springdoc-openapi 3 (Swagger UI), Maven |
| Frontend | Vue 3.5 + TypeScript 6, Vite 8, PrimeVue 4 (MIT), TanStack Vue Query, Pinia, vue-router, vue-i18n, date-fns; API types generated from OpenAPI (`openapi-typescript`) |
| Tests | JUnit 5 + AssertJ, MockMvc integration tests on real PostgreSQL (Testcontainers); Vitest |

```
calendarManager/
├── docker-compose.yml    PostgreSQL container (host port from .env)
├── .env.example          DB port and credentials, shared by docker compose and the backend
├── backend/              Spring Boot REST API (package com.calendarmanager)
│   └── src/main/resources/db/migration   Flyway SQL = database schema
├── frontend/             Vue 3 web app
└── docs/                 data model, UI design, screenshots
```

## Prerequisites

- **Java 17+** and **Docker** with Compose. Maven is not needed: use the wrapper `./mvnw`.
- **Node.js 24 LTS**, e.g. `nvm install 24 && nvm use` (the repo contains `.nvmrc`).
- Your user must be allowed to use Docker: `sudo usermod -aG docker $USER`, then log in again.

## Quick start

```bash
# 1. Configuration (DB port 15432, credentials) – used by docker compose and the backend
cp .env.example .env

# 2. Database
docker compose up -d          # wait until `docker compose ps` shows "healthy"

# 3. Backend – http://localhost:8080 (Swagger UI: /swagger-ui.html)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # 'demo' seeds sample data into an empty DB
#   without demo data:  ./mvnw spring-boot:run

# 4. Frontend – http://localhost:5173
cd ../frontend
npm ci
npm run dev
```

The **`demo` profile** seeds an empty database with sample data:

- A category tree: Entertainment › TV Program › Das Erste / ZDF / Sport1; Entertainment › Cinema; Health ›
  Doctors; and a top-level "Sports Club" calendar.
- Tags, three example views, and overlapping events from yesterday to three days ahead.

If the database container is not running, the backend stops at startup with a connection error on
`127.0.0.1:15432`. It never falls back to the host's own PostgreSQL.

### Configuration

| Variable (`.env` or environment) | Default | Used by |
|---|---|---|
| `CM_DB_HOST` | `127.0.0.1` | backend |
| `CM_DB_PORT` | `15432` | docker compose (host port), backend |
| `CM_DB_NAME` / `CM_DB_USER` / `CM_DB_PASSWORD` | `calendar_manager` / `calendar` / `calendar` | both |
| `SERVER_PORT` | `8080` | backend |
| `CM_BACKEND_URL` | `http://localhost:8080` | Vite dev proxy, `npm run gen:api` |

## Development

### Backend

```bash
cd backend
./mvnw verify                 # unit tests + integration tests (Testcontainers, needs Docker)
./mvnw verify -DCM_TEST_DB_URL=jdbc:postgresql://127.0.0.1:15432/calendar_manager_test
                              # integration tests against an existing, empty test database instead
```

Without Docker and without `CM_TEST_DB_URL`, the integration tests are skipped and the unit tests still run.
`TestCalendarManagerApplication` (in `src/test`) starts the app against a throw-away Testcontainers
database.

### Frontend

```bash
cd frontend
npm run dev          # dev server with /api proxy
npm run test:unit    # Vitest (time axis, event layout, header builder, month grid, holidays, locales)
npm run type-check   # vue-tsc
npm run lint         # oxlint + eslint
npm run build        # type-check + production build into dist/
npm run gen:api      # regenerate src/api/schema.d.ts from the running backend's OpenAPI spec
```

### Languages

- UI texts live in `frontend/src/i18n/locales/en.ts` (the schema) and `de.ts`. TypeScript and a unit test report
  missing German keys. Date patterns per language are in `frontend/src/i18n/dates.ts`.
- The backend's messages are in `backend/src/main/resources/messages.properties` (English) and
  `messages_de.properties`.

### Settings stored in the browser

| `localStorage` key | Values | Default |
|---|---|---|
| `cm.locale` | `en`, `de` | `en` |
| `cm.holidayRegion` | `off`, `DE` (nationwide), `DE-BW` … `DE-TH` (federal state) | `DE` |
| `cm.theme` | `light`, `dark`, `system` | `system` |
| `cm.density` | `compact`, `comfortable`, `spacious` | `comfortable` |

## REST API overview

All endpoints live under `/api`. Errors are returned as RFC 9457 problem details. Their `title`, `detail` and
validation messages are in English, or in German for requests with `Accept-Language: de`:

- 400 validation (with an `errors` map)
- 404 not found
- 409 conflict: duplicate names, stale `version`, deleting a non-empty category without `cascade`
- 422 business rule violated

| Resource | Endpoints |
|---|---|
| Tags | `GET/POST /tags`, `GET/PUT/DELETE /tags/{id}` |
| Categories | `GET/POST /categories`, `GET /categories/tree`, `GET/PUT /categories/{id}`, `GET /categories/{id}/usage`, `DELETE /categories/{id}?cascade=` |
| Events | `GET /events?from&to&categoryId&q&page&size&sort&direction`, `POST /events`, `GET/PUT/DELETE /events/{id}`, `GET /events/day-summary?categoryId&from&to&zone` (days with events, for the month view) |
| Views | `GET/POST /views`, `GET/PUT/DELETE /views/{id}`, `POST /views/resolve` (preview a filter), `GET /views/{id}/schedule?from&to` (calendars + events for the day grid) |

## Roadmap ideas

- Recurring events
- Drag to move or resize events
- Week view
- Authentication and multiple users
- Desktop and Android clients: the REST API and the generated OpenAPI contract are client-agnostic
