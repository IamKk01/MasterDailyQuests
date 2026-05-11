package net.mastercraft.masterDailyQuests.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.OfflinePlayer;

public class PAPIExpansion extends PlaceholderExpansion {

    private final MasterDailyQuests plugin;

    public PAPIExpansion(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "mdailyquests";
    }

    @Override
    public String getAuthor() {
        return "IamKk01"; // Your username!
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equalsIgnoreCase("time_left")) {
            return plugin.getTimeUntilResetFormatted();
        }
        return null; // Invalid placeholder
    }
}