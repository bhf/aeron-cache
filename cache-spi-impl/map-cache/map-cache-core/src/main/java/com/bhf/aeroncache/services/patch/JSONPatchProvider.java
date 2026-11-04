package com.bhf.aeroncache.services.patch;

import com.bhf.aeroncache.models.Reusable;
import com.bhf.aeroncache.models.results.CacheOperationStatus;
import com.bhf.aeroncache.models.results.PatchValueResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JSONPatchProvider<I extends Reusable, K extends Reusable, V extends Reusable> implements ValuePatchProvider<I,K,V>{

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Apply a JSON merge patch to an existing value. The patch is applied in-place, modifying the existing value.
     *
     * @param patch
     * @param existingValue
     * @param <V>
     * @return
     */
    @Override
    public <V extends Reusable> boolean applyPatch(V patch, V existingValue){
        try {
            String existingJson = existingValue.value().toString();
            String patchJson = patch.value().toString();
            JsonNode mergedNode = objectMapper.readerForUpdating(objectMapper.readTree(existingJson)).readValue(patchJson);
            String mergedJson = objectMapper.writeValueAsString(mergedNode);

            existingValue.clear();
            existingValue.copyFrom(mergedJson);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to apply patch, reason: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Produce an RFC 7386 JSON merge patch describing the change from a pre-existing value to the newly
     * added value, populating the supplied out-parameter. The outcome is carried on the out-parameter's
     * status:
     * <ul>
     *   <li>{@link CacheOperationStatus#SUCCESS} - a non-empty delta was produced (carried as the entry
     *       value); send it as a patch update.</li>
     *   <li>{@link CacheOperationStatus#UNKNOWN_KEY} - there was no previous value (a first insert), so
     *       there is nothing to diff against. There is no delta, but the caller should still send the whole
     *       new value as an initial baseline, otherwise a patch subscriber that joined before the key
     *       existed would never see the first insert and would hold a stale (empty) view. This mirrors the
     *       near cache's {@code BASELINE} outcome.</li>
     *   <li>{@link CacheOperationStatus#ERROR} - a valid merge patch could not be computed (unparseable
     *       JSON); the caller should fall back to sending the whole value rather than nothing.</li>
     *   <li>{@link CacheOperationStatus#NONE} - the values were equal, so there was no effective change
     *       and no update need be sent.</li>
     * </ul>
     */
    @Override
    public void produceMergePatch(K key, V previousValue, V newValue, PatchValueResult<I, K, V> mergePatchOut) {
        mergePatchOut.clear();
        if (previousValue == null) {
            // No prior value to diff against (a first insert): signal a baseline so the caller sends the
            // whole value as an initial ADD_ITEM rather than nothing. UNKNOWN_KEY reads honestly here - the
            // key held no value before this add.
            mergePatchOut.getEntryKey().copyFrom(key);
            mergePatchOut.setStatus(CacheOperationStatus.UNKNOWN_KEY);
            return;
        }
        mergePatchOut.getEntryKey().copyFrom(key);
        try {
            JsonNode oldNode = objectMapper.readTree(previousValue.value().toString());
            JsonNode newNode = objectMapper.readTree(newValue.value().toString());
            JsonNode patchNode = computeMergePatch(oldNode, newNode);

            if (patchNode.isObject() && patchNode.isEmpty()) {
                // nothing changed, no patch update required
                return;
            }

            String patchJson = objectMapper.writeValueAsString(patchNode);
            mergePatchOut.getEntryValue().copyFrom(patchJson);
            mergePatchOut.setStatus(CacheOperationStatus.SUCCESS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to compute merge patch for key {}, reason: {}", key, e.getMessage());
            mergePatchOut.setStatus(CacheOperationStatus.ERROR);
        }
    }

    /**
     * Compute an RFC 7386 JSON merge patch that transforms {@code source} into {@code target}.
     */
    private JsonNode computeMergePatch(JsonNode source, JsonNode target) {
        if (!source.isObject() || !target.isObject()) {
            return target;
        }

        ObjectNode patch = objectMapper.createObjectNode();

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
