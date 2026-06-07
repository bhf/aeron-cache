package com.bhf.aeroncache.services;

import com.bhf.aeroncache.AeronCache;
import com.bhf.aeroncache.utils.ClusterUtils;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.driver.MediaDriver;
import lombok.extern.log4j.Log4j2;
import org.agrona.MutableDirectBuffer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A reconnecting version of {@link AeronCache}.
 */
@Log4j2
public class ReconnectingAeronCache implements AeronCache {

    public static final int NOT_CONNECTED = -1;
    private final String egressIP;
    private final String ingressEndpoints;
    private final EgressListener client;
    private final String alias;
    private final String aeronDirectory;
    private final MediaDriver mediaDriver;
    private AeronCluster aeronCluster;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final Consumer<Boolean> connectionStateCallback;

    public ReconnectingAeronCache(String egressIP, String ingressEndpoints, EgressListener client, String alias, String aeronDirectory, Consumer<Boolean> callback) {
        this.egressIP = egressIP;
        this.ingressEndpoints = ingressEndpoints;
        this.client = client;
        this.alias = alias;
        this.aeronDirectory = aeronDirectory;
        this.mediaDriver = null;
        this.connectionStateCallback = callback;
    }

    public ReconnectingAeronCache(String egressIP, String ingressEndpoints, EgressListener client, String alias, MediaDriver mediaDriver, Consumer<Boolean> callback) {
        this.egressIP = egressIP;
        this.ingressEndpoints = ingressEndpoints;
        this.client = client;
        this.alias = alias;
        this.mediaDriver = mediaDriver;
        this.aeronDirectory = mediaDriver != null ? mediaDriver.aeronDirectoryName() : null;
        this.connectionStateCallback = callback;
    }

    /**
     * Connect to the cache cluster.
     */
    public void connect() {
        try {
            if (aeronCluster != null) {
                try {
                    ClusterUtils.close(aeronCluster);
                } catch (Exception e) {
                    log.warn("Error closing old cluster connection for alias {}", alias, e);
                }
            }
            log.info("Connecting to cluster with alias {}...", alias);
            
            String dir = mediaDriver != null ? mediaDriver.aeronDirectoryName() : aeronDirectory;
            AeronCluster.Context clusterCtx = ClusterUtils.createClusterContext(egressIP, ingressEndpoints, client, alias, dir)
                    .errorHandler(throwable -> {
                        log.error("Cluster error for alias {}: {}", alias, throwable.getMessage());
                        if (connectionStateCallback != null) {
                            connectionStateCallback.accept(false);
                        }
                    });

            this.aeronCluster = AeronCluster.connect(clusterCtx);
            
            if (aeronCluster != null && connectionStateCallback!=null) {
                    connectionStateCallback.accept(true);
            }

            log.info("Connected to cluster with alias {}", alias);
        } catch (Exception e) {
            log.error("Failed to connect to cluster with alias {}", alias, e);
            if (connectionStateCallback != null) {
                connectionStateCallback.accept(false);
            }
        }
    }

    /**
     * Check the connection is still running and set the reconnecting flag.
     */
    private void checkConnection() {
        if ((aeronCluster == null || aeronCluster.isClosed()) && !reconnecting.get()) {
            if (reconnecting.compareAndSet(false, true)) {
                try {
                    log.info("Cluster connection lost for alias {}. Attempting to reconnect...", alias);
                    connect();
                } finally {
                    reconnecting.set(false);
                }
            }
        }
    }

    @Override
    public void sendKeepAlive() {
        checkConnection();
        if (aeronCluster != null && !aeronCluster.isClosed()) {
            aeronCluster.sendKeepAlive();
        }
    }

    @Override
    public int pollEgress() {
        checkConnection();
        if (aeronCluster != null && !aeronCluster.isClosed()) {
            return aeronCluster.pollEgress();
        }
        return 0;
    }

    @Override
    public long offer(MutableDirectBuffer msgBuffer, int msgBufferOffset, int length) {
        checkConnection();
        if (aeronCluster != null && !aeronCluster.isClosed()) {
            long result = aeronCluster.offer(msgBuffer, msgBufferOffset, length);
            if (result == NOT_CONNECTED) {
                log.warn("Offer failed: NOT_CONNECTED on {}. Will check if we should reconnect.", alias);
            }
            return result;
        }
        return NOT_CONNECTED;
    }

    @Override
    public boolean isConnected() {
        return aeronCluster != null && !aeronCluster.isClosed();
    }

    @Override
    public void close() {
        log.info("Closing ReconnectingAeronCache on {}", alias);
        ClusterUtils.close(aeronCluster);
    }
}
