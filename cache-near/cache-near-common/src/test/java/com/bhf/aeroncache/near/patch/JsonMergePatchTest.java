package com.bhf.aeroncache.near.patch;

import com.bhf.aeroncache.near.patch.JsonMergePatch.MergePatchResult.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("JsonMergePatch")
class JsonMergePatchTest {

    @Test
    @DisplayName("computes a delta containing only the changed and added fields")
    void computesDeltaForChangedAndAddedFields() {
        var result = JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3,\"c\":4}");
        assertEquals(Kind.DELTA, result.kind());
        // Only the fields that actually changed/were added are present; unchanged "a" is omitted.
        assertEquals("{\"b\":3,\"c\":4}", result.delta());
    }

    @Test
    @DisplayName("computes a nested delta carrying only the changed nested field")
    void computesNestedDelta() {
        var result = JsonMergePatch.computeMergePatch(
                "{\"a\":1,\"nested\":{\"x\":1,\"y\":2}}",
                "{\"a\":1,\"nested\":{\"x\":9,\"y\":2}}");
        assertEquals(Kind.DELTA, result.kind());
        assertEquals("{\"nested\":{\"x\":9}}", result.delta());
    }

    @Test
    @DisplayName("represents a removed field as a null in the delta (RFC 7386)")
    void representsRemovedFieldAsNull() {
        var result = JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1}");
        assertEquals(Kind.DELTA, result.kind());
        assertEquals("{\"b\":null}", result.delta());
    }

    @Test
    @DisplayName("reports UNCHANGED (no delta) when nothing changed")
    void reportsUnchangedWhenNothingChanged() {
        var result = JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":2}");
        assertEquals(Kind.UNCHANGED, result.kind());
    }

    @Test
    @DisplayName("reports FAILED for non-JSON or null input so the caller falls back to the whole value")
    void reportsFailedForNonJson() {
        assertEquals(Kind.FAILED, JsonMergePatch.computeMergePatch("not json", "also not json").kind());
        assertEquals(Kind.FAILED, JsonMergePatch.computeMergePatch(null, "{\"a\":1}").kind());
    }

    @Test
    @DisplayName("applying a patch merges changed and added fields onto the existing value")
    void applyMergesChangedAndAddedFields() {
        var merged = JsonMergePatch.applyMergePatch("{\"a\":1,\"b\":2}", "{\"b\":3,\"c\":4}");
        assertEquals("{\"a\":1,\"b\":3,\"c\":4}", merged);
    }

    @Test
    @DisplayName("a computed delta applied onto the old value reconstructs the new value")
    void applyRoundTripsWithCompute() {
        var oldValue = "{\"a\":1,\"b\":2,\"c\":3}";
        var newValue = "{\"a\":1,\"b\":9,\"c\":3,\"d\":4}";

        var result = JsonMergePatch.computeMergePatch(oldValue, newValue);
        assertEquals(Kind.DELTA, result.kind());
        var merged = JsonMergePatch.applyMergePatch(oldValue, result.delta());

        assertEquals(newValue, merged);
    }

    @Test
    @DisplayName("applying a patch with no prior value returns the patch as the whole value")
    void applyWithNoExistingReturnsPatch() {
        assertEquals("{\"a\":1}", JsonMergePatch.applyMergePatch(null, "{\"a\":1}"));
    }
}
