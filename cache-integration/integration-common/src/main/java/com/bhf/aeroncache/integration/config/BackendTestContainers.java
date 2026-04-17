package com.bhf.aeroncache.integration.config;

import org.testcontainers.containers.GenericContainer;

import java.util.List;

public record BackendTestContainers(List<GenericContainer<?>> clusterContainers, GenericContainer<?> httpContainer, GenericContainer<?> httpNearContainer, GenericContainer<?> wsContainer, GenericContainer<?> sseContainer) {
}
