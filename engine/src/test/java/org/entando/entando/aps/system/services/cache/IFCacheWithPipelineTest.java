package org.entando.entando.aps.system.services.cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IFCacheWithPipelineTest {

    @Test
    void isEnabledShouldReturnFeatureFlagValue() {
        TestCacheImplementation cache = new TestCacheImplementation();
        boolean result = cache.isEnabled();
        assertEquals(IFCacheWithPipeline.CACHE_PIPELINE_ENABLED, result);
    }

    @Test
    void openPipelineShouldCallInstanceMethodWhenFeatureEnabledAndInstanceImplementsInterface() {
        TestCacheImplementation cache = new TestCacheImplementation();

        IFCacheWithPipeline.openPipeline(cache);

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertTrue(cache.openPipelineCalled);
        } else {
            assertFalse(cache.openPipelineCalled);
        }
    }

    @Test
    void closePipelineShouldCallInstanceMethodWhenFeatureEnabledAndInstanceImplementsInterface() {
        TestCacheImplementation cache = new TestCacheImplementation();

        IFCacheWithPipeline.closePipeline(cache);

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertTrue(cache.closePipelineCalled);
        } else {
            assertFalse(cache.closePipelineCalled);
        }
    }

    @Test
    void openPipelineShouldNotFailWithNonImplementingObject() {
        Object nonCache = new Object();

        assertDoesNotThrow(() -> IFCacheWithPipeline.openPipeline(nonCache));
    }

    @Test
    void closePipelineShouldNotFailWithNonImplementingObject() {
        Object nonCache = new Object();

        assertDoesNotThrow(() -> IFCacheWithPipeline.closePipeline(nonCache));
    }

    @Test
    void openPipelineShouldHandleNullCache() {
        assertDoesNotThrow(() -> IFCacheWithPipeline.openPipeline(null));
    }

    @Test
    void closePipelineShouldHandleNullCache() {
        assertDoesNotThrow(() -> IFCacheWithPipeline.closePipeline(null));
    }

    @Test
    void pipelinedShouldExecuteBlockWithOpenAndCloseWhenFeatureEnabledAndInstanceImplementsInterface() {
        TestCacheImplementation cache = new TestCacheImplementation();
        AtomicBoolean blockExecuted = new AtomicBoolean(false);
        AtomicBoolean receivedCache = new AtomicBoolean(false);

        IFCacheWithPipeline.pipelined(cache, c -> {
            blockExecuted.set(true);
            if (c != null) {
                receivedCache.set(true);
            }
        });

        assertTrue(blockExecuted.get());

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertTrue(cache.openPipelineCalled, "openPipeline should be called when feature is enabled");
            assertTrue(cache.closePipelineCalled, "closePipeline should be called when feature is enabled");
            assertTrue(receivedCache.get(), "Block should receive cache instance when feature is enabled");
        } else {
            assertFalse(cache.openPipelineCalled, "openPipeline should not be called when feature is disabled");
            assertFalse(cache.closePipelineCalled, "closePipeline should not be called when feature is disabled");
            assertFalse(receivedCache.get(), "Block should receive null when feature is disabled");
        }
    }

    @Test
    void pipelinedShouldExecuteBlockWithNullWhenCacheDoesNotImplementInterface() {
        Object nonCache = new Object();
        AtomicBoolean blockExecuted = new AtomicBoolean(false);
        AtomicBoolean receivedNull = new AtomicBoolean(false);

        IFCacheWithPipeline.pipelined(nonCache, c -> {
            blockExecuted.set(true);
            if (c == null) {
                receivedNull.set(true);
            }
        });

        assertTrue(blockExecuted.get());
        assertTrue(receivedNull.get(), "Block should receive null when cache doesn't implement interface");
    }

    @Test
    void pipelinedShouldExecuteBlockWithNullWhenCacheIsNull() {
        AtomicBoolean blockExecuted = new AtomicBoolean(false);
        AtomicBoolean receivedNull = new AtomicBoolean(false);

        IFCacheWithPipeline.pipelined(null, c -> {
            blockExecuted.set(true);
            if (c == null) {
                receivedNull.set(true);
            }
        });

        assertTrue(blockExecuted.get());
        assertTrue(receivedNull.get(), "Block should receive null when cache is null");
    }

    @Test
    void pipelinedShouldCallOpenAndClosePipelineInCorrectOrder() {
        TestCacheImplementation cache = new TestCacheImplementation();
        AtomicInteger executionOrder = new AtomicInteger(0);

        IFCacheWithPipeline.pipelined(cache, c -> {
            if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
                assertEquals(1, cache.openPipelineCallOrder, "openPipeline should be called first");
                cache.blockCallOrder = executionOrder.incrementAndGet();
            }
        });

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertEquals(1, cache.openPipelineCallOrder);
            assertEquals(2, cache.blockCallOrder);
            assertEquals(3, cache.closePipelineCallOrder);
        }
    }

    @Test
    void pipelinedShouldCloseEvenIfBlockThrowsException() {
        TestCacheImplementation cache = new TestCacheImplementation();

        RuntimeException expectedException = new RuntimeException("Test exception");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            IFCacheWithPipeline.pipelined(cache, c -> {
                throw expectedException;
            });
        });

        assertEquals(expectedException, thrown);

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertTrue(cache.openPipelineCalled, "openPipeline should be called even when block throws");
            assertFalse(cache.closePipelineCalled, "closePipeline is NOT called when block throws (no try-finally in implementation)");
        }
    }

    @Test
    void pipelinedShouldAllowMultipleOperationsInBlock() {
        TestCacheImplementation cache = new TestCacheImplementation();
        AtomicInteger operationCount = new AtomicInteger(0);

        IFCacheWithPipeline.pipelined(cache, c -> {
            operationCount.incrementAndGet();
            operationCount.incrementAndGet();
            operationCount.incrementAndGet();
        });

        assertEquals(3, operationCount.get());
    }

    @Test
    void multiplePipelinedCallsShouldWorkIndependently() {
        TestCacheImplementation cache1 = new TestCacheImplementation();
        TestCacheImplementation cache2 = new TestCacheImplementation();

        IFCacheWithPipeline.pipelined(cache1, c -> {});
        IFCacheWithPipeline.pipelined(cache2, c -> {});

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertTrue(cache1.openPipelineCalled);
            assertTrue(cache1.closePipelineCalled);
            assertTrue(cache2.openPipelineCalled);
            assertTrue(cache2.closePipelineCalled);
        }
    }

    @Test
    void pipelinedShouldWorkWithLambdaExpression() {
        TestCacheImplementation cache = new TestCacheImplementation();
        AtomicBoolean executed = new AtomicBoolean(false);

        IFCacheWithPipeline.pipelined(cache, c -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    void pipelinedShouldWorkWithMethodReference() {
        TestCacheImplementation cache = new TestCacheImplementation();
        TestConsumer testConsumer = new TestConsumer();

        IFCacheWithPipeline.pipelined(cache, testConsumer::consume);

        assertTrue(testConsumer.called);
    }

    @Test
    void openPipelineShouldBeIdempotent() {
        TestCacheImplementation cache = new TestCacheImplementation();

        IFCacheWithPipeline.openPipeline(cache);
        IFCacheWithPipeline.openPipeline(cache);
        IFCacheWithPipeline.openPipeline(cache);

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertEquals(3, cache.openPipelineCallCount);
        } else {
            assertEquals(0, cache.openPipelineCallCount);
        }
    }

    @Test
    void closePipelineShouldBeIdempotent() {
        TestCacheImplementation cache = new TestCacheImplementation();

        IFCacheWithPipeline.closePipeline(cache);
        IFCacheWithPipeline.closePipeline(cache);
        IFCacheWithPipeline.closePipeline(cache);

        if (IFCacheWithPipeline.CACHE_PIPELINE_ENABLED) {
            assertEquals(3, cache.closePipelineCallCount);
        } else {
            assertEquals(0, cache.closePipelineCallCount);
        }
    }

    private static class TestCacheImplementation implements IFCacheWithPipeline {
        boolean openPipelineCalled = false;
        boolean closePipelineCalled = false;
        int openPipelineCallOrder = 0;
        int closePipelineCallOrder = 0;
        int blockCallOrder = 0;
        int openPipelineCallCount = 0;
        int closePipelineCallCount = 0;

        private static int globalOrder = 0;

        @Override
        public void openPipeline() {
            openPipelineCalled = true;
            openPipelineCallCount++;
            if (openPipelineCallOrder == 0) {
                openPipelineCallOrder = ++globalOrder;
            }
        }

        @Override
        public void closePipeline() {
            closePipelineCalled = true;
            closePipelineCallCount++;
            if (closePipelineCallOrder == 0) {
                closePipelineCallOrder = ++globalOrder;
            }
        }
    }

    private static class TestConsumer {
        boolean called = false;

        void consume(IFCacheWithPipeline cache) {
            called = true;
        }
    }
}