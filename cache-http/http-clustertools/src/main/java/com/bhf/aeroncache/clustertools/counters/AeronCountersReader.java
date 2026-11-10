package com.bhf.aeroncache.clustertools.counters;

import com.bhf.aeroncache.clustertools.config.ClusterToolsConfig;
import com.bhf.aeroncache.http.responses.AeronCounter;
import com.bhf.aeroncache.http.responses.AeronCountersResponse;
import io.aeron.CncFileDescriptor;
import io.aeron.CommonContext;
import lombok.extern.log4j.Log4j2;
import org.agrona.DirectBuffer;
import org.agrona.IoUtil;
import org.agrona.concurrent.status.CountersReader;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class AeronCountersReader {

    private final ClusterToolsConfig config;

    public AeronCountersReader(final ClusterToolsConfig config) {
        this.config = config;
    }

    /**
     * Resolve the Aeron media driver directory this reader scrapes.
     *
     * @return the configured driver directory, or the Aeron default when unset.
     */
    public String aeronDirectoryName() {
        final String configured = config.getAeronDirectoryName();
        return configured != null ? configured : CommonContext.getAeronDirectoryName();
    }

    /**
     * Map the configured driver's CnC file and read a snapshot of all allocated counters.
     *
     * @return the scraped counters.
     * @throws RuntimeException if the CnC file does not exist, cannot be mapped, or its version is
     *                          incompatible with this Aeron version (e.g. the driver is not running).
     */
    public AeronCountersResponse read() {
        return read(aeronDirectoryName());
    }

    /**
     * Map the given driver's CnC file and read a snapshot of all allocated counters.
     *
     * @param aeronDirectoryName the Aeron media driver directory to scrape.
     * @return the scraped counters.
     * @throws RuntimeException if the CnC file does not exist, cannot be mapped, or its version is
     *                          incompatible with this Aeron version (e.g. the driver is not running).
     */
    public static AeronCountersResponse read(final String aeronDirectoryName) {
        final long now = System.currentTimeMillis();
        final File cncFile = new File(aeronDirectoryName, CncFileDescriptor.CNC_FILE);

        final MappedByteBuffer cncByteBuffer = IoUtil.mapExistingFile(cncFile, "cnc");
        try {
            final DirectBuffer cncMetaData = CncFileDescriptor.createMetaDataBuffer(cncByteBuffer);
            final int cncVersion = cncMetaData.getInt(CncFileDescriptor.cncVersionOffset(0));
            CncFileDescriptor.checkVersion(cncVersion);

            final CountersReader countersReader = new CountersReader(
                    CncFileDescriptor.createCountersMetaDataBuffer(cncByteBuffer, cncMetaData),
                    CncFileDescriptor.createCountersValuesBuffer(cncByteBuffer, cncMetaData));

            final List<AeronCounter> counters = new ArrayList<>();
            countersReader.forEach((counterId, typeId, keyBuffer, label) ->
                    counters.add(new AeronCounter(
                            counterId, typeId, countersReader.getCounterValue(counterId), label)));

            log.info("Scraped {} Aeron counters from {} (cnc version {})",
                    counters.size(), cncFile, cncVersion);

            return new AeronCountersResponse(
                    aeronDirectoryName, cncVersion, now, counters.size(), counters);
        } finally {
            IoUtil.unmap(cncByteBuffer);
        }
    }
}
