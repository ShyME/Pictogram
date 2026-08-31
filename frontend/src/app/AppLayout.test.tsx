import { expect, test, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../test/render";
import { AppLayout } from "./AppLayout";

vi.mock("react-router", () => ({
  useLoaderData: () => ({
    profile: { userId: "u-1", username: "ada", displayName: "Ada", bio: null },
  }),
  useNavigate: () => vi.fn(),
  Outlet: () => <p>feed content</p>,
}));

test("shows the signed-in username, a sign-out control, and the routed page", () => {
  renderWithProviders(<AppLayout />);

  expect(screen.getByText("@ada")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /sign out/i })).toBeInTheDocument();
  expect(screen.getByText("feed content")).toBeInTheDocument();
});
