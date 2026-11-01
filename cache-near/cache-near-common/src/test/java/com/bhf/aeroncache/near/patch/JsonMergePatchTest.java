package com.bhf.aeroncache.near.patch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("JsonMergePatch")
class JsonMergePatchTest {

    @Test
    @DisplayName("computes a delta containing only the changed and added fields")
    void computesDeltaForChangedAndAddedFields() {
        var delta = JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":3,\"c\":4}");
        // Only the fields that actually changed/were added are present; unchanged "a" is omitted.
        assertEquals("{\"b\":3,\"c\":4}", delta);
    }

    @Test
    @DisplayName("computes a nested delta carrying only the changed nested field")
    void computesNestedDelta() {
        var delta = JsonMergePatch.computeMergePatch(
                "{\"a\":1,\"nested\":{\"x\":1,\"y\":2}}",
                "{\"a\":1,\"nested\":{\"x\":9,\"y\":2}}");
        assertEquals("{\"nested\":{\"x\":9}}", delta);
    }

    @Test
    @DisplayName("represents a removed field as a null in the delta (RFC 7386)")
    void representsRemovedFieldAsNull() {
        var delta = JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1}");
        assertEquals("{\"b\":null}", delta);
    }

    @Test
    @DisplayName("returns null when nothing changed")
    void returnsNullWhenUnchanged() {
        assertNull(JsonMergePatch.computeMergePatch("{\"a\":1,\"b\":2}", "{\"a\":1,\"b\":2}"));
    }

    @Test
    @DisplayName("returns null for non-JSON input so the caller falls back to the whole value")
    void returnsNullForNonJson() {
        assertNull(JsonMergePatch.computeMergePatch("not json", "also not json"));
        assertNull(JsonMergePatch.computeMergePatch(null, "{\"a\":1}"));
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

        var delta = JsonMergePatch.computeMergePatch(oldValue, newValue);
        var merged = JsonMergePatch.applyMergePatch(oldValue, delta);

        assertEquals(newValue, merged);
    }

    @Test
    @DisplayName("applying a patch with no prior value returns the patch as the whole value")
    void applyWithNoExistingReturnsPatch() {
        assertEquals("{\"a\":1}", JsonMergePatch.applyMergePatch(null, "{\"a\":1}"));
    }
}
