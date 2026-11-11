package com.bhf.aeroncache.integration.soak.common;

/**
 * One cell of the streaming soak coverage matrix: a single subscription variation the workload exercises. The
 * dimensions are cache kind (regular/counter), delivery mode (full/patch), scope (whole-cache/single-key),
 * snapshot hydration, cardinality (single/multi-cache) and whether the cache is warmed (near-cache synthetic
 * patch generation). {@link #cell()} is the stable, human-readable id used for coverage accounting and in
 * failure messages.
 */
public record Variant(Variant.Kind kind, Variant.Mode mode, Variant.Scope scope,
                      boolean hydrate, Variant.Card card, boolean warm) {

    public enum Kind { REGULAR, COUNTER }

    public enum Mode { FULL, PATCH }

    public enum Scope { WHOLE, KEY }

    public enum Card { SINGLE, MULTI }

    public String cell() {
        return kind + "/" + mode + "/" + scope + "/" + (hydrate ? "hydrate" : "nohydrate") + "/" + card
                + (warm ? "/warm" : "");
    }

    public boolean counters() {
        return kind == Kind.COUNTER;
    }

    public boolean whole() {
        return scope == Scope.WHOLE;
    }

    public boolean multi() {
        return card == Card.MULTI;
    }
}
