package com.sshakusora.riautomobility.item;

import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.network.RIAutomobilityNetwork;
import com.sshakusora.riautomobility.network.packet.VehicleHighlightPacket;
import com.sshakusora.riautomobility.world.VehicleLocatorSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VehicleKeyItem extends Item {
    private static final int HIGHLIGHT_DURATION_TICKS = 200;

    public VehicleKeyItem(Properties properties) {
        super(properties);
    }

    public static boolean isKey(ItemStack stack) {
        return stack.getItem() instanceof VehicleKeyItem;
    }

    public static boolean isBlankKey(ItemStack stack) {
        return isKey(stack) && getVehicleId(stack).isEmpty();
    }

    public static boolean isBoundTo(ItemStack stack, UUID automobileId) {
        return getVehicleId(stack).map(automobileId::equals).orElse(false);
    }

    public static Optional<UUID> getVehicleId(ItemStack stack) {
        if (!isKey(stack) || stack.getTag() == null) {
            return Optional.empty();
        }
        return VehicleKeyData.getVehicleId(stack.getTag());
    }

    public static Optional<String> getVehicleName(ItemStack stack) {
        if (!isKey(stack) || stack.getTag() == null) {
            return Optional.empty();
        }
        return VehicleKeyData.getVehicleName(stack.getTag());
    }

    public static void bind(ItemStack stack, UUID automobileId) {
        if (isKey(stack)) {
            VehicleKeyData.bind(stack.getOrCreateTag(), automobileId);
        }
    }

    public static void bind(ItemStack stack, UUID automobileId, @Nullable Component automobileName) {
        bind(stack, automobileId);
        updateVehicleName(stack, automobileName);
    }

    public static void updateVehicleName(ItemStack stack, @Nullable Component automobileName) {
        if (isKey(stack)) {
            VehicleKeyData.setVehicleName(
                    stack.getOrCreateTag(),
                    automobileName == null ? null : automobileName.getString()
            );
        }
    }

    public static void clearBinding(ItemStack stack) {
        if (stack.getTag() != null) {
            VehicleKeyData.clear(stack.getTag());
            if (stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        }
    }

    public static ItemStack createBound(UUID automobileId) {
        ItemStack stack = new ItemStack(RIAutomobilityItems.VEHICLE_KEY.get());
        bind(stack, automobileId);
        return stack;
    }

    public static ItemStack createBound(UUID automobileId, @Nullable Component automobileName) {
        ItemStack stack = new ItemStack(RIAutomobilityItems.VEHICLE_KEY.get());
        bind(stack, automobileId, automobileName);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        Optional<UUID> vehicleId = getVehicleId(stack);
        if (vehicleId.isEmpty() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            player.displayClientMessage(Component.translatable("message.riautomobility.vehicle_key.blank"), true);
            return InteractionResultHolder.consume(stack);
        }

        MinecraftServer server = serverLevel.getServer();
        RIAutomobileEntity loaded = findLoadedAutomobile(server, vehicleId.get());
        VehicleLocatorSavedData locator = VehicleLocatorSavedData.get(server);
        if (loaded != null && loaded.isKeyed()) {
            locator.update(loaded);
        }
        VehicleLocatorSavedData.Location location = locator.find(vehicleId.get());

        if (location == null) {
            if (loaded == null && locator.isDestroyed(vehicleId.get())) {
                clearBinding(stack);
                serverPlayer.getInventory().setChanged();
                player.displayClientMessage(Component.translatable("message.riautomobility.vehicle_key.invalid"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.riautomobility.vehicle_key.unavailable"), true);
            }
            return InteractionResultHolder.consume(stack);
        }

        VehicleKeyData.setVehicleName(stack.getOrCreateTag(), location.name());
        serverPlayer.getInventory().setChanged();

        player.displayClientMessage(Component.translatable(
                "message.riautomobility.vehicle_key.location",
                Mth.floor(location.x()), Mth.floor(location.y()), Mth.floor(location.z()),
                location.dimension().location().toString()
        ), true);
        if (location.dimension().equals(serverLevel.dimension())) {
            RIAutomobilityNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new VehicleHighlightPacket(vehicleId.get(), HIGHLIGHT_DURATION_TICKS)
            );
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.consume(stack);
    }

    @Nullable
    private static RIAutomobileEntity findLoadedAutomobile(MinecraftServer server, UUID automobileId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(automobileId);
            if (entity instanceof RIAutomobileEntity automobile) {
                return automobile;
            }
        }
        return null;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return getVehicleId(stack).isPresent()
                ? "item.riautomobility.vehicle_key.bound"
                : "item.riautomobility.vehicle_key";
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getVehicleId(stack).isPresent();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        getVehicleId(stack).ifPresent(id -> tooltip.add(Component.translatable(
                "tooltip.riautomobility.vehicle_key.id",
                getVehicleName(stack).orElseGet(() -> id.toString().substring(0, 8))
        ).withStyle(ChatFormatting.GRAY)));
    }
}
