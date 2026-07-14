package com.sshakusora.riautomobility.carpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarPackManagerTest {
    @Test
    void synchronizedClientResourcesKeepTheActiveEditorPreview() {
        CarPackManager.CarPack server = new CarPackManager.CarPack(
                "server", "Server", Path.of("server.riauto"), null, "server-digest");
        CarPackManager.CarPack preview = new CarPackManager.CarPack(
                "preview", "Preview", Path.of("preview.riauto"), null, "preview-digest");

        assertEquals(List.of(server, preview),
                CarPackManager.appendClientPreview(List.of(server), preview));
        assertEquals(List.of(server),
                CarPackManager.appendClientPreview(List.of(server), null));
    }
}
