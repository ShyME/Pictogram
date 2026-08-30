import { type ReactNode, useState } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { createQueryClient } from "@shared";

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
