import { useQuery } from "@tanstack/react-query";
import { fetchPlaceholder } from "../api/placeholder-api";
import { PlaceholderCard } from "../components/PlaceholderCard";
import { placeholder } from "../model/placeholder";

export function PlaceholderPage() {
  const { data } = useQuery({
    queryKey: ["placeholder"],
    queryFn: fetchPlaceholder,
    initialData: placeholder,
  });

  return (
    <main className="grid min-h-dvh place-items-center bg-neutral-50 p-6">
      <PlaceholderCard content={data} />
    </main>
  );
}
