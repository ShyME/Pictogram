import { createBrowserRouter } from "react-router";
import { PlaceholderPage } from "@features/placeholder";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <PlaceholderPage />,
  },
]);
