-- =====================================================================
-- calendarManager — PostgreSQL schema (PostgreSQL 15+; developed on 18)
-- =====================================================================
-- Category tree (adjacency list). ABSTRACT categories are containers
-- without a calendar; CALENDAR categories own events. Tags are attached
-- to categories and inherited by all descendants (computed, not stored).
-- Calendar views are saved filters over categories and tags.
-- =====================================================================

CREATE TABLE tag (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    color       VARCHAR(7)   NOT NULL DEFAULT '#64748B',
    description VARCHAR(500),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT tag_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT tag_color_hex CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);
CREATE UNIQUE INDEX tag_name_uq ON tag (lower(name));

CREATE TABLE category (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id   BIGINT REFERENCES category (id),   -- NO ACTION: children must be removed first
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    kind        VARCHAR(16)  NOT NULL,
    color       VARCHAR(7)   NOT NULL DEFAULT '#6366F1',
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT category_kind_chk CHECK (kind IN ('ABSTRACT', 'CALENDAR')),
    CONSTRAINT category_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT category_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT category_color_hex CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT category_sibling_name_uq UNIQUE NULLS NOT DISTINCT (parent_id, name)
);
CREATE INDEX category_parent_idx ON category (parent_id);

CREATE TABLE category_tag (
    category_id BIGINT NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    tag_id      BIGINT NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (category_id, tag_id)
);
CREATE INDEX category_tag_tag_idx ON category_tag (tag_id);

CREATE TABLE event (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    place       VARCHAR(200),
    description VARCHAR(1000),
    start_time  TIMESTAMPTZ  NOT NULL,
    end_time    TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT event_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT event_time_range_chk CHECK (end_time > start_time)
);
CREATE INDEX event_category_time_idx ON event (category_id, start_time, end_time);
CREATE INDEX event_time_idx ON event (start_time, end_time);

CREATE TABLE calendar_view (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    filter_mode VARCHAR(10)  NOT NULL DEFAULT 'ALL',
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT calendar_view_mode_chk CHECK (filter_mode IN ('ALL', 'INCLUDE', 'EXCLUDE')),
    CONSTRAINT calendar_view_name_not_blank CHECK (btrim(name) <> '')
);
CREATE UNIQUE INDEX calendar_view_name_uq ON calendar_view (lower(name));

-- Categories selected by a view: INCLUDE mode = shown subtrees, EXCLUDE mode = hidden subtrees.
CREATE TABLE view_category_filter (
    view_id     BIGINT NOT NULL REFERENCES calendar_view (id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    PRIMARY KEY (view_id, category_id)
);

-- Tags selected by a view. MATCH: INCLUDE mode = required (any of), EXCLUDE mode = hidden.
-- EXCEPT (EXCLUDE mode only): calendars carrying such a tag are never hidden.
CREATE TABLE view_tag_filter (
    view_id BIGINT      NOT NULL REFERENCES calendar_view (id) ON DELETE CASCADE,
    tag_id  BIGINT      NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    role    VARCHAR(10) NOT NULL,
    PRIMARY KEY (view_id, tag_id),
    CONSTRAINT view_tag_filter_role_chk CHECK (role IN ('MATCH', 'EXCEPT'))
);
