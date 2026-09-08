/// <reference types="vite/client" />

interface ImportMetaEnv {
  // Host-based `task chat` dev only: chat runs bare on 8081 with no Caddy in front, so
  // point the WebSocket at that port instead of the shared app origin — see
  // chatConnection.ts (#169).
  readonly VITE_CHAT_PORT?: string;
}
