package com.sshakusora.riautomobility.editor.upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CarPackUploadServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void commitsNewPackAndRemovesBackupAfterApplySucceeds() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");

        CarPackUploadService.installAtomically(source, target, true, () -> {}, () -> {
            throw new AssertionError("rollback must not run");
        });

        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(source));
        assertNoBackupFiles();
    }

    @Test
    void restoresOverwrittenPackWhenApplyFails() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");
        AtomicBoolean rolledBack = new AtomicBoolean();

        IOException error = assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, true,
                () -> { throw new IOException("reload failed"); },
                () -> rolledBack.set(true)
        ));

        assertEquals("reload failed", error.getMessage());
        assertEquals("old", Files.readString(target));
        assertFalse(Files.exists(source));
        assertTrue(rolledBack.get());
        assertNoBackupFiles();
    }

    @Test
    void removesNewPackWhenApplyFailsWithoutPreviousPack() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = temporaryDirectory.resolve("installed.riauto");

        assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, false,
                () -> { throw new IOException("reload failed"); },
                () -> {}
        ));

        assertFalse(Files.exists(target));
        assertNoBackupFiles();
    }

    @Test
    void leavesExistingPackUntouchedWhenOverwriteIsDisabled() throws Exception {
        Path source = write("upload.riauto", "new");
        Path target = write("installed.riauto", "old");

        assertThrows(IOException.class, () -> CarPackUploadService.installAtomically(
                source, target, false, () -> {}, () -> {}
        ));

        assertEquals("old", Files.readString(target));
        assertEquals("new", Files.readString(source));
    }

    private Path write(String name, String contents) throws IOException {
        return Files.writeString(temporaryDirectory.resolve(name), contents);
    }

    private void assertNoBackupFiles() throws IOException {
        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".backup")));
        }
    }
}
