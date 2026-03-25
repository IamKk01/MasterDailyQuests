package net.mastercraft.masterDailyQuests.commands;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.gui.AdminEditorGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final MasterDailyQuests plugin;

    public AdminCommand(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mdailyquests.admin.*")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("editor")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use the visual editor.");
                return true;
            }
            AdminEditorGUI.open(player, plugin, 0); // Open on Page 0
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("reroll")) {
            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            if (args[2].equalsIgnoreCase("all")) {
                plugin.getDataManager().rerollAll(target.getUniqueId());
                sender.sendMessage("§aSuccessfully rerolled all quests for " + target.getName());
                target.sendMessage("§eYour daily quests have been completely reset!");
            } else {
                try {
                    int pos = Integer.parseInt(args[2]);
                    plugin.getDataManager().rerollQuest(target.getUniqueId(), pos);
                    sender.sendMessage("§aSuccessfully rerolled quest " + pos + " for " + target.getName());
                    target.sendMessage("§eYour daily quest in slot " + pos + " has been rerolled/reset!");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cPosition must be a valid number or 'all'.");
                }
            }
            return true;
        }

        sender.sendMessage("§eMasterDailyQuests Admin Commands:");
        sender.sendMessage("§e/dqa editor §7- Opens the GUI editor");
        sender.sendMessage("§e/dqa reroll <player> <position/all> §7- Rerolls or resets a player's quest");
        return true;
    }
}