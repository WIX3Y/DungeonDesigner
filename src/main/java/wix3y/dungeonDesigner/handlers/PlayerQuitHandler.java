package wix3y.dungeonDesigner.handlers;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.PlayerDataUtil;

public class PlayerQuitHandler implements Listener {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;

    public PlayerQuitHandler(DungeonDesigner plugin, PlayerDataUtil playerDataUtil) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.playerDataUtil = playerDataUtil;
    }

    /**
     * Unload player data from cache upon player quit and write data to database
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuid = event.getPlayer().getUniqueId().toString();
            playerDataUtil.removePlayerFromCache(uuid);
        });
    }
}