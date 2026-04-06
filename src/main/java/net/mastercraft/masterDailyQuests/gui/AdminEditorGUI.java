package net.mastercraft.masterDailyQuests.gui;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminEditorGUI implements InventoryHolder {

    private final Inventory inventory;
    private final int page;

    public AdminEditorGUI(MasterDailyQuests plugin, int page) {
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, "Daily Quests Editor (Page " + (page + 1) + ")");
        initializeItems(plugin);
    }

    private void initializeItems(MasterDailyQuests plugin) {
        ItemStack borderPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = borderPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            borderPane.setItemMeta(paneMeta);
        }

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderPane);
            }
        }

        ItemStack createBtn = new ItemStack(Material.ANVIL);
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName("§a§lCreate Quest");
            createBtn.setItemMeta(createMeta);
        }
        inventory.setItem(49, createBtn);

        List<String> allQuests = new ArrayList<>(plugin.getQuestManager().getQuestIds());
        Collections.sort(allQuests);

        int maxPerPage = 28;
        int startIndex = page * maxPerPage;
        int endIndex = Math.min(startIndex + maxPerPage, allQuests.size());

        if (page > 0) {
            ItemStack prevBtn = new ItemStack(Material.ARROW);
            ItemMeta meta = prevBtn.getItemMeta();
            meta.setDisplayName("§e§lPrevious Page");
            prevBtn.setItemMeta(meta);
            inventory.setItem(48, prevBtn);
        }

        if (endIndex < allQuests.size()) {
            ItemStack nextBtn = new ItemStack(Material.ARROW);
            ItemMeta meta = nextBtn.getItemMeta();
            meta.setDisplayName("§e§lNext Page");
            nextBtn.setItemMeta(meta);
            inventory.setItem(50, nextBtn);
        }

        int[] innerSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < (endIndex - startIndex); i++) {
            String questId = allQuests.get(startIndex + i);
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);

            String type = qConf.getString("type", "UNKNOWN");
            String target = qConf.getString("target", "ANY");
            int amount = qConf.getInt("amount", 1);
            int rewardsCount = qConf.getList("rewards") != null ? qConf.getList("rewards").size() : 0;

            ItemStack questItem = new ItemStack(Material.BOOK);
            ItemMeta meta = questItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eQuest: §f" + questId);
                meta.setLore(List.of(
                        "§7Type: §b" + type,
                        "§7Target: §b" + plugin.getRealTargetName(target),
                        "§7Amount: §b" + amount,
                        "§7Rewards: §b" + rewardsCount + " items",
                        "",
                        "§a[Left-Click] §7to edit",
                        "§c[Shift+Right-Click] §7to delete"
                ));
                questItem.setItemMeta(meta);
            }
            inventory.setItem(innerSlots[i], questItem);
        }
    }

    public static void open(Player player, MasterDailyQuests plugin, int page) {
        player.openInventory(new AdminEditorGUI(plugin, page).getInventory());
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public int getPage() { return page; }
}