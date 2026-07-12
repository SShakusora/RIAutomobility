package com.sshakusora.riautomobility.editor;

import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VehicleImportRegistries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RIAutomobility.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RIAutomobility.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, RIAutomobility.MODID);

    public static final RegistryObject<Block> VEHICLE_IMPORT_TABLE = BLOCKS.register(
            "vehicle_import_table",
            () -> new VehicleImportTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.METAL))
    );
    public static final RegistryObject<Item> VEHICLE_IMPORT_TABLE_ITEM = ITEMS.register(
            "vehicle_import_table",
            () -> new BlockItem(VEHICLE_IMPORT_TABLE.get(), new Item.Properties())
    );
    public static final RegistryObject<MenuType<VehicleImportMenu>> VEHICLE_IMPORT_MENU = MENUS.register(
            "vehicle_import",
            () -> IForgeMenuType.create(VehicleImportMenu::new)
    );

    private VehicleImportRegistries() {}
}
