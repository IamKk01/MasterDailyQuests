package net.mastercraft.masterDailyQuests;

import net.mastercraft.masterDailyQuests.commands.AdminCommand;
import net.mastercraft.masterDailyQuests.commands.PlayerCommand;
import net.mastercraft.masterDailyQuests.listeners.ChatListener;
import net.mastercraft.masterDailyQuests.listeners.DungeonEventListener;
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

    private final Map<String, String> nameCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        questManager = new QuestManager(this);
        dataManager = new DataManager(this);

        // --- COMMAND REGISTRATION WITH TAB COMPLETION ---
        PlayerCommand playerCmd = new PlayerCommand(this);
        getCommand("dailyquests").setExecutor(playerCmd);
        getCommand("dailyquests").setTabCompleter(playerCmd);

        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("dqa").setExecutor(adminCmd);
        getCommand("dqa").setTabCompleter(adminCmd);
        // ------------------------------------------------

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        QuestListener questListener = new QuestListener(this);
        getServer().getPluginManager().registerEvents(questListener, this);

        if (getServer().getPluginManager().getPlugin("ShopGUIPlus") != null) {
            getServer().getPluginManager().registerEvents(new ShopGUIPlusListener(questListener), this);
            getLogger().info("Successfully hooked into ShopGUI+!");
        }

        if (getServer().getPluginManager().getPlugin("MasterDungeons") != null) {
            getServer().getPluginManager().registerEvents(new DungeonEventListener(questListener), this);
            getLogger().info("Successfully hooked into MasterDungeons Events!");
        }

        getLogger().info("MasterDailyQuests has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllCachedData();
            getLogger().info("Saved all player quest data successfully.");
        }
        nameCache.clear();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public QuestManager getQuestManager() { return questManager; }

    public String getRealTargetName(String raw) {
        if (raw == null || raw.equalsIgnoreCase("ANY")) return "Any";

        if (nameCache.containsKey(raw)) {
            return nameCache.get(raw);
        }

        String resolvedName = null;

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

        else if (raw.startsWith("MYTHIC:")) {
            String id = raw.substring(7);
            if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
                try {
                    resolvedName = getMythicName(id);
                } catch (Exception ignored) {}
            }
            if (resolvedName == null) resolvedName = formatFriendlyName(id);
        }

        else {
            resolvedName = formatFriendlyName(raw);
        }

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