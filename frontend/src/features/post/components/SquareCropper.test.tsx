import { createRef } from "react";
import { expect, test } from "vitest";
import { render, screen } from "@testing-library/react";
import { SquareCropper, type CropperHandle } from "./SquareCropper";

test("shows the photo in a frame with a zoom control", () => {
  render(<SquareCropper src="blob:preview" />);

  expect(screen.getByRole("img")).toHaveAttribute("src", "blob:preview");
  expect(screen.getByRole("slider", { name: /zoom/i })).toBeInTheDocument();
});

test("getCroppedBlob rejects before the image has loaded", async () => {
  const ref = createRef<CropperHandle>();
  render(<SquareCropper ref={ref} src="blob:preview" />);

  await expect(ref.current?.getCroppedBlob()).rejects.toThrow(/not ready/i);
});
