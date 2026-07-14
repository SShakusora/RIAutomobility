package com.sshakusora.riautomobility.editor.client;

import com.sshakusora.riautomobility.model.bbmodel.BbModelBounds;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
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
    void exportFileNameUsesDisplayNameAndAddsCollisionSuffix() throws IOException {
        String fileStem = VehicleImportScreen.exportFileStem("测试车辆");
        assertEquals("测试车辆", fileStem);
        assertEquals("测试车辆.riauto",
                VehicleImportScreen.nextAvailableExportPath(temporaryDirectory, fileStem).getFileName().toString());

        Files.createFile(temporaryDirectory.resolve("测试车辆.riauto"));
        Files.createFile(temporaryDirectory.resolve("测试车辆 (1).riauto"));
        assertEquals("测试车辆 (2).riauto",
                VehicleImportScreen.nextAvailableExportPath(temporaryDirectory, fileStem).getFileName().toString());
    }

    @Test
    void exportFileNameReplacesCharactersUnsupportedByFileManagers() {
        assertEquals("My_Car_Test", VehicleImportScreen.exportFileStem(" My/Car:Test. "));
        assertEquals("_CON", VehicleImportScreen.exportFileStem("CON"));
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
                new Vec3(0.25D, 0.5D, -0.75D), 4.0F, -0.35D, 1.62F);

        assertEquals(0.25D, eye.x);
        assertEquals(2.02D, eye.y, 1.0E-6D);
        assertEquals(-0.75D, eye.z);
    }

    @Test
    void passengerAndCameraShareTheSameEntityPosition() {
        var seat = new Vec3(-0.4D, 0.25D, 0.8D);
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
    void automaticThirdPersonCameraUsesModelDiagonalAndKeepsVanillaMinimum() {
        Vec3 small = VehicleEditorDraft.automaticThirdPersonCameraOffset(16.0F, 16.0F, 16.0F);
        Vec3 large = VehicleEditorDraft.automaticThirdPersonCameraOffset(48.0F, 32.0F, 96.0F);

        assertEquals(Vec3.ZERO, small);
        assertTrue(large.x < 0.0D);
        assertEquals(0.0D, large.y);
        assertEquals(0.0D, large.z);
    }

    @Test
    void automaticWheelSizeUsesThinXAxisAsWheelWidth() {
        VehicleEditorDraft.AutomaticWheelModelSize size = VehicleEditorDraft.automaticWheelModelSize(
                new BbModelBounds.Size(4.3F, 10.3F, 10.3F));

        assertEquals(5.15F, size.radiusPx(), 0.001F);
        assertEquals(4.3F, size.widthPx(), 0.001F);
        assertEquals(0.0F, size.rotationY(), 0.001F);
    }

    @Test
    void automaticWheelSizeRotatesAThinZAxisOntoTheWheelAxle() {
        VehicleEditorDraft.AutomaticWheelModelSize size = VehicleEditorDraft.automaticWheelModelSize(
                new BbModelBounds.Size(12.0F, 10.0F, 3.0F));

        assertEquals(6.0F, size.radiusPx(), 0.001F);
        assertEquals(3.0F, size.widthPx(), 0.001F);
        assertEquals(-90.0F, size.rotationY(), 0.001F);
    }

    @Test
    void parsesAttachmentResourceListsAndRemovesDuplicates() {
        assertEquals(List.of(
                        new ResourceLocation("automobility", "trailer"),
                        new ResourceLocation("riautomobility", "cargo_rack")),
                VehicleEditorDraft.parseResourceLocations(
                        "automobility:trailer, riautomobility:cargo_rack; automobility:trailer"));
        assertEquals(List.of(), VehicleEditorDraft.parseResourceLocations("  "));
    }

    @Test
    void rejectsInvalidAttachmentResourceIds() {
        assertThrows(IllegalArgumentException.class,
                () -> VehicleEditorDraft.parseResourceLocations("automobility:valid, INVALID ID"));
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
