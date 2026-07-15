package com.sshakusora.riautomobility.content;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public interface NetworkComponentSpec {
    ResourceLocation id();

    JsonObject toJson();
}
