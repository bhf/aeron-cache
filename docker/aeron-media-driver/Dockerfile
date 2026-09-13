# syntax=docker/dockerfile:1
#
# Aeron C media driver (aeronmd), built from source and run as a k8s sidecar.
#
# The driver version MUST match the io.aeron:aeron-all version the Java apps use
# (gradle/libs.versions.toml -> aeron). The Makefile extracts that version and
# passes it as --build-arg AERON_VERSION so the two never drift; the default here
# is only a fallback for a bare `docker build`.

ARG AERON_VERSION=1.50.0

# ---- build stage: compile aeronmd from the pinned Aeron source tag ----
FROM debian:bookworm AS build
ARG AERON_VERSION
# Aeron 1.50.0 requires CMake >= 3.30; bookworm ships 3.25, so pull a current CMake
# from PyPI (manylinux wheels cover amd64/arm64) rather than the apt package.
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        git \
        ca-certificates \
        python3-pip \
        zlib1g-dev \
        uuid-dev \
        libbsd-dev \
    && rm -rf /var/lib/apt/lists/* \
    && pip3 install --no-cache-dir --break-system-packages "cmake>=3.30"

WORKDIR /src
RUN git clone --depth 1 --branch "${AERON_VERSION}" https://github.com/real-logic/aeron.git .

# Build ONLY the C media driver target. The archive/cluster C++ APIs are the parts
# that pull in the Java/SBE codec generation, so they stay off and no JDK is needed.
RUN cmake -S . -B build \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_AERON_DRIVER=ON \
        -DBUILD_AERON_ARCHIVE_API=OFF \
        -DAERON_TESTS=OFF \
        -DAERON_SYSTEM_TESTS=OFF \
        -DAERON_BUILD_SAMPLES=OFF \
        -DAERON_BUILD_DOCUMENTATION=OFF \
    && cmake --build build --target aeronmd -j"$(nproc)"

# aeronmd is dynamically linked against libaeron_driver.so, so ship the binary and
# its Aeron shared libs. Locations have shifted across versions, so find rather than
# hard-code, and stage the libs so a matching aeron client dir isn't required.
RUN mkdir -p /out/bin /out/lib \
    && cp "$(find build -name aeronmd -type f | head -n1)" /out/bin/aeronmd \
    && find build -name 'libaeron*.so*' -exec cp -av {} /out/lib/ \; \
    && ls -l /out/lib

# ---- runtime stage: slim image with just the driver + entrypoint ----
FROM debian:bookworm-slim AS runtime
ARG AERON_VERSION
LABEL org.opencontainers.image.title="aeroncache-media-driver" \
      org.opencontainers.image.description="Aeron C media driver (aeronmd) sidecar for aeron-cache" \
      org.opencontainers.image.version="${AERON_VERSION}" \
      org.opencontainers.image.source="https://github.com/real-logic/aeron"

RUN apt-get update && apt-get install -y --no-install-recommends \
        libuuid1 \
        libbsd0 \
        zlib1g \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /out/bin/aeronmd /opt/aeron/aeronmd
COPY --from=build /out/lib/ /usr/local/lib/
COPY entrypoint.sh /opt/aeron/entrypoint.sh
RUN chmod +x /opt/aeron/aeronmd /opt/aeron/entrypoint.sh && ldconfig

# Run non-root. The pod securityContext must give the driver and the Java app the
# same runAsUser / fsGroup so both can open the CnC + term files in shared /dev/shm.
USER 1000:1000

ENTRYPOINT ["/opt/aeron/entrypoint.sh"]
