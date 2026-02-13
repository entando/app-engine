package org.entando.entando.plugins.jpredis.utils;

import org.entando.entando.aps.system.services.cache.RedisEnvironmentVariables;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opentest4j.TestAbortedException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

public class RedisTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final int REDIS_PORT = 6379;
    private static final String REDIS_IMAGE = "redis:7";

    private static GenericContainer redisContainer;

    private static MockedStatic<RedisEnvironmentVariables> mockedRedisEnvironment;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new TestAbortedException("Docker is not available");
        }
        if (redisContainer == null || !redisContainer.isRunning()) {
            GenericContainer container = new GenericContainer(REDIS_IMAGE).withExposedPorts(REDIS_PORT);
            container.start();
            redisContainer = container;
        }
        mockedRedisEnvironment = Mockito.mockStatic(RedisEnvironmentVariables.class);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.active()).thenReturn(true);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.redisAddress())
                .thenReturn("redis://localhost:" + redisContainer.getMappedPort(REDIS_PORT));
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.ioThreadPoolSize()).thenReturn(8);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (mockedRedisEnvironment != null) {
            mockedRedisEnvironment.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(GenericContainer.class)
                || parameterContext.getParameter().getType().equals(MockedStatic.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(GenericContainer.class)) {
            return redisContainer;
        } else if (parameterContext.getParameter().getType().equals(MockedStatic.class)) {
            return mockedRedisEnvironment;
        }
        return null;
    }
}
