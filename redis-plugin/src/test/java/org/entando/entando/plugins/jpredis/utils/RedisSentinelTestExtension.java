package org.entando.entando.plugins.jpredis.utils;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
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
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class RedisSentinelTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final int REDIS_PORT = 6379;
    private static final int REDIS_SENTINEL_PORT = 26379;

    public static final String REDIS_SERVICE = "redis";
    public static final String REDIS_SLAVE_SERVICE = "redis-slave";
    public static final String REDIS_SENTINEL_SERVICE = "redis-sentinel";

    private static DockerComposeContainer composeContainer;
    private static boolean composeStarted = false;

    private MockedStatic<RedisEnvironmentVariables> mockedRedisEnvironment;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new TestAbortedException("Docker is not available");
        }
        if (!composeStarted) {
            DockerComposeContainer container = new DockerComposeContainer(new File("docker-compose-sentinel.yaml"))
                    .withExposedService(REDIS_SERVICE, REDIS_PORT,
                            Wait.forLogMessage(".*Ready to accept connections.*", 1)
                                    .withStartupTimeout(Duration.ofMinutes(1)))
                    .withExposedService(REDIS_SLAVE_SERVICE, REDIS_PORT,
                            Wait.forLogMessage(".*Ready to accept connections.*", 1)
                                    .withStartupTimeout(Duration.ofMinutes(1)))
                    .withExposedService(REDIS_SENTINEL_SERVICE, REDIS_SENTINEL_PORT,
                            Wait.forLogMessage(".*Sentinel ID is.*", 1)
                                    .withStartupTimeout(Duration.ofMinutes(1)));

            container.start();
            composeContainer = container;
            composeStarted = true;
        }
        mockedRedisEnvironment = Mockito.mockStatic(RedisEnvironmentVariables.class);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.active()).thenReturn(true);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.sentinelActive()).thenReturn(true);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.redisMasterName()).thenReturn("mymaster");
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.redisPassword()).thenReturn("str0ng_passw0rd");
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.frontEndCacheCheckDelay()).thenReturn(100);
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.redisAddresses())
                .thenReturn(String.join(",", Collections.nCopies(2, "redis://localhost:" +
                        composeContainer.getServicePort(REDIS_SENTINEL_SERVICE, REDIS_SENTINEL_PORT))));
        mockedRedisEnvironment.when(() -> RedisEnvironmentVariables.ioThreadPoolSize()).thenReturn(8);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (mockedRedisEnvironment != null) {
            mockedRedisEnvironment.close();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public static @interface ServicePort {

        String value();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        String service = parameterContext.getParameter().getAnnotation(ServicePort.class).value();
        return List.of(REDIS_SERVICE, REDIS_SLAVE_SERVICE, REDIS_SENTINEL_SERVICE).contains(service);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        String service = parameterContext.getParameter().getAnnotation(ServicePort.class).value();
        switch (service) {
            case REDIS_SERVICE:
                return composeContainer.getServicePort(REDIS_SERVICE, REDIS_PORT);
            case REDIS_SLAVE_SERVICE:
                return composeContainer.getServicePort(REDIS_SLAVE_SERVICE, REDIS_PORT);
            case REDIS_SENTINEL_SERVICE:
                return composeContainer.getServicePort(REDIS_SENTINEL_SERVICE, REDIS_SENTINEL_PORT);
        }
        return null;
    }
}
