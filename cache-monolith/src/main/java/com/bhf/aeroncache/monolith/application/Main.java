package com.bhf.aeroncache.monolith.application;


import com.bhf.aeroncache.application.ClusterLauncher;
import com.bhf.aeroncache.clustertools.application.ClusterToolsHTTPApplication;
import com.bhf.aeroncache.http.application.HttpApplication;
import com.bhf.aeroncache.sse.application.SSEApplication;
import com.bhf.aeroncache.utils.ClusterUtils;
import com.bhf.aeroncache.ws.application.WebsocketApplication;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    static void main() {
        String aeronDirectory = "/dev/shm/aeron/";
        try (var mediaDriver = ClusterUtils.launchEmbeddedMediaDriver(aeronDirectory);
             final ShutdownSignalBarrier barrier = new ShutdownSignalBarrier()) {
            System.out.println("Launching single node Aeron Cache cluster");
            ClusterLauncher.main(new String[]{});

            System.out.println("Launching HTTP ClusterTools");
            ClusterToolsHTTPApplication.main(null);
            int clusterToolsPort = ClusterToolsHTTPApplication.BOUND_PORT;

            System.out.println("Launching HTTP interface");
            HttpApplication.main(null);
            int httpPort = HttpApplication.BOUND_PORT;

            System.out.println("Launching WS interface");
            WebsocketApplication.main(null);
            int wsPort = WebsocketApplication.BOUND_PORT;

            System.out.println("Launching SSE interface");
            SSEApplication.main(null);
            int ssePort = SSEApplication.BOUND_PORT;

            generateUIConfig(httpPort, clusterToolsPort, wsPort, ssePort);
            barrier.await();
        }
    }


    private static void generateUIConfig(int httpPort, int clusterToolsPort, int wsPort, int ssePort) {
        String template =
                "AERON_CACHE_API=http://localhost:" + httpPort + "/api/v1\n" +
                "AERON_CACHE_NEAR_API=http://localhost:7073/api/v1/near\n" +
                "AERON_CACHE_WS_API=ws:localhost:" + wsPort + "\n" +
                "JAEGER=http://localhost:16686\n" +
                "PROMETHEUS=http://localhost:9090\n" +
                "AERON_CACHE_SSE_API=http://localhost:" + ssePort;

        try {
            Files.write(Paths.get("aeron-cache-ui.env"), template.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
