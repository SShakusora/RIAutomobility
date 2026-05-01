package com.sshakusora.riautomobility.network.packet;

import com.sshakusora.riautomobility.content.FrameSpec;
import com.sshakusora.riautomobility.content.RIAutomobilityComponentManager;
import com.sshakusora.riautomobility.content.WheelSpec;
import com.sshakusora.riautomobility.model.DynamicJsonModelLoader;
import com.sshakusora.riautomobility.frame.RIAutomobileFrame;
import com.sshakusora.riautomobility.model.RIAutomobileModels;
import com.sshakusora.riautomobility.reload.RIAutomobilityReloadManager;
import com.sshakusora.riautomobility.wheel.RIAutomobileWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncCustomComponentsPacket {
    private final Map<ResourceLocation, FrameSpec> frames;
    private final Map<ResourceLocation, WheelSpec> wheels;

    public SyncCustomComponentsPacket(Map<ResourceLocation, FrameSpec> frames, Map<ResourceLocation, WheelSpec> wheels) {
        this.frames = frames;
        this.wheels = wheels;
    }

    public static SyncCustomComponentsPacket create() {
        return new SyncCustomComponentsPacket(
                RIAutomobilityComponentManager.getCustomFrames(),
                RIAutomobilityComponentManager.getCustomWheels()
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
        return new SyncCustomComponentsPacket(frames, wheels);
    }

    public static void handle(SyncCustomComponentsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RIAutomobileFrame.reload();
            RIAutomobileWheel.reload();
            RIAutomobilityComponentManager.applyCustomComponents(msg.frames, msg.wheels);
            RIAutomobileModels.registerDynamicModels(msg.frames.values(), msg.wheels.values());

            if (Minecraft.getInstance().getEntityModels() != null) {
                DynamicJsonModelLoader.loadIntoEntityModelSet(Minecraft.getInstance().getEntityModels(), Minecraft.getInstance().getResourceManager());
            }

            RIAutomobileModels.rebuildDynamicModelsNow();

            if (Minecraft.getInstance().level != null) {
                RIAutomobilityReloadManager.refreshLevel(Minecraft.getInstance().level);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
