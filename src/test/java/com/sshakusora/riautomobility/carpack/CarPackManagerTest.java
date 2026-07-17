package com.sshakusora.riautomobility.carpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarPackManagerTest {
    @TempDir
    Path directory;

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

    @Test
    void discoversPublishedPacksFromTheExplicitCatalogDirectoryOnly() throws IOException {
        Path archive = directory.resolve("shared-car.riauto");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            write(zip, "riauto.json", "{\"format\":1,\"id\":\"test:shared_car\"," +
                    "\"name\":\"Shared Car\",\"components\":{\"frames\":[\"test:shared_car\"]," +
                    "\"wheels\":[],\"engines\":[]},\"files\":{\"component.json\":" +
                    "\"data/test/riautomobility/frames/shared_car.json\"}}");
            write(zip, "component.json", "{}");
        }
        Files.writeString(directory.resolve("unfinished.part"), "not a pack");
        Files.createDirectories(directory.resolve("cache"));

        List<CarPackManager.CarPack> packs = CarPackManager.discoverCarPacks(directory);

        assertEquals(1, packs.size());
        assertEquals("riautomobility/shared-car", packs.get(0).id());
        assertEquals(archive, packs.get(0).path());
    }

    private static void write(ZipOutputStream zip, String name, String contents) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(contents.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
