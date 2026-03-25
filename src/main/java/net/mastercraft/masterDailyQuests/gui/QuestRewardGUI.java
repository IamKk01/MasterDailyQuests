package net.mastercraft.masterDailyQuests.gui;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestRewardGUI implements InventoryHolder {

    private final Inventory inventory;
    private final String questId;
    private final String eventType;
    private final MasterDailyQuests plugin;

    public final NamespacedKey KEY_UUID;
    public final NamespacedKey KEY_TYPE;
    public final NamespacedKey KEY_AMOUNT;
    public final NamespacedKey KEY_CMD;
    public final NamespacedKey KEY_BTN;

    public QuestRewardGUI(Player player, String questId, String eventType, MasterDailyQuests plugin, ItemStack[] cachedContents) {
        this.questId = questId;
        this.eventType = eventType;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, "Quest Rewards: " + questId);

        this.KEY_UUID = new NamespacedKey(plugin, "reward_uuid");
        this.KEY_TYPE = new NamespacedKey(plugin, "reward_type");
        this.KEY_AMOUNT = new NamespacedKey(plugin, "reward_amount");
        this.KEY_CMD = new NamespacedKey(plugin, "reward_cmd");
        this.KEY_BTN = new NamespacedKey(plugin, "reward_btn");

        initializeItems(cachedContents);
    }

    private void initializeItems(ItemStack[] cachedContents) {
        if (cachedContents != null) {
            for (int i = 0; i < 45; i++) {
                if (cachedContents[i] != null) {
                    inventory.setItem(i, formatForEditor(cachedContents[i].clone()));
                }
            }
        } else {
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
            if (qConf != null) {
                List<ItemStack> existingRewards = (List<ItemStack>) qConf.getList("rewards");
                if (existingRewards != null) {
                    for (int i = 0; i < Math.min(existingRewards.size(), 45); i++) {
                        inventory.setItem(i, formatForEditor(existingRewards.get(i).clone()));
                    }
                }
            }
        }

        // Setup Bottom Bar
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgM = bg.getItemMeta();
        if (bgM != null) {
            bgM.setDisplayName(" ");
            bg.setItemMeta(bgM);
        }
        for (int i = 45; i < 54; i++) inventory.setItem(i, bg);

        inventory.setItem(45, createBtn(Material.EXPERIENCE_BOTTLE, "§a+ Add EXP Reward", "BTN_EXP"));
        inventory.setItem(46, createBtn(Material.GOLD_INGOT, "§e+ Add Money Reward", "BTN_MONEY"));
        inventory.setItem(47, createBtn(Material.COMMAND_BLOCK, "§c+ Add Command Reward", "BTN_CMD"));
        inventory.setItem(53, createBtn(Material.LIME_DYE, "§a§lSAVE REWARDS", "BTN_SAVE"));
    }

    private ItemStack createBtn(Material mat, String name, String id) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(KEY_BTN, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack formatForEditor(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (!meta.getPersistentDataContainer().has(KEY_UUID, PersistentDataType.STRING)) {
            meta.getPersistentDataContainer().set(KEY_UUID, PersistentDataType.STRING, UUID.randomUUID().toString());
        }
        if (!meta.getPersistentDataContainer().has(KEY_TYPE, PersistentDataType.STRING)) {
            meta.getPersistentDataContainer().set(KEY_TYPE, PersistentDataType.STRING, "ITEM");
        }
        if (!meta.getPersistentDataContainer().has(KEY_AMOUNT, PersistentDataType.INTEGER)) {
            meta.getPersistentDataContainer().set(KEY_AMOUNT, PersistentDataType.INTEGER, item.getAmount());
        }

        String type = meta.getPersistentDataContainer().getOrDefault(KEY_TYPE, PersistentDataType.STRING, "ITEM");
        int amount = meta.getPersistentDataContainer().getOrDefault(KEY_AMOUNT, PersistentDataType.INTEGER, item.getAmount());

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line.contains("§8[Reward]"));

        lore.add("§8[Reward] §7--- Settings ---");
        lore.add("§8[Reward] §eType: §f" + type);

        if (type.equals("CMD")) {
            String cmd = meta.getPersistentDataContainer().getOrDefault(KEY_CMD, PersistentDataType.STRING, "none");
            lore.add("§8[Reward] §eCommand: §b/" + cmd);
        } else {
            lore.add("§8[Reward] §eAmount: §b" + amount);
        }

        lore.add("§8[Reward] §7----------------");
        lore.add("§8[Reward] §cShift-Left Click to Delete");
        lore.add("§8[Reward] §bLeft Click to Edit Amount/Cmd");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack cleanEditorItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemStack cleanItem = item.clone();
        ItemMeta meta = cleanItem.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            lore.removeIf(line -> line.contains("§8[Reward]"));
            meta.setLore(lore);
            cleanItem.setItemMeta(meta);
        }
        return cleanItem;
    }

    public static void open(Player player, String questId, String eventType, MasterDailyQuests plugin, ItemStack[] cachedContents) {
        player.openInventory(new QuestRewardGUI(player, questId, eventType, plugin, cachedContents).getInventory());
    }

    @Override
    public Inventory getInventory() { return inventory; }
    public String getQuestId() { return questId; }
    public String getEventType() { return eventType; }
    public MasterDailyQuests getPlugin() { return plugin; }
}