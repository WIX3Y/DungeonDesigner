# Dungeon Designer
Design custom dungeons with key-locked gates, timers, and challenges. Track completion times and reward players for finishing the dungeon or completing bonus objectives.

## Commands
### ddstart \<dungeon\> \<instance\> \<player\>
Description: Start a dungeon run for a specific player \
Example usage: /ddstart TestDungeon 1 WIX3Y \
Aliases: dungeondesignerstart \
Permission: dungeondesigner.manage
### ddunlock \<dungeon\> \<instance\> \<reward-id\> \<player\>
Description: Unlocks a dungeon gate and reward for a specific player running a dungeon \
Example usage: /ddunlock TestDungeon 1 0 WIX3Y \
Aliases: dungeondesignerunlock \
Permission: dungeondesigner.manage
### ddreward \<dungeon\> \<instance\> \<player\>
Description: Sets a dungeon as completed for a player, calculating the dungeon run time and closing the main gate \
Example usage: /ddreward TestDungeon 1 WIX3Y \
Aliases: dungeondesignerreward \
Permission: dungeondesigner.manage
### ddend \<dungeon\> \<instance\> \<player\>
Description: Stops a dungeon run and sets dungeon as unoccupied \
Example usage: /ddend TestDungeon 1 WIX3Y \
Aliases: dungeondesignerend \
Permission: dungeondesigner.manage
### ddaddreward \<dungeon-id\> \<reward-id\> \<chance\>
Description: Add a reward to a specific loot chest loot table for a specific dungeon \
Example usage: /ddaddreward TestDungeon 0 10 \
Aliases: dungeondesigneraddreward, ddadd, dungeondesigneradd \
Permission: dungeondesigner.addreward
### ddreload
Description: Reloads the plugin \
Example usage: /ddreload \
Aliases: dungeondesignerreload \
Permission: dungeondesigner.reload
