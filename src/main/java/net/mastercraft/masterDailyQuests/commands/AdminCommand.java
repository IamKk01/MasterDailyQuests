package net.mastercraft.masterDailyQuests.commands;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.gui.AdminEditorGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

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
            AdminEditorGUI.open(player, plugin, 0);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("mdailyquests.admin.*")) {
                commands.add("editor");
                commands.add("reroll");
            }
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("reroll") && sender.hasPermission("mdailyquests.admin.*")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    commands.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], commands, completions);
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reroll") && sender.hasPermission("mdailyquests.admin.*")) {
                commands.add("all");
                int maxSlots = plugin.getConfigManager().getQuestSlots().size();
                for (int i = 1; i <= maxSlots; i++) {
                    commands.add(String.valueOf(i));
                }
                StringUtil.copyPartialMatches(args[2], commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}