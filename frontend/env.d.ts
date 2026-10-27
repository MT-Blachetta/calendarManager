/// <reference types="vite/client" />

declare module 'primevue/toasteventbus' {
  const ToastEventBus: { emit(event: 'add', message: Record<string, unknown>): void }
  export default ToastEventBus
}
