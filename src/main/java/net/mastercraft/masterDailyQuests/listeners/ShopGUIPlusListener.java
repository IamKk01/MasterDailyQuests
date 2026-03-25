package net.mastercraft.masterDailyQuests.listeners;

import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopManager.ShopAction;
import net.brcdev.shopgui.shop.ShopTransactionResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ShopGUIPlusListener implements Listener {

    private final QuestListener questListener;

    public ShopGUIPlusListener(QuestListener questListener) {
        this.questListener = questListener;
    }

    @EventHandler
    public void onShopTransaction(ShopPostTransactionEvent event) {
        // In the ShopGUI+ API, we must get the transaction details from the 'Result' object
        ShopTransactionResult result = event.getResult();

        Player player = result.getPlayer();
        ItemStack item = result.getShopItem().getItem();
        String target = item.getType().name();
        int amount = result.getAmount();
        ShopAction action = result.getShopAction();

        // Check the action and apply progress
        if (action == ShopAction.BUY) {
            questListener.checkQuestProgress(player, "BUYING", target, amount);
        } else if (action == ShopAction.SELL || action == ShopAction.SELL_ALL) {
            questListener.checkQuestProgress(player, "SELLING", target, amount);
        }
    }
}