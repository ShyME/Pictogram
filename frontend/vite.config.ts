/// <reference types="vitest/config" />
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const resolve = (p: string) => fileURLToPath(new URL(p, import.meta.url));

// Backend runs on the host (compose.dev.yaml); proxied below so the SPA can use
// same-origin relative URLs in every environment.
const BACKEND = "http://localhost:8080";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Keep these aliases in sync with `paths` in tsconfig.app.json.
  resolve: {
    alias: {
      "@app": resolve("./src/app/index.ts"),
      "@shared": resolve("./src/shared/index.ts"),
      "@features": resolve("./src/features"),
    },
  },
  server: {
    proxy: {
      "/api": { target: BACKEND, changeOrigin: true },
      "/oauth2": { target: BACKEND, changeOrigin: true },
      "/login/oauth2": { target: BACKEND, changeOrigin: true },
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    css: true,
  },
});
