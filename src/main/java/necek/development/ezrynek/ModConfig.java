package necek.development.ezrynek;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.io.*;
import java.util.*;

public class ModConfig {
    private static ModConfig instance;
    private final List<MarketItem> marketItems = new ArrayList<>();
    private final Set<String> defaultDisplayNames = new HashSet<>();
    private final File configFile;
    private final Gson gson;
    private boolean debug = false;
    private String rendermode = "fill";

    private ModConfig() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        configFile = new File("config/ezrynek/prices.json");
        configFile.getParentFile().mkdirs();

        addDefaultItem("Anarchiczny_miecz", new Identifier("minecraft", "netherite_sword"), Map.of("sharpness", 6), List.of(), 0, 1);
        addDefaultItem("Excalibur", new Identifier("minecraft", "netherite_sword"), Map.of(), List.of(Text.of("Zapełnienie paska zapewnia")), 5, 1);
        addDefaultItem("Smoczy_miecz", new Identifier("minecraft", "netherite_sword"), Map.of("sharpness", 6), List.of(Text.of("perłami kresu!")), 1, 1);
        addDefaultItem("Anarchiczny_kilof", new Identifier("minecraft", "netherite_pickaxe"), Map.of("fortune", 5), List.of(), 0, 1);
        addDefaultItem("lopata_grincha", new Identifier("minecraft", "diamond_shovel"), Map.of(), List.of(Text.of("rotacja zostaje wylosowana!")), 4572321, 1);
        addDefaultItem("Boski_topor", new Identifier("minecraft", "iron_axe"), Map.of(), List.of(Text.of("fale uderzeniową")), 3462232, 1);
        addDefaultItem("Arcus_magnus", new Identifier("minecraft", "bow"), Map.of(), List.of(Text.of("niszczycielskie combo!")), 5324, 1);
        addDefaultItem("Serce", new Identifier("minecraft", "red_dye"), Map.of(), List.of(Text.of("kliknij PRAWYM, aby je wykorzystać")), 1, 1);

        loadPrices();
    }

    private void addDefaultItem(String displayName, Identifier itemId, Map<String, Integer> enchants, List<Text> lore, int customModelData, int defaultPrice) {
        MarketItem item = new MarketItem(displayName, itemId, enchants, lore, customModelData, defaultPrice);
        marketItems.add(item);
        defaultDisplayNames.add(displayName);
    }

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public List<MarketItem> getMarketItems() {
        return marketItems;
    }

    public void updatePrice(Identifier itemId, Map<String, Integer> enchants, List<Text> lore, int customModelData, int newPrice) {
        for (MarketItem item : marketItems) {
            if (item.getItemId().equals(itemId) &&
                    item.getEnchants().equals(enchants) &&
                    item.getLore().equals(lore) &&
                    item.getCustomModelData() == customModelData) {
                item.setDefaultPrice(newPrice);
                savePrices();
                return;
            }
        }
        MarketItem newItem = new MarketItem(itemId.toString(), itemId, enchants, lore, customModelData, newPrice);
        marketItems.add(newItem);
        savePrices();
    }

    public void updatePriceForDefaultItem(String displayName, int newPrice) {
        for (MarketItem item : marketItems) {
            if (item.getDisplayName().equalsIgnoreCase(displayName) && defaultDisplayNames.contains(displayName)) {
                item.setDefaultPrice(newPrice);
                savePrices();
                break;
            }
        }
    }

    public void removeItem(String displayName) {
        marketItems.removeIf(item -> item.getDisplayName().equalsIgnoreCase(displayName) && !defaultDisplayNames.contains(displayName));
        savePrices();
    }

    public boolean isDefaultItem(String displayName) {
        return defaultDisplayNames.contains(displayName);
    }

    private void savePrices() {
        try {
            Map<String, Integer> prices = new HashMap<>();
            for (MarketItem item : marketItems) {
                String key = generateKey(item);
                prices.put(key, item.getDefaultPrice());
            }
            try (Writer writer = new FileWriter(configFile)) {
                gson.toJson(prices, writer);
            }
        } catch (IOException e) {
            System.err.println("Nie udało się zapisać cen: " + e.getMessage());
        }
    }

    private void loadPrices() {
        if (!configFile.exists()) return;

        try {
            Map<String, Integer> prices;
            try (Reader reader = new FileReader(configFile)) {
                prices = gson.fromJson(reader, new TypeToken<Map<String, Integer>>(){}.getType());
            }
            if (prices != null) {
                for (MarketItem item : marketItems) {
                    String key = generateKey(item);
                    if (prices.containsKey(key)) {
                        item.setDefaultPrice(prices.get(key));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Nie udało się wczytać cen: " + e.getMessage());
        }
    }

    public int getPrice(Identifier itemId) {
        for (MarketItem item : marketItems) {
            if (item.getItemId().equals(itemId)) {
                return item.getDefaultPrice();
            }
        }
        return Integer.MAX_VALUE;
    }

    public static int extractPriceFromLore(List<Text> lore) {
        if (lore != null && lore.size() >= 4) {
            String priceLine = lore.get(lore.size() - 5).getString();
            if (priceLine.contains("Koszt")) {
                try {
                    int startIndex = priceLine.lastIndexOf("(") + 2;
                    int endIndex = priceLine.lastIndexOf(")");
                    String priceStr = priceLine.substring(startIndex, endIndex)
                            .replaceAll("[^\\d]", "");
                    return Integer.parseInt(priceStr);
                } catch (Exception e) {
                    return Integer.MAX_VALUE;
                }
            } else {
                priceLine = lore.get(lore.size() - 4).getString();
                if (priceLine.contains("Koszt")) {
                    try {
                        int startIndex = priceLine.lastIndexOf("(") + 2;
                        int endIndex = priceLine.lastIndexOf(")");
                        String priceStr = priceLine.substring(startIndex, endIndex)
                                .replaceAll("[^\\d]", "");
                        return Integer.parseInt(priceStr);
                    } catch (Exception e) {
                        return Integer.MAX_VALUE;
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getRenderMode() {
        return rendermode;
    }

    public void setRenderMode(String rendermode) {
        this.rendermode = rendermode;
    }

    private String generateKey(MarketItem item) {
        return String.format("%s:%d:%s:%s",
                item.getItemId().toString(),
                item.getCustomModelData(),
                item.getLore().toString(),
                item.getEnchants().toString());
    }
}