package io.github.hankaviator.gdialertweak;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.function.BooleanSupplier;

/** Keep playback and asynchronous completion intact; replace only the audio payload. */
final class DisclosureAudio {
    private DisclosureAudio() {}

    static File createSilence(File directory) throws IOException {
        // Valid 100 ms, 16 kHz, mono PCM WAV. An empty or corrupt file would fail playback.
        int samples = 1600;
        ByteBuffer wav = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        wav.put(new byte[]{'R', 'I', 'F', 'F'}).putInt(36 + samples * 2);
        wav.put(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '}).putInt(16);
        wav.putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000);
        wav.putShort((short) 2).putShort((short) 16);
        wav.put(new byte[]{'d', 'a', 't', 'a'}).putInt(samples * 2);
        File file = new File(directory, "gappuccino-recording-silence.wav");
        Files.write(file.toPath(), wav.array());
        return file;
    }

    static Object wrapFuture(Class<?> futureType, Object original, File silence,
                             BooleanSupplier enabled) {
        return Proxy.newProxyInstance(futureType.getClassLoader(), new Class<?>[]{futureType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> proxy == args[0];
                            case "hashCode" -> System.identityHashCode(proxy);
                            default -> "RecordingDisclosureFuture(" + original + ")";
                        };
                    }
                    Object value;
                    try { value = method.invoke(original, args); }
                    catch (InvocationTargetException error) { throw error.getCause(); }
                    // get(), including timed get(), still waits for the original provider.
                    // Exceptions, cancellation and listeners keep their original behavior.
                    if (method.getName().equals("get") && enabled.getAsBoolean()) {
                        if (value instanceof String) return silence.getAbsolutePath();
                        if (value instanceof File) return silence;
                    }
                    return value;
                });
    }
}
