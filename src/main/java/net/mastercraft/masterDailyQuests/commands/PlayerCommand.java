package net.mastercraft.masterDailyQuests.commands;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import net.mastercraft.masterDailyQuests.gui.PlayerMainGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommand implements CommandExecutor {

    private final MasterDailyQuests plugin;

    public PlayerCommand(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("mdailyquests.use")) {
            player.sendMessage("§cYou do not have permission to use this.");
            return true;
        }

        // Updated to pass the main plugin instance
        PlayerMainGUI.open(player, plugin);
        return true;
    }
}