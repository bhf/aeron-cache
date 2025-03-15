package com.bhf.aeroncache.models;

public class RequestId implements Reusable<RequestId>{

    private final StringBuilder sb = new StringBuilder();
    @Override
    public void clear() {
        sb.setLength(0);
    }

    @Override
    public void copyFrom(RequestId source) {
        sb.append(source.sb);
    }

    public String getRequestId(){
        return sb.toString();
    }

    public void setRequestId(String requestId){
        clear();
        this.sb.append(requestId);
    }
}
