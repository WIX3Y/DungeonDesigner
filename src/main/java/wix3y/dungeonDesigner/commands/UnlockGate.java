package wix3y.dungeonDesigner.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.ConfigUtil;
import wix3y.dungeonDesigner.util.PlayerDataUtil;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.util.List;

public class UnlockGate implements CommandExecutor {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public UnlockGate(DungeonDesigner plugin, PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
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
     * Unlock a reward gate in a specific dungeon instance
     *
     * @param sender who executed the command
     * @param command the command
     * @param label the command name
     * @param args arguments for the command (player to display statistics for, or none)
     * @return whether command was successful
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 4) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>/ddunlock <dungeon> <instance> <reward-id> <player>"));
            return true;
        }

        DungeonInfo dungeonInfo = configUtil.getDungeon(args[0] + args[1]);
        if(dungeonInfo == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid dungeon name or instance"));
            return true;
        }

        if (!dungeonInfo.isOccupied()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Dungeon is currently not in use."));
            return true;
        }

        Player player = Bukkit.getPlayer(args[3]);
        if (player == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid player name."));
            return true;
        }

        int rewardID;
        try {
            rewardID = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid reward-id. Reward ID must be an integer."));
            return true;
        }

        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        int instance;
        try {
            instance = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Dungeon instance must be an integer."));
            return true;
        }
        if (!runningDungeonInfo.getIsRunning() || !runningDungeonInfo.getDungeonName().equals(args[0]) || runningDungeonInfo.getDungeonInstance() != instance) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Player " + player + " is not running dungeon " + args[0] + ". Gate will not be unlocked."));
            return true;
        }

        Area gate;
        DungeonRewardInfo rewardInfo;
        if (rewardID == 0) {
            gate = dungeonInfo.getDefaultReward().gate();
            rewardInfo = dungeonInfo.getDefaultReward();
            runningDungeonInfo.setAllOpenedKey(true);
        }
        else if (dungeonInfo.getBonusRewards().size() >= rewardID) {
            gate = dungeonInfo.getBonusRewards().get(rewardID-1).gate();
            rewardInfo = dungeonInfo.getBonusRewards().get(rewardID-1);
            runningDungeonInfo.setAllOpenedBonusKeys(rewardID-1, true);
        }
        else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Reward ID out of bounds."));
            return true;
        }

        // Add reward items in chest
        int numbRewards = rewardInfo.numbRewards();
        List<ItemStack> items = rewardInfo.rewards();
        if (items.isEmpty()) {
            plugin.getLogger().warning("No rewards found.");
        }
        else {
            List<Integer> chances = rewardInfo.rewardChances();
            int totalChance = 0;
            for (int chance : chances) {
                totalChance += chance;
            }
            Location chestLocation = rewardInfo.rewardChest();

            chestLocation.getBlock().setType(Material.CHEST);
            Chest chest = (Chest) chestLocation.getBlock().getState();
            Inventory inventory = chest.getInventory();
            inventory.clear();

            for (int i = 0; i < numbRewards && i < 27; i++) {
                int rand = (int) (Math.random() * totalChance);
                int index = 0;
                for (int chance: chances) {
                    if (rand < chance) {
                        int slot = (int) (Math.random() * 27);
                        while (slot <= 54) {
                            if (inventory.getItem(slot % 27) == null) {
                                inventory.setItem(slot % 27, items.get(index));
                                break;
                            }
                            slot++;
                        }
                        break;
                    }
                    rand -= chance;
                    index++;
                }
            }
            chest.getInventory().setContents(inventory.getContents());
        }

        // Open gate
        World world = gate.start().getWorld();
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
                    block.setType(Material.AIR, false);
                }
            }
        }

        // send feedback to player
        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You have unlocked 1/1 pieces of bonus gate " + rewardID + "!"));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);

        return true;
    }
}