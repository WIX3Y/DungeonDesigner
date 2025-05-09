package wix3y.dungeonDesigner.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.DungeonDesigner;

public class Reload implements CommandExecutor {
    private final DungeonDesigner plugin;

    public Reload(DungeonDesigner plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.reload();
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeon Designer</gradient>]</dark_gray> <gray>>> <green>Config reloaded!"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeon Designer</gradient>]</dark_gray> <gray>>> <yellow>Note: A server restart is required to add new dungeons and new columns to the database!"));
        return true;
    }
}