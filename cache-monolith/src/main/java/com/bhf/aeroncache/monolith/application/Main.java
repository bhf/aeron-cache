package com.bhf.aeroncache.monolith.application;


import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.sse.application.SSEApplication;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    static void main(String[] args) {
        String aeronDirectory = System.getProperty("aeron.dir", System.getenv().getOrDefault("AERON_DIR", "aeron"));
        boolean gatewayEnabled = Boolean.parseBoolean(
                System.getProperty("aeron.transport.gateway.enabled", System.getenv().getOrDefault("AERON_TRANSPORT_GATEWAY_ENABLED", "false")));

        int toolsPort = 0;
        int httpInterfacePort = 0;
        int wsInterfacePort = 0;
        int sseInterfacePort = 0;

        if (args.length == 4) {
            toolsPort = Integer.valueOf(args[0]);
            httpInterfacePort = Integer.valueOf(args[1]);
            wsInterfacePort = Integer.valueOf(args[2]);
            sseInterfacePort = Integer.valueOf(args[3]);
        }

        System.out.println("Launching with CluserToolsPort: " + toolsPort + ", HttpPort: " + httpInterfacePort
                + ", WSPort: " + wsInterfacePort + ", SSEPort:" + sseInterfacePort);

        try (var mediaDriver = ClusterUtils.launchEmbeddedMediaDriver(aeronDirectory);
             final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier()) {
            System.out.println("Launching single node Aeron Cache cluster");
            ClusterLauncher.main(new String[]{});

            System.out.println("Launching HTTP ClusterTools");
            ClusterToolsHTTPApplication.setPORT(toolsPort);
            ClusterToolsHTTPApplication.main(null);
            int clusterToolsPort = ClusterToolsHTTPApplication.BOUND_PORT;

            System.out.println("Launching HTTP interface");
            HttpApplication.setDEFAULT_HTTP_PORT(httpInterfacePort);
            HttpApplication.main(null);
            int httpPort = HttpApplication.BOUND_PORT;
            HttpApplication.setCLUSTER_TOOLS_PORT(clusterToolsPort);

            System.out.println("Launching WS interface");
            WebsocketApplication.setDEFAULT_WS_PORT(wsInterfacePort);
            WebsocketApplication.main(null);
            int wsPort = WebsocketApplication.BOUND_PORT;

            System.out.println("Launching SSE interface");
            SSEApplication.setDEFAULT_SSE_PORT(sseInterfacePort);
            SSEApplication.main(null);
            int ssePort = SSEApplication.BOUND_PORT;

            System.out.println("Launching Aeron Gateway");
            if (gatewayEnabled) {
                int gatewayHealthPort = GatewayApplication.start(0);
                System.out.println("Aeron Gateway HTTP health server bound to port " + gatewayHealthPort);
            } else {
                System.out.println("Aeron Gateway disabled");
            }

            generateUIConfig(httpPort, clusterToolsPort, wsPort, ssePort);
            barrier.await();
        }
    }


    private static void generateUIConfig(int httpPort, int clusterToolsPort, int wsPort, int ssePort) {
        String template =
                "AERON_CACHE_API=http://localhost:" + httpPort + "/api/v1\n" +
                "AERON_CACHE_WS_API=ws:localhost:" + wsPort + "\n" +
                "AERON_CACHE_SSE_API=http://localhost:" + ssePort;

        try {
            Files.write(Paths.get("aeron-cache-ui.env"), template.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
