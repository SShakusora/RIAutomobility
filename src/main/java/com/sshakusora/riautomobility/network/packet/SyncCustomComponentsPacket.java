package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackArchiveStore;
import com.sshakusora.riautomobility.carpack.CarPackManifestEntry;
import com.sshakusora.riautomobility.content.EngineSpec;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.network.packet.client.SyncCustomComponentsClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCustomComponentsPacket {
    private final Map<ResourceLocation, FrameSpec> frames;
    private final Map<ResourceLocation, WheelSpec> wheels;
    private final Map<ResourceLocation, EngineSpec> engines;
    private final List<CarPackManifestEntry> carPacks;

    public SyncCustomComponentsPacket(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels,
                                      Map<ResourceLocation, EngineSpec> engines,
                                      List<CarPackManifestEntry> carPacks) {
        this.frames = frames;
        this.wheels = wheels;
        this.engines = engines;
        this.carPacks = carPacks;
    }

    public static SyncCustomComponentsPacket create() {
        return new SyncCustomComponentsPacket(
                RIAutomobilityComponentManager.getCustomFrames(),
                RIAutomobilityComponentManager.getCustomWheels(),
                RIAutomobilityComponentManager.getCustomEngines(),
                CarPackArchiveStore.prepareManifest()
        );
    }

    public static void encode(SyncCustomComponentsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.frames.size());
        for (FrameSpec spec : msg.frames.values()) {
            spec.write(buf);
        }
        buf.writeVarInt(msg.wheels.size());
        for (WheelSpec spec : msg.wheels.values()) {
            spec.write(buf);
        }
        buf.writeVarInt(msg.engines.size());
        for (EngineSpec spec : msg.engines.values()) spec.write(buf);
        buf.writeVarInt(msg.carPacks.size());
        for (CarPackManifestEntry entry : msg.carPacks) {
            entry.write(buf);
        }
    }

    public static SyncCustomComponentsPacket decode(FriendlyByteBuf buf) {
        int frameCount = buf.readVarInt();
        if (frameCount < 0 || frameCount > 4096) {
            throw new IllegalArgumentException("Invalid custom frame count: " + frameCount);
        }
        Map<ResourceLocation, FrameSpec> frames = new LinkedHashMap<>();
        for (int i = 0; i < frameCount; i++) {
            FrameSpec spec = FrameSpec.read(buf);
            frames.put(spec.id(), spec);
        }

        int wheelCount = buf.readVarInt();
        if (wheelCount < 0 || wheelCount > 4096) {
            throw new IllegalArgumentException("Invalid custom wheel count: " + wheelCount);
        }
        Map<ResourceLocation, WheelSpec> wheels = new LinkedHashMap<>();
        for (int i = 0; i < wheelCount; i++) {
            WheelSpec spec = WheelSpec.read(buf);
            wheels.put(spec.id(), spec);
        }
        int engineCount = buf.readVarInt();
        if (engineCount < 0 || engineCount > 4096) throw new IllegalArgumentException("Invalid custom engine count: " + engineCount);
        Map<ResourceLocation, EngineSpec> engines = new LinkedHashMap<>();
        for (int i = 0; i < engineCount; i++) {
            EngineSpec spec = EngineSpec.read(buf);
            engines.put(spec.id(), spec);
        }
        int packCount = buf.readVarInt();
        if (packCount < 0 || packCount > CarPackManifestEntry.MAX_PACKS) {
            throw new IllegalArgumentException("Invalid RIAutomobility car pack count: " + packCount);
        }
        List<CarPackManifestEntry> carPacks = new ArrayList<>(packCount);
        for (int i = 0; i < packCount; i++) {
            carPacks.add(CarPackManifestEntry.read(buf));
        }
        return new SyncCustomComponentsPacket(frames, wheels, engines, carPacks);
    }

    public static void handle(SyncCustomComponentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SyncCustomComponentsClientHandler.handle(msg.frames, msg.wheels, msg.engines, msg.carPacks)));
        ctx.get().setPacketHandled(true);
    }
}
