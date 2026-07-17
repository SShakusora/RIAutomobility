package com.sshakusora.riautomobility.carpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CarPackSharedDirectoryMonitorTest {
    @TempDir
    Path directory;

    @Test
    void stateTracksOnlyPublishedPacksAndTheRevisionMarker() throws Exception {
        Files.writeString(directory.resolve("ignored.part"), "partial");
        CarPackSharedDirectoryMonitor.DirectoryState empty =
                CarPackSharedDirectoryMonitor.DirectoryState.capture(directory);
        assertEquals(0, empty.packs().size());

        Files.writeString(directory.resolve("vehicle.riauto"), "one");
        CarPackSharedDirectoryMonitor.DirectoryState published =
                CarPackSharedDirectoryMonitor.DirectoryState.capture(directory);
        assertNotEquals(empty, published);
        assertEquals(1, published.packs().size());

        Files.writeString(directory.resolve(CarPackSharedDirectoryMonitor.REVISION_FILE_NAME), "revision-2");
        CarPackSharedDirectoryMonitor.DirectoryState revised =
                CarPackSharedDirectoryMonitor.DirectoryState.capture(directory);
        assertNotEquals(published, revised);
        assertEquals("revision-2", revised.revision());
    }
}
