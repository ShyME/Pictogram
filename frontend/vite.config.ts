import { fileURLToPath } from "node:url";
import { configDefaults, defineConfig } from "vitest/config";
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
    // Playwright owns e2e/ — Vitest would try to run its specs and choke on the runner.
    exclude: [...configDefaults.exclude, "e2e/**"],
  },
});
