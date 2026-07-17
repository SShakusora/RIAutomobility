package com.sshakusora.riautomobility.carpack;

import com.mojang.logging.LogUtils;
import com.sshakusora.riautomobility.Config;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Detects shared-directory changes and reloads this server without blocking its tick thread on I/O. */
public final class CarPackSharedDirectoryMonitor {
    public static final String REVISION_FILE_NAME = ".riautomobility.revision";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean POLL_RUNNING = new AtomicBoolean();
    private static volatile MinecraftServer activeServer;
    private static volatile DirectoryState knownState;
    private static ExecutorService executor;

    private CarPackSharedDirectoryMonitor() {
    }

    public static synchronized void start(MinecraftServer server, DirectoryState initialState) {
        stopInternal();
        activeServer = server;
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "RIAutomobility shared car pack monitor");
            thread.setDaemon(true);
            return thread;
        });
        knownState = initialState;
        LOGGER.info("Monitoring RIAutomobility car packs in {} every {} second(s)",
                CarPackManager.getServerCarPackDirectory(), Config.getSharedCarPackScanIntervalSeconds());
    }

    public static void tick(MinecraftServer server) {
        int intervalTicks = Config.getSharedCarPackScanIntervalSeconds() * 20;
        if (server != activeServer || server.getTickCount() % intervalTicks != 0 || !POLL_RUNNING.compareAndSet(false, true)) {
            return;
        }
        ExecutorService currentExecutor;
        synchronized (CarPackSharedDirectoryMonitor.class) {
            currentExecutor = executor;
        }
        if (currentExecutor == null) {
            POLL_RUNNING.set(false);
            return;
        }
        currentExecutor.execute(() -> poll(server));
    }

    private static void poll(MinecraftServer server) {
        try {
            Path directory = CarPackManager.getServerCarPackDirectory();
            CarPackDirectoryLock.withExclusive(directory, () -> {
                if (server != activeServer) {
                    return;
                }
                DirectoryState current = DirectoryState.capture(directory);
                if (current.equals(knownState)) {
                    return;
                }
                LOGGER.info("Detected a change in shared RIAutomobility car packs; reloading this server");
                CarPackRuntime.reloadServerAndSync(server);
                knownState = DirectoryState.capture(directory);
                LOGGER.info("Reloaded shared RIAutomobility car packs and synchronized online players");
            });
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            LOGGER.error("Unable to reload changed shared RIAutomobility car packs; the previous catalog remains active",
                    exception);
        } finally {
            POLL_RUNNING.set(false);
        }
    }

    /** Avoids an unnecessary second reload in the server process that published the change. */
    public static void acknowledgeLocalChange(Path directory) {
        if (activeServer == null || !CarPackManager.getServerCarPackDirectory().equals(directory)) {
            return;
        }
        try {
            knownState = DirectoryState.capture(directory);
        } catch (IOException exception) {
            LOGGER.debug("Unable to update the local shared car pack directory state", exception);
        }
    }

    public static synchronized void stop(MinecraftServer server) {
        if (server == activeServer) {
            stopInternal();
        }
    }

    private static void stopInternal() {
        activeServer = null;
        knownState = null;
        POLL_RUNNING.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    record DirectoryState(String revision, List<PackFileState> packs) {
        DirectoryState {
            packs = List.copyOf(packs);
        }

        static DirectoryState capture(Path directory) throws IOException {
            Files.createDirectories(directory);
            List<PackFileState> packs = new ArrayList<>();
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
                for (Path entry : entries) {
                    String name = entry.getFileName().toString();
                    if (!name.toLowerCase(Locale.ROOT).endsWith(CarPackManager.CAR_PACK_EXTENSION)
                            || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                        continue;
                    }
                    BasicFileAttributes attributes = Files.readAttributes(
                            entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    packs.add(new PackFileState(name, attributes.size(), attributes.lastModifiedTime()));
                }
            }
            packs.sort(Comparator.comparing(PackFileState::name));
            Path revisionFile = directory.resolve(REVISION_FILE_NAME);
            String revision = Files.isRegularFile(revisionFile, LinkOption.NOFOLLOW_LINKS)
                    && Files.size(revisionFile) <= 4096L ? Files.readString(revisionFile) : "";
            return new DirectoryState(revision, packs);
        }
    }

    record PackFileState(String name, long size, FileTime modified) {
    }
}
