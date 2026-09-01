package me.imshy.pictogram.media;

/**
 * Sweeps away media that no post references and that have outlived the grace period — the
 * bytes and the row both go (media/CONTEXT.md: <em>Orphan</em>). Covers "uploaded but never
 * posted" and "the post was deleted" alike. Triggered on a schedule from the composition
 * root; safe to call by hand and idempotent.
 */
public interface OrphanCollection {

    /** Runs a sweep now; returns how many media it removed. */
    int collectOrphans();
}
