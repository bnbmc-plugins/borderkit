package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.commands.PassportCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BorderKit extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(getCommand("passport")).setExecutor(new PassportCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
