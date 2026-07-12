package com.sshakusora.riautomobility.carpack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public final class CarPackRepositorySource implements RepositorySource {
    private static final PackSource PACK_SOURCE = PackSource.create(
            name -> Component.translatable("pack.nameAndSource", name, Component.literal("RIAutomobility")),
            true
    );

    private final PackType packType;

    public CarPackRepositorySource(PackType packType) {
        this.packType = packType;
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        var carPacks = packType == PackType.CLIENT_RESOURCES
                ? CarPackManager.discoverClientResourcePacks()
                : CarPackManager.discoverCarPacks();
        for (CarPackManager.CarPack carPack : carPacks) {
            Pack pack = Pack.readMetaAndCreate(
                    carPack.id(),
                    Component.literal(carPack.displayName()),
                    true,
                    carPack.resources(),
                    packType,
                    Pack.Position.TOP,
                    PACK_SOURCE
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        }
    }
}
