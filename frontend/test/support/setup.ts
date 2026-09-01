import "@testing-library/jest-dom/vitest";

// jsdom implements neither of these; components that preview a picked file need them.
if (typeof URL.createObjectURL !== "function") {
  URL.createObjectURL = () => "blob:preview";
  URL.revokeObjectURL = () => {};
}
