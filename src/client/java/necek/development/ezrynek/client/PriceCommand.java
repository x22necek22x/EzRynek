package necek.development.ezrynek.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import necek.development.ezrynek.MarketItem;
import necek.development.ezrynek.ModConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PriceCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("cena")
                .executes(PriceCommand::showPrices)
                .then(ClientCommandManager.argument("item", StringArgumentType.word())
                        .suggests(PriceCommand::suggestItems)
                        .then(ClientCommandManager.argument("cena", IntegerArgumentType.integer(0))
                                .executes(PriceCommand::setPrice)))
                .then(ClientCommandManager.literal("necek_hand")
                        .then(ClientCommandManager.argument("cena", IntegerArgumentType.integer(0))
                                .then(ClientCommandManager.argument("nazwa_itemka", StringArgumentType.word())
                                        .executes(PriceCommand::setPriceInHand)))));
    }

    private static CompletableFuture<Suggestions> suggestItems(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();

        // Podpowiedzi dla przedmiotów z konfiguracji
        for (MarketItem item : ModConfig.getInstance().getMarketItems()) {
            String displayName = item.getDisplayName().toLowerCase();
            if (displayName.startsWith(input)) {
                builder.suggest(displayName);
            }
        }

        // Podpowiedzi dla podstawowych przedmiotów Minecraft
        if ("minecraft+".startsWith(input)) {
            builder.suggest("minecraft+");
        } else if (input.startsWith("minecraft+")) {
            String partialId = input.substring("minecraft+".length());
            builder.suggest("minecraft+ender_pearl");
        }

        return builder.buildFuture();
    }

    private static int showPrices(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        for (MarketItem item : ModConfig.getInstance().getMarketItems()) {
            source.sendFeedback(Text.of(
                    String.format("§e%s§r: §a$%d", item.getDisplayName(), item.getDefaultPrice())
            ));
        }
        return 1;
    }

    private static int setPrice(CommandContext<FabricClientCommandSource> context) {
        String itemName = StringArgumentType.getString(context, "item");
        int newPrice = IntegerArgumentType.getInteger(context, "cena");
        FabricClientCommandSource source = context.getSource();

        if (newPrice == 0) {
            if (ModConfig.getInstance().isDefaultItem(itemName)) {
                ModConfig.getInstance().updatePriceForDefaultItem(itemName, 1);
                source.sendFeedback(Text.of(
                        String.format("§cNie można usunąć domyślnego przedmiotu. Ustawiono cenę §e%s§c na §a$1", itemName)
                ));
                return 1;
            } else {
                ModConfig.getInstance().removeItem(itemName);
                source.sendFeedback(Text.of(
                        String.format("§cUsunięto przedmiot §e%s§c z listy", itemName)
                ));
                return 1;
            }
        }

        // Obsługa przedmiotów Minecraft w formacie "minecraft+item_id"
        if (itemName.startsWith("minecraft+")) {
            String itemIdPart = itemName.substring("minecraft+".length());
            try {
                Identifier itemId = new Identifier("minecraft", itemIdPart);
                boolean exists = false;

                // Sprawdź, czy istnieje już przedmiot o tym ID i domyślnych właściwościach
                for (MarketItem item : ModConfig.getInstance().getMarketItems()) {
                    if (item.getItemId().equals(itemId) &&
                            item.getEnchants().isEmpty() &&
                            item.getLore().isEmpty() &&
                            item.getCustomModelData() == 0) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    // Dodaj nowy przedmiot z automatycznie generowaną nazwą "minecraft+item_id"
                    MarketItem newItem = new MarketItem(
                            "minecraft+" + itemIdPart, // Nazwa wyświetlana
                            itemId,
                            new HashMap<>(),
                            List.of(),
                            0,
                            newPrice
                    );
                    ModConfig.getInstance().getMarketItems().add(newItem);
                }

                // Aktualizuj cenę
                ModConfig.getInstance().updatePrice(itemId, new HashMap<>(), List.of(), 0, newPrice);
                source.sendFeedback(Text.of(
                        String.format("§aUstawiono cenę §e%s§a na §e$%d", itemName, newPrice)
                ));
                return 1;
            } catch (Exception e) {
                source.sendFeedback(Text.of("§cNieprawidłowa nazwa przedmiotu Minecraft: " + itemName));
                return 0;
            }
        }

        // Standardowe przedmioty z konfiguracji
        for (MarketItem item : ModConfig.getInstance().getMarketItems()) {
            if (item.getDisplayName().equalsIgnoreCase(itemName)) {
                ModConfig.getInstance().updatePrice(item.getItemId(), item.getEnchants(), item.getLore(), item.getCustomModelData(), newPrice);
                source.sendFeedback(Text.of(
                        String.format("§aUstawiono cenę §e%s§a na §e$%d", item.getDisplayName(), newPrice)
                ));
                return 1;
            }
        }

        source.sendFeedback(Text.of("§cNie znaleziono przedmiotu o nazwie " + itemName));
        return 0;
    }

    private static int setPriceInHand(CommandContext<FabricClientCommandSource> context) {
        int newPrice = IntegerArgumentType.getInteger(context, "cena");
        String itemName = StringArgumentType.getString(context, "nazwa_itemka");
        FabricClientCommandSource source = context.getSource();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            ItemStack stack = client.player.getMainHandStack();

            if (!stack.isEmpty()) {
                Identifier itemId = stack.getItem().getRegistryEntry().registryKey().getValue();
                String displayName = itemName;
                NbtCompound nbt = stack.getNbt();
                int customModelData = nbt != null && nbt.contains("CustomModelData") ? nbt.getInt("CustomModelData") : 0;
                int priceFromLore = ModConfig.extractPriceFromLore(getLore(stack));
                int priceToSet = (priceFromLore != Integer.MAX_VALUE) ? priceFromLore : newPrice;

                NbtList enchantmentsNbtList = stack.getEnchantments();
                Map<String, Integer> enchantmentsMap = convertNbtListToMap(enchantmentsNbtList);

                MarketItem newItem = new MarketItem(displayName, itemId, enchantmentsMap, getLore(stack), customModelData, priceToSet);
                ModConfig.getInstance().getMarketItems().add(newItem);
                ModConfig.getInstance().updatePrice(itemId, enchantmentsMap, getLore(stack), customModelData, priceToSet);

                source.sendFeedback(Text.of(
                        String.format("§aUstawiono cenę przedmiotu w ręce §e%s§a na §e$%d", displayName, priceToSet)
                ));
                return 1;
            }
        }

        source.sendFeedback(Text.of("§cNie trzymasz żadnego przedmiotu w ręce"));
        return 0;
    }

    private static Map<String, Integer> convertNbtListToMap(NbtList nbtList) {
        Map<String, Integer> enchantmentsMap = new HashMap<>();
        for (int i = 0; i < nbtList.size(); i++) {
            NbtCompound compound = nbtList.getCompound(i);
            String enchantmentId = compound.getString("id");
            int level = compound.getInt("lvl");
            enchantmentsMap.put(enchantmentId, level);
        }
        return enchantmentsMap;
    }

    private static List<Text> getLore(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("display", 10)) {
            NbtCompound display = nbt.getCompound("display");
            if (display.contains("Lore", 9)) {
                NbtList loreList = display.getList("Lore", 8);
                return loreList.stream()
                        .map(tag -> Text.Serializer.fromJson(tag.asString()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}