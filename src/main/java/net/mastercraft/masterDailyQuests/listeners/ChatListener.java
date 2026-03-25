package net.mastercraft.masterDailyQuests.listeners;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.gui.QuestConfigGUI;
import net.mastercraft.masterDailyQuests.gui.QuestRewardGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final MasterDailyQuests plugin;

    private static final Map<UUID, String> pendingInputs = new HashMap<>();
    private static final Map<UUID, String> pendingQuestIds = new HashMap<>();
    private static final Map<UUID, String> pendingQuestTypes = new HashMap<>();

    private static final Map<UUID, String> rewardEditUuids = new HashMap<>();
    private static final Map<UUID, String> rewardEditTypes = new HashMap<>();
    private static final Map<UUID, ItemStack[]> rewardCaches = new HashMap<>();

    public ChatListener(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    public static void addPendingInput(Player player, String inputType, String questId, String questEventType) {
        UUID uuid = player.getUniqueId();
        pendingInputs.put(uuid, inputType);
        pendingQuestIds.put(uuid, questId);
        pendingQuestTypes.put(uuid, questEventType);

        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Please type the " + inputType.toLowerCase() + " in chat. Type 'cancel' to abort.");
    }

    public static void addRewardEdit(Player player, String questId, String questType, String itemUuid, String editType, ItemStack[] cache) {
        UUID uuid = player.getUniqueId();
        pendingQuestIds.put(uuid, questId);
        pendingQuestTypes.put(uuid, questType);
        rewardEditUuids.put(uuid, itemUuid);
        rewardEditTypes.put(uuid, editType);
        rewardCaches.put(uuid, cache);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (rewardEditUuids.containsKey(uuid)) {
            event.setCancelled(true);
            String message = event.getMessage();
            String questId = pendingQuestIds.get(uuid);
            String questType = pendingQuestTypes.get(uuid);
            ItemStack[] cache = rewardCaches.get(uuid);

            if (!message.equalsIgnoreCase("cancel")) {
                String targetUuid = rewardEditUuids.get(uuid);
                String editType = rewardEditTypes.get(uuid);
                NamespacedKey keyUuid = new NamespacedKey(plugin, "reward_uuid");

                for (int i = 0; i < 45; i++) {
                    if (cache[i] != null && cache[i].hasItemMeta()) {
                        ItemMeta meta = cache[i].getItemMeta();
                        String itemUuid = meta.getPersistentDataContainer().get(keyUuid, PersistentDataType.STRING);

                        if (targetUuid.equals(itemUuid)) {
                            if (editType.equals("CMD")) {
                                NamespacedKey keyCmd = new NamespacedKey(plugin, "reward_cmd");
                                meta.getPersistentDataContainer().set(keyCmd, PersistentDataType.STRING, message.replace("/", ""));
                                player.sendMessage("§aCommand updated!");
                            } else {
                                try {
                                    int amt = Integer.parseInt(message);
                                    NamespacedKey keyAmt = new NamespacedKey(plugin, "reward_amount");
                                    meta.getPersistentDataContainer().set(keyAmt, PersistentDataType.INTEGER, amt);
                                    player.sendMessage("§aAmount updated!");
                                } catch (NumberFormatException e) {
                                    player.sendMessage("§cInvalid number. Edit cancelled.");
                                }
                            }
                            cache[i].setItemMeta(meta);
                            break;
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Edit cancelled.");
            }

            removePending(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> QuestRewardGUI.open(player, questId, questType, plugin, cache));
            return;
        }

        if (pendingInputs.containsKey(uuid)) {
            event.setCancelled(true);
            String message = event.getMessage();
            String inputType = pendingInputs.get(uuid);
            String questId = pendingQuestIds.get(uuid);
            String questType = pendingQuestTypes.get(uuid);

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Configuration cancelled.");
                removePending(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> QuestConfigGUI.open(player, questId, questType, plugin));
                return;
            }

            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);

            if (inputType.equalsIgnoreCase("TARGET")) {
                message = message.trim().replace(" ", "_").toUpperCase();
                qConf.set(inputType.toLowerCase(), message);
                player.sendMessage(ChatColor.GREEN + "Successfully set target to: " + ChatColor.WHITE + message);
            }
            else if (inputType.equalsIgnoreCase("AMOUNT")) {
                try {
                    int amount = Integer.parseInt(message.trim());
                    qConf.set(inputType.toLowerCase(), amount);
                    player.sendMessage(ChatColor.GREEN + "Successfully set amount to: " + ChatColor.WHITE + amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid number! Please type a valid integer.");
                    Bukkit.getScheduler().runTask(plugin, () -> QuestConfigGUI.open(player, questId, questType, plugin));
                    return;
                }
            }

            plugin.getQuestManager().saveQuest(questId);
            removePending(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> QuestConfigGUI.open(player, questId, questType, plugin));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        removePending(uuid);
        // Tells DataManager to save async and unload from RAM
        plugin.getDataManager().unloadPlayer(uuid);
    }

    private void removePending(UUID uuid) {
        pendingInputs.remove(uuid);
        pendingQuestIds.remove(uuid);
        pendingQuestTypes.remove(uuid);
        rewardEditUuids.remove(uuid);
        rewardEditTypes.remove(uuid);
        rewardCaches.remove(uuid);
    }
}