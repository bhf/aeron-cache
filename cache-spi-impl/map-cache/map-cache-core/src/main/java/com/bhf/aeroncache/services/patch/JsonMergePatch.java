package com.bhf.aeroncache.services.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * RFC 7386 JSON merge patch helpers operating on raw JSON strings.
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
     * @return a {@link MergePatchResult}
     */
    public static MergePatchResult computeMergePatch(String previousJson, String newJson) {
        if (previousJson == null || newJson == null) {
            return MergePatchResult.failed();
        }
        try {
            JsonNode oldNode = OBJECT_MAPPER.readTree(previousJson);
            JsonNode newNode = OBJECT_MAPPER.readTree(newJson);
            JsonNode patchNode = computeMergePatch(oldNode, newNode);

            if (patchNode.isObject() && patchNode.isEmpty()) {
                // Nothing changed; no patch update required.
                return MergePatchResult.unchanged();
            }
            return MergePatchResult.delta(OBJECT_MAPPER.writeValueAsString(patchNode));
        } catch (JsonProcessingException e) {
            log.warn("Failed to compute merge patch, falling back to whole value, reason: {}", e.getMessage());
            return MergePatchResult.failed();
        }
    }

    /**
     * Apply an RFC 7386 JSON merge patch to an existing value.
     *
     * @param existingJson the value currently held locally; {@code null} is treated as no prior value.
     * @param patchJson    the merge patch to apply.
     * @return the merged value as a JSON string, or empty when the merge failed.
     */
    public static Optional<String> applyMergePatchChecked(String existingJson, String patchJson) {
        if (existingJson == null) {
            return Optional.ofNullable(patchJson);
        }
        try {
            JsonNode mergedNode = OBJECT_MAPPER.readerForUpdating(OBJECT_MAPPER.readTree(existingJson))
                    .readValue(patchJson);
            return Optional.of(OBJECT_MAPPER.writeValueAsString(mergedNode));
        } catch (JsonProcessingException e) {
            log.warn("Failed to apply merge patch, reason: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Apply an RFC 7386 JSON merge patch to an existing value.
     *
     * @param existingJson the value currently held locally.
     * @param patchJson    the merge patch to apply.
     * @return the merged value, or {@code patchJson} if the merge could not be performed.
     */
    public static String applyMergePatch(String existingJson, String patchJson) {
        return applyMergePatchChecked(existingJson, patchJson).orElse(patchJson);
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
