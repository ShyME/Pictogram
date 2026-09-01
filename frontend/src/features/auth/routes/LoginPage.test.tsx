import { screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { renderWithProviders } from "../../../test/render";
import { LoginPage } from "./LoginPage";

function renderAt(path: string) {
  renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <LoginPage />
    </MemoryRouter>,
  );
}

test("offers a 'Continue with Google' link into the backend OIDC flow", () => {
  renderAt("/login");

  const link = screen.getByRole("link", { name: /continue with google/i });
  expect(link).toHaveAttribute("href", "/oauth2/authorization/google");
});

test("shows no error message on a clean visit", () => {
  renderAt("/login");

  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

test("explains an unverified Google email when bounced back with that reason", () => {
  renderAt("/login?error=email-unverified");

  expect(screen.getByRole("alert")).toHaveTextContent(/verif/i);
});

test("falls back to a generic message for an unknown or generic error reason", () => {
  renderAt("/login?error=sign-in-failed");

  expect(screen.getByRole("alert")).toHaveTextContent(/didn't complete/i);
});
