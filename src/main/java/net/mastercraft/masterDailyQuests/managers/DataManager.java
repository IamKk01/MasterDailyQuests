package net.mastercraft.masterDailyQuests.managers;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final MasterDailyQuests plugin;
    private final File playerDataFolder;

    private final Map<UUID, FileConfiguration> playerCache = new HashMap<>();

    public DataManager(MasterDailyQuests plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllCachedData, 12000L, 12000L);
    }

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    public FileConfiguration getPlayerData(UUID uuid) {
        if (playerCache.containsKey(uuid)) {
            return playerCache.get(uuid);
        }

        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        playerCache.put(uuid, config);
        return config;
    }

    public void savePlayerDataAsync(UUID uuid) {
        if (playerCache.containsKey(uuid)) {
            FileConfiguration config = playerCache.get(uuid);
            File file = getPlayerFile(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save data for " + uuid);
                }
            });
        }
    }

    public void saveAllCachedData() {
        for (Map.Entry<UUID, FileConfiguration> entry : playerCache.entrySet()) {
            try {
                entry.getValue().save(getPlayerFile(entry.getKey()));
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data for " + entry.getKey());
            }
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerDataAsync(uuid);
        playerCache.remove(uuid);
    }

    public int getProgress(UUID uuid, String questId) {
        return getPlayerData(uuid).getInt("quests." + questId, 0);
    }

    public void setProgress(UUID uuid, String questId, int amount) {
        getPlayerData(uuid).set("quests." + questId, amount);
    }

    public void addProgress(UUID uuid, String questId, int amountToAdd) {
        setProgress(uuid, questId, getProgress(uuid, questId) + amountToAdd);
    }

    // --- UPDATED: Date checking logic ---
    public List<String> getActiveQuests(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        String today = java.time.LocalDate.now().toString();
        String lastReset = config.getString("last_reset_day", "");

        // If their last reset day isn't today, instantly reroll them!
        if (!lastReset.equals(today) || !config.contains("active_quests") || config.getStringList("active_quests").isEmpty()) {
            generateNewQuests(uuid);
        }
        return config.getStringList("active_quests");
    }

    public void generateNewQuests(UUID uuid) {
        Map<String, List<String>> difficultyPools = new HashMap<>();
        for (String questId : plugin.getQuestManager().getQuestIds()) {
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
            String diff = qConf != null ? qConf.getString("difficulty", "EASY").toUpperCase() : "EASY";
            difficultyPools.computeIfAbsent(diff, k -> new ArrayList<>()).add(questId);
        }

        List<String> assigned = new ArrayList<>();
        ConfigurationSection diffSection = plugin.getConfig().getConfigurationSection("player_interface.difficulty_slots");

        if (diffSection != null) {
            for (String requiredDiff : diffSection.getKeys(false)) {
                requiredDiff = requiredDiff.toUpperCase();
                List<String> pool = difficultyPools.get(requiredDiff);

                if (pool != null && !pool.isEmpty()) {
                    Collections.shuffle(pool);
                    String selectedQuest = pool.get(0);

                    assigned.add(selectedQuest);
                    setProgress(uuid, selectedQuest, 0);
                }
            }
        } else {
            List<String> allQuests = new ArrayList<>(plugin.getQuestManager().getQuestIds());
            Collections.shuffle(allQuests);
            for(int i = 0; i < Math.min(3, allQuests.size()); i++) {
                assigned.add(allQuests.get(i));
                setProgress(uuid, allQuests.get(i), 0);
            }
        }

        // --- NEW: Save today's date so we know they already rolled today ---
        getPlayerData(uuid).set("last_reset_day", java.time.LocalDate.now().toString());
        getPlayerData(uuid).set("active_quests", assigned);
        savePlayerDataAsync(uuid);
    }

    public void rerollQuest(UUID uuid, int position) {
        List<String> active = getActiveQuests(uuid);
        if (position < 1 || position > active.size()) return;

        String oldQuest = active.get(position - 1);
        FileConfiguration oldConf = plugin.getQuestManager().getQuest(oldQuest);
        String targetDifficulty = oldConf != null ? oldConf.getString("difficulty", "EASY").toUpperCase() : "EASY";

        List<String> validReplacementQuests = new ArrayList<>();
        for (String questId : plugin.getQuestManager().getQuestIds()) {
            FileConfiguration qConf = plugin.getQuestManager().getQuest(questId);
            String diff = qConf != null ? qConf.getString("difficulty", "EASY").toUpperCase() : "EASY";

            if (diff.equals(targetDifficulty) && !active.contains(questId)) {
                validReplacementQuests.add(questId);
            }
        }

        if (validReplacementQuests.isEmpty()) {
            setProgress(uuid, oldQuest, 0);
        } else {
            Collections.shuffle(validReplacementQuests);
            String newQuest = validReplacementQuests.get(0);

            active.set(position - 1, newQuest);
            setProgress(uuid, newQuest, 0);

            getPlayerData(uuid).set("active_quests", active);
            savePlayerDataAsync(uuid);
        }
    }

    public void rerollAll(UUID uuid) {
        generateNewQuests(uuid);
    }
}