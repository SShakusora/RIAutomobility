package com.sshakusora.riautomobility;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> SHARED_CAR_PACK_DIRECTORY;
    private static final ForgeConfigSpec.IntValue SHARED_CAR_PACK_SCAN_INTERVAL_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Settings for server-side RIAuto car pack storage and synchronization.")
                .push("carPacks");
        SHARED_CAR_PACK_DIRECTORY = builder
                .comment(
                        "Directory containing the server's .riauto files.",
                        "Leave empty to use <game directory>/riautomobility as before.",
                        "Relative paths are resolved against the game directory. All servers that should share",
                        "car packs must point this setting at the same directory. Caches remain server-local."
                )
                .define("sharedDirectory", "");
        SHARED_CAR_PACK_SCAN_INTERVAL_SECONDS = builder
                .comment("How often a running server checks the shared directory for changes.")
                .defineInRange("scanIntervalSeconds", 3, 1, 300);
        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }

    public static Path getServerCarPackDirectory() {
        return resolveServerCarPackDirectory(FMLPaths.GAMEDIR.get(), SHARED_CAR_PACK_DIRECTORY.get());
    }

    public static int getSharedCarPackScanIntervalSeconds() {
        return SHARED_CAR_PACK_SCAN_INTERVAL_SECONDS.get();
    }

    static Path resolveServerCarPackDirectory(Path gameDirectory, String configuredDirectory) {
        Path normalizedGameDirectory = gameDirectory.toAbsolutePath().normalize();
        String configured = configuredDirectory == null ? "" : configuredDirectory.strip();
        if (configured.isEmpty()) {
            return normalizedGameDirectory.resolve("riautomobility");
        }
        try {
            Path path = Path.of(configured);
            return (path.isAbsolute() ? path : normalizedGameDirectory.resolve(path)).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalStateException("Invalid shared RIAutomobility car pack directory: " + configured, exception);
        }
    }
}
