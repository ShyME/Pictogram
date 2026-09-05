/// <reference types="vite/client" />

interface ImportMetaEnv {
  // Overrides chat's default port (compose's Caddy-fronted 8082) for host-based `task
  // chat` dev, which runs bare on 8081 — see chatConnection.ts (#164).
  readonly VITE_CHAT_PORT?: string;
}
