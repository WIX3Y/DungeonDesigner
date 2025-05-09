package wix3y.dungeonDesigner.util.datastructures;

import org.bukkit.Location;
import org.bukkit.Material;

public record Area(Location start, Location stop, Material material) {
}