import { afterEach, expect, test, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { pathOf, stubFetch } from "../../../test/mock-fetch";
import { setAccessToken } from "../model/session";
import { SignOutButton } from "./SignOutButton";

const navigate = vi.fn();
vi.mock("react-router", () => ({ useNavigate: () => navigate }));

afterEach(() => {
  navigate.mockClear();
  setAccessToken(null);
  vi.unstubAllGlobals();
});

test("ends the session and returns to /login", async () => {
  setAccessToken("live");
  const calls = stubFetch(() => new Response(null, { status: 204 }));
  renderWithProviders(<SignOutButton />);

  fireEvent.click(screen.getByRole("button", { name: /sign out/i }));

  await waitFor(() => {
    expect(navigate).toHaveBeenCalledWith("/login", { replace: true });
  });
  expect(calls.map(pathOf)).toContain("/api/auth/logout");
});
