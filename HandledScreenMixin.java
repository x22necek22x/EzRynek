package necek.development.ezrynek.client.mixin;

import necek.development.ezrynek.client.MarketInventoryHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void renderCustomOverlay(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

        // Sprawdź, czy aktualnie otwarty ekran jest instancją HandledScreen
        if (client.currentScreen != null && client.currentScreen instanceof HandledScreen) {
            HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;

            // Sprawdź, czy nazwa GUI zawiera "Market"
            if (handledScreen.getTitle().getString().contains("Market")) {
                // Jeśli tak, renderuj overlay
                MarketInventoryHandler.renderMarketOverlay(matrices, handledScreen, client);
            }
        }
    }
}
