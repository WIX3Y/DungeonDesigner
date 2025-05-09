package wix3y.dungeonDesigner.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.ConfigUtil;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.PlayerDataUtil;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.util.ArrayList;
import java.util.List;

public class StartDungeon implements CommandExecutor {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public StartDungeon(DungeonDesigner plugin, PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
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
     * Start a dungeon run
     *
     * @param sender who executed the command
     * @param command the command
     * @param label the command name
     * @param args arguments for the command (player to display statistics for, or none)
     * @return whether command was successful
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>/ddstart <dungeon> <instance> <player>"));
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

        if (dungeonInfo.isOccupied()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Dungeon is already occupied"));
            return true;
        }

        // reinstall gates and empty loot chests
        reset(dungeonInfo);

        dungeonInfo.setOccupied(true);
        player.teleport(dungeonInfo.getStartPoint());

        for (String startupCmd: dungeonInfo.getStartupCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsePlaceholders(player, startupCmd));
        }

        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        runningDungeonInfo.setIsRunning(true);
        runningDungeonInfo.setDungeonName(args[0]);
        runningDungeonInfo.setDungeonInstance(Integer.parseInt(args[1]));
        runningDungeonInfo.initializeKeys(dungeonInfo.getDefaultReward().gateKeys());
        List<List<Location>> dungeonBonusKeyLocations = new ArrayList<>();
        for (DungeonRewardInfo rewardInfo: dungeonInfo.getBonusRewards()) {
            dungeonBonusKeyLocations.add(rewardInfo.gateKeys());
        }
        runningDungeonInfo.initializeBonusKeys(dungeonBonusKeyLocations);
        long startTime = System.currentTimeMillis();
        runningDungeonInfo.setStartTime(startTime);

         Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RunningDungeonInfo currentRunningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
            if (startTime == currentRunningDungeonInfo.getStartTime() && dungeonInfo.isOccupied()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red> You ran out of time for the " + dungeonInfo.getName() + " dungeon."));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dungeondesignerend " + dungeonInfo.getName() + " " + dungeonInfo.getInstance() + " " + player.getName());
            }
        }, 20L * dungeonInfo.getMaxRunTime());

        return true;
    }

    /**
     * Reset the dungeon (reinstall gates and empty c´loot chests)
     *
     * @param dungeonInfo information about the dungeon
     */
    private void reset(DungeonInfo dungeonInfo) {
        // reinstall main gate
        DungeonRewardInfo defaultReward = dungeonInfo.getDefaultReward();
        Area gate = defaultReward.gate();
        World world = gate.start().getWorld();
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

        // empty default loot chest
        Location rewardChest = defaultReward.rewardChest();
        rewardChest.getChunk().load();
        Block block = world.getBlockAt(rewardChest);
        if (block.getType() == Material.CHEST) {
            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();
            inventory.clear();
            chest.getInventory().setContents(inventory.getContents());
        }
        else {
            plugin.getLogger().warning("Expected reward chest at location " + rewardChest + " but no chest was found.");
        }

        List<DungeonRewardInfo> bonusRewards = dungeonInfo.getBonusRewards();
        for (DungeonRewardInfo bonusReward : bonusRewards) {
            // reinstall bonus gate
            gate = bonusReward.gate();
            material = gate.material();
            gateMinX = Math.min(gate.start().getBlockX(), gate.stop().getBlockX());
            gateMaxX = Math.max(gate.start().getBlockX(), gate.stop().getBlockX());
            gateMinY = Math.min(gate.start().getBlockY(), gate.stop().getBlockY());
            gateMaxY = Math.max(gate.start().getBlockY(), gate.stop().getBlockY());
            gateMinZ = Math.min(gate.start().getBlockZ(), gate.stop().getBlockZ());
            gateMaxZ = Math.max(gate.start().getBlockZ(), gate.stop().getBlockZ());

            for (int x = gateMinX; x <= gateMaxX; x++) {
                for (int y = gateMinY; y <= gateMaxY; y++) {
                    for (int z = gateMinZ; z <= gateMaxZ; z++) {
                        block = world.getBlockAt(x, y, z);
                        block.setType(material, false);
                    }
                }
            }

            // empty bonus loot chest
            rewardChest = bonusReward.rewardChest();
            block = world.getBlockAt(rewardChest);
            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                Inventory inventory = chest.getInventory();
                inventory.clear();
                chest.getInventory().setContents(inventory.getContents());
            } else {
                plugin.getLogger().warning("Expected reward chest at location " + rewardChest + " but no chest was found.");
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