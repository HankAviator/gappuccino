package io.github.hankaviator.gdialertweak;

import org.junit.Test;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class DisclosureAudioTest {
    public interface AudioFuture extends Future<Object> {
        void addListener(Runnable listener, Executor executor);
    }
    public static final class Provider implements AudioFuture {
        final CompletableFuture<Object> future = new CompletableFuture<>();
        public void addListener(Runnable listener, Executor executor) {
            future.whenComplete((value, error) -> executor.execute(listener));
        }
        public boolean cancel(boolean interrupt) { return future.cancel(interrupt); }
        public boolean isCancelled() { return future.isCancelled(); }
        public boolean isDone() { return future.isDone(); }
        public Object get() throws InterruptedException, ExecutionException { return future.get(); }
        public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }
    private static AudioFuture wrap(Provider original, AtomicBoolean enabled) {
        return (AudioFuture) DisclosureAudio.wrapFuture(AudioFuture.class, original,
                new File("silence.wav"), enabled::get);
    }

    @Test public void silenceIsValidPcmWithNonemptySilentSamples() throws Exception {
        File directory = Files.createTempDirectory("disclosure-audio").toFile();
        File file = DisclosureAudio.createSilence(directory);
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            ByteBuffer wav = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals("RIFF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
            assertEquals(bytes.length - 8, wav.getInt(4));
            assertEquals("WAVE", new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII));
            assertEquals(1, wav.getShort(20));
            assertEquals(1, wav.getShort(22));
            assertEquals(16000, wav.getInt(24));
            assertEquals(16, wav.getShort(34));
            assertEquals(3200, wav.getInt(40));
            for (int i = 44; i < bytes.length; i++) assertEquals(0, bytes[i]);
        } finally { Files.deleteIfExists(file.toPath()); Files.deleteIfExists(directory.toPath()); }
    }

    @Test public void providerStillControlsCompletionAndListeners() throws Exception {
        Provider original = new Provider();
        AudioFuture wrapped = wrap(original, new AtomicBoolean(true));
        AtomicBoolean notified = new AtomicBoolean();
        wrapped.addListener(() -> notified.set(true), Runnable::run);
        assertFalse(wrapped.isDone());
        assertThrows(TimeoutException.class, () -> wrapped.get(1, TimeUnit.MILLISECONDS));
        assertFalse(notified.get());
        original.future.complete("original.wav");
        assertTrue(notified.get());
        assertTrue(wrapped.isDone());
        assertEquals(new File("silence.wav").getAbsolutePath(), wrapped.get());
        assertEquals(new File("silence.wav").getAbsolutePath(), wrapped.get(1, TimeUnit.SECONDS));
    }

    @Test public void failuresAndCancellationAreNotReportedAsSuccess() {
        Provider failed = new Provider();
        IllegalStateException cause = new IllegalStateException("provider failed");
        failed.future.completeExceptionally(cause);
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> wrap(failed, new AtomicBoolean(true)).get());
        assertSame(cause, error.getCause());
        Provider cancelled = new Provider();
        AudioFuture wrapped = wrap(cancelled, new AtomicBoolean(true));
        assertTrue(wrapped.cancel(false));
        assertTrue(cancelled.isCancelled());
        assertTrue(wrapped.isCancelled());
        assertThrows(CancellationException.class, wrapped::get);
    }

    @Test public void disabledAndStructuredPayloadsStayOriginal() throws Exception {
        Provider original = new Provider();
        original.future.complete(new File("original.wav"));
        AtomicBoolean enabled = new AtomicBoolean(false);
        AudioFuture wrapped = wrap(original, enabled);
        assertEquals(new File("original.wav"), wrapped.get());
        enabled.set(true);
        assertEquals(new File("silence.wav"), wrapped.get());
        Object structuredAudio = new Object();
        Provider structured = new Provider();
        structured.future.complete(structuredAudio);
        assertSame(structuredAudio, wrap(structured, enabled).get());
        assertEquals(wrapped, wrapped);
    }
}
