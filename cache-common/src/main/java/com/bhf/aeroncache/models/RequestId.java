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

    @Override
    public void copyFrom(Reusable<RequestId> source) {
        this.copyFrom(source.value());
    }

    @Override
    public RequestId value() {
        return this;
    }

    public String getRequestId(){
        return sb.toString();
    }

    public void setRequestId(String requestId){
        clear();
        this.sb.append(requestId);
    }
}
