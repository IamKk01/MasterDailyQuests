package net.mastercraft.masterDailyQuests.managers;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class QuestManager {

    private final MasterDailyQuests plugin;
    private final File questsFolder;
    private final Map<String, FileConfiguration> questConfigs = new HashMap<>();

    public QuestManager(MasterDailyQuests plugin) {
        this.plugin = plugin;
        this.questsFolder = new File(plugin.getDataFolder(), "quests");

        if (!questsFolder.exists()) {
            questsFolder.mkdirs();
        }

        loadQuests();
    }

    public void loadQuests() {
        questConfigs.clear();
        File[] files = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String id = file.getName().replace(".yml", "");
                questConfigs.put(id, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public FileConfiguration getQuest(String id) {
        return questConfigs.get(id);
    }

    public Set<String> getQuestIds() {
        return questConfigs.keySet();
    }

    public void createQuest(String id, String type) {
        File file = new File(questsFolder, id + ".yml");
        try {
            if (file.createNewFile()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("type", type);
                config.set("target", "ANY");
                config.set("amount", 1);
                config.save(file);
                questConfigs.put(id, config);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create quest file for " + id);
            e.printStackTrace();
        }
    }

    public void saveQuest(String id) {
        if (questConfigs.containsKey(id)) {
            File file = new File(questsFolder, id + ".yml");
            try {
                questConfigs.get(id).save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save quest file for " + id);
                e.printStackTrace();
            }
        }
    }

    // ==========================================
    // NEW: Safely delete from RAM and Disk
    // ==========================================
    public void deleteQuest(String id) {
        // 1. Remove it from the live server memory
        if (questConfigs.containsKey(id)) {
            questConfigs.remove(id);
        }

        // 2. Permanently delete the configuration file
        File file = new File(questsFolder, id + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }
}