package wix3y.dungeonDesigner;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wix3y.dungeonDesigner.commands.*;
import wix3y.dungeonDesigner.handlers.PlayerDeathHandler;
import wix3y.dungeonDesigner.handlers.PlayerInteractHandler;
import wix3y.dungeonDesigner.handlers.PlayerJoinHandler;
import wix3y.dungeonDesigner.handlers.PlayerQuitHandler;
import wix3y.dungeonDesigner.placeholders.PapiDDExpansion;
import wix3y.dungeonDesigner.util.ConfigUtil;
import wix3y.dungeonDesigner.util.DatabaseManager;
import wix3y.dungeonDesigner.util.PlayerDataUtil;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.io.File;

public final class DungeonDesigner extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;
    private PlayerInteractHandler playerInteractHandler;
    private PlayerQuitHandler playerQuitHandler;
    private PlayerDeathHandler playerDeathHandler;
    private StartDungeon startDungeonCommand;
    private DungeonReward dungeonRewardCommand;
    private EndDungeon endDungeonCommand;
    private UnlockGate unlockGateCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createTestDungeon();
        FileConfiguration config = this.getConfig();
        configUtil = new ConfigUtil(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.connect(config.getString("MySQL.ip"), config.getString("MySQL.port"), config.getString("MySQL.database"), config.getString("MySQL.username"), config.getString("MySQL.password"), config.getInt("MySQL.poolsize"));
        databaseManager.initialize("DD_TIMER", configUtil.getUniqueDungeons(), 1000000000);
        databaseManager.initialize("DD_COMPLETION", configUtil.getUniqueDungeons(), 0);

        playerDataUtil = new PlayerDataUtil(this, databaseManager, configUtil.getUniqueDungeons());

        new PlayerJoinHandler(this, playerDataUtil);
        playerQuitHandler = new PlayerQuitHandler(this, playerDataUtil, configUtil);
        playerDeathHandler = new PlayerDeathHandler(this, playerDataUtil, configUtil);
        playerInteractHandler = new PlayerInteractHandler(this, playerDataUtil, configUtil);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiDDExpansion(playerDataUtil).register();
        }

        getCommand("ddreload").setExecutor(new Reload(this));
        getCommand("ddaddreward").setExecutor(new RewardItem(this));
        startDungeonCommand = new StartDungeon(this, playerDataUtil, configUtil);
        getCommand("ddstart").setExecutor(startDungeonCommand);
        dungeonRewardCommand = new DungeonReward(this, playerDataUtil, configUtil);
        getCommand("ddreward").setExecutor(dungeonRewardCommand);
        endDungeonCommand = new EndDungeon(playerDataUtil, configUtil);
        getCommand("ddend").setExecutor(endDungeonCommand);
        unlockGateCommand = new UnlockGate(this, playerDataUtil, configUtil);
        getCommand("ddunlock").setExecutor(unlockGateCommand);

        run(config.getInt("MySQL.updatefreq"));

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("     <gradient:#00AA44:#99FFBB:#00AA44>Dungeon Designer</gradient>"));
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("          <gray>v1.0.0"));
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("          <green>Enabled"));
        Bukkit.getConsoleSender().sendMessage("");

    }

    private void run(int updatefreq) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> playerDataUtil.writeToDatabase(),
                updatefreq, updatefreq);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (String dungeon: configUtil.getDungeons()) {
                DungeonInfo dungeonInfo = configUtil.getDungeon(dungeon);
                if (dungeonInfo.isOccupied()) {
                    for (Location keyLocation: dungeonInfo.getDefaultReward().gateKeys()) {
                        World world = keyLocation.getWorld();
                        Location centered = keyLocation.clone().add(0.5, 0.4, 0.5);
                        world.spawnParticle(Particle.WAX_OFF, centered, 2, 0.2, 0.2, 0.2, 1.0);
                    }

                    for (DungeonRewardInfo dungeonRewardInfo: dungeonInfo.getBonusRewards()) {
                        for (Location keyLocation: dungeonRewardInfo.gateKeys()) {
                            World world = keyLocation.getWorld();
                            Location centered = keyLocation.clone().add(0.5, 0.4, 0.5);
                            world.spawnParticle(Particle.WAX_OFF, centered, 2, 0.2, 0.2, 0.2, 1.0);
                        }
                    }
                }
            }
        }, 0, 40);
    }


    public void reload() {
        this.reloadConfig();
        this.configUtil = new ConfigUtil(this);
        playerInteractHandler.reloadConfigUtil(configUtil);
        startDungeonCommand.reloadConfigUtil(configUtil);
        dungeonRewardCommand.reloadConfigUtil(configUtil);
        endDungeonCommand.reloadConfigUtil(configUtil);
        unlockGateCommand.reloadConfigUtil(configUtil);
        playerQuitHandler.reloadConfigUtil(configUtil);
        playerDeathHandler.reloadConfigUtil(configUtil);
    }

    private void createTestDungeon() {
        File dungeonsDir = new File(getDataFolder(), "dungeons");
        if (dungeonsDir.exists()) {
            return;
        }

        File testDungeon = new File(getDataFolder(), "dungeons/testdungeon.yml");

        if (!testDungeon.exists()) {
            saveResource("dungeons/testdungeon.yml", false);
        }
    }

    @Override
    public void onDisable() {
        for (Player player: Bukkit.getOnlinePlayers()) {
            RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
            if (runningDungeonInfo.getIsRunning()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red> You were kicked from the dungeon due to a server restart."));
                forceExit(player, configUtil.getDungeon(runningDungeonInfo.getDungeonName() + runningDungeonInfo.getDungeonInstance()));
            }
        }

        playerDataUtil.writeToDatabase();
        databaseManager.disconnect();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("     <gradient:#00AA44:#99FFBB:#00AA44>Dungeon Designer</gradient>"));
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("          <gray>v1.0.0"));
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("         <red>Disabled"));
        Bukkit.getConsoleSender().sendMessage("");
    }

    public void forceExit(Player player, DungeonInfo dungeonInfo) {
        for (String exitCmd: dungeonInfo.getExitCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsePlaceholders(player, exitCmd));
        }

        // teleport player out of the dungeon
        player.teleport(dungeonInfo.getExitPoint());

        // teleport any remaining players out of the dungeon
        Area dungeonArea = dungeonInfo.getDungeonArea();
        World world = dungeonArea.start().getWorld();
        int minX = Math.min(dungeonArea.start().getBlockX(), dungeonArea.stop().getBlockX());
        int maxX = Math.max(dungeonArea.start().getBlockX(), dungeonArea.stop().getBlockX());
        int minY = Math.min(dungeonArea.start().getBlockY(), dungeonArea.stop().getBlockY());
        int maxY = Math.max(dungeonArea.start().getBlockY(), dungeonArea.stop().getBlockY());
        int minZ = Math.min(dungeonArea.start().getBlockZ(), dungeonArea.stop().getBlockZ());
        int maxZ = Math.max(dungeonArea.start().getBlockZ(), dungeonArea.stop().getBlockZ());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Location playerLocation = onlinePlayer.getLocation();
            if (!playerLocation.getWorld().equals(world)) {
                continue;
            }

            int x = playerLocation.getBlockX();
            int y = playerLocation.getBlockY();
            int z = playerLocation.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                onlinePlayer.teleport(dungeonInfo.getExitPoint());
            }
        }
    }

    /**
     * Parse placeholderAPI placeholders in text
     *
     * @param player the player to parse the placeholder for
     * @param text the text
     * @return the parsed text
     */
    private String parsePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
