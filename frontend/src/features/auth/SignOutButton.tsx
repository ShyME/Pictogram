import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { signOut } from "./auth-api";

export function SignOutButton() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [signingOut, setSigningOut] = useState(false);

  async function handleClick() {
    setSigningOut(true);
    await signOut();
    queryClient.clear();
    navigate("/login", { replace: true });
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={signingOut}
      className="rounded-lg border border-neutral-200 px-3 py-1.5 text-sm font-medium text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-50"
    >
      Sign out
    </button>
  );
}
