package net.bnbdiscord.borderkit.jsSupport.helpers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;

public abstract class AsyncThenable extends Thenable {
    private final Plugin plugin;

    public AsyncThenable(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void then(Value onResolve, Value onReject) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var retval = doWork();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    onResolve.execute(retval);
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    onReject.execute(e);
                });
            }
        });
    }

    public abstract Object doWork();
}
