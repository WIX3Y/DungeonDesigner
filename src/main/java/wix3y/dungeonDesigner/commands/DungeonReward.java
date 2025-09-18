package wix3y.dungeonDesigner.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.*;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.util.Map;

public class DungeonReward implements CommandExecutor {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public DungeonReward(DungeonDesigner plugin, PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
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
     * Get dungeon timer and completion percentage and set the dungeon as finished
     *
     * @param sender who executed the command
     * @param command the command
     * @param label the command name
     * @param args arguments for the command
     * @return whether command was successful
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>/ddreward <dungeon> <instance> <player>"));
            return true;
        }

        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid player name"));
            return true;
        }

        DungeonInfo dungeonInfo = configUtil.getDungeon(args[0] + args[1]);
        if(dungeonInfo == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid dungeon name or instance"));
            return true;
        }

        // check that the player is currently running this dungeon
        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        if (!runningDungeonInfo.getIsRunning() || !(runningDungeonInfo.getDungeonName().equals(dungeonInfo.getName()) && runningDungeonInfo.getDungeonInstance() == dungeonInfo.getInstance())) {
            return true;
        }

        runningDungeonInfo.setIsRunning(false);
        runningDungeonInfo.setIsLooting(true);
        long startTime = runningDungeonInfo.getStartTime();
        long milliTime = System.currentTimeMillis() - startTime;
        double time = milliTime / 1000.0;
        String formattedTime = String.format("%.2f", time);
        playerDataUtil.decreaseTimer(player, dungeonInfo.getName(), (int) milliTime);

        int percentage = getPercentage(runningDungeonInfo);
        playerDataUtil.increaseCompletion(player, dungeonInfo.getName(), percentage);

        playerDataUtil.increaseNumbRuns(player, dungeonInfo.getName());

        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You completed <yellow>" + percentage + "%</yellow> of the " + dungeonInfo.getName() + " dungeon in <yellow>" + formattedTime + "</yellow> seconds!"));

        dungeonFinish(player, dungeonInfo);

        // cancel ran out of time tasks
        int taskIdWarning = runningDungeonInfo.getTaskIdWarning();
        if (taskIdWarning != -1) {
            Bukkit.getScheduler().cancelTask(taskIdWarning);
        }
        int taskId = runningDungeonInfo.getTaskId();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }


        // give player 5 minutes to collect their loot
        // warning when 1 minute is left
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RunningDungeonInfo currentRunningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
            if (currentRunningDungeonInfo != null && startTime == currentRunningDungeonInfo.getStartTime() && dungeonInfo.isOccupied()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red> You will be kicked out of the dungeon in 1 minute."));
            }
        }, 20L * 60 * 4);
        // kick out of dungeon
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RunningDungeonInfo currentRunningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
            if (currentRunningDungeonInfo != null && startTime == currentRunningDungeonInfo.getStartTime() && dungeonInfo.isOccupied()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red> You have been kicked out of the dungeon."));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dungeondesignerend " + dungeonInfo.getName() + " " + dungeonInfo.getInstance() + " " + player.getName());
            }
        }, 20L * 60 * 5);

        return true;
    }

    /**
     * Get dungeon completion percentage
     *
     * @param runningDungeonInfo information about the dungeon that was run
     * @return the dungeon completion percentage
     */
    private int getPercentage(RunningDungeonInfo runningDungeonInfo) {
        int completion = 0;
        int total = 0;
        for (Map<Location, Boolean> openedBonusKeys: runningDungeonInfo.getAllOpenedBonusKeys()) {
            for(Boolean open: openedBonusKeys.values()) {
                if (open) {
                    completion++;
                }
                total++;
            }
        }

        if (total == 0) {
            return 100;
        }
        else {
            return (int) ((double) 100 * completion / total);
        }
    }

    /**
     * Teleport any other players in the dungeon to the finish line and close the main gate
     *
     * @param player the player who ran the dungeon
     * @param dungeonInfo information about the dungeon
     */
    private void dungeonFinish(Player player, DungeonInfo dungeonInfo) {
        // teleport any other players to location
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
            if (!playerLocation.getWorld().equals(world) || onlinePlayer == player) {
                continue;
            }

            int x = playerLocation.getBlockX();
            int y = playerLocation.getBlockY();
            int z = playerLocation.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                onlinePlayer.teleport(dungeonInfo.getEndPoint());
            }
        }

        // reinstall main gate
        DungeonRewardInfo defaultReward = dungeonInfo.getDefaultReward();
        Area gate = defaultReward.gate();
        Material material = gate.material();
        int gateMinX = Math.min(gate.start().getBlockX(), gate.stop().getBlockX());
        int gateMaxX = Math.max(gate.start().getBlockX(), gate.stop().getBlockX());
        int gateMinY = Math.min(gate.start().getBlockY(), gate.stop().getBlockY());
        int gateMaxY = Math.max(gate.start().getBlockY(), gate.stop().getBlockY());
        int gateMinZ = Math.min(gate.start().getBlockZ(), gate.stop().getBlockZ());
        int gateMaxZ = Math.max(gate.start().getBlockZ(), gate.stop().getBlockZ());

        for (int x = gateMinX; x <= gateMaxX; x++) {
            for (int y = gateMinY; y <= gateMaxY; y++) {
                for (int z = gateMinZ; z <= gateMaxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(material, false);
                }
            }
        }
    }
}