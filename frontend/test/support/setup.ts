import "@testing-library/jest-dom/vitest";

if (typeof URL.createObjectURL !== "function") {
  URL.createObjectURL = () => "blob:preview";
  URL.revokeObjectURL = () => {};
}
