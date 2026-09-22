package com.bhf.aeroncache.monolith.application;


import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication;
import com.bhf.aeroncache.gateway.application.GatewayApplication;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.sse.application.SSEApplication;
import com.bhf.aeroncache.transport.TransportMedia;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    static void main(String[] args) {
        String aeronDirectory = System.getProperty("aeron.dir", System.getenv().getOrDefault("AERON_DIR", "aeron"));

        // The monolith always runs its in-process components (the cluster node and the http/ws/sse
        // interfaces) with LAUNCH_EMBEDDED=false so they attach to a single shared media driver at
        // AERON_DIR rather than each launching their own. This flag controls who provides that
        // shared driver: true (default) launches an embedded driver in this process; false attaches
        // to an external media driver (the aeronmd sidecar) already running at AERON_DIR.
        boolean launchEmbeddedDriver = Boolean.parseBoolean(
                System.getProperty("monolith.embedded.driver",
                        System.getenv().getOrDefault("MONOLITH_EMBEDDED_DRIVER", "true")));

        boolean aeronEnabled = Boolean.parseBoolean(
                System.getProperty("aeron.gateway.enabled", System.getenv().getOrDefault("AERON_GATEWAY_ENABLED", "false")));

        boolean websocketEnabled = Boolean.parseBoolean(
                System.getProperty("websocket.gateway.enabled", System.getenv().getOrDefault("WEBSOCKET_GATEWAY_ENABLED", "true")));

        boolean sseEnabled = Boolean.parseBoolean(
                System.getProperty("sse.gateway.enabled", System.getenv().getOrDefault("SSE_GATEWAY_ENABLED", "true")));

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

        System.out.println("Launching Aeron Cache Monolith with the following configuration: aeronEnabled="
                + aeronEnabled + ", websocketEnabled=" + websocketEnabled + ", sseEnabled=" + sseEnabled);

        System.out.println("Launching with CluserToolsPort: " + toolsPort + ", HttpPort: " + httpInterfacePort
                + ", WSPort: " + wsInterfacePort + ", SSEPort:" + sseInterfacePort);

        System.out.println(launchEmbeddedDriver
                ? "Launching embedded Aeron media driver at " + aeronDirectory
                : "Attaching to external Aeron media driver at " + aeronDirectory + " (MONOLITH_EMBEDDED_DRIVER=false)");

        try (var mediaDriver = launchEmbeddedDriver ? ClusterUtils.launchEmbeddedMediaDriver(aeronDirectory) : null;
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

            int wsPort = 0;
            if(websocketEnabled) {
                System.out.println("Launching WS interface");
                WebsocketApplication.setDEFAULT_WS_PORT(wsInterfacePort);
                WebsocketApplication.main(null);
                wsPort = WebsocketApplication.BOUND_PORT;
            }
            else{
                System.out.println("Websocket interface disabled");
            }

            int ssePort = 0;

            if (sseEnabled) {
                System.out.println("Launching SSE interface");
                SSEApplication.setDEFAULT_SSE_PORT(sseInterfacePort);
                SSEApplication.main(null);
                ssePort = SSEApplication.BOUND_PORT;
            } else {
                System.out.println("SSE interface disabled");
            }

            if (aeronEnabled) {
                TransportMedia gatewayMedia = TransportMedia.fromEnv();
                System.out.println("Launching Aeron Gateway using " + gatewayMedia.media().toUpperCase()
                        + " transport (set " + TransportMedia.MEDIA_ENV + "=ipc|udp to change; default udp)");
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
