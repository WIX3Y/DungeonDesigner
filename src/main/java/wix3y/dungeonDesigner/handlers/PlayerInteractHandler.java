package wix3y.dungeonDesigner.handlers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.*;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

import java.util.List;
import java.util.Map;

public class PlayerInteractHandler implements Listener {
    private final DungeonDesigner plugin;
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public PlayerInteractHandler(DungeonDesigner plugin, PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
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
     * Check if player stepped on a timer stopping pressure plate in a dungeon
     *
     * @param event the player join event
     */
    @EventHandler
    public void onPressurePlateStep(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        Player player = event.getPlayer();

        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        if(!runningDungeonInfo.getIsRunning()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Material type = block.getType();
        if (!type.toString().endsWith("_PRESSURE_PLATE")) {
            return;
        }

        Location location = block.getLocation();

        DungeonInfo dungeonInfo = configUtil.getDungeon(runningDungeonInfo.getDungeonName() + runningDungeonInfo.getDungeonInstance());
        if (dungeonInfo != null && dungeonInfo.isOccupied() && location.equals(dungeonInfo.getEndPoint())) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dungeondesignerreward " + dungeonInfo.getName() + " " + dungeonInfo.getInstance() + " " + player.getName());
        }
    }

    /**
     * Check if player interacted with a dungeon key block
     *
     * @param event the player interact event
     */
    @EventHandler
    public void onLocationInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // prevent double trigger
        }

        Player player = event.getPlayer();

        List<String> dungeons = configUtil.getDungeons();

        for (String dungeon: dungeons) {
            DungeonInfo dungeonInfo = configUtil.getDungeon(dungeon);
            if (!dungeonInfo.isOccupied()) {
                continue;
            }

            Area dungeonArea = dungeonInfo.getDungeonArea();
            World world = dungeonArea.start().getWorld();
            if (player.getWorld() != world) {
                continue;
            }

            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) {
                continue;
            }

            int minX = Math.min(dungeonArea.start().getBlockX(), dungeonArea.stop().getBlockX());
            int maxX = Math.max(dungeonArea.start().getBlockX(), dungeonArea.stop().getBlockX());
            int minY = Math.min(dungeonArea.start().getBlockY(), dungeonArea.stop().getBlockY());
            int maxY = Math.max(dungeonArea.start().getBlockY(), dungeonArea.stop().getBlockY());
            int minZ = Math.min(dungeonArea.start().getBlockZ(), dungeonArea.stop().getBlockZ());
            int maxZ = Math.max(dungeonArea.start().getBlockZ(), dungeonArea.stop().getBlockZ());

            Location blockPos = clickedBlock.getLocation();
            int x = blockPos.getBlockX();
            int y = blockPos.getBlockY();
            int z = blockPos.getBlockZ();
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                checkInteraction(player, dungeonInfo, blockPos);
            }
        }
    }

    /**
     * Check if the player interacted with a key block in the dungeon, if they did unlock they key
     *
     * @param player the player that interacted with a block
     * @param dungeonInfo information about the dungeon
     * @param location the location of the interacted block
     */
    private void checkInteraction(Player player, DungeonInfo dungeonInfo, Location location) {
        DungeonRewardInfo dungeonRewardInfo = dungeonInfo.getDefaultReward();
        for (Location keyLocation: dungeonRewardInfo.gateKeys()) {
            if (keyLocation.equals(location)) {

                RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
                if (runningDungeonInfo.getIsRunning() && runningDungeonInfo.getDungeonName().equals(dungeonInfo.getName()) && runningDungeonInfo.getDungeonInstance() == dungeonInfo.getInstance()) {
                    // player is running the current dungeon

                    Map<Location, Boolean> keys = runningDungeonInfo.getOpenedKeys();
                    if (!keys.get(keyLocation)) {
                        // open the key
                        runningDungeonInfo.setOpenedKey(keyLocation, true);
                        int numbKeys = keys.size();
                        int numbOpenedKeys = 0;
                        for (Boolean value: keys.values()) {
                            if (value) {
                                numbOpenedKeys++;
                            }
                        }
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You have unlocked " + numbOpenedKeys + "/" + numbKeys + " pieces of the main gate!"));
                        checkGateRemove(numbKeys, numbOpenedKeys, dungeonRewardInfo);
                    }
                    else {
                        // already opened
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You have already unlocked this pieces of the main gate!"));
                    }
                }
                else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red>Only the player running this dungeon can unlock this."));
                }

                return;
            }
        }

        List<DungeonRewardInfo> dungeonRewardInfos = dungeonInfo.getBonusRewards();
        for (int i=0; i<dungeonRewardInfos.size(); i++) {
            dungeonRewardInfo = dungeonRewardInfos.get(i);

            for (Location keyLocation: dungeonRewardInfo.gateKeys()) {
                if (keyLocation.equals(location)) {
                    RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
                    if (runningDungeonInfo.getIsRunning() && runningDungeonInfo.getDungeonName().equals(dungeonInfo.getName()) && runningDungeonInfo.getDungeonInstance() == dungeonInfo.getInstance()) {
                        // player is running the current dungeon

                        Map<Location, Boolean> keys = runningDungeonInfo.getOpenedBonusKeys(i);
                        if (!keys.get(keyLocation)) {
                            // open the key
                            runningDungeonInfo.setOpenedBonusKeys(i, keyLocation, true);
                            int numbKeys = keys.size();
                            int numbOpenedKeys = 0;
                            for (Boolean value: keys.values()) {
                                if (value) {
                                    numbOpenedKeys++;
                                }
                            }
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You have unlocked " + numbOpenedKeys + "/" + numbKeys + " pieces of bonus gate " + (i + 1) + "!"));
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1);
                            checkGateRemove(numbKeys, numbOpenedKeys, dungeonRewardInfo);
                        }
                        else {
                            // already opened
                            player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> You have already unlocked this pieces of bonus gate " + (i + 1) + "!"));
                        }
                    }
                    else {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeons</gradient>]</dark_gray> <gray>>> <red>Only the player running this dungeon can unlock this."));
                    }

                    return;
                }
            }
        }
    }

    /**
     * Check if a dungeon gate should be removed and remove it if that is the case (and put rewards in reward chest)
     *
     * @param total the total number of keys for the gate
     * @param claimed the number of claimed keys for the gate
     * @param rewardInfo information about the reward chest and gate
     */
    private void checkGateRemove(int total, int claimed, DungeonRewardInfo rewardInfo) {
        if (claimed == total) {
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
            Area gate = rewardInfo.gate();
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
        }
    }
}