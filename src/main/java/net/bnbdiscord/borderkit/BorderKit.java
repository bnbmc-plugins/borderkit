package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.commands.PassportCommand;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Objects;

public final class BorderKit extends JavaPlugin {

    private DatabaseManager db;

    @Override
    public void onEnable() {
        // Plugin startup logic
        db = new DatabaseManager();

        var passportCommand = Objects.requireNonNull(getCommand("passport"));
        var passportCode = new PassportCommand(this, db);
        passportCommand.setExecutor(passportCode);
//        passportCommand.setTabCompleter(passportCode);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
