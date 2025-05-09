package wix3y.dungeonDesigner.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.util.*;
import wix3y.dungeonDesigner.util.datastructures.Area;
import wix3y.dungeonDesigner.util.datastructures.DungeonInfo;
import wix3y.dungeonDesigner.util.datastructures.RunningDungeonInfo;

public class EndDungeon implements CommandExecutor {
    private final PlayerDataUtil playerDataUtil;
    private ConfigUtil configUtil;

    public EndDungeon(PlayerDataUtil playerDataUtil, ConfigUtil configUtil) {
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
     * End a dungeon run and reset the dungeon
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
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>/ddend <dungeon> <instance> <player>"));
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

        for (String exitCmd: dungeonInfo.getExitCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsePlaceholders(player, exitCmd));
        }

        // teleport player out of the dungeon
        player.teleport(dungeonInfo.getExitPoint());

        // reset player dungeon running data if the exiting player was running the dungeon
        RunningDungeonInfo runningDungeonInfo = playerDataUtil.getPlayerDungeonRunData(player.getUniqueId().toString());
        if (!(runningDungeonInfo.getDungeonName().equals(dungeonInfo.getName()) && runningDungeonInfo.getDungeonInstance() == dungeonInfo.getInstance())) {
            return true;
        }
        runningDungeonInfo.reset();

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

        // set dungeon to no longer occupied
        dungeonInfo.setOccupied(false);

        return true;
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