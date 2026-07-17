package com.sshakusora.riautomobility.carpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CarPackDirectoryLockTest {
    @TempDir
    Path directory;

    @Test
    void serializesCatalogTransactionsWithinThisJvmAndCreatesTheOsLockFile() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> {
                CarPackDirectoryLock.withExclusive(directory, () -> {
                    firstEntered.countDown();
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
                });
                return null;
            });
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> {
                CarPackDirectoryLock.withExclusive(directory, secondEntered::countDown);
                return null;
            });

            assertFalse(secondEntered.await(150, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertTrue(Files.isRegularFile(directory.resolve(CarPackDirectoryLock.LOCK_FILE_NAME)));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }
}
