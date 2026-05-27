package com.bhf.aeroncache.services.tracing.impl;

import com.bhf.aeroncache.models.requests.*;
import com.bhf.aeroncache.services.tracing.CacheTracingService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.TimeUnit;

@Log4j2
public class OtelTracingService implements CacheTracingService {
    private final String nodeId;
    Resource serviceNameResource;
    SdkTracerProvider tracerProvider;
    OpenTelemetrySdk openTelemetry;
    OtlpGrpcSpanExporter jaegerOtlpExporter;

    public OtelTracingService(String jaegerEndpoint, String serviceName, String nodeId) {
        this.nodeId = nodeId;
        jaegerOtlpExporter =
                OtlpGrpcSpanExporter.builder()
                        .setEndpoint(jaegerEndpoint)
                        .setTimeout(30, TimeUnit.SECONDS)
                        .build();
        serviceNameResource =
                Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName));
        tracerProvider =
                SdkTracerProvider.builder()
                        .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build())
                        .setResource(Resource.getDefault().merge(serviceNameResource))
                        .build();
        openTelemetry =
                OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        log.info("Created OTEL Tracing Service for {} on {}", serviceName, jaegerEndpoint);
    }

    private static SpanContext getSpanContext(String requestId) {
        String[] splitRequest = requestId.split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
        return spanContext;
    }

    @Override
    public void startHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("DeleteCache SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("deleteCacheRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startHandleClearCache(ClearCacheRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("ClearCache SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("clearCacheRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endHandleClearCache(ClearCacheRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("RemoveEntry SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("removeEntryRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endRemoveCacheEntry(RemoveCacheEntryRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("AddEntry SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("addEntryRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endAddCacheEntry(AddCacheEntryRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("GetEntry SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("getEntryRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endGetCacheEntry(GetCacheEntryRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("GetAll SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("getAllEntriesRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endGetAllCacheEntries(GetAllCacheEntriesRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startCreateCacheRequest(CreateCacheRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("CreateCache SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("createCacheRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endCreateCacheRequest(CreateCacheRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("GetAllStats SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("getAllStatsRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endGetAllStatsRequest(GetCacheStatsRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startCacheSubscriptionRequest(CacheSubscriptionRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("CacheSubscribe SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("subscribeCacheRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endCacheSubscriptionRequest(CacheSubscriptionRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startCacheUnsubscribeRequest(CacheUnsubscribeRequestDetails requestDetails) {
        SpanContext spanContext = getSpanContext(requestDetails.getRequestId());
        log.info("CacheUnsubscribe SpanContext with traceId {} spanId {} ", spanContext.getTraceId(), spanContext.getSpanId());
        Span spanNoOp = Span.wrap(spanContext);
        Tracer tracer = tracerProvider.get("aeron-cache-cluster-tracer");
        Span span = tracer.spanBuilder("unsubscribeCacheRequest").setParent(Context.current().with(spanNoOp)).startSpan();
        span.setAttribute("nodeId", nodeId);
        span.makeCurrent();
    }

    @Override
    public void endCacheUnsubscribeRequest(CacheUnsubscribeRequestDetails requestDetails) {
        Span.current().end();
    }

    @Override
    public void startBulkOpsRequest(BulkCacheOpsRequestDetails requestDetails) {

    }

    @Override
    public void endBulkOpsRequest(BulkCacheOpsRequestDetails requestDetails) {

    }
}
