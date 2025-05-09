package wix3y.dungeonDesigner.util.datastructures;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record DungeonRewardInfo(List<Location> gateKeys, Area gate, Location rewardChest,
                                List<ItemStack> rewards, List<Integer> rewardChances,
                                Integer numbRewards) {
}