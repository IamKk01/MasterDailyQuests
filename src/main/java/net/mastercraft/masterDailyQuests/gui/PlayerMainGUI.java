package net.mastercraft.masterDailyQuests.gui;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PlayerMainGUI implements InventoryHolder {

    private final Inventory inventory;

    public PlayerMainGUI(Player player, MasterDailyQuests plugin) {
        ConfigManager config = plugin.getConfigManager();

        String title = config.getPlayerGuiName().replace("&", "§");
        int size = config.getPlayerGuiSize();

        this.inventory = Bukkit.createInventory(this, size, title);

        renderCustomItems(player, plugin, size);

        List<Integer> slots = config.getQuestSlots();
        List<String> activeQuests = plugin.getDataManager().getActiveQuests(player.getUniqueId());

        if (activeQuests != null && !activeQuests.isEmpty()) {
            int slotIndex = 0;

            for (String questId : activeQuests) {
                if (slotIndex >= slots.size()) break;

                FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
                if (qConf == null) continue;

                int slot = slots.get(slotIndex);
                String type = qConf.getString("type", "UNKNOWN");
                String target = qConf.getString("target", "ANY");
                int amount = qConf.getInt("amount", 1);

                int progress = plugin.getDataManager().getProgress(player.getUniqueId(), questId);
                boolean isCompleted = progress >= amount;

                Material displayMaterial = getMaterialForType(type);
                ItemStack questItem = new ItemStack(displayMaterial);
                ItemMeta meta = questItem.getItemMeta();

                if (meta != null) {
                    String questName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + " Task";
                    meta.setDisplayName("§3" + questName);

                    // Uses the new fetcher to get real Mob/Item names
                    String realTargetName = plugin.getRealTargetName(target);
                    String taskStr = "§7" + getActionWord(type) + " " + amount + " " + realTargetName;

                    List<String> dynamicLore = new ArrayList<>();

                    dynamicLore.add("§3Task:");
                    dynamicLore.add("§3▌ " + taskStr);
                    dynamicLore.add("");

                    dynamicLore.add("§3Progress:");
                    if (isCompleted) {
                        dynamicLore.add("§3▌ §c§nCompleted");
                    } else {
                        dynamicLore.add("§3▌ §6Progress: §f(" + progress + "/" + amount + ")");
                    }
                    dynamicLore.add("");

                    dynamicLore.add("§3Rewards:");

                    List<ItemStack> rewards = (List<ItemStack>) qConf.getList("rewards");
                    if (rewards == null || rewards.isEmpty()) {
                        dynamicLore.add("§3▌ §7- None");
                    } else {
                        org.bukkit.NamespacedKey keyType = new org.bukkit.NamespacedKey(plugin, "reward_type");
                        org.bukkit.NamespacedKey keyAmount = new org.bukkit.NamespacedKey(plugin, "reward_amount");

                        for (ItemStack reward : rewards) {
                            if (reward == null || reward.getType() == Material.AIR) continue;
                            ItemMeta rMeta = reward.getItemMeta();
                            if (rMeta == null) continue;

                            String rType = rMeta.getPersistentDataContainer().getOrDefault(keyType, PersistentDataType.STRING, "ITEM");
                            int rAmount = rMeta.getPersistentDataContainer().getOrDefault(keyAmount, PersistentDataType.INTEGER, reward.getAmount());

                            String rewardName;
                            if (rType.equals("EXP")) {
                                rewardName = "§a" + rAmount + " EXP";
                            } else if (rType.equals("MONEY")) {
                                rewardName = "§e$" + rAmount;
                            } else if (rType.equals("CMD")) {
                                rewardName = rMeta.hasDisplayName() ? rMeta.getDisplayName() : "§cSpecial Reward";
                            } else {
                                String itemName = rMeta.hasDisplayName() ? rMeta.getDisplayName() : "§f" + plugin.formatFriendlyName(reward.getType().name());
                                rewardName = "§b" + rAmount + "x " + itemName;
                            }

                            dynamicLore.add("§3▌ §7- " + rewardName);
                        }
                    }

                    meta.setLore(dynamicLore);
                    questItem.setItemMeta(meta);
                }

                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, questItem);
                }
                slotIndex++;
            }
        }
    }

    private void renderCustomItems(Player player, MasterDailyQuests plugin, int invSize) {
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("player_interface.items");
        if (itemsSection == null) return;

        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        for (String key : itemsSection.getKeys(false)) {
            String basePath = "player_interface.items." + key + ".item.";

            String matName = plugin.getConfig().getString(basePath + "material", "PAPER");
            Material mat = Material.getMaterial(matName.toUpperCase());
            if (mat == null) mat = Material.PAPER;

            ItemStack customItem = new ItemStack(mat);
            ItemMeta meta = customItem.getItemMeta();

            if (meta != null) {
                if (plugin.getConfig().contains(basePath + "custom_model_data")) {
                    meta.setCustomModelData(plugin.getConfig().getInt(basePath + "custom_model_data"));
                }

                boolean usePlaceholders = plugin.getConfig().getBoolean("player_interface.items." + key + ".use_placeholders", false);

                String name = plugin.getConfig().getString(basePath + "name", "");
                name = ChatColor.translateAlternateColorCodes('&', name);
                if (usePlaceholders && hasPapi) {
                    name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
                }
                meta.setDisplayName(name);

                List<String> lore = plugin.getConfig().getStringList(basePath + "lore");
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    line = ChatColor.translateAlternateColorCodes('&', line);
                    if (usePlaceholders && hasPapi) {
                        line = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, line);
                    }
                    coloredLore.add(line);
                }
                meta.setLore(coloredLore);
                customItem.setItemMeta(meta);
            }

            List<Integer> itemSlots = plugin.getConfig().getIntegerList(basePath + "slot");
            for (int s : itemSlots) {
                if (s >= 0 && s < invSize) {
                    inventory.setItem(s, customItem);
                }
            }
        }
    }

    private Material getMaterialForType(String type) {
        switch (type.toUpperCase()) {
            case "MINING": return Material.DIAMOND_PICKAXE;
            case "KILLING": return Material.DIAMOND_SWORD;
            case "CRAFTING": return Material.CRAFTING_TABLE;
            case "BREWING": return Material.BREWING_STAND;
            case "ENCHANTING": return Material.ENCHANTING_TABLE;
            case "SELLING": return Material.GOLD_INGOT;
            case "TRADING": return Material.EMERALD;
            case "BUYING": return Material.CHEST;
            default: return Material.WRITTEN_BOOK;
        }
    }

    private String getActionWord(String type) {
        switch (type.toUpperCase()) {
            case "MINING": return "Mine";
            case "KILLING": return "Kill";
            case "CRAFTING": return "Craft";
            case "BREWING": return "Brew";
            case "ENCHANTING": return "Enchant";
            case "SELLING": return "Sell";
            case "TRADING": return "Trade";
            case "BUYING": return "Buy";
            default: return "Complete";
        }
    }

    public static void open(Player player, MasterDailyQuests plugin) {
        player.openInventory(new PlayerMainGUI(player, plugin).getInventory());
    }

    @Override
    public Inventory getInventory() { return inventory; }
}