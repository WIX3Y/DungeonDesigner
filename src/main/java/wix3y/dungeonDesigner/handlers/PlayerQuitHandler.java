package wix3y.dungeonDesigner.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.ConfigUtil;
import wix3y.dungeonDesigner.util.PlayerDataUtil;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

public class PlayerQuitHandler implements Listener {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public PlayerQuitHandler(DungeonDesigner plugin, PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.playerDataUtil = playerDataUtil;
        this.configUtil = configUtil;
    }

    /**
     * Reload parameters dependent on the config util
     *
     * @param configUtil the new config util
     */
    public void reloadConfigUtil(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

    /**
     * Unload player data from cache upon player quit and write data to database
     * Reset dungeon if player quit while running it
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        if (runningDungeonInfo.getIsRunning() || runningDungeonInfo.getIsLooting()) {
            DungeonInfo dungeonInfo = configUtil.getDungeon(runningDungeonInfo.getDungeonName() + runningDungeonInfo.getDungeonInstance());
            plugin.forceExit(player, dungeonInfo);
            runningDungeonInfo.reset();
            dungeonInfo.setOccupied(false);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuid = player.getUniqueId().toString();
            playerDataUtil.removePlayerFromCache(uuid);
        });
    }
}