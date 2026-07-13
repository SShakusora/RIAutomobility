package com.sshakusora.riautomobility.carpack;

import com.mojang.brigadier.CommandDispatcher;
import com.sshakusora.riautomobility.RIAutomobility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RIAutomobility.MODID)
public final class CarPackCommands {
    private CarPackCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riautomobility")
                .then(Commands.literal("carpacks")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> reload(context.getSource())))));
    }

    private static int reload(CommandSourceStack source) {
        var server = source.getServer();
        source.sendSuccess(() -> Component.translatable("commands.riautomobility.carpacks.reload.started"), true);
        try {
            CarPackRuntime.reloadServer();
            CarPackEvents.CommonEvents.syncAll(server);
            source.sendSuccess(() -> Component.translatable("commands.riautomobility.carpacks.reload.success"), true);
            return 1;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.translatable("commands.riautomobility.carpacks.reload.failed", exception.getMessage()));
            return 0;
        }
    }
}
