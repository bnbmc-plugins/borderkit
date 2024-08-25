package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.commands.PassportCommand;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.server.ServerRoot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class BorderKit extends JavaPlugin {

    private DatabaseManager db;

    private ServerRoot server;

    @Override
    public void onEnable() {
        // Plugin startup logic
        db = new DatabaseManager();

        server = new ServerRoot(this, db);

        var passportCommand = Objects.requireNonNull(getCommand("passport"));
        var passportCode = new PassportCommand(this, db, server);
        passportCommand.setExecutor(passportCode);
//        passportCommand.setTabCompleter(passportCode);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
