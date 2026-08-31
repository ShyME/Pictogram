import type { ReactNode } from "react";
import { afterEach, expect, test, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { jsonResponse, problemResponse, stubFetch } from "../../../test/mock-fetch";
import { EditProfilePage } from "./EditProfilePage";
import type { EditProfileData } from "./edit-profile-loader";

const navigate = vi.fn();
let loaderData: EditProfileData;

vi.mock("react-router", () => ({
  useLoaderData: () => loaderData,
  useNavigate: () => navigate,
  Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a>,
}));

afterEach(() => {
  navigate.mockClear();
  vi.unstubAllGlobals();
});

const render = (over: Partial<EditProfileData["profile"]> = {}) => {
  loaderData = {
    profile: { userId: "u-1", username: "ada", displayName: "Ada", bio: "a bio", ...over },
  };
  return renderWithProviders(<EditProfilePage />);
};

const usernameField = () => screen.getByLabelText("Username");
const save = () => fireEvent.click(screen.getByRole("button", { name: /save changes/i }));

test("pre-fills the form with the current profile", () => {
  render();

  expect(usernameField()).toHaveValue("ada");
  expect(screen.getByLabelText(/display name/i)).toHaveValue("Ada");
  expect(screen.getByLabelText(/bio/i)).toHaveValue("a bio");
});

test("warns that old links break once the username is changed, and not before", () => {
  render();

  expect(screen.queryByRole("status")).not.toBeInTheDocument();

  fireEvent.change(usernameField(), { target: { value: "ada_lovelace" } });

  expect(screen.getByRole("status")).toHaveTextContent(/breaks existing links/i);
  expect(screen.getByRole("status")).toHaveTextContent("/u/ada");
});

test("a malformed username shows the shape hint and blocks submit", () => {
  stubFetch(() => new Response(null, { status: 500 }));
  render();

  fireEvent.change(usernameField(), { target: { value: "No Good" } });

  expect(screen.getByRole("alert")).toHaveTextContent(/lowercase letters, digits or underscore/i);
  expect(screen.getByRole("button", { name: /save changes/i })).toBeDisabled();
  expect(fetch).not.toHaveBeenCalled();
});

test("saving a valid edit sends the user to their (possibly renamed) profile", async () => {
  stubFetch(() =>
    jsonResponse({ userId: "u-1", username: "ada_lovelace", displayName: "Ada L.", bio: "a bio" }),
  );
  render();

  fireEvent.change(usernameField(), { target: { value: "ada_lovelace" } });
  fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: "Ada L." } });
  save();

  await waitFor(() => {
    expect(navigate).toHaveBeenCalledWith("/u/ada_lovelace", { replace: true });
  });
});

test("a taken username is reported inline as taken, not as a shape problem", async () => {
  stubFetch(() => problemResponse("username-taken", 409));
  render();

  fireEvent.change(usernameField(), { target: { value: "grace" } });
  save();

  await waitFor(() => {
    expect(screen.getByRole("alert")).toHaveTextContent(/already taken/i);
  });
  expect(navigate).not.toHaveBeenCalled();
});

test("an over-long display name or bio is reported inline", async () => {
  stubFetch(() => problemResponse("profile-details-invalid", 400));
  render();

  fireEvent.change(screen.getByLabelText(/bio/i), { target: { value: "way too long" } });
  save();

  expect(await screen.findByText(/display name or bio is too long/i)).toBeInTheDocument();
  expect(navigate).not.toHaveBeenCalled();
});

test("cancel links back to the current profile", () => {
  render();

  expect(screen.getByRole("link", { name: /cancel/i })).toHaveAttribute("href", "/u/ada");
});
