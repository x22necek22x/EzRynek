package necek.development.ezrynek.client;

import com.mojang.blaze3d.systems.RenderSystem;
import necek.development.ezrynek.MarketItem;
import necek.development.ezrynek.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class MarketInventoryHandler extends DrawableHelper {
    private static final int[] MARKET_SLOTS = {
            2, 3, 4, 5, 6, 7, 8,
            11, 12, 13, 14, 15, 16, 17,
            20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 34, 35
    };

    private static boolean needsUpdate = true;
    private static List<Slot> highlightedSlots = new ArrayList<>();

    public static void renderMarketOverlay(MatrixStack matrices, HandledScreen<?> screen, MinecraftClient client) {
        ScreenHandler handler = screen.getScreenHandler();
        int guiLeft = (screen.width - 176) / 2;
        int guiTop = (screen.height - 166) / 2 - 28;

        client.textRenderer.drawWithShadow(matrices, Text.of("ezRynek mod by necek/czatdzipiti"), 2, 2, 0xFF5555);
        client.textRenderer.drawWithShadow(matrices, Text.of("Jebac cwela nancio | discord.gg/aNrkuVeWgX"), 2, 14, 0xFF5555);

        if (needsUpdate) {
            highlightedSlots.clear();
            ModConfig config = ModConfig.getInstance();
            List<MarketItem> marketItems = config.getMarketItems();

            for (int slotIndex : MARKET_SLOTS) {
                Slot slot = handler.getSlot(slotIndex);
                ItemStack stack = slot.getStack();

                if (!stack.isEmpty()) {
                    for (MarketItem marketItem : marketItems) {
                        if (isMatchingItem(stack, marketItem)) {
                            highlightedSlots.add(slot);
                            break;
                        }
                    }
                }
            }
            needsUpdate = false;
        }

        for (Slot slot : highlightedSlots) {
            drawSlotHighlight(matrices, guiLeft, guiTop, slot, client);
        }
    }

    private static boolean isMatchingItem(ItemStack stack, MarketItem marketItem) {
        boolean validIdentifier = stack.getItem().getRegistryEntry().registryKey().getValue().equals(marketItem.getItemId());
        boolean validCustomModelData = getCustomModelData(stack) == marketItem.getCustomModelData();
        boolean validLore = hasRequiredLore(stack, marketItem.getLore().stream().map(Text::getString).collect(Collectors.toList()));
        boolean validEnchantments = hasRequiredEnchantments(stack, marketItem.getEnchants());
        boolean validPrice = isPriceValid(stack, marketItem.getDefaultPrice());

        return validIdentifier && validCustomModelData && validLore && validEnchantments && validPrice;
    }

    private static boolean hasRequiredEnchantments(ItemStack stack, Map<String, Integer> requiredEnchantments) {
        if (requiredEnchantments.isEmpty()) return true;

        Map<net.minecraft.enchantment.Enchantment, Integer> itemEnchantments = net.minecraft.enchantment.EnchantmentHelper.get(stack);
        return requiredEnchantments.entrySet().stream()
                .allMatch(entry -> {
                    net.minecraft.enchantment.Enchantment enchant = Registry.ENCHANTMENT.get(new Identifier(entry.getKey()));
                    return itemEnchantments.getOrDefault(enchant, 0) >= entry.getValue();
                });
    }

    private static boolean hasRequiredLore(ItemStack stack, List<String> requiredLore) {
        if (requiredLore.isEmpty()) return true;

        List<Text> lore = getLore(stack);
        if (lore == null) return false;

        return requiredLore.stream()
                .anyMatch(required -> lore.stream().anyMatch(line -> line.getString().contains(required)));
    }

    private static boolean isPriceValid(ItemStack stack, int configPrice) {
        List<Text> lore = getLore(stack);
        if (lore == null) return false;

        int marketPrice = ModConfig.extractPriceFromLore(lore) * stack.getCount();
        return marketPrice <= configPrice;
    }

    private static void drawSlotHighlight(MatrixStack matrices, int guiLeft, int guiTop, Slot slot, MinecraftClient client) {
        int x = guiLeft + slot.x;
        int y = guiTop + slot.y;
        int color = 0x8000FF00;

        fill(matrices, x, y, x + 16, y + 16, color);
    }

    private static List<Text> getLore(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("display", 10)) {
            NbtCompound display = nbt.getCompound("display");
            if (display.contains("Lore", 9)) {
                NbtList loreList = display.getList("Lore", 8);
                return loreList.stream().map(tag -> Text.Serializer.fromJson(tag.asString())).collect(Collectors.toList());
            }
        }
        return null;
    }

    private static int getCustomModelData(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("CustomModelData", 3)) {
            return nbt.getInt("CustomModelData");
        }
        return 0;
    }

    public static void markForUpdate() {
        needsUpdate = true;
    }
}
