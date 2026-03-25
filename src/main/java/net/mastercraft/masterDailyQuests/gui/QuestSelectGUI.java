package net.mastercraft.masterDailyQuests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class QuestSelectGUI implements InventoryHolder {

    private final Inventory inventory;

    public QuestSelectGUI() {
        this.inventory = Bukkit.createInventory(this, 27, "Select Quest Event");
        initializeItems();
    }

    private void initializeItems() {
        inventory.setItem(10, createGuiItem(Material.DIAMOND_PICKAXE, "§e§lMINING", "Mine a specific block"));
        inventory.setItem(11, createGuiItem(Material.DIAMOND_SWORD, "§c§lKILLING", "Kill a mob or player"));
        inventory.setItem(12, createGuiItem(Material.CRAFTING_TABLE, "§6§lCRAFTING", "Craft a specific item"));
        inventory.setItem(13, createGuiItem(Material.BREWING_STAND, "§d§lBREWING", "Brew a potion"));
        inventory.setItem(14, createGuiItem(Material.ENCHANTING_TABLE, "§5§lENCHANTING", "Enchant an item"));
        inventory.setItem(15, createGuiItem(Material.GOLD_INGOT, "§a§lSELLING", "Sell to shop"));
        inventory.setItem(16, createGuiItem(Material.EMERALD, "§2§lTRADING", "Trade with villager"));
        // Extra slot for Buying
        inventory.setItem(22, createGuiItem(Material.CHEST, "§b§lBUYING", "Buy from shop"));
    }

    private ItemStack createGuiItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.List.of("§7" + lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void open(Player player) {
        player.openInventory(new QuestSelectGUI().getInventory());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}