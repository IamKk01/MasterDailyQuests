package net.mastercraft.masterDailyQuests.listeners;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.gui.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class InventoryListener implements Listener {

    private final MasterDailyQuests plugin;

    public InventoryListener(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        InventoryHolder clickedHolder = event.getClickedInventory().getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getRawSlot();

        // --------------------------------------------------------
        // 1. Admin Editor GUI (Main Menu)
        // --------------------------------------------------------
        if (topHolder instanceof AdminEditorGUI gui) {
            event.setCancelled(true);
            if (clickedHolder == topHolder && clickedItem != null) {
                if (clickedItem.getType() == Material.ANVIL) {
                    QuestSelectGUI.open(player);
                } else if (clickedItem.getType() == Material.ARROW && clickedItem.hasItemMeta()) {
                    String name = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                    if (name.equals("Previous Page")) {
                        AdminEditorGUI.open(player, plugin, gui.getPage() - 1);
                    } else if (name.equals("Next Page")) {
                        AdminEditorGUI.open(player, plugin, gui.getPage() + 1);
                    }
                } else if (clickedItem.getType() == Material.BOOK && clickedItem.hasItemMeta()) {
                    String questId = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()).replace("Quest: ", "");
                    String eventType = plugin.getQuestManager().getQuest(questId).getString("type", "UNKNOWN");
                    QuestConfigGUI.open(player, questId, eventType, plugin);
                }
            }
            return;
        }

        // --------------------------------------------------------
        // 2. Quest Selection GUI
        // --------------------------------------------------------
        if (topHolder instanceof QuestSelectGUI) {
            event.setCancelled(true);
            if (clickedHolder == topHolder && clickedItem != null && clickedItem.hasItemMeta()) {
                String eventType = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                String questId = eventType + "_" + (System.currentTimeMillis() % 100000);

                plugin.getQuestManager().createQuest(questId, eventType);
                QuestConfigGUI.open(player, questId, eventType, plugin);
            }
            return;
        }

        // --------------------------------------------------------
        // 3. Quest Configuration GUI
        // --------------------------------------------------------
        if (topHolder instanceof QuestConfigGUI gui) {
            event.setCancelled(true);
            if (clickedHolder == topHolder && clickedItem != null) {
                if (clickedItem.getType() == Material.LIME_DYE) {
                    AdminEditorGUI.open(player, plugin, 0);
                } else if (clickedItem.getType() == Material.NAME_TAG) {
                    ChatListener.addPendingInput(player, "TARGET", gui.getQuestId(), gui.getEventType());
                } else if (clickedItem.getType() == Material.PAPER) {
                    ChatListener.addPendingInput(player, "AMOUNT", gui.getQuestId(), gui.getEventType());
                } else if (clickedItem.getType() == Material.GOLD_BLOCK) {
                    QuestRewardGUI.open(player, gui.getQuestId(), gui.getEventType(), plugin, null);
                }
            }
            return;
        }

        // --------------------------------------------------------
        // 4. Player Main GUI
        // --------------------------------------------------------
        if (topHolder instanceof PlayerMainGUI) {
            event.setCancelled(true);
            return;
        }

        // --------------------------------------------------------
        // 5. Quest Reward GUI
        // --------------------------------------------------------
        if (topHolder instanceof QuestRewardGUI gui) {
            Inventory topInv = event.getView().getTopInventory();

            if (clickedHolder == event.getView().getBottomInventory()) {
                if (event.getClick().isShiftClick()) {
                    event.setCancelled(true);
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        addItemToEditor(topInv, clickedItem, player, gui);
                    }
                }
                return;
            }

            if (clickedHolder == topHolder) {
                event.setCancelled(true);

                if (slot >= 45 && slot <= 53) {
                    if (clickedItem != null && clickedItem.hasItemMeta()) {
                        String typeStr = clickedItem.getItemMeta().getPersistentDataContainer().get(gui.KEY_BTN, PersistentDataType.STRING);
                        if (typeStr != null) {
                            if (typeStr.equals("BTN_SAVE")) {
                                List<ItemStack> savedLoot = new ArrayList<>();
                                for (int i = 0; i < 45; i++) {
                                    ItemStack item = topInv.getItem(i);
                                    if (item != null && item.getType() != Material.AIR) {
                                        savedLoot.add(gui.cleanEditorItem(item));
                                    }
                                }

                                plugin.getQuestManager().getQuest(gui.getQuestId()).set("rewards", savedLoot);
                                plugin.getQuestManager().saveQuest(gui.getQuestId());

                                player.closeInventory();
                                player.sendMessage("§aRewards successfully saved!");
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                                QuestConfigGUI.open(player, gui.getQuestId(), gui.getEventType(), plugin);

                            } else {
                                int firstEmpty = getFirstEmptyLootSlot(topInv);
                                if (firstEmpty != -1) {
                                    ItemStack specialItem = null;
                                    if (typeStr.equals("BTN_EXP")) {
                                        specialItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
                                        setSpecialMeta(specialItem, "§a§lExp Reward", "EXP", gui);
                                    } else if (typeStr.equals("BTN_MONEY")) {
                                        specialItem = new ItemStack(Material.GOLD_INGOT);
                                        setSpecialMeta(specialItem, "§e§lMoney Reward", "MONEY", gui);
                                    } else if (typeStr.equals("BTN_CMD")) {
                                        specialItem = new ItemStack(Material.COMMAND_BLOCK);
                                        setSpecialMeta(specialItem, "§c§lCommand Reward", "CMD", gui);
                                    }

                                    if (specialItem != null) {
                                        topInv.setItem(firstEmpty, gui.formatForEditor(specialItem));
                                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
                                    }
                                } else {
                                    player.sendMessage("§cReward editor is full!");
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                                }
                            }
                        }
                    }
                    return;
                }

                if (slot >= 0 && slot <= 44) {
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        addItemToEditor(topInv, cursor, player, gui);
                        event.getView().setCursor(null);
                        return;
                    }

                    if (clickedItem != null && clickedItem.getType() != Material.AIR && clickedItem.hasItemMeta()) {
                        String uuid = clickedItem.getItemMeta().getPersistentDataContainer().get(gui.KEY_UUID, PersistentDataType.STRING);
                        if (uuid != null) {
                            if (event.getClick().isShiftClick() && event.getClick().isLeftClick()) {
                                topInv.setItem(slot, null);
                                compactLootEditor(topInv);
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                            } else if (event.getClick().isLeftClick() && !event.getClick().isShiftClick()) {
                                String type = clickedItem.getItemMeta().getPersistentDataContainer().getOrDefault(gui.KEY_TYPE, PersistentDataType.STRING, "ITEM");

                                ChatListener.addRewardEdit(player, gui.getQuestId(), gui.getEventType(), uuid, type, topInv.getContents());
                                player.closeInventory();

                                if (type.equals("CMD")) {
                                    player.sendMessage("§aType the Command without '/'. Use %player% for the player's name. Type 'cancel' to abort.");
                                } else {
                                    player.sendMessage("§aType the Amount. Type 'cancel' to abort.");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setSpecialMeta(ItemStack item, String name, String type, QuestRewardGUI gui) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.getPersistentDataContainer().set(gui.KEY_TYPE, PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
    }

    private void addItemToEditor(Inventory topInv, ItemStack item, Player player, QuestRewardGUI gui) {
        int firstEmpty = getFirstEmptyLootSlot(topInv);
        if (firstEmpty != -1) {
            ItemStack toAdd = item.clone();
            toAdd.setAmount(1);
            topInv.setItem(firstEmpty, gui.formatForEditor(toAdd));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
        } else {
            player.sendMessage("§cReward editor is full!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private int getFirstEmptyLootSlot(Inventory inv) {
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) return i;
        }
        return -1;
    }

    private void compactLootEditor(Inventory inv) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
                inv.setItem(i, null);
            }
        }
        for (int i = 0; i < items.size(); i++) inv.setItem(i, items.get(i));
    }
}