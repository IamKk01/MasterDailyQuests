package net.mastercraft.masterDailyQuests.listeners;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

import java.util.List;

public class QuestListener implements Listener {

    private final MasterDailyQuests plugin;

    public QuestListener(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    public void checkQuestProgress(Player player, String actionType, String actionTarget, int amountToAdd) {
        List<String> activeQuests = plugin.getDataManager().getActiveQuests(player.getUniqueId());
        if (activeQuests == null || activeQuests.isEmpty()) return;

        // Formats everything to uppercase so "MYTHIC:SkeletonKing" matches "MYTHIC:SKELETONKING" safely
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

    private void processQuestProgress(Player player, String questId, int amountToAdd) {
        FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
        if (qConf == null) return;

        int requiredAmount = qConf.getInt("amount", 1);
        int currentProgress = plugin.getDataManager().getProgress(player.getUniqueId(), questId);

        if (currentProgress < requiredAmount) {
            int newProgress = currentProgress + amountToAdd;
            if (newProgress > requiredAmount) {
                newProgress = requiredAmount;
            }

            plugin.getDataManager().setProgress(player.getUniqueId(), questId, newProgress);

            if (newProgress >= requiredAmount && currentProgress < requiredAmount) {
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
        }
    }

    // ==========================================
    // EVENTS
    // ==========================================

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        String target = event.getBlock().getType().name();
        checkQuestProgress(event.getPlayer(), "MINING", target, 1);
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            LivingEntity deadEntity = event.getEntity();

            // 1. Default Vanilla Target
            String target = deadEntity.getType().name();

            // 2. Check for MasterDungeons Custom Mob
            if (Bukkit.getPluginManager().isPluginEnabled("MasterDungeons")) {
                NamespacedKey mdKey = new NamespacedKey("masterdungeons", "md_mob_id");
                if (deadEntity.getPersistentDataContainer().has(mdKey, PersistentDataType.STRING)) {
                    target = "MD:" + deadEntity.getPersistentDataContainer().get(mdKey, PersistentDataType.STRING);

                    // Tip: If you ever want to check levels in the future, you can pull it here:
                    // int mobLevel = deadEntity.getPersistentDataContainer().getOrDefault(new NamespacedKey("masterdungeons", "md_mob_level"), PersistentDataType.INTEGER, 1);
                }
            }

            // 3. Check for MythicMobs Custom Mob
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

    // Isolated in a helper method to prevent ClassNotFoundExceptions if MythicMobs isn't installed
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
            checkQuestProgress(player, "CRAFTING", result.getType().name(), result.getAmount());
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        String target = event.getItem().getType().name();
        checkQuestProgress(player, "ENCHANTING", target, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (event.getClickedInventory().getType() == InventoryType.BREWING) {
            if (event.getSlot() >= 0 && event.getSlot() <= 2) {
                checkQuestProgress(player, "BREWING", clickedItem.getType().name(), 1);
            }
        }

        if (event.getClickedInventory().getType() == InventoryType.MERCHANT) {
            if (event.getSlot() == 2) {
                checkQuestProgress(player, "TRADING", clickedItem.getType().name(), clickedItem.getAmount());
            }
        }
    }
}