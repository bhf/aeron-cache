/*
 * Copyright 2014-2024 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bhf.aeroncache.services;

import com.bhf.aeroncache.messages.CreateCacheDecoder;
import com.bhf.aeroncache.messages.CreateCacheEncoder;
import com.bhf.aeroncache.messages.MessageHeaderDecoder;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.MutableBoolean;
import org.agrona.concurrent.IdleStrategy;

import java.util.Objects;

/**
 * Auction service implementing the business logic.
 */
// tag::new_service[]
public class BasicClusteredService implements ClusteredService
// end::new_service[]
{
    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    private final MutableDirectBuffer snapshotBuffer = new ExpandableArrayBuffer();
    private Cluster cluster;
    private IdleStrategy idleStrategy;


    public void onStart(final Cluster cluster, final Image snapshotImage) {
        this.cluster = cluster;
        this.idleStrategy = cluster.idleStrategy();

        if (null != snapshotImage) {
            loadSnapshot(cluster, snapshotImage);
        }
    }


    public void onSessionMessage(final ClientSession session, final long timestamp, final DirectBuffer buffer, final int offset, final int length, final Header header) {
        headerDecoder.wrap(buffer, offset);
        final int templateId = headerDecoder.templateId();

        System.out.println("Got templateId=" + templateId);

        if (templateId == CreateCacheEncoder.TEMPLATE_ID) {
            System.out.println("Got create cache message");
            CreateCacheDecoder decoder = new CreateCacheDecoder();
            decoder.wrapAndApplyHeader(buffer, offset, headerDecoder);
            long cacheName = decoder.cacheName();
            System.out.println("Create on " + cacheName);
        }
    }

    void sendMessage(final ClientSession session, MutableDirectBuffer msgBuffer, int len) {
        while (session.offer(msgBuffer, 0, len) < 0)
        {
            idleStrategy.idle();                                                                 // <6>
        }
    }

    public void onTakeSnapshot(final ExclusivePublication snapshotPublication) {

        /*idleStrategy.reset();
        while (snapshotPublication.offer(snapshotBuffer, 0, SNAPSHOT_MESSAGE_LENGTH) < 0)            // <2>
        {
            idleStrategy.idle();
        }*/
    }

    private void loadSnapshot(final Cluster cluster, final Image snapshotImage) {
        final MutableBoolean isAllDataLoaded = new MutableBoolean(false);
        final FragmentHandler fragmentHandler = (buffer, offset, length, header) ->         // <1>
        {

            isAllDataLoaded.set(true);
        };

        while (!snapshotImage.isEndOfStream())                                              // <4>
        {
            final int fragmentsPolled = snapshotImage.poll(fragmentHandler, 1);

            if (isAllDataLoaded.value)                                                      // <5>
            {
                break;
            }

            idleStrategy.idle(fragmentsPolled);                                             // <6>
        }

        assert snapshotImage.isEndOfStream();                                               // <7>
        assert isAllDataLoaded.value;
    }

    public void onRoleChange(final Cluster.Role newRole) {
    }

    /**
     * {@inheritDoc}
     */
    public void onTerminate(final Cluster cluster) {
    }

    /**
     * {@inheritDoc}
     */
    public void onSessionOpen(final ClientSession session, final long timestamp) {
        System.out.println("onSessionOpen(" + session + ")");
    }

    /**
     * {@inheritDoc}
     */
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason) {
        System.out.println("onSessionClose(" + session + ")");
    }

    /**
     * {@inheritDoc}
     */
    public void onTimerEvent(final long correlationId, final long timestamp) {
    }

}
