package wix3y.dungeonDesigner.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.datastructures.AmountWriteChecked;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataUtil {
    private final DungeonDesigner plugin;
    private final DatabaseManager databaseManager;
    private final List<String> allDungeons;
    private final Map<String, Map<String, AmountWriteChecked>> playerDataCacheTimer = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AmountWriteChecked>> playerDataCacheCompletion = new ConcurrentHashMap<>();
    private final Map<String, RunningDungeonInfo> playerDataDungeonRun = new ConcurrentHashMap<>();

    public PlayerDataUtil(DungeonDesigner plugin, DatabaseManager databaseManager, List<String> allDungeons) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.allDungeons = allDungeons;
    }

    /**
     * Increase completion progress for dungeon for specific player if new completion is higher than old completion
     *
     * @param player the player
     * @param dungeonID ID of the dungeon to increase completion for
     * @param amount the new completion amount
     */
    public void increaseCompletion(Player player, String dungeonID, int amount) {
        String uuid = player.getUniqueId().toString();
        if (!playerDataCacheCompletion.containsKey(uuid)) {
            plugin.getLogger().warning("Player " + player.getName() + " does not exist in player data cache, adding player...");
            addPlayerToCache(uuid);
        }

            if (playerDataCacheCompletion.get(uuid).containsKey(dungeonID)) {
                AmountWriteChecked completionData = playerDataCacheCompletion.get(uuid).get(dungeonID);
                if (completionData.getAmount() < amount) {
                    completionData.setAmount(amount);
                    completionData.setDirty(true);
                }
            }
            else {
                plugin.getLogger().severe("Player " + player.getName() + " does not have dungeon completion data for dungeon " + dungeonID);
            }
    }

    /**
     * decrease timer for dungeon for specific player if new time is lower than old time
     *
     * @param player the player
     * @param dungeonID ID of the dungeon to decrease time for
     * @param amount the new time
     */
    public void decreaseTimer(Player player, String dungeonID, int amount) {
        String uuid = player.getUniqueId().toString();
        if (!playerDataCacheTimer.containsKey(uuid)) {
            plugin.getLogger().warning("Player " + player.getName() + " does not exist in player data cache, adding player...");
            addPlayerToCache(uuid);
        }

        if (playerDataCacheTimer.get(uuid).containsKey(dungeonID)) {
            AmountWriteChecked timerData = playerDataCacheTimer.get(uuid).get(dungeonID);
            if (timerData.getAmount() > amount) {
                timerData.setAmount(amount);
                timerData.setDirty(true);
            }
        }
        else {
            plugin.getLogger().severe("Player " + player.getName() + " does not have dungeon timer data for dungeon " + dungeonID);
        }
    }

    /**
     * Add player data from MySQL to cache
     *
     * @param uuid uuid of the player
     */
    public void addPlayerToCache(String uuid) {
        Map<String, AmountWriteChecked> playerCompletionData = databaseManager.getPlayerData("DD_COMPLETION", uuid, allDungeons);
        Map<String, AmountWriteChecked> playerTimerData = databaseManager.getPlayerData("DD_TIMER", uuid, allDungeons);
        playerDataCacheCompletion.put(uuid, playerCompletionData);
        playerDataCacheTimer.put(uuid, playerTimerData);
        playerDataDungeonRun.put(uuid, new RunningDungeonInfo());
    }

    /**
     * Write player data from cache to MySQL for all players in cache
     *
     */
    public void writeToDatabase() {
        for (String uuid: playerDataCacheCompletion.keySet()) {
            writeToDatabase(uuid);
        }
    }

    /**
     * Write player data from cache to MySQL
     *
     * @param uuid uuid of the player
     */
    public void writeToDatabase(String uuid) {
        Map<String, AmountWriteChecked> playerCompletionData = playerDataCacheCompletion.get(uuid);
        for (String column: playerCompletionData.keySet()) {
            AmountWriteChecked amount = playerCompletionData.get(column);
            if (amount.isDirty()) {
                amount.setDirty(false);
                // Asynchronous execution means newer data may be written to the database possibly resulting
                // in the exact same data being written during the next execution of this method
                databaseManager.writeInt( "DD_COMPLETION", uuid, column, amount.getAmount());
            }
        }

        Map<String, AmountWriteChecked> playerTimerData = playerDataCacheTimer.get(uuid);
        for (String column: playerTimerData.keySet()) {
            AmountWriteChecked amount = playerTimerData.get(column);
            if (amount.isDirty()) {
                amount.setDirty(false);
                // Asynchronous execution means newer data may be written to the database possibly resulting
                // in the exact same data being written during the next execution of this method
                databaseManager.writeInt( "DD_TIMER", uuid, column, amount.getAmount());
            }
        }
    }

    /**
     * Remove player data from cache and write it to MySQL
     *
     * @param uuid uuid of the player
     */
    public void removePlayerFromCache(String uuid) {
        writeToDatabase(uuid);
        playerDataCacheCompletion.remove(uuid);
        playerDataCacheTimer.remove(uuid);
        playerDataDungeonRun.remove(uuid);
    }

    /**
     * Get player dungeon timer from cache
     *
     * @param uuid uuid of the player
     * @param dungeon the dungeon ID
     * @return the players fastest dungeon completion time
     */
    public int getPlayerTimerData(String uuid, String dungeon) {
        try {
            return playerDataCacheTimer.get(uuid).get(dungeon).getAmount();
        } catch (NullPointerException e) {
            plugin.getLogger().warning("Timer for dungeon " + dungeon + " for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " does not exist in cache!");
            return 3600000;
        }
    }

    /**
     * Get player dungeon completion from cache
     *
     * @param uuid uuid of the player
     * @param dungeon the dungeon ID
     * @return the players max dungeon completion percentage
     */
    public int getPlayerCompletionData(String uuid, String dungeon) {
        try {
            return playerDataCacheCompletion.get(uuid).get(dungeon).getAmount();
        } catch (NullPointerException e) {
            plugin.getLogger().warning("Completion for dungeon " + dungeon + " for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " does not exist in cache!");
            return 0;
        }
    }

    /**
     * Get player's currently run dungeon from cache
     *
     * @param uuid uuid of the player
     * @return the players currently run dungeon
     */
    public RunningDungeonInfo getPlayerDungeonRunData(String uuid) {
        try {
            return playerDataDungeonRun.get(uuid);
        } catch (NullPointerException e) {
            plugin.getLogger().warning("Running dungeon for player " + Bukkit.getPlayer(UUID.fromString(uuid)) + " does not exist in cache!");
            return null;
        }
    }

}