# calendarManager — web frontend

Vue 3 + TypeScript single-page app. See the [root README](../README.md) for setup and the
[UI/UX design](../docs/ui-design.md) for the concepts.

```
src/
├── api/          openapi-fetch client, generated schema.d.ts, Vue Query composables (queries.ts)
├── components/
│   ├── day/      DayGrid (time axis, hierarchical header, columns, hover band) and DayEventCard
│   ├── event/    EventFormDialog, EventDetailsDrawer ("At the same time")
│   ├── category/ CategoryTreeList, CategoryForm
│   ├── view/     ViewEditor (filter editor with live preview)
│   ├── tag/, common/, layout/
├── composables/  useCatalog (tags/categories/views lookups)
├── lib/          time axis, event lane layout, header builder (pure, unit-tested in __tests__)
├── pages/        Day, Events, Categories, Tags, Views
├── stores/       Pinia: UI preferences, event editor state
└── theme.ts      PrimeVue Aura preset (indigo / slate)
```

Requires Node.js 24 (`nvm use`). Commands: `npm run dev | build | test:unit | type-check | lint | gen:api`.
