package wix3y.dungeonDesigner.handlers;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.PlayerDataUtil;

public class PlayerJoinHandler implements Listener {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;

    public PlayerJoinHandler(DungeonDesigner plugin, PlayerDataUtil playerDataUtil) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
        this.playerDataUtil = playerDataUtil;
    }

    /**
     * Load player data into cache upon player join
     *
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuid = event.getPlayer().getUniqueId().toString();
            playerDataUtil.addPlayerToCache(uuid);
        });
    }
}