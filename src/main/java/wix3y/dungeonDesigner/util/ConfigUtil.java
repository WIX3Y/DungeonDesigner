package wix3y.dungeonDesigner.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import wix3y.dungeonDesigner.DungeonDesigner;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.DungeonRewardInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigUtil {
    private final Map<String, DungeonInfo> dungeons = new ConcurrentHashMap<>();
    private final List<String> uniqueDungeons = new ArrayList<>();

    public ConfigUtil(DungeonDesigner plugin) {
        File dungeonsDir = new File(plugin.getDataFolder(), "dungeons");
        File[] files = dungeonsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));

        if (files == null) {
            return;
        }

        // create map of all dungeons with their info
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Dungeon Name
            List<String> section = new ArrayList<>( config.getConfigurationSection("").getKeys(false));
            if (section.size() != 1) {
                plugin.getLogger().severe("File " + file.getName() + " does not contain exactly 1 dungeon.");
                continue;
            }
            String dungeonName = section.getFirst();
            uniqueDungeons.add(dungeonName);

            // Dungeon Instance
            List<String> instances = new ArrayList<>( config.getConfigurationSection(dungeonName + ".Instances").getKeys(false));
            if (instances.isEmpty()) {
                plugin.getLogger().severe("File " + file.getName() + " does not contain any dungeon instances.");
                continue;
            }

            // Startup Commands
            List<String> startupCommands = config.contains(dungeonName + ".StartupCommands") ? config.getStringList(dungeonName + ".StartupCommands") : new ArrayList<>();

            // Exit Commands
            List<String> exitCommands = config.contains(dungeonName + ".ExitCommands") ? config.getStringList(dungeonName + ".ExitCommands") : new ArrayList<>();

            // Dungeon World
            String worldName = config.contains(dungeonName + ".WorldLocation") ? config.getString(dungeonName + ".WorldLocation") : "";
            if (worldName == null) {
                worldName = "";
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().severe("Invalid world " + worldName + " in file " + file.getName() + ".");
                continue;
            }

            // Exit Point
            List<Integer> exitPointCoords = config.contains(dungeonName + ".ExitPoint") ? config.getIntegerList(dungeonName + ".ExitPoint") : new ArrayList<>();
            if (exitPointCoords.size() != 3) {
                plugin.getLogger().severe("ExitPoint for dungeon in file " + file.getName() + " is not an integer list of length 3.");
                continue;
            }
            int x = exitPointCoords.get(0);
            int y = exitPointCoords.get(1);
            int z = exitPointCoords.get(2);
            Location exitPoint = new Location(world, x, y, z);

            // Number Of Rewards in default reward chest
            int numbRewards = config.contains(dungeonName + ".DefaultReward.NumberOfRewards") ? config.getInt(dungeonName + ".DefaultReward.NumberOfRewards") : 0;

            // List of Default Rewards
            List<ItemStack> rewards = new ArrayList<>();
            List<?> rewardsStrings = config.contains(dungeonName + ".DefaultReward.Rewards") ? config.getList(dungeonName + ".DefaultReward.Rewards") : new ArrayList<>();
            if (rewardsStrings != null) {
                for (Object o : rewardsStrings) {
                    if (o instanceof ItemStack) {
                        rewards.add((ItemStack) o);
                    }
                }
            }

            // List of Default Reward Chances
            List<Integer> rewardChances = config.contains(dungeonName + ".DefaultReward.RewardChances") ? config.getIntegerList(dungeonName + ".DefaultReward.RewardChances") : new ArrayList<>();
            if (rewardChances.size() != rewards.size()) {
                plugin.getLogger().warning(file.getName() + " - Miss-match list length for default rewards and their chances.");
                if (rewardChances.size() > rewards.size()) {
                    rewardChances = rewardChances.subList(0, rewards.size());
                }
            }

            List<List<ItemStack>> bonusRewardsItems = new ArrayList<>();
            List<List<Integer>> bonusRewardChances = new ArrayList<>();
            List<Integer> bonusNumbRewards = new ArrayList<>();

            List<String> bonusRewardsSection = new ArrayList<>( config.getConfigurationSection(dungeonName + ".BonusRewards").getKeys(false));
            for (String bonusReward: bonusRewardsSection) {

                // List of List of Bonus Rewards
                List<ItemStack> bonusRewardSublist = new ArrayList<>();
                List<?> bonusRewardStringSublist = config.contains(dungeonName + ".BonusRewards." + bonusReward + ".Rewards") ? config.getList(dungeonName + ".BonusRewards." + bonusReward + ".Rewards") : new ArrayList<>();
                if (bonusRewardStringSublist != null) {
                    for (Object o : bonusRewardStringSublist) {
                        if (o instanceof ItemStack) {
                            bonusRewardSublist.add((ItemStack) o);
                        }
                    }
                }
                bonusRewardsItems.add(bonusRewardSublist);

                // List of List of Bonus Reward Chances
                List<Integer> bonusRewardChance = config.contains(dungeonName + ".BonusRewards." + bonusReward + ".RewardChances") ? config.getIntegerList(dungeonName + ".BonusRewards." + bonusReward + ".RewardChances") : new ArrayList<>();
                if (bonusRewardChance.size() != bonusRewardSublist.size()) {
                    plugin.getLogger().warning(file.getName() + " - Miss-match list length for bonus rewards and their chances.");
                    if (bonusRewardChance.size() > bonusRewardSublist.size()) {
                        bonusRewardChance = bonusRewardChance.subList(0, bonusRewardSublist.size());
                    }
                }
                bonusRewardChances.add(bonusRewardChance);

                // List of Bonus Number of Rewards
                int numbBonusRewards = config.contains(dungeonName + ".BonusRewards." + bonusReward + ".NumberOfRewards") ? config.getInt(dungeonName + ".BonusRewards." + bonusReward + ".NumberOfRewards") : 1;
                bonusNumbRewards.add(numbBonusRewards);
            }

            for(String instance: instances) {
                // Dungeon Instance as integer
                int instanceInt;
                try {
                    instanceInt =  Integer.parseInt(instance);
                } catch (NumberFormatException e) {
                    plugin.getLogger().severe("Instance " + instance + " in file " + file.getName() + " is not an integer. Instance will be ignored");
                    continue;
                }

                String basePath = dungeonName + ".Instances." + instanceInt;

                // Max Run Time
                int maxRunTime = config.contains(dungeonName + ".MaxRunTime") ? config.getInt(dungeonName + ".MaxRunTime") : 60;

                // Dungeon Area
                List<Integer> dungeonAreaStart = config.contains(basePath + ".DungeonArea.Start") ? config.getIntegerList(basePath + ".DungeonArea.Start") : new ArrayList<>();
                if (dungeonAreaStart.size() != 3) {
                    plugin.getLogger().severe("DungeonArea Start for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                int x1 = dungeonAreaStart.get(0);
                int y1 = dungeonAreaStart.get(1);
                int z1 = dungeonAreaStart.get(2);
                List<Integer> dungeonAreaEnd = config.contains(basePath + ".DungeonArea.End") ? config.getIntegerList(basePath + ".DungeonArea.End") : new ArrayList<>();
                if (dungeonAreaEnd.size() != 3) {
                    plugin.getLogger().severe("DungeonArea End for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                int x2 = dungeonAreaEnd.get(0);
                int y2 = dungeonAreaEnd.get(1);
                int z2 = dungeonAreaEnd.get(2);
                Area dungeonArea = new Area(new Location(world, x1, y1, z1), new Location(world, x2, y2, z2), null);

                // Start Point
                List<Integer> startPointCoords = config.contains(basePath + ".StartPoint") ? config.getIntegerList(basePath + ".StartPoint") : new ArrayList<>();
                if (startPointCoords.size() != 3) {
                    plugin.getLogger().severe("StartPoint for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                x = startPointCoords.get(0);
                y = startPointCoords.get(1);
                z = startPointCoords.get(2);
                Location startPoint = new Location(world, x, y, z);

                // End Point
                List<Integer> endPointCoords = config.contains(basePath + ".EndPoint") ? config.getIntegerList(basePath + ".EndPoint") : new ArrayList<>();
                if (endPointCoords.size() != 3) {
                    plugin.getLogger().severe("EndPoint for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                x = endPointCoords.get(0);
                y = endPointCoords.get(1);
                z = endPointCoords.get(2);
                Location endPoint = new Location(world, x, y, z);

                // Gate Area
                List<Integer> gateAreaStart = config.contains(basePath + ".GateArea.Start") ? config.getIntegerList(basePath + ".GateArea.Start") : new ArrayList<>();
                if (gateAreaStart.size() != 3) {
                    plugin.getLogger().severe("GateArea Start for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                x1 = gateAreaStart.get(0);
                y1 = gateAreaStart.get(1);
                z1 = gateAreaStart.get(2);
                List<Integer> gateAreaEnd = config.contains(basePath + ".GateArea.End") ? config.getIntegerList(basePath + ".GateArea.End") : new ArrayList<>();
                if (gateAreaEnd.size() != 3) {
                    plugin.getLogger().severe("GateArea End for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                x2 = gateAreaEnd.get(0);
                y2 = gateAreaEnd.get(1);
                z2 = gateAreaEnd.get(2);
                Area gate = new Area(new Location(world, x1, y1, z1), new Location(world, x2, y2, z2), Material.WAXED_EXPOSED_COPPER_GRATE);

                // Gate Key Locations
                List<Location> gateKeys = new ArrayList<>();
                List<String> keys = new ArrayList<>( config.getConfigurationSection(basePath + ".GateKeys").getKeys(false));
                if (keys.isEmpty()) {
                    plugin.getLogger().severe("Instance " + instanceInt + " in file " + file.getName() + " does not contain any keys to open the main gate.");
                    continue;
                }
                for (String key: keys) {
                    List<Integer> gateKey = config.contains(basePath + ".GateKeys." + key) ? config.getIntegerList(basePath + ".GateKeys." + key) : new ArrayList<>();
                    if (gateKey.size() != 3) {
                        plugin.getLogger().severe("GateKey" + key + " for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                        continue;
                    }
                    gateKeys.add(new Location(world, gateKey.get(0), gateKey.get(1), gateKey.get(2)));
                }
                if (gateKeys.isEmpty()) {
                    plugin.getLogger().severe("Instance " + instanceInt + " in file " + file.getName() + " does not contain any valid keys to open the main gate.");
                    continue;
                }

                // Default Reward Chest Location
                List<Integer> rewardChestCoords = config.contains(basePath + ".RewardChest") ? config.getIntegerList(basePath + ".RewardChest") : new ArrayList<>();
                if (rewardChestCoords.size() != 3) {
                    plugin.getLogger().severe("RewardChest for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                    continue;
                }
                x = rewardChestCoords.get(0);
                y = rewardChestCoords.get(1);
                z = rewardChestCoords.get(2);
                Location rewardChest = new Location(world, x, y, z);

                // Default Reward
                DungeonRewardInfo defaultReward = new DungeonRewardInfo(gateKeys, gate, rewardChest, rewards, rewardChances, numbRewards);

                // List of Bonus Rewards
                List<DungeonRewardInfo> bonusRewards = new ArrayList<>();
                for (int i=0; i<bonusRewardsItems.size(); i++) {

                    // Bonus Gate Key Locations
                    List<Location> bonusGateKeys = new ArrayList<>();
                    List<String> bonusKeys;
                    try {
                        bonusKeys = new ArrayList<>(config.getConfigurationSection(basePath + ".Bonuses." + (i + 1) + ".GateKeys").getKeys(false));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Missing section: " + basePath + ".Bonuses." + (i + 1) + ".GateKeys in file " + file.getName());
                        continue;
                    }
                    for (String key: bonusKeys) {
                        List<Integer> gateKey = config.contains(basePath + ".Bonuses." + (i + 1) + ".GateKeys." + key) ? config.getIntegerList(basePath + ".Bonuses." + (i + 1) + ".GateKeys." + key) : new ArrayList<>();
                        if (gateKey.size() != 3) {
                            plugin.getLogger().severe("GateKey" + key + " for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                            continue;
                        }
                        bonusGateKeys.add(new Location(world, gateKey.get(0), gateKey.get(1), gateKey.get(2)));
                    }
                    if (bonusGateKeys.isEmpty()) {
                        plugin.getLogger().info("Reward " + (i+1) + " for instance " + instance + " in file " + file.getName() + " does not have any keys. Associated reward and gate can only be opened by command.");
                        continue;
                    }

                    // Bonus Gate Area
                    List<Integer> bonusGateAreaStart = config.contains(basePath + ".Bonuses." + (i + 1) + ".GateArea.Start") ? config.getIntegerList(basePath + ".Bonuses." + (i + 1) + ".GateArea.Start") : new ArrayList<>();
                    if (bonusGateAreaStart.size() != 3) {
                        plugin.getLogger().severe("Bonus GateArea Start for reward " + (i+1) + " for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                        continue;
                    }
                    x1 = bonusGateAreaStart.get(0);
                    y1 = bonusGateAreaStart.get(1);
                    z1 = bonusGateAreaStart.get(2);
                    List<Integer> bonusGateAreaEnd = config.contains(basePath + ".Bonuses." + (i + 1) + ".GateArea.End") ? config.getIntegerList(basePath + ".Bonuses." + (i + 1) + ".GateArea.End") : new ArrayList<>();
                    if (bonusGateAreaEnd.size() != 3) {
                        plugin.getLogger().severe("Bonus GateArea End for reward " + (i+1) + " for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                        continue;
                    }
                    x2 = bonusGateAreaEnd.get(0);
                    y2 = bonusGateAreaEnd.get(1);
                    z2 = bonusGateAreaEnd.get(2);
                    Area bonusGate = new Area(new Location(world, x1, y1, z1), new Location(world, x2, y2, z2), Material.WAXED_EXPOSED_COPPER_GRATE);

                    // Bonus Reward Chest Location
                    List<Integer> BonusRewardChestCoords = config.contains(basePath + ".Bonuses." + (i + 1) + ".RewardChest") ? config.getIntegerList(basePath + ".Bonuses." + (i + 1) + ".RewardChest") : new ArrayList<>();
                    if (BonusRewardChestCoords.size() != 3) {
                        plugin.getLogger().severe("Bonus RewardChest for reward " + (i+1) + " for instance " + instance + " in file " + file.getName() + " is not an integer list of length 3.");
                        continue;
                    }
                    x = BonusRewardChestCoords.get(0);
                    y = BonusRewardChestCoords.get(1);
                    z = BonusRewardChestCoords.get(2);
                    Location bonusRewardChest = new Location(world, x, y, z);

                    bonusRewards.add(new DungeonRewardInfo(bonusGateKeys, bonusGate, bonusRewardChest, bonusRewardsItems.get(i), bonusRewardChances.get(i), bonusNumbRewards.get(i)));
                }

                dungeons.put(dungeonName + instanceInt, new DungeonInfo(dungeonName, instanceInt, false, startupCommands, exitCommands, maxRunTime, dungeonArea, startPoint, endPoint, exitPoint, defaultReward, bonusRewards));
            }
        }
    }

    /**
     * Get all dungeon identifiers
     *
     * @return list of dungeons
     */
    public List<String> getDungeons() {
        return new ArrayList<>(dungeons.keySet());
    }

    /**
     * Get all unique dungeon
     *
     * @return list of unique dungeons
     */
    public List<String> getUniqueDungeons() {
        return uniqueDungeons;
    }

    /**
     * Get information about dungeon
     * @param dungeonID the ID of the dungeon
     * @return information about a specific dungeon
     */
    public DungeonInfo getDungeon(String dungeonID) {
        return dungeons.get(dungeonID);
    }
}