package com.sshakusora.riautomobility.item;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RIAutomobilityItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RIAutomobility.MODID);

    public static final RegistryObject<VehicleKeyItem> VEHICLE_KEY = ITEMS.register(
            "vehicle_key",
            () -> new VehicleKeyItem(new Item.Properties().stacksTo(1))
    );

    private RIAutomobilityItems() {
    }
}
