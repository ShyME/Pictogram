import { useRouteError } from "react-router";

export function RouteError() {
  const error = useRouteError();

  return (
    <main className="grid min-h-dvh place-items-center bg-neutral-50 p-6">
      <div className="max-w-sm text-center">
        <h1 className="text-lg font-semibold text-neutral-900">Something went wrong</h1>
        <p className="mt-2 text-sm text-neutral-500">Reload the page to try again.</p>
        {import.meta.env.DEV && error instanceof Error && (
          <pre className="mt-4 overflow-x-auto text-xs text-neutral-400">{error.message}</pre>
        )}
      </div>
    </main>
  );
}
