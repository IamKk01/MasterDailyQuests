package net.mastercraft.masterDailyQuests.listeners;

import net.mastercraft.masterdungeons.events.DungeonClearEvent;
import net.mastercraft.masterdungeons.events.DungeonStageAdvanceEvent;
import net.mastercraft.masterdungeons.events.DungeonStartEvent;
import net.mastercraft.masterdungeons.game.DungeonSession;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class DungeonEventListener implements Listener {

    private final QuestListener questListener;

    public DungeonEventListener(QuestListener questListener) {
        this.questListener = questListener;
    }

    @EventHandler
    public void onDungeonPlay(DungeonStartEvent event) {
        DungeonSession session = event.getSession();
        if (session == null || session.getDungeon() == null) return;

        String dungeonName = session.getDungeon().getName().toUpperCase();

        for (UUID uuid : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                questListener.checkQuestProgress(player, "PLAY_DUNGEON", dungeonName, 1);
            }
        }
    }

    @EventHandler
    public void onDungeonStageReach(DungeonStageAdvanceEvent event) {
        DungeonSession session = event.getSession();
        if (session == null || session.getDungeon() == null) return;

        String dungeonName = session.getDungeon().getName().toUpperCase();
        int stageReached = session.getCurrentStageIndex() + 1;

        for (UUID uuid : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                questListener.updateQuestProgressAbsolute(player, "REACH_STAGE", dungeonName, stageReached);
            }
        }
    }

    @EventHandler
    public void onDungeonClear(DungeonClearEvent event) {
        DungeonSession session = event.getSession();
        if (session == null || session.getDungeon() == null) return;

        String dungeonName = session.getDungeon().getName().toUpperCase();

        for (UUID uuid : session.getActivePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Adds +1 to "FINISH_DUNGEON" quest progress
                questListener.checkQuestProgress(player, "FINISH_DUNGEON", dungeonName, 1);
            }
        }
    }
}