export function followRelationshipKey(userId: string) {
  return ["follow", "relationship", userId] as const;
}

export function followListKey(mode: "followers" | "following", userId: string) {
  return ["follow", "list", mode, userId] as const;
}
