import { ContinueWithGoogleButton } from "../components/ContinueWithGoogleButton";

export function LoginPage() {
  return (
    <main className="grid min-h-dvh place-items-center bg-neutral-50 p-6">
      <div className="w-full max-w-sm rounded-2xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
        <h1 className="text-2xl font-semibold tracking-tight text-neutral-900">Pictogram</h1>
        <p className="mt-2 text-sm text-neutral-500">Sign in to see your feed.</p>
        <ContinueWithGoogleButton className="mt-6" />
      </div>
    </main>
  );
}
