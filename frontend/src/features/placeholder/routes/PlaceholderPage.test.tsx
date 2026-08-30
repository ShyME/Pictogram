import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { PlaceholderPage } from "./PlaceholderPage";

test("renders the placeholder heading", () => {
  renderWithProviders(<PlaceholderPage />);

  expect(
    screen.getByRole("heading", { name: /pictogram/i }),
  ).toBeInTheDocument();
});
