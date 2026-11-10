package com.bhf.aeroncache.services.patch;

public class JsonMergePatchComputer implements MergePatchComputer {

    @Override
    public MergePatchResult computeMergePatch(String previousJson, String newJson) {
        return JsonMergePatch.computeMergePatch(previousJson, newJson);
    }
}
