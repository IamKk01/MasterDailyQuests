package net.mastercraft.masterDailyQuests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class QuestSelectGUI implements InventoryHolder {

    private final Inventory inventory;

    public QuestSelectGUI() {
        this.inventory = Bukkit.createInventory(this, 36, "Select Quest Type");
        initializeItems();
    }

    private void initializeItems() {
        // Standard Vanilla Quests (Top Row)
        inventory.setItem(10, createBtn(Material.DIAMOND_PICKAXE, "§eMINING", "§7Break specific blocks"));
        inventory.setItem(11, createBtn(Material.DIAMOND_SWORD, "§eKILLING", "§7Kill specific mobs"));
        inventory.setItem(12, createBtn(Material.CRAFTING_TABLE, "§eCRAFTING", "§7Craft specific items"));
        inventory.setItem(13, createBtn(Material.BREWING_STAND, "§eBREWING", "§7Brew specific potions"));
        inventory.setItem(14, createBtn(Material.ENCHANTING_TABLE, "§eENCHANTING", "§7Enchant specific items"));
        inventory.setItem(15, createBtn(Material.GOLD_INGOT, "§eSELLING", "§7Sell items to the shop"));
        inventory.setItem(16, createBtn(Material.EMERALD, "§eTRADING", "§7Trade with villagers"));

        // Economy & Dungeon Quests (Bottom Row)
        inventory.setItem(19, createBtn(Material.CHEST, "§eBUYING", "§7Buy items from the shop"));
        inventory.setItem(21, createBtn(Material.IRON_DOOR, "§ePLAY_DUNGEON", "§7Play a specific dungeon"));
        inventory.setItem(23, createBtn(Material.LADDER, "§eREACH_STAGE", "§7Reach a specific dungeon stage"));
        inventory.setItem(25, createBtn(Material.BEACON, "§eFINISH_DUNGEON", "§7Successfully complete a dungeon"));

        // Border styling
        ItemStack borderPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = borderPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            borderPane.setItemMeta(paneMeta);
        }

        // Fill empty slots with border
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, borderPane);
            }
        }
    }

    private ItemStack createBtn(Material mat, String name, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(desc, "", "§a► Click to create!"));
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