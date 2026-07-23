package com.sshakusora.riautomobility.editor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.model.bbmodel.BbModelBounds;
import com.sshakusora.riautomobility.model.bbmodel.BbModelData;
import com.sshakusora.riautomobility.model.bbmodel.BbModelParser;
import io.github.foundationgames.automobility.automobile.AutomobileEngine;
import io.github.foundationgames.automobility.automobile.AutomobileFrame;
import io.github.foundationgames.automobility.automobile.AutomobileWheel;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
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
    void extractsTheTextureMarkedAsDefault() throws IOException {
        BbModelData.Texture other = texture(0, "data:image/png;base64,AA==", false);
        BbModelData.Texture selected = texture(1, EMBEDDED_PNG, true);
        BbModelData.Document document = new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(other, selected), List.of(), List.of());

        assertArrayEquals(embeddedPngBytes(), VehiclePackBuilder.defaultEmbeddedTexture(document));
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
    void editorDraftRoundTripsPersistentFieldsWithoutExposingTheLocalPath() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        VehicleEditorDraft original = new VehicleEditorDraft(
                AutomobileFrame.EMPTY, AutomobileWheel.EMPTY, AutomobileEngine.EMPTY);
        original.target = VehicleEditorDraft.Target.WHEEL;
        original.setDisplayName("Saved wheel");
        original.setModelFile(VehicleEditorDraft.Target.WHEEL,
                Path.of("C:", "Users", "Player", "model-cached.bbmodel"));
        original.showPart(VehicleEditorDraft.Target.WHEEL);
        original.wheelSize = 1.25F;
        original.wheelGrip = 0.8F;
        original.wheelRadius = 7.0F;
        original.wheelWidth = 4.5F;
        original.wheelRotationY = -90.0F;

        var saved = original.save();
        assertEquals("model-cached.bbmodel",
                saved.getCompound("Parts").getCompound("WHEEL").getString("ModelFile"));
        assertFalse(saved.toString().contains("Users"));

        VehicleEditorDraft restored = new VehicleEditorDraft(
                AutomobileFrame.EMPTY, AutomobileWheel.EMPTY, AutomobileEngine.EMPTY);
        restored.load(saved);

        assertEquals(VehicleEditorDraft.Target.WHEEL, restored.target);
        assertEquals("Saved wheel", restored.displayName());
        assertEquals(Path.of("model-cached.bbmodel"), restored.modelFile());
        assertTrue(restored.isPartVisible(VehicleEditorDraft.Target.WHEEL));
        assertEquals(1.25F, restored.wheelSize);
        assertEquals(0.8F, restored.wheelGrip);
        assertEquals(7.0F, restored.wheelRadius);
        assertEquals(4.5F, restored.wheelWidth);
        assertEquals(-90.0F, restored.wheelRotationY);
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
    void dropdownPositionsAreTruncatedToOneDecimalPlace() {
        assertEquals("0.7", VehicleImportScreen.truncatedPosition(0.7259620126724243D));
        assertEquals("-0.7", VehicleImportScreen.truncatedPosition(-0.7259625D));
        assertEquals("2.1", VehicleImportScreen.truncatedPosition(2.138D));
        assertEquals("-2.1", VehicleImportScreen.truncatedPosition(-2.138D));
        assertEquals("0.0", VehicleImportScreen.truncatedPosition(0.0D));
    }

    @Test
    void itemExportUsesAndCleansAUploadStagingFileInsteadOfExports() throws IOException {
        Path root = temporaryDirectory.resolve("riautomobility");

        Path staging = VehicleImportScreen.createItemUploadStagingPath(root, "generated-frame");

        assertEquals(root.resolve(CarPackManager.CACHE_DIRECTORY_NAME).resolve("uploads"),
                staging.getParent());
        assertFalse(staging.startsWith(root.resolve("exports")));
        assertTrue(Files.exists(staging));
        VehicleImportScreen.cleanupItemUploadArchive(staging);
        assertFalse(Files.exists(staging));
    }

    @Test
    void importedRiautoAuthorSurvivesExportByAnotherPlayer() {
        assertEquals("PlayerA", VehicleImportScreen.resolveExportAuthor("PlayerA", "PlayerB"));
        assertEquals("PlayerB", VehicleImportScreen.resolveExportAuthor("", "PlayerB"));
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
            assertNotNull(zip.getEntry("assets/riautomobility_preview/models/entity/automobile/frame/frame_preview.bbmodel"));
            assertNotNull(zip.getEntry("assets/riautomobility_preview/models/entity/automobile/wheel/wheel_preview.bbmodel"));
            assertArrayEquals(embeddedPngBytes(), zip.getInputStream(zip.getEntry(
                    "assets/riautomobility_preview/textures/entity/automobile/frame/frame_preview.png")).readAllBytes());
            assertArrayEquals(embeddedPngBytes(), zip.getInputStream(zip.getEntry(
                    "assets/riautomobility_preview/textures/entity/automobile/wheel/wheel_preview.png")).readAllBytes());
        }
    }

    @Test
    void exportedRiautoV1StoresTheTextureOnlyAsAnExternalPng() throws IOException {
        byte[] source = Files.readAllBytes(writeBbModel("v1-wheel.bbmodel"));
        var exported = BbModelRuntimeSanitizer.externalize(source, "test",
                "textures/entity/automobile/wheel/test-wheel");
        var component = new JsonObject();
        component.add("model", new JsonObject());
        Map<String, byte[]> entries = new LinkedHashMap<>();
        JsonObject files = new JsonObject();
        VehiclePackBuilder.addV1ModelEntries(
                entries, files, component, exported, "test", "wheel", "test-wheel");
        Path archive = temporaryDirectory.resolve("wheel-v1.riauto");
        VehiclePackBuilder.writeArchive(archive, entries);

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertEquals(1, CarPackArchiveStore.RIAUTO_FORMAT_VERSION);

            var modelEntry = zip.stream().filter(entry -> entry.getName().endsWith(".bbmodel"))
                    .findFirst().orElseThrow();
            var model = JsonParser.parseReader(new InputStreamReader(
                    zip.getInputStream(modelEntry), StandardCharsets.UTF_8)).getAsJsonObject();
            var texture = model.getAsJsonArray("textures").get(0).getAsJsonObject();
            assertFalse(texture.has("source"));
            String resource = texture.get("relative_path").getAsString();
            assertEquals(resource, component.getAsJsonObject("model").get("texture").getAsString());
            assertTrue(files.entrySet().stream().anyMatch(entry -> entry.getValue().getAsString()
                    .equals("assets/" + resource.replace(':', '/'))));
            assertEquals(1, zip.stream().filter(entry -> entry.getName().endsWith(".png")).count());
            assertNotNull(zip.getEntry("texture-0.png"));
            assertTrue(zip.stream().noneMatch(entry -> entry.getName().contains("/")));
        }
    }

    @Test
    void buildsAValidArchiveFromAnImmutableExportRequest() throws IOException {
        var component = new JsonObject();
        component.addProperty("size", 1.0F);
        component.addProperty("grip", 0.8F);
        component.addProperty("radius", 5.0F);
        component.addProperty("width", 3.0F);
        component.addProperty("show_in_creative_tab", true);
        component.add("model", new JsonObject());
        String path = "auto_0123456789abcdef0123456789abcdef";
        var request = new VehiclePackBuilder.ExportRequest(
                VehicleEditorDraft.Target.WHEEL, writeBbModel("async-wheel.bbmodel"),
                "riautomobility", path, "Async Wheel", "Test Author", component,
                "riautomobility-" + path + "-wheel", false);

        Path archive = VehiclePackBuilder.build(request, temporaryDirectory.resolve("async-wheel.riauto"));

        assertDoesNotThrow(() -> CarPackArchiveStore.validateRiautoArchive(archive));
        assertEquals("Test Author", CarPackArchiveStore.readAuthor(archive));
    }

    @Test
    void animatedModelSurvivesCompressedArchiveRoundTrip() throws IOException {
        Path source = Files.writeString(temporaryDirectory.resolve("animated-frame.bbmodel"), ("""
                {
                  "meta":{"format_version":"5.0","model_format":"modded_entity"},
                  "textures":[{"uuid":"texture","name":"body.png","source":"%s"}],
                  "elements":[{"uuid":"cube-node","type":"cube","from":[0,0,0],"to":[16,16,16],
                    "faces":{"north":{"uv":[0,0,16,16],"texture":0}}}],
                  "groups":[{"uuid":"animated-bone","name":"Animated Bone","children":["cube-node"]}],
                  "outliner":["animated-bone"],
                  "animations":[{"uuid":"drive-animation","name":"drive","length":1,"loop":"loop",
                    "animators":{"animated-bone":{"type":"bone","keyframes":[
                      {"channel":"rotation","time":0,"interpolation":"bezier",
                       "data_points":[{"x":"query.vehicle_steering * 30","y":0,"z":0},{"x":15,"y":0,"z":0}],
                       "bezier_left_time":[-0.1,-0.1,-0.1],"bezier_left_value":[0,0,0],
                       "bezier_right_time":[0.1,0.1,0.1],"bezier_right_value":[5,0,0]}
                    ]}}}]
                }
                """).formatted(EMBEDDED_PNG));
        String path = "auto_11111111111111111111111111111111";
        var request = new VehiclePackBuilder.ExportRequest(
                VehicleEditorDraft.Target.FRAME, source, "riautomobility", path,
                "Animated Frame", "Test Author", frameComponent(path), "animated-frame", false);
        Path archive = VehiclePackBuilder.build(request, temporaryDirectory.resolve("animated-frame.riauto"));

        var imported = VehiclePackImporter.importComponent(
                archive, temporaryDirectory.resolve("animated-import"));
        JsonObject restored = JsonParser.parseString(Files.readString(imported.modelFile())).getAsJsonObject();
        BbModelData.Document document = BbModelParser.parse(restored);
        BbModelData.Animation animation = document.animations().get(0);
        BbModelData.GroupNode root = assertInstanceOf(BbModelData.GroupNode.class, document.roots().get(0));
        BbModelData.Keyframe keyframe = animation.animators().get(root.uuid()).keyframes().get(0);

        assertEquals("drive-animation", animation.uuid());
        assertEquals("drive", animation.name());
        assertEquals("loop", animation.loop());
        assertEquals(2, keyframe.dataPoints().size());
        assertEquals("query.vehicle_steering * 30", keyframe.dataPoints().get(0).x().getAsString());
        assertArrayEquals(new float[]{-0.1F, -0.1F, -0.1F}, keyframe.bezierLeftTime());
        assertArrayEquals(new float[]{5.0F, 0.0F, 0.0F}, keyframe.bezierRightValue());
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertNotNull(zip.getEntry("texture-0.png"));
        }
    }

    @Test
    void asyncExportRecalculatesAutomaticWheelDimensions() throws IOException {
        Path model = writeGeometryBbModel();
        var component = new JsonObject();
        component.addProperty("size", 1.0F);
        component.addProperty("grip", 0.8F);
        component.addProperty("radius", 99.0F);
        component.addProperty("width", 99.0F);
        component.addProperty("show_in_creative_tab", true);
        component.add("model", new JsonObject());
        String path = "auto_abcdef0123456789abcdef0123456789";
        var request = new VehiclePackBuilder.ExportRequest(
                VehicleEditorDraft.Target.WHEEL, model, "riautomobility", path,
                "Automatic Wheel", "Test Author", component, "automatic-wheel", false,
                false, true);

        Path archive = VehiclePackBuilder.build(request, temporaryDirectory.resolve("automatic-wheel.riauto"));
        VehicleEditorDraft.AutomaticWheelModelSize expected = VehicleEditorDraft.automaticWheelModelSize(
                BbModelBounds.measure(VehiclePackBuilder.validateSource(model)).size());

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entry = zip.getEntry("component.json");
            var exported = JsonParser.parseReader(new InputStreamReader(
                    zip.getInputStream(entry), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(expected.radiusPx(), exported.get("radius").getAsFloat(), 0.001F);
            assertEquals(expected.widthPx(), exported.get("width").getAsFloat(), 0.001F);
            assertEquals(expected.rotationY(),
                    exported.getAsJsonObject("model").get("rotation_y").getAsFloat(), 0.001F);
        }
    }

    private Path writeBbModel(String name) throws IOException {
        String json = "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                + "\"textures\":[{\"name\":\"body.png\",\"source\":\"" + EMBEDDED_PNG + "\"}],"
                + "\"elements\":[],\"outliner\":[]}";
        return Files.writeString(temporaryDirectory.resolve(name), json);
    }

    private Path writeGeometryBbModel() throws IOException {
        String json = "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"modded_entity\"},"
                + "\"textures\":[{\"name\":\"body.png\",\"source\":\"" + EMBEDDED_PNG + "\"}],"
                + "\"elements\":[{\"type\":\"cube\",\"uuid\":\"wheel\",\"name\":\"Wheel\","
                + "\"from\":[-2,-5,-5],\"to\":[2,5,5],"
                + "\"faces\":{\"north\":{\"uv\":[0,0,4,10],\"texture\":0}}}],"
                + "\"outliner\":[\"wheel\"]}";
        return Files.writeString(temporaryDirectory.resolve("automatic-wheel.bbmodel"), json);
    }

    private static JsonObject frameComponent(String path) {
        return JsonParser.parseString(("""
                {"weight":0.3,"model":{"type":"bbmodel",
                 "model_id":"riautomobility:%s",
                 "bbmodel":"riautomobility:models/entity/automobile/frame/%s.bbmodel"},
                 "wheel_base":{"forward_separation":16,"side_separation":10},
                 "length_px":24,"engine_pos_back":8,"engine_pos_up":2,
                 "rear_attachment_pos":12,"front_attachment_pos":12,
                 "dimensions":{"width":1.5,"height":1},"seats":[],
                 "camera_positions":[],"hitboxes":[],"show_in_creative_tab":true}
                """).formatted(path, path)).getAsJsonObject();
    }

    private static BbModelData.Document document(String source) {
        BbModelData.Texture texture = texture(0, source, true);
        return new BbModelData.Document(
                "5.0", "modded_entity", 16, 16, List.of(texture), List.of(), List.of());
    }

    private static BbModelData.Texture texture(int index, String source, boolean useAsDefault) {
        return new BbModelData.Texture(
                index, "texture-uuid-" + index, Integer.toString(index), "body.png", "", "", source,
                "default", useAsDefault, 16, 16);
    }

    private static byte[] embeddedPngBytes() {
        return Base64.getDecoder().decode(EMBEDDED_PNG.substring("data:image/png;base64,".length()));
    }
}
