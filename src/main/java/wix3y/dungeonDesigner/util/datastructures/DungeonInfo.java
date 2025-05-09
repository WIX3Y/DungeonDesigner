package wix3y.dungeonDesigner.util.datastructures;

import org.bukkit.Location;

import java.util.List;

public class DungeonInfo {
    private final String name;
    private final int instance;
    private boolean occupied;
    private final List<String> startupCommands;
    private final List<String> exitCommands;
    private final int maxRunTime;
    private final Area dungeon;
    private final Location startPoint;
    private final Location endPoint;
    private final Location exitPoint;
    private final DungeonRewardInfo defaultReward;
    private final List<DungeonRewardInfo> bonusRewards;

    public DungeonInfo (String name, int instance, boolean occupied, List<String> startupCommands,
                        List<String> exitCommands, int maxRunTime, Area dungeon, Location startPoint,
                        Location endPoint, Location exitPoint, DungeonRewardInfo defaultReward,
                        List<DungeonRewardInfo> bonusRewards) {

        this.name = name;
        this.instance = instance;
        this.occupied = occupied;
        this.startupCommands = startupCommands;
        this.exitCommands = exitCommands;
        this.maxRunTime = maxRunTime;
        this.dungeon = dungeon;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.exitPoint = exitPoint;
        this.defaultReward = defaultReward;
        this.bonusRewards = bonusRewards;
    }

    public String getName() {
        return name;
    }

    public int getInstance() {
        return instance;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public List<String> getStartupCommands() {
        return startupCommands;
    }

    public List<String> getExitCommands() {
        return exitCommands;
    }

    public int getMaxRunTime() {
        return maxRunTime;
    }

    public Area getDungeonArea() {
        return dungeon;
    }

    public Location getStartPoint() {
        return startPoint;
    }

    public Location getEndPoint() {
        return endPoint;
    }

    public Location getExitPoint() {
        return exitPoint;
    }

    public DungeonRewardInfo getDefaultReward() {
        return defaultReward;
    }

    public List<DungeonRewardInfo> getBonusRewards() {
        return bonusRewards;
    }

    public void setOccupied(boolean value) {
        occupied = value;
    }
}