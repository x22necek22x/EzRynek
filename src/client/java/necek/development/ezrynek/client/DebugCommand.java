package necek.development.ezrynek.client;

import com.mojang.brigadier.CommandDispatcher;
import necek.development.ezrynek.ModConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class DebugCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("rynek-debug")
                .executes(context -> {
                    boolean newState = !ModConfig.getInstance().isDebugEnabled();
                    ModConfig.getInstance().setDebug(newState);

                    context.getSource().sendFeedback(Text.of(
                            String.format("§aDebug mode: %s", newState ? "§2WŁĄCZONY" : "§cWYŁĄCZONY")
                    ));
                    return 1;
                }));
    }
}