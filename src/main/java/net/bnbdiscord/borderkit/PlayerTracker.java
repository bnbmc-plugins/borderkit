package net.bnbdiscord.borderkit;

import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import static net.bnbdiscord.borderkit.Utils.setCommandBlockStrength;

public class PlayerTracker {
    private final Plugin plugin;
    private final BlockCommandSender commandBlock;
    private final Player player;
    private final Location initialLocation;
    private final int distance;
    private final BukkitTask task;

    public PlayerTracker(Plugin plugin, BlockCommandSender commandBlock, Player player, int distance) {
        this.plugin = plugin;
        this.commandBlock = commandBlock;
        this.player = player;
        this.initialLocation = player.getLocation();
        this.distance = distance;

        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkPlayer, 0, 1);
    }

    private boolean isOutsideRange(Location location) {
        if (location.getWorld().equals(initialLocation.getWorld())) {
            if (location.distance(initialLocation) < distance) {
                // The player is too close to the command block
                return false;
            }
        }
        return true;
    }

    private void checkPlayer() {
        if (!isOutsideRange(player.getLocation())) {
            return;
        }

        setCommandBlockStrength(plugin, commandBlock, 0);
        plugin.getServer().getScheduler().cancelTask(task.getTaskId());
    }
}
