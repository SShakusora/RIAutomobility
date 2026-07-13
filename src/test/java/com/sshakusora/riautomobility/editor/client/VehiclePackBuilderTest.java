package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class VehiclePackBuilderTest {
    @TempDir
    Path temporaryDirectory;
    private static final String EMBEDDED_PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";

    @Test
    void acceptsEmbeddedPngTextures() {
        assertDoesNotThrow(() -> VehiclePackBuilder.validateEmbeddedTextures(document(EMBEDDED_PNG)));
    }

    @Test
    void rejectsExternalTexturePaths() {
        assertThrows(IOException.class, () -> VehiclePackBuilder.validateEmbeddedTextures(document("")));
    }

    @Test
    void rejectsModelsWithoutTextures() {
        BbModelData.Document document = new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(), List.of(), List.of());

        assertThrows(IOException.class, () -> VehiclePackBuilder.validateEmbeddedTextures(document));
    }

    @Test
    void generatesStableFormatUniqueComponentPaths() {
        String first = VehicleEditorDraft.generateComponentPath();
        String second = VehicleEditorDraft.generateComponentPath();

        assertEquals("riautomobility", VehicleEditorDraft.GENERATED_NAMESPACE);
        assertTrue(first.matches("auto_[0-9a-f]{32}"));
        assertTrue(second.matches("auto_[0-9a-f]{32}"));
        assertNotEquals(first, second);
    }

    @Test
    void convertsLegacySeatHeightPixelsIntoSeatYBlocks() {
        assertEquals(0.0D, VehicleEditorDraft.normalizedSeatYOffset(4.0F));
        assertEquals(0.5D, VehicleEditorDraft.normalizedSeatYOffset(12.0F));
        assertEquals(-0.0625D, VehicleEditorDraft.normalizedSeatYOffset(3.0F));
    }

    @Test
    void newSeatDefaultsToZeroHeight() {
        assertEquals(0.0D, VehicleEditorDraft.defaultSeatPosition().y);
    }

    @Test
    void firstPersonEyeUsesSelectedSeatAndPlayerEyeHeight() {
        var eye = VehicleEditorDraft.firstPersonEyePosition(
                new net.minecraft.world.phys.Vec3(0.25D, 0.5D, -0.75D), 4.0F, -0.35D, 1.62F);

        assertEquals(0.25D, eye.x);
        assertEquals(2.02D, eye.y, 1.0E-6D);
        assertEquals(-0.75D, eye.z);
    }

    @Test
    void passengerAndCameraShareTheSameEntityPosition() {
        var seat = new net.minecraft.world.phys.Vec3(-0.4D, 0.25D, 0.8D);
        var passenger = VehicleEditorDraft.passengerPosition(seat, 6.0F, -0.35D);
        var eye = VehicleEditorDraft.firstPersonEyePosition(seat, 6.0F, -0.35D, 1.62F);

        assertEquals(-0.4D, passenger.x);
        assertEquals(0.275D, passenger.y, 1.0E-6D);
        assertEquals(0.8D, passenger.z);
        assertEquals(passenger.x, eye.x);
        assertEquals(passenger.y + 1.62D, eye.y, 1.0E-6D);
        assertEquals(passenger.z, eye.z);
    }

    @Test
    void frameItemScaleMatchesAutomobilityRendererFormula() {
        assertEquals(1.0F / (28.0F / 16.0F * 0.77F), VehicleEditorDraft.frameItemScale(28.0F));
        assertTrue(VehicleEditorDraft.frameItemScale(16.0F) > VehicleEditorDraft.frameItemScale(32.0F));
    }

    @Test
    void mirrorsNewWheelPositionAcrossTheVehicle() {
        VehicleEditorDraft.WheelPoint left = new VehicleEditorDraft.WheelPoint(14.0F, -6.0F, 1.25F, 0.0F, "front", "left");

        VehicleEditorDraft.WheelPoint right = left.mirrored();

        assertEquals(14.0F, right.forward());
        assertEquals(6.0F, right.right());
        assertEquals(1.25F, right.scale());
        assertEquals(180.0F, right.yaw());
        assertEquals("front", right.end());
        assertEquals("right", right.side());
    }

    @Test
    void combinedPreviewContainsEveryImportedPart() throws IOException {
        Path frame = writeBbModel("frame.bbmodel");
        Path wheel = writeBbModel("wheel.bbmodel");
        Map<VehicleEditorDraft.Target, Path> files = Map.of(
                VehicleEditorDraft.Target.FRAME, frame,
                VehicleEditorDraft.Target.WHEEL, wheel
        );
        Map<VehicleEditorDraft.Target, String> keys = Map.of(
                VehicleEditorDraft.Target.FRAME, "frame_preview",
                VehicleEditorDraft.Target.WHEEL, "wheel_preview"
        );

        Path archive = VehiclePackBuilder.buildPreview(files, keys, temporaryDirectory.resolve("preview.riauto"));
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertDoesNotThrow(() -> {
                if (zip.getEntry("assets/riautomobility_preview/models/entity/automobile/frame/"
                        + keys.get(VehicleEditorDraft.Target.FRAME) + ".bbmodel") == null) throw new IOException("missing frame");
                if (zip.getEntry("assets/riautomobility_preview/models/entity/automobile/wheel/"
                        + keys.get(VehicleEditorDraft.Target.WHEEL) + ".bbmodel") == null) throw new IOException("missing wheel");
            });
        }
    }

    private Path writeBbModel(String name) throws IOException {
        String json = "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                + "\"textures\":[{\"name\":\"body.png\",\"source\":\"" + EMBEDDED_PNG + "\"}],"
                + "\"elements\":[],\"outliner\":[]}";
        return Files.writeString(temporaryDirectory.resolve(name), json);
    }

    private static BbModelData.Document document(String source) {
        BbModelData.Texture texture = new BbModelData.Texture(
                0, "texture-uuid", "0", "body.png", "", "", source,
                "default", true, 16, 16);
        return new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(texture), List.of(), List.of());
    }
}
