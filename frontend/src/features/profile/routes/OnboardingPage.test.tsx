import { afterEach, expect, test, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../test/render";
import { jsonResponse, problemResponse, stubFetch } from "../../../test/mock-fetch";
import { OnboardingPage } from "./OnboardingPage";

const navigate = vi.fn();
vi.mock("react-router", () => ({ useNavigate: () => navigate }));

afterEach(() => {
  navigate.mockClear();
  vi.unstubAllGlobals();
});

const typeUsername = (value: string) =>
  fireEvent.change(screen.getByLabelText("Username"), { target: { value } });
const clickCreate = () =>
  fireEvent.click(screen.getByRole("button", { name: /create profile/i }));

test("shows an inline shape hint and blocks submit while the username is malformed", () => {
  stubFetch(() => new Response(null, { status: 500 }));
  renderWithProviders(<OnboardingPage />);

  typeUsername("Ada Lovelace!");

  expect(screen.getByRole("alert")).toHaveTextContent(/lowercase letters, digits or underscore/i);
  expect(screen.getByRole("button", { name: /create profile/i })).toBeDisabled();
  expect(fetch).not.toHaveBeenCalled();
});

test("a taken username gets a distinct 'taken' message, not the shape hint", async () => {
  stubFetch(() => problemResponse("username-taken", 409));
  renderWithProviders(<OnboardingPage />);

  typeUsername("ada_lovelace");
  clickCreate();

  await waitFor(() => {
    expect(screen.getByRole("alert")).toHaveTextContent(/already taken/i);
  });
  expect(screen.getByRole("alert")).not.toHaveTextContent(/lowercase letters/i);
  expect(navigate).not.toHaveBeenCalled();
});

test("the 'taken' message clears as soon as the user edits the username", async () => {
  stubFetch(() => problemResponse("username-taken", 409));
  renderWithProviders(<OnboardingPage />);

  typeUsername("ada_lovelace");
  clickCreate();
  await screen.findByText(/already taken/i);

  typeUsername("ada_lovelace2");

  expect(screen.queryByText(/already taken/i)).not.toBeInTheDocument();
});

test("an over-long display name or bio is reported inline", async () => {
  stubFetch(() => problemResponse("profile-details-invalid", 400));
  renderWithProviders(<OnboardingPage />);

  typeUsername("ada_lovelace");
  clickCreate();

  expect(await screen.findByText(/display name or bio is too long/i)).toBeInTheDocument();
  expect(navigate).not.toHaveBeenCalled();
});

test("a well-formed, free username creates the profile and heads to the feed", async () => {
  stubFetch(() => jsonResponse({ userId: "u-1", username: "ada_lovelace" }, 201));
  renderWithProviders(<OnboardingPage />);

  typeUsername("ada_lovelace");
  fireEvent.change(screen.getByLabelText(/display name/i), { target: { value: "Ada" } });
  clickCreate();

  await waitFor(() => {
    expect(navigate).toHaveBeenCalledWith("/", { replace: true });
  });
});
