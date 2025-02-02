package necek.development.ezrynek.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class MainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MarketInventoryHandler.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            PriceCommand.register(dispatcher);
            DebugCommand.register(dispatcher);
        });
    }
}
