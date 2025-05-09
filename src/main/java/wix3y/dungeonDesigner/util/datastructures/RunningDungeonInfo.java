package wix3y.dungeonDesigner.util.datastructures;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunningDungeonInfo {
    private boolean isRunning;
    private String dungeonName;
    private int dungeonInstance;
    private Long startTime;
    private Map<Location, Boolean> openedKey;
    private List<Map<Location, Boolean>> openedBonusKeys;

    public RunningDungeonInfo () {
        reset();
    }

    public boolean getIsRunning() {
        return isRunning;
    }

    public String getDungeonName() {
        return dungeonName;
    }

    public int getDungeonInstance() {
        return dungeonInstance;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Map<Location, Boolean> getOpenedKeys() {
        return openedKey;
    }

    public Map<Location, Boolean> getOpenedBonusKeys(int index) {
        return openedBonusKeys.get(index);
    }

    public List<Map<Location, Boolean>> getAllOpenedBonusKeys() {
        return openedBonusKeys;
    }

    public void setIsRunning(boolean value) {
        isRunning = value;
    }

    public void setDungeonName(String value) {
        dungeonName = value;
    }

    public void setDungeonInstance(int value) {
        dungeonInstance = value;
    }

    public void setStartTime(Long value) {
        startTime = value;
    }

    public void initializeKeys(List<Location> locations) {
        openedKey = new ConcurrentHashMap<>();
        for (Location location: locations) {
            openedKey.put(location, false);
        }
    }

    public void initializeBonusKeys(List<List<Location>> locationsList) {
        openedBonusKeys = new ArrayList<>();
        for (List<Location> locations: locationsList) {
            Map<Location, Boolean> openedBonusKey = new ConcurrentHashMap<>();
            for (Location location: locations) {
                openedBonusKey.put(location, false);
            }
            openedBonusKeys.add(openedBonusKey);
        }
    }

    public void setOpenedKey(Location location, Boolean value) {
        openedKey.put(location, value);
    }

    public void setAllOpenedKey(Boolean value) {
        openedKey.replaceAll((l, v) -> value);
    }

    public void  setOpenedBonusKeys(int index, Location location, Boolean value) {
        openedBonusKeys.get(index).put(location, value);
    }

    public void  setAllOpenedBonusKeys(int index, Boolean value) {
        openedBonusKeys.get(index).replaceAll((l, v) -> value);
    }

    public void reset() {
        isRunning = false;
        dungeonName = "";
        dungeonInstance = -1;
        startTime = -1L;
        openedKey = new ConcurrentHashMap<>();
        openedBonusKeys = new ArrayList<>();
    }
}