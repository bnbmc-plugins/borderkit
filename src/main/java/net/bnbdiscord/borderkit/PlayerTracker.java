package net.bnbdiscord.borderkit;

import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import static net.bnbdiscord.borderkit.Utils.setCommandBlockStrength;

public class PlayerTracker implements Listener {
    private final Plugin plugin;
    private final BlockCommandSender commandBlock;
    private final Player player;
    private final Location initialLocation;
    private final int distance;

    public PlayerTracker(Plugin plugin, BlockCommandSender commandBlock, Player player, int distance) {
        this.plugin = plugin;
        this.commandBlock = commandBlock;
        this.player = player;
        this.initialLocation = player.getLocation();
        this.distance = distance;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer() != player) return;

        var playerLocation = player.getLocation();

        if (playerLocation.getWorld().equals(initialLocation.getWorld())) {
            if (playerLocation.distance(initialLocation) < distance) {
                // The player is too close to the command block
                return;
            }
        }

        setCommandBlockStrength(plugin, commandBlock, 0);
        HandlerList.unregisterAll(this);
    }
}
