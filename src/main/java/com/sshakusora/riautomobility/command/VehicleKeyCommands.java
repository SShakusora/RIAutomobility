package com.sshakusora.riautomobility.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sshakusora.riautomobility.RIAutomobility;
import com.sshakusora.riautomobility.entity.RIAutomobileEntity;
import com.sshakusora.riautomobility.item.VehicleKeyItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public final class VehicleKeyCommands {
    private VehicleKeyCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riautomobility")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("key")
                        .then(Commands.literal("recover")
                                .then(Commands.argument("vehicle", EntityArgument.entity())
                                        .executes(context -> recover(
                                                context.getSource(),
                                                EntityArgument.getEntity(context, "vehicle")
                                        ))))));
    }

    private static int recover(CommandSourceStack source, Entity target) throws CommandSyntaxException {
        if (!(target instanceof RIAutomobileEntity automobile) || !automobile.isKeyed()) {
            source.sendFailure(Component.translatable("command.riautomobility.vehicle_key.not_keyed"));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ItemStack key = VehicleKeyItem.createBound(automobile.getUUID(), automobile.getCustomName());
        if (!player.getInventory().add(key)) {
            player.drop(key, false);
        }
        source.sendSuccess(
                () -> Component.translatable("command.riautomobility.vehicle_key.recovered", automobile.getName()),
                true
        );
        return 1;
    }
}
