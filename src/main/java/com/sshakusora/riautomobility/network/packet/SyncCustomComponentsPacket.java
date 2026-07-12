package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.carpack.CarPackManager;
import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.network.packet.client.SyncCustomComponentsClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCustomComponentsPacket {
    private final Map<ResourceLocation, FrameSpec> frames;
    private final Map<ResourceLocation, WheelSpec> wheels;
    private final Map<String, String> carPackDigests;

    public SyncCustomComponentsPacket(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels, Map<String, String> carPackDigests) {
        this.frames = frames;
        this.wheels = wheels;
        this.carPackDigests = carPackDigests;
    }

    public static SyncCustomComponentsPacket create() {
        return new SyncCustomComponentsPacket(
                RIAutomobilityComponentManager.getCustomFrames(),
                RIAutomobilityComponentManager.getCustomWheels(),
                CarPackManager.getDigests()
        );
    }

    public static void encode(SyncCustomComponentsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.frames.size());
        for (FrameSpec spec : msg.frames.values()) {
            spec.write(buf);
        }
        buf.writeInt(msg.wheels.size());
        for (WheelSpec spec : msg.wheels.values()) {
            spec.write(buf);
        }
        buf.writeVarInt(msg.carPackDigests.size());
        for (Map.Entry<String, String> entry : msg.carPackDigests.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static SyncCustomComponentsPacket decode(FriendlyByteBuf buf) {
        int frameCount = buf.readInt();
        Map<ResourceLocation, FrameSpec> frames = new LinkedHashMap<>();
        for (int i = 0; i < frameCount; i++) {
            FrameSpec spec = FrameSpec.read(buf);
            frames.put(spec.id(), spec);
        }

        int wheelCount = buf.readInt();
        Map<ResourceLocation, WheelSpec> wheels = new LinkedHashMap<>();
        for (int i = 0; i < wheelCount; i++) {
            WheelSpec spec = WheelSpec.read(buf);
            wheels.put(spec.id(), spec);
        }
        int digestCount = buf.readVarInt();
        if (digestCount < 0 || digestCount > 1024) {
            throw new IllegalArgumentException("Invalid RIAutomobility car pack digest count: " + digestCount);
        }
        Map<String, String> carPackDigests = new LinkedHashMap<>();
        for (int i = 0; i < digestCount; i++) {
            carPackDigests.put(buf.readUtf(), buf.readUtf());
        }
        return new SyncCustomComponentsPacket(frames, wheels, carPackDigests);
    }

    public static void handle(SyncCustomComponentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SyncCustomComponentsClientHandler.handle(msg.frames, msg.wheels, msg.carPackDigests)));
        ctx.get().setPacketHandled(true);
    }
}
