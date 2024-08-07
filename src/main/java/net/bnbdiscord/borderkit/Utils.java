package net.bnbdiscord.borderkit;

import org.bukkit.Bukkit;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class Utils {
    public static boolean setCommandBlockStrength(Plugin plugin, CommandSender commandSender, int strength) {
        if (commandSender instanceof BlockCommandSender bcSender && bcSender.getBlock().getState() instanceof CommandBlock cb) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                cb.setSuccessCount(strength);
                cb.update();
            });
            return true;
        }
        return false;
    }
}
