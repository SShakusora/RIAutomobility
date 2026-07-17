package com.sshakusora.riautomobility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {
    @TempDir
    Path gameDirectory;

    @Test
    void emptySettingUsesTheLegacyLocalDirectory() {
        assertEquals(gameDirectory.resolve("riautomobility").toAbsolutePath().normalize(),
                Config.resolveServerCarPackDirectory(gameDirectory, ""));
    }

    @Test
    void relativeSettingIsResolvedAgainstTheGameDirectory() {
        assertEquals(gameDirectory.resolve("../shared/packs").toAbsolutePath().normalize(),
                Config.resolveServerCarPackDirectory(gameDirectory, "../shared/packs"));
    }

    @Test
    void absoluteSettingIsPreserved() {
        Path shared = gameDirectory.resolve("shared").toAbsolutePath();
        assertEquals(shared.normalize(), Config.resolveServerCarPackDirectory(gameDirectory, shared.toString()));
    }

    @Test
    void invalidSettingReportsAConfigurationError() {
        assertThrows(IllegalStateException.class,
                () -> Config.resolveServerCarPackDirectory(gameDirectory, "bad\0path"));
    }
}
