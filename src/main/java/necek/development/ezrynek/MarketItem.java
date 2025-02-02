package necek.development.ezrynek;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class MarketItem {
    private final String displayName;
    private final Identifier itemId;
    private final Map<String, Integer> enchants;
    private final List<Text> lore; // Zmienione z List<String> na List<Text>
    private final int customModelData;
    private int defaultPrice;

    public MarketItem(String displayName, Identifier itemId, Map<String, Integer> enchants, List<Text> lore, int customModelData, int defaultPrice) {
        this.displayName = displayName;
        this.itemId = itemId;
        this.enchants = enchants;
        this.lore = lore;
        this.customModelData = customModelData;
        this.defaultPrice = defaultPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Identifier getItemId() {
        return itemId;
    }

    public Map<String, Integer> getEnchants() {
        return enchants;
    }

    public List<Text> getLore() { // Zmieniony typ zwracany
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(int defaultPrice) {
        this.defaultPrice = defaultPrice;
    }
}