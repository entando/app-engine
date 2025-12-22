package org.entando.entando.aps.servlet;

import org.junit.jupiter.api.Test;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IFSuppressNIOExceptionTest {

    @Test
    void shouldDetectBrokenPipeInIOException() {
        IOException exception = new IOException("Broken pipe");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectBrokenPipeCaseInsensitive() {
        IOException exception = new IOException("BROKEN PIPE");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectBrokenPipeInMixedCase() {
        IOException exception = new IOException("Error: BrOkEn PiPe occurred");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectConnectionReset() {
        IOException exception = new IOException("Connection reset");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectConnectionResetCaseInsensitive() {
        IOException exception = new IOException("CONNECTION RESET");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectConnectionResetByPeer() {
        IOException exception = new IOException("Connection reset by peer");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldNotDetectOtherIOException() {
        IOException exception = new IOException("File not found");
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldNotDetectNonIOException() {
        RuntimeException exception = new RuntimeException("Broken pipe");
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectBrokenPipeInNestedIOException() {
        IOException ioException = new IOException("Broken pipe");
        RuntimeException wrappedException = new RuntimeException("Wrapper", ioException);
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(wrappedException));
    }

    @Test
    void shouldDetectConnectionResetInNestedIOException() {
        IOException ioException = new IOException("Connection reset");
        RuntimeException wrappedException = new RuntimeException("Wrapper", ioException);
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(wrappedException));
    }

    @Test
    void shouldDetectBrokenPipeInDeeplyNestedIOException() {
        IOException ioException = new IOException("Broken pipe");
        RuntimeException level1 = new RuntimeException("Level 1", ioException);
        RuntimeException level2 = new RuntimeException("Level 2", level1);
        RuntimeException level3 = new RuntimeException("Level 3", level2);
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(level3));
    }

    @Test
    void shouldHandleIOExceptionWithNullMessage() {
        IOException exception = new IOException((String) null);
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldHandleNullCause() {
        RuntimeException exception = new RuntimeException("Some error");
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldHandleExceptionWithNullMessageInChain() {
        IOException ioException = new IOException((String) null);
        RuntimeException wrappedException = new RuntimeException("Wrapper", ioException);
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(wrappedException));
    }

    @Test
    void shouldNotDetectSimilarButDifferentMessages() {
        IOException exception1 = new IOException("pipe is broken");
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception1));

        IOException exception2 = new IOException("connection was reset");
        assertFalse(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception2));
    }

    @Test
    void shouldDetectWhenMessageContainsBrokenPipeAsSubstring() {
        IOException exception = new IOException("Error writing to stream: broken pipe error");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldDetectWhenMessageContainsConnectionResetAsSubstring() {
        IOException exception = new IOException("Network error: connection reset occurred");
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(exception));
    }

    @Test
    void shouldHandleComplexExceptionChainWithMultipleIOExceptions() {
        IOException innerMost = new IOException("Some other error");
        IOException middle = new IOException("Connection reset", innerMost);
        RuntimeException outer = new RuntimeException("Wrapper", middle);
        assertTrue(IFSuppressNIOException.isBrokenPipeOrConnectionReset(outer));
    }

    @Test
    void isEnabledShouldReturnFeatureFlagValue() {
        TestImplementation implementation = new TestImplementation();
        boolean result = implementation.isEnabled();
        assertEquals(IFSuppressNIOException.SUPPRESS_NIO_EXCEPTIONS, result);
    }

    private static class TestImplementation implements IFSuppressNIOException {
    }
}