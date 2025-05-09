package wix3y.dungeonDesigner.commands;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import wix3y.dungeonDesigner.DungeonDesigner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RewardItem implements CommandExecutor {
    private final DungeonDesigner plugin;

    public RewardItem(DungeonDesigner plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("<red>Only players can run this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>/ddaddreward <dungeon-id> <reward-id> <chance>"));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>dungeon ID is the name displayed at the top of the dungeon's file (case sensitive), this should also match the file name excluding the \".yml\"."));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>reward ID is 0 for default loot chest reward."));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>chance can be any integer larger than 0."));
            return true;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must hold the reward item while running this command."));
            return true;
        }

        int chance;
        try {
            chance = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid chance, chance must be an integer larger than 0."));
            return true;
        }
        if (chance <= 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid chance, chance must be an integer larger than 0."));
            return true;
        }

        File dungeonFile = new File(plugin.getDataFolder(), "dungeons/" + args[0].toLowerCase() + ".yml");
        if(!dungeonFile.exists()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid dungeon ID."));
            return true;
        }
        YamlConfiguration dungeonYML = YamlConfiguration.loadConfiguration(dungeonFile);

        String path;
        if (args[1].equals("0")) {
            if (!dungeonYML.contains(args[0] + ".DefaultReward")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>DefaultReward section missing for dungeon " + args[0] + "."));
                return true;
            }
            path = args[0] + ".DefaultReward";
        }
        else {
            if (!dungeonYML.contains(args[0] + ".BonusRewards." + args[1])) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>BonusRewards section with id " + args[1] + " missing for dungeon " + args[0] + "."));
                return true;
            }
            path = args[0] + ".BonusRewards." + args[1];
        }

        List<ItemStack> rewards = new ArrayList<>();
        List<?> rewardsStrings = dungeonYML.getList(path + ".Rewards");
        List<Integer> chances = dungeonYML.getIntegerList(path + ".RewardChances");
        if (rewardsStrings != null) {
            for (Object o : rewardsStrings) {
                if (o instanceof ItemStack) {
                    rewards.add((ItemStack) o);
                }
            }
        }
        rewards.add(itemStack);
        chances.add(chance);
        dungeonYML.set(path + ".Rewards", rewards);
        dungeonYML.set(path + ".RewardChances", chances);

        try {
            dungeonYML.save(dungeonFile);
        } catch (Exception e) {
            sender.sendMessage("<red>Something went wrong when saving the dungeon file. Saving new reward failed.");
            e.printStackTrace();
            return true;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<dark_gray>[<gradient:#00AA44:#99FFBB:#00AA44>Dungeon Designer</gradient>]</dark_gray> <gray>>> <green>Held item(s) added as reward for dungeon!"));

        plugin.reloadConfig();
        return true;
    }
}