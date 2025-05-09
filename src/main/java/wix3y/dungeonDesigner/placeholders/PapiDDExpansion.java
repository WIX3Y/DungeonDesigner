package wix3y.dungeonDesigner.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wix3y.dungeonDesigner.util.PlayerDataUtil;

public class PapiDDExpansion extends PlaceholderExpansion {
    private final PlayerDataUtil playerDataUtil;

    public PapiDDExpansion (PlayerDataUtil playerDataUtil) {
        this.playerDataUtil = playerDataUtil;
    }

    /**
     * Plugin's placeholder identifier
     *
     * @return the plugins placeholder identifier
     */
    @Override
    public @NotNull String getIdentifier() {
        return "DD";
    }

    /**
     * Plugin's author
     *
     * @return the plugins author
     */
    @Override
    public @NotNull String getAuthor() {
        return "WIX3Y";
    }

    /**
     * Plugin version
     *
     * @return the plugins version
     */
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    /**
     * Plugin's placeholder parsing
     *
     * @param player the player to parse for
     * @param params the placeholder
     * @return the parsed placeholder
     */
    @Override @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        if (params.startsWith("completion_")) {
            String dungeonID = params.substring(11).toUpperCase();
            return String.valueOf(playerDataUtil.getPlayerCompletionData(player.getUniqueId().toString(), dungeonID));
        }
        else if (params.startsWith("time_")) {
            String dungeonID = params.substring(5).toUpperCase();
            return String.valueOf(playerDataUtil.getPlayerTimerData(player.getUniqueId().toString(), dungeonID));
        }

        return null;
    }

    /**
     * Plugin placeholders persist over reloads
     *
     * @return true
     */
    @Override
    public boolean persist() {
        return true;
    }
}