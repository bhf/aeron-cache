package com.bhf.aeroncache;

import com.bhf.aeroncache.messages.CreateCacheEncoder;
import com.bhf.aeroncache.messages.MessageHeaderEncoder;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.EventCode;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.bhf.aeroncache.ClusterNodeApplication.calculatePort;


/**
 * Client for connecting to the cluster.
 */
public class ClusterClientApplication implements EgressListener
{
    private final MutableDirectBuffer msgBuffer = new ExpandableArrayBuffer();
    private final IdleStrategy idleStrategy = new BackoffIdleStrategy();

    /**
     * {@inheritDoc}
     */
    public void onMessage(
        final long clusterSessionId,
        final long timestamp,
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final Header header)
    {


    }

    /**
     * {@inheritDoc}
     */
    public void onSessionEvent(
        final long correlationId,
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final EventCode code,
        final String detail)
    {
        printOutput(
            "SessionEvent(" + correlationId + ", " + leadershipTermId + ", " +
            leaderMemberId + ", " + code + ", " + detail + ")");
    }

    /**
     * {@inheritDoc}
     */
    public void onNewLeader(
        final long clusterSessionId,
        final long leadershipTermId,
        final int leaderMemberId,
        final String ingressEndpoints)
    {
        printOutput("NewLeader(" + clusterSessionId + ", " + leadershipTermId + ", " + leaderMemberId + ")");
    }

    private void sendMessageToCluster(final AeronCluster aeronCluster)
    {
        long keepAliveDeadlineMs = 0;
        long nextMsgDeadlineMs = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
        int msgsLeftToSend = 100;

        while (!Thread.currentThread().isInterrupted())
        {
            final long currentTimeMs = System.currentTimeMillis();

            if (nextMsgDeadlineMs <= currentTimeMs && msgsLeftToSend > 0)
            {
                nextMsgDeadlineMs = currentTimeMs + ThreadLocalRandom.current().nextInt(100);
                keepAliveDeadlineMs = currentTimeMs + 1_000;
                --msgsLeftToSend;

            }
            else if (keepAliveDeadlineMs <= currentTimeMs)
            {
                if (msgsLeftToSend > 0)
                {
                    aeronCluster.sendKeepAlive();
                    keepAliveDeadlineMs = currentTimeMs + 1_000;
                }
                else
                {
                    break;
                }
            }

            idleStrategy.idle(aeronCluster.pollEgress());
        }
    }

    private long sendBid(final AeronCluster aeronCluster)
    {
        CreateCacheEncoder createEncoder = new CreateCacheEncoder();
        MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        createEncoder.wrapAndApplyHeader(msgBuffer, 0, headerEncoder);
        createEncoder.cacheName(System.currentTimeMillis());

        idleStrategy.reset();
        while (aeronCluster.offer(msgBuffer, 0, createEncoder.encodedLength()+headerEncoder.encodedLength()) < 0)
        {
            idleStrategy.idle(aeronCluster.pollEgress());
        }

        return 0;
    }


    /**
     * Ingress endpoints generated from a list of hostnames.
     *
     * @param hostnames for the cluster members.
     * @return a formatted string of ingress endpoints for connecting to a cluster.
     */
    public static String ingressEndpoints(final List<String> hostnames)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++)
        {
            sb.append(i).append('=');
            sb.append(hostnames.get(i)).append(':').append(
                calculatePort(i, ClusterNodeApplication.CLIENT_FACING_PORT_OFFSET));
            sb.append(',');
        }

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    private void printOutput(final String message)
    {
        System.out.println("Client message {}"+message);
    }

    /**
     * Main method for launching the process.
     *
     * @param args passed to the process.
     */
    public static void main(final String[] args)
    {
        final String[] hostnames = System.getProperty(
            "aeron.cluster.tutorial.hostnames", "localhost,localhost,localhost").split(",");
        final String ingressEndpoints = ingressEndpoints(Arrays.asList(hostnames));

        final ClusterClientApplication client = new ClusterClientApplication();

        try (
            MediaDriver mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()                      // <1>
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true));
            AeronCluster aeronCluster = AeronCluster.connect(
                new AeronCluster.Context()
                .egressListener(client)                                                                         // <2>
                .egressChannel("aeron:udp?endpoint=localhost:0")                                                // <3>
                .aeronDirectoryName(mediaDriver.aeronDirectoryName())
                .ingressChannel("aeron:udp")                                                                    // <4>
                .ingressEndpoints(ingressEndpoints)))                                                           // <5>
        {

            client.sendMessageToCluster(aeronCluster);
        }
    }
}
