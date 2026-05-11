package net.mastercraft.masterDailyQuests.managers;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConfigManager {

    private final MasterDailyQuests plugin;

    public ConfigManager(MasterDailyQuests plugin) {
        this.plugin = plugin;
    }

    public int getPlayerGuiSize() {
        return plugin.getConfig().getInt("player_interface.size", 27);
    }

    public String getPlayerGuiName() {
        return plugin.getConfig().getString("player_interface.inventory_name", "Daily Quests");
    }

    public List<Integer> getQuestSlots() {
        List<Integer> slots = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("player_interface.quests");

        if (section != null) {
            List<String> keys = new ArrayList<>(section.getKeys(false));
            keys.sort(Comparator.comparingInt(Integer::parseInt));

            for (String key : keys) {
                slots.add(section.getInt(key));
            }
        }
        return slots;
    }

    // --- NEW: Gets the specific slot for Easy, Medium, or Hard quests ---
    public int getDifficultySlot(String difficulty) {
        return plugin.getConfig().getInt("player_interface.difficulty_slots." + difficulty.toUpperCase(), -1);
    }

    public void reload() {
        plugin.reloadConfig();
    }
}