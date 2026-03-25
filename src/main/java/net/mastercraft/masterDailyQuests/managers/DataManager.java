package net.mastercraft.masterDailyQuests.managers;

import net.mastercraft.masterDailyQuests.MasterDailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final MasterDailyQuests plugin;
    private final File playerDataFolder;

    // Memory Cache: Faster than reading from a file!
    private final Map<UUID, FileConfiguration> playerCache = new HashMap<>();

    public DataManager(MasterDailyQuests plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        // Start Auto-Saver (Runs every 10 minutes asynchronously)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllCachedData, 12000L, 12000L);
    }

    // --- Core Data Handling ---

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

    // Saves specific player data safely in the background
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

    // Saves everyone (Used for onDisable and Auto-Saves)
    public void saveAllCachedData() {
        for (Map.Entry<UUID, FileConfiguration> entry : playerCache.entrySet()) {
            try {
                entry.getValue().save(getPlayerFile(entry.getKey()));
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save data for " + entry.getKey());
            }
        }
    }

    // Unloads player from RAM when they quit
    public void unloadPlayer(UUID uuid) {
        savePlayerDataAsync(uuid);
        playerCache.remove(uuid);
    }

    // --- Quest Progress Logic ---

    public int getProgress(UUID uuid, String questId) {
        return getPlayerData(uuid).getInt("quests." + questId, 0);
    }

    public void setProgress(UUID uuid, String questId, int amount) {
        getPlayerData(uuid).set("quests." + questId, amount);
        // Notice we do NOT call save() here anymore! It stays in RAM until they quit or auto-save runs.
    }

    public void addProgress(UUID uuid, String questId, int amountToAdd) {
        setProgress(uuid, questId, getProgress(uuid, questId) + amountToAdd);
    }

    // --- Active Quests Logic ---

    public List<String> getActiveQuests(UUID uuid) {
        FileConfiguration config = getPlayerData(uuid);
        if (!config.contains("active_quests") || config.getStringList("active_quests").isEmpty()) {
            generateNewQuests(uuid);
        }
        return config.getStringList("active_quests");
    }

    public void generateNewQuests(UUID uuid) {
        List<String> allQuests = new ArrayList<>(plugin.getQuestManager().getQuestIds());
        List<String> assigned = new ArrayList<>();

        if (!allQuests.isEmpty()) {
            int max = plugin.getConfigManager().getQuestSlots().size();
            Collections.shuffle(allQuests);
            for(int i = 0; i < Math.min(max, allQuests.size()); i++) {
                assigned.add(allQuests.get(i));
                setProgress(uuid, allQuests.get(i), 0);
            }
        }

        getPlayerData(uuid).set("active_quests", assigned);
        savePlayerDataAsync(uuid); // We save immediately on generation just to be safe
    }

    public void rerollQuest(UUID uuid, int position) {
        List<String> active = getActiveQuests(uuid);
        if (position < 1 || position > active.size()) return;

        List<String> allQuests = new ArrayList<>(plugin.getQuestManager().getQuestIds());
        if (allQuests.isEmpty()) return;

        allQuests.removeAll(active);

        if (allQuests.isEmpty()) {
            setProgress(uuid, active.get(position - 1), 0);
        } else {
            Collections.shuffle(allQuests);
            String newQuest = allQuests.get(0);
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