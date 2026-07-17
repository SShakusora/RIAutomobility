package com.sshakusora.riautomobility.carpack;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes reads and writes of the shared car-pack catalog across threads and server JVMs.
 */
public final class CarPackDirectoryLock {
    public static final String LOCK_FILE_NAME = ".riautomobility.lock";
    private static final ReentrantLock JVM_LOCK = new ReentrantLock(true);

    private CarPackDirectoryLock() {
    }

    public static <T> T withExclusive(Path directory, CheckedSupplier<T> action) throws Exception {
        JVM_LOCK.lockInterruptibly();
        try {
            Files.createDirectories(directory);
            Path lockFile = directory.resolve(LOCK_FILE_NAME);
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                return action.get();
            }
        } finally {
            JVM_LOCK.unlock();
        }
    }

    public static void withExclusive(Path directory, CheckedAction action) throws Exception {
        withExclusive(directory, () -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface CheckedAction {
        void run() throws Exception;
    }
}
