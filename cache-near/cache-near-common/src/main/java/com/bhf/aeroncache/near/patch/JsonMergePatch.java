package com.bhf.aeroncache.near.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;

/**
 * RFC 7386 JSON merge patch helpers operating on raw JSON strings, used by the near cache layer.
 *
 * <p>This is the near-cache counterpart to the cluster's write-side patch provider: because a near cache
 * holds a warm full copy and receives whole-value (ADD_ITEM) updates, it can compute a merge-patch delta
 * between the value it already holds and the incoming value, and forward just that delta to a subscriber
 * that asked for patch updates - and, symmetrically, a downstream consumer can apply such a delta onto its
 * own local copy. The two directions are deliberately kept here, self-contained to the near cache layer and
 * independent of the cluster's write-time patch handling.
 */
@Log4j2
public final class JsonMergePatch {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonMergePatch() {
    }

    /**
     * Compute an RFC 7386 JSON merge patch describing the change from {@code previousJson} to
     * {@code newJson}.
     *
     * @param previousJson the value already held locally.
     * @param newJson      the new whole value.
     * @return the merge patch as a JSON string, or {@code null} when nothing changed or either side is not
     *         parseable JSON (in which case the caller should fall back to sending the whole value).
     */
    public static String computeMergePatch(String previousJson, String newJson) {
        if (previousJson == null || newJson == null) {
            return null;
        }
        try {
            JsonNode oldNode = OBJECT_MAPPER.readTree(previousJson);
            JsonNode newNode = OBJECT_MAPPER.readTree(newJson);
            JsonNode patchNode = computeMergePatch(oldNode, newNode);

            if (patchNode.isObject() && patchNode.isEmpty()) {
                // Nothing changed; no patch update required.
                return null;
            }
            return OBJECT_MAPPER.writeValueAsString(patchNode);
        } catch (JsonProcessingException e) {
            log.warn("Failed to compute merge patch, falling back to whole value, reason: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Apply an RFC 7386 JSON merge patch to an existing value, returning the merged value.
     *
     * @param existingJson the value currently held locally.
     * @param patchJson    the merge patch to apply.
     * @return the merged value as a JSON string, or the raw {@code patchJson} if the merge could not be
     *         performed (best effort: treat the patch as a replacement value).
     */
    public static String applyMergePatch(String existingJson, String patchJson) {
        if (existingJson == null) {
            return patchJson;
        }
        try {
            JsonNode mergedNode = OBJECT_MAPPER.readerForUpdating(OBJECT_MAPPER.readTree(existingJson))
                    .readValue(patchJson);
            return OBJECT_MAPPER.writeValueAsString(mergedNode);
        } catch (JsonProcessingException e) {
            log.warn("Failed to apply merge patch, treating patch as replacement value, reason: {}", e.getMessage());
            return patchJson;
        }
    }

    /**
     * Compute an RFC 7386 JSON merge patch that transforms {@code source} into {@code target}. For
     * non-objects the patch is simply the target value (scalar/array replacement).
     */
    private static JsonNode computeMergePatch(JsonNode source, JsonNode target) {
        if (!source.isObject() || !target.isObject()) {
            return target;
        }

        ObjectNode patch = OBJECT_MAPPER.createObjectNode();

        var sourceFields = source.fieldNames();
        while (sourceFields.hasNext()) {
            String field = sourceFields.next();
            if (!target.has(field)) {
                patch.set(field, NullNode.getInstance());
            }
        }

        var targetFields = target.fieldNames();
        while (targetFields.hasNext()) {
            String field = targetFields.next();
            JsonNode targetValue = target.get(field);
            if (!source.has(field)) {
                patch.set(field, targetValue);
            } else {
                JsonNode sourceValue = source.get(field);
                if (sourceValue.isObject() && targetValue.isObject()) {
                    JsonNode nested = computeMergePatch(sourceValue, targetValue);
                    if (!nested.isObject() || !nested.isEmpty()) {
                        patch.set(field, nested);
                    }
                } else if (!sourceValue.equals(targetValue)) {
                    patch.set(field, targetValue);
                }
            }
        }

        return patch;
    }
}
