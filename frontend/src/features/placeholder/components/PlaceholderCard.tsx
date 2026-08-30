import type { Placeholder } from "../model/placeholder";

export function PlaceholderCard({ content }: { content: Placeholder }) {
  return (
    <section className="max-w-md rounded-2xl border border-neutral-200 bg-white p-8 text-center shadow-sm">
      <h1 className="text-2xl font-semibold tracking-tight text-neutral-900">
        {content.title}
      </h1>
      <p className="mt-3 text-sm text-neutral-500">{content.tagline}</p>
    </section>
  );
}
