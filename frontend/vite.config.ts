import { fileURLToPath } from "node:url";
import { configDefaults, defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const resolve = (p: string) => fileURLToPath(new URL(p, import.meta.url));

const BACKEND = "http://localhost:8080";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@app": resolve("./src/app"),
      "@shared": resolve("./src/shared"),
      "@features": resolve("./src/features"),
      "@test-support": resolve("./test/support"),
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
    setupFiles: ["./test/support/setup.ts"],
    css: true,
    exclude: [...configDefaults.exclude, "e2e/**"],
  },
});
