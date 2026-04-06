package net.mastercraft.masterDailyQuests.listeners;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestListener implements Listener {

    private final MasterDailyQuests plugin;

    public QuestListener(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    // Standard Cumulative Progress
    public void checkQuestProgress(Player player, String actionType, String actionTarget, int amountToAdd) {
        List<String> activeQuests = plugin.getDataManager().getActiveQuests(player.getUniqueId());
        if (activeQuests == null || activeQuests.isEmpty()) return;

        String formattedTarget = actionTarget.replace("minecraft:", "").toUpperCase();

        for (String questId : activeQuests) {
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
            if (qConf == null) continue;

            String type = qConf.getString("type", "").toUpperCase();
            if (type.equals(actionType)) {
                String target = qConf.getString("target", "ANY").toUpperCase();
                if (target.equals("ANY") || target.equals(formattedTarget)) {
                    processQuestProgress(player, questId, amountToAdd);
                }
            }
        }
    }

    // Absolute Progress (For "Reach Stage" Quests)
    public void updateQuestProgressAbsolute(Player player, String actionType, String actionTarget, int highestStageReached) {
        List<String> activeQuests = plugin.getDataManager().getActiveQuests(player.getUniqueId());
        if (activeQuests == null || activeQuests.isEmpty()) return;

        String formattedTarget = actionTarget.replace("minecraft:", "").toUpperCase();

        for (String questId : activeQuests) {
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
            if (qConf == null) continue;

            String type = qConf.getString("type", "").toUpperCase();
            if (type.equals(actionType)) {
                String target = qConf.getString("target", "ANY").toUpperCase();
                if (target.equals("ANY") || target.equals(formattedTarget)) {
                    processQuestProgressAbsolute(player, questId, highestStageReached);
                }
            }
        }
    }

    private void processQuestProgress(Player player, String questId, int amountToAdd) {
        FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
        if (qConf == null) return;

        int requiredAmount = qConf.getInt("amount", 1);
        int currentProgress = plugin.getDataManager().getProgress(player.getUniqueId(), questId);

        if (currentProgress < requiredAmount) {
            int newProgress = Math.min(currentProgress + amountToAdd, requiredAmount);
            plugin.getDataManager().setProgress(player.getUniqueId(), questId, newProgress);

            if (newProgress >= requiredAmount && currentProgress < requiredAmount) {
                grantQuestRewards(player, qConf);
            }
        }
    }

    private void processQuestProgressAbsolute(Player player, String questId, int stageReached) {
        FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
        if (qConf == null) return;

        int requiredAmount = qConf.getInt("amount", 1);
        int currentProgress = plugin.getDataManager().getProgress(player.getUniqueId(), questId);

        if (currentProgress < requiredAmount && stageReached > currentProgress) {
            int newProgress = Math.min(stageReached, requiredAmount);
            plugin.getDataManager().setProgress(player.getUniqueId(), questId, newProgress);

            if (newProgress >= requiredAmount) {
                grantQuestRewards(player, qConf);
            }
        }
    }

    private void grantQuestRewards(Player player, FileConfiguration qConf) {
        player.sendMessage("§a§lQuest Completed! §7You have received your rewards.");
        List<ItemStack> rewards = (List<ItemStack>) qConf.getList("rewards");
        if (rewards != null) {
            NamespacedKey keyType = new NamespacedKey(plugin, "reward_type");
            NamespacedKey keyAmount = new NamespacedKey(plugin, "reward_amount");
            NamespacedKey keyCmd = new NamespacedKey(plugin, "reward_cmd");

            for (ItemStack reward : rewards) {
                if (reward == null || reward.getType() == Material.AIR) continue;
                ItemMeta meta = reward.getItemMeta();
                if (meta == null) continue;

                String type = meta.getPersistentDataContainer().getOrDefault(keyType, PersistentDataType.STRING, "ITEM");
                int amount = meta.getPersistentDataContainer().getOrDefault(keyAmount, PersistentDataType.INTEGER, reward.getAmount());

                if (type.equals("EXP")) {
                    player.giveExp(amount);
                } else if (type.equals("MONEY")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + amount);
                } else if (type.equals("CMD")) {
                    String cmd = meta.getPersistentDataContainer().getOrDefault(keyCmd, PersistentDataType.STRING, "");
                    if (!cmd.isEmpty()) {
                        cmd = cmd.replace("%player%", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                } else {
                    ItemStack giveItem = reward.clone();
                    giveItem.setAmount(amount);
                    ItemMeta giveMeta = giveItem.getItemMeta();
                    if (giveMeta != null) {
                        giveMeta.getPersistentDataContainer().remove(keyType);
                        giveMeta.getPersistentDataContainer().remove(keyAmount);
                        giveMeta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "reward_uuid"));
                        giveItem.setItemMeta(giveMeta);
                    }
                    player.getInventory().addItem(giveItem);
                }
            }
        }
    }

    // ==========================================
    // UTILITY: ITEMSADDER ITEM MATCHER
    // ==========================================
    private String getItemsAdderId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                ClassLoader iaLoader = Bukkit.getPluginManager().getPlugin("ItemsAdder").getClass().getClassLoader();
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack", true, iaLoader);
                Object customStack = customStackClass.getMethod("byItemStack", org.bukkit.inventory.ItemStack.class).invoke(null, item);
                if (customStack != null) {
                    return (String) customStackClass.getMethod("getNamespacedID").invoke(customStack);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }


    // ==========================================
    // EVENTS
    // ==========================================

    private final Map<UUID, String> pendingIAMines = new HashMap<>();

    // 1. Catches the block before ItemsAdder can destroy it
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMineLowest(BlockBreakEvent event) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) return;

        try {
            ClassLoader iaLoader = Bukkit.getPluginManager().getPlugin("ItemsAdder").getClass().getClassLoader();
            Class<?> customBlockClass = Class.forName("dev.lone.itemsadder.api.CustomBlock", true, iaLoader);
            Object customBlock = customBlockClass.getMethod("byAlreadyPlaced", org.bukkit.block.Block.class).invoke(null, event.getBlock());

            if (customBlock != null) {
                String id = (String) customBlockClass.getMethod("getNamespacedID").invoke(customBlock);
                pendingIAMines.put(event.getPlayer().getUniqueId(), "IA:" + id);
            }
        } catch (Exception ignored) {}
    }

    // 2. Processes the quest logic after all plugins have fired
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String target = event.getBlock().getType().name();

        // Check if we caught an ItemsAdder block for this player a millisecond ago
        if (pendingIAMines.containsKey(player.getUniqueId())) {
            target = pendingIAMines.remove(player.getUniqueId());

            // ItemsAdder INTENTIONALLY cancels the event to prevent the vanilla block
            // from dropping. We must grant quest progress even if it is cancelled!
            checkQuestProgress(player, "MINING", target, 1);
            return;
        }

        // For standard vanilla blocks, respect WorldGuard/protection plugins
        if (event.isCancelled()) return;

        checkQuestProgress(player, "MINING", target, 1);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            LivingEntity deadEntity = event.getEntity();

            String target = deadEntity.getType().name();

            if (Bukkit.getPluginManager().isPluginEnabled("MasterDungeons")) {
                NamespacedKey mdKey = new NamespacedKey("masterdungeons", "md_mob_id");
                if (deadEntity.getPersistentDataContainer().has(mdKey, PersistentDataType.STRING)) {
                    target = "MD:" + deadEntity.getPersistentDataContainer().get(mdKey, PersistentDataType.STRING);
                }
            }

            if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
                try {
                    String mmId = getMythicMobId(deadEntity);
                    if (mmId != null) {
                        target = "MYTHIC:" + mmId;
                    }
                } catch (Exception ignored) {}
            }

            checkQuestProgress(player, "KILLING", target, 1);
        }
    }

    private String getMythicMobId(org.bukkit.entity.Entity entity) {
        if (io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().isMythicMob(entity)) {
            return io.lumine.mythic.bukkit.MythicBukkit.inst().getAPIHelper().getMythicMobInstance(entity).getType().getInternalName();
        }
        return null;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack result = event.getRecipe().getResult();
            String target = result.getType().name();

            String iaId = getItemsAdderId(result);
            if (iaId != null) target = "IA:" + iaId;

            checkQuestProgress(player, "CRAFTING", target, result.getAmount());
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        String target = event.getItem().getType().name();

        String iaId = getItemsAdderId(event.getItem());
        if (iaId != null) target = "IA:" + iaId;

        checkQuestProgress(player, "ENCHANTING", target, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        String target = clickedItem.getType().name();
        String iaId = getItemsAdderId(clickedItem);
        if (iaId != null) target = "IA:" + iaId;

        if (event.getClickedInventory().getType() == InventoryType.BREWING) {
            if (event.getSlot() >= 0 && event.getSlot() <= 2) {
                checkQuestProgress(player, "BREWING", target, 1);
            }
        }

        if (event.getClickedInventory().getType() == InventoryType.MERCHANT) {
            if (event.getSlot() == 2) {
                checkQuestProgress(player, "TRADING", target, clickedItem.getAmount());
            }
        }
    }
}