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

    @Override
    public void startHandleDeleteCache(DeleteCacheRequestDetails requestDetails) {
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
        var splitRequest = requestDetails.getRequestId().split("@");
        var traceIdHex = splitRequest[0];
        var spanIdHex = splitRequest[1];
        SpanContext spanContext = SpanContext.createFromRemoteParent(traceIdHex, spanIdHex, TraceFlags.getSampled(), TraceState.getDefault());
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
}
