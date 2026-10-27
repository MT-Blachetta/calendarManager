-- The default view: shows every calendar of every category and tag.
INSERT INTO calendar_view (name, description, filter_mode, is_system, sort_order)
VALUES ('All calendars', 'Every calendar of every category and tag.', 'ALL', TRUE, 0);
