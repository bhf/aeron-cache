package com.bhf.aeroncache.integration.soak.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static com.bhf.aeroncache.integration.soak.common.Variant.Card;
import static com.bhf.aeroncache.integration.soak.common.Variant.Kind;
import static com.bhf.aeroncache.integration.soak.common.Variant.Mode;
import static com.bhf.aeroncache.integration.soak.common.Variant.Scope;

/**
 * The streaming soak coverage matrix: the set of {@link Variant}s to exercise, cycled round-robin in a
 * seeded-shuffled order so the first full pass covers every cell, plus the per-cell coverage accounting a green
 * run asserts on.
 *
 * <p>The matrix crosses kind x mode x scope x hydrate x cardinality, with two domain carve-outs: patch mode is a
 * JSON-merge concern exercised only on regular caches (counters stream their value as {@code ADD_ITEM}, so
 * counter+patch is skipped), and the optional {@code warm} dimension (near-cache synthetic patch generation)
 * layers onto the regular/patch/hydrate variants across both scopes and cardinalities only.
 */
final class VariantMatrix {

    private final List<Variant> variants;
    private final Map<String, Long> coverage = new TreeMap<>();

    VariantMatrix(boolean includeCounters, boolean includeSynthetic, Random shuffleSource) {
        this.variants = build(includeCounters, includeSynthetic);
        Collections.shuffle(this.variants, shuffleSource);
    }

    private static List<Variant> build(boolean includeCounters, boolean includeSynthetic) {
        var out = new ArrayList<Variant>();
        var kinds = includeCounters ? List.of(Kind.REGULAR, Kind.COUNTER) : List.of(Kind.REGULAR);
        for (var kind : kinds) {
            // Patch mode is a JSON-value merge concern exercised only on regular caches (counters stream their
            // value as ADD_ITEM); mirror the existing integration tests and skip counter+patch.
            var modes = kind == Kind.REGULAR ? List.of(Mode.FULL, Mode.PATCH) : List.of(Mode.FULL);
            for (var mode : modes) {
                for (var scope : Scope.values()) {
                    for (var hydrate : List.of(Boolean.FALSE, Boolean.TRUE)) {
                        for (var card : Card.values()) {
                            out.add(new Variant(kind, mode, scope, hydrate, card, false));
                        }
                    }
                }
            }
        }
        // Synthetic patch generation (near cache): a hydrating PATCH subscription to a WARM (primed) regular
        // cache is served merge-patch deltas computed locally from the warm copy. Regular-only (counters never
        // patch) and always hydrating, so these cells layer a `warm` flag onto the regular/patch/hydrate
        // variants across both scopes and cardinalities.
        if (includeSynthetic) {
            for (var scope : Scope.values()) {
                for (var card : Card.values()) {
                    out.add(new Variant(Kind.REGULAR, Mode.PATCH, scope, true, card, true));
                }
            }
        }
        return out;
    }

    int size() {
        return variants.size();
    }

    /** The variant for the given round index, cycling the matrix round-robin. */
    Variant at(long roundIndex) {
        return variants.get((int) (roundIndex % variants.size()));
    }

    void recordCovered(Variant v) {
        coverage.merge(v.cell(), 1L, Long::sum);
    }

    /** The cell of the first variant never exercised, or {@code null} if every cell has been covered. */
    String firstUncoveredCell() {
        for (var v : variants) {
            if (coverage.getOrDefault(v.cell(), 0L) == 0L) {
                return v.cell();
            }
        }
        return null;
    }

    Map<String, Long> coverage() {
        return coverage;
    }
}
