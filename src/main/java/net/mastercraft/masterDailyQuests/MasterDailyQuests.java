package net.mastercraft.masterDailyQuests;

import net.mastercraft.masterDailyQuests.commands.AdminCommand;
import net.mastercraft.masterDailyQuests.commands.PlayerCommand;
import net.mastercraft.masterDailyQuests.listeners.ChatListener;
import net.mastercraft.masterDailyQuests.listeners.InventoryListener;
import net.mastercraft.masterDailyQuests.listeners.QuestListener;
import net.mastercraft.masterDailyQuests.listeners.ShopGUIPlusListener;
import net.mastercraft.masterDailyQuests.managers.ConfigManager;
import net.mastercraft.masterDailyQuests.managers.DataManager;
import net.mastercraft.masterDailyQuests.managers.QuestManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class MasterDailyQuests extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private QuestManager questManager;

    // THE OPTIMIZATION: Stores names in RAM so we don't spam Reflection
    private final Map<String, String> nameCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        questManager = new QuestManager(this);
        dataManager = new DataManager(this);

        getCommand("dailyquests").setExecutor(new PlayerCommand(this));
        getCommand("dqa").setExecutor(new AdminCommand(this));

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        QuestListener questListener = new QuestListener(this);
        getServer().getPluginManager().registerEvents(questListener, this);

        if (getServer().getPluginManager().getPlugin("ShopGUIPlus") != null) {
            getServer().getPluginManager().registerEvents(new ShopGUIPlusListener(questListener), this);
            getLogger().info("Successfully hooked into ShopGUI+!");
        }

        getLogger().info("MasterDailyQuests has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllCachedData();
            getLogger().info("Saved all player quest data successfully.");
        }
        nameCache.clear(); // Clear memory on shutdown
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public QuestManager getQuestManager() { return questManager; }

    // ==========================================
    // UTILITY: DYNAMIC NAME FETCHER (Optimized)
    // ==========================================

    public String getRealTargetName(String raw) {
        if (raw == null || raw.equalsIgnoreCase("ANY")) return "Any";

        // 1. Check if we already found this name recently! (Instant lookup)
        if (nameCache.containsKey(raw)) {
            return nameCache.get(raw);
        }

        String resolvedName = null;

        // 2. Check for MasterDungeons Mobs
        if (raw.startsWith("MD:")) {
            String id = raw.substring(3);
            if (getServer().getPluginManager().isPluginEnabled("MasterDungeons")) {
                try {
                    Plugin mdPlugin = getServer().getPluginManager().getPlugin("MasterDungeons");
                    if (mdPlugin != null) {
                        Object mobManager = mdPlugin.getClass().getMethod("getMobManager").invoke(mdPlugin);
                        Object mob = mobManager.getClass().getMethod("getMob", String.class).invoke(mobManager, id);

                        if (mob != null) {
                            String realName = null;
                            try { realName = (String) mob.getClass().getMethod("getName").invoke(mob); } catch (Exception e1) {
                                try { realName = (String) mob.getClass().getField("name").get(mob); } catch (Exception e2) {
                                    try { realName = (String) mob.getClass().getField("displayName").get(mob); } catch (Exception e3) {}
                                }
                            }
                            if (realName != null) resolvedName = ChatColor.translateAlternateColorCodes('&', realName);
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (resolvedName == null) resolvedName = formatFriendlyName(id);
        }

        // 3. Check for MythicMobs
        else if (raw.startsWith("MYTHIC:")) {
            String id = raw.substring(7);
            if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
                try {
                    resolvedName = getMythicName(id);
                } catch (Exception ignored) {}
            }
            if (resolvedName == null) resolvedName = formatFriendlyName(id);
        }

        // 4. Vanilla items/mobs
        else {
            resolvedName = formatFriendlyName(raw);
        }

        // Save the result to our RAM cache so we never have to run the heavy code for this mob again
        nameCache.put(raw, resolvedName);
        return resolvedName;
    }

    private String getMythicName(String id) {
        io.lumine.mythic.api.mobs.MythicMob mm = io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getMythicMob(id).orElse(null);
        if (mm != null && mm.getDisplayName() != null) {
            return ChatColor.translateAlternateColorCodes('&', mm.getDisplayName().get());
        }
        return null;
    }

    public String formatFriendlyName(String raw) {
        String[] words = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}