import { screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { renderWithProviders } from "../../../test/render";
import { LoginPage } from "./LoginPage";

test("offers a 'Continue with Google' link into the backend OIDC flow", () => {
  renderWithProviders(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );

  const link = screen.getByRole("link", { name: /continue with google/i });
  expect(link).toHaveAttribute("href", "/oauth2/authorization/google");
});
