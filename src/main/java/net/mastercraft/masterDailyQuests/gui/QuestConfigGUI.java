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

import java.util.List;

public class QuestConfigGUI implements InventoryHolder {

    private final Inventory inventory;
    private final String questId;
    private final String eventType;

    public QuestConfigGUI(String questId, String eventType, MasterDailyQuests plugin) {
        this.questId = questId;
        this.eventType = eventType;
        this.inventory = Bukkit.createInventory(this, 27, "Configuring: " + eventType);

        FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
        String currentTarget = qConf != null ? qConf.getString("target", "ANY") : "ANY";
        int currentAmount = qConf != null ? qConf.getInt("amount", 1) : 1;
        int rewardCount = qConf != null && qConf.getList("rewards") != null ? qConf.getList("rewards").size() : 0;

        initializeItems(currentTarget, currentAmount, rewardCount, plugin);
    }

    private void initializeItems(String target, int amount, int rewardCount, MasterDailyQuests plugin) {
        inventory.setItem(10, createGuiItem(Material.NAME_TAG, "§e§lChoose Target",
                "§8■ §7Current Task Target: §b" + plugin.getRealTargetName(target), // Fetches actual name
                "",
                "§e► Click to type a new target ID"
        ));

        inventory.setItem(12, createGuiItem(Material.PAPER, "§b§lChoose Amount",
                "§8■ §7Current Amount: §a" + amount,
                "",
                "§b► Click to type a new amount"
        ));

        inventory.setItem(14, createGuiItem(Material.GOLD_BLOCK, "§6§lSet Reward",
                "§8■ §7Configured Rewards: §e" + rewardCount,
                "",
                "§6► Click to open Reward Editor"
        ));

        inventory.setItem(16, createGuiItem(Material.LIME_DYE, "§a§lSAVE", "§7Go back to Editor"));
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static void open(Player player, String questId, String eventType, MasterDailyQuests plugin) {
        player.openInventory(new QuestConfigGUI(questId, eventType, plugin).getInventory());
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public String getQuestId() { return questId; }
    public String getEventType() { return eventType; }
}