import { type Placeholder, placeholder } from "../model/placeholder";

// Stand-in for a real backend call so the slice shows where `api/` fetches go.
export async function fetchPlaceholder(): Promise<Placeholder> {
  return placeholder;
}
