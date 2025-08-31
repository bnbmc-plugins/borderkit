package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.commands.PassportCommand;
import net.bnbdiscord.borderkit.commands.PassportCommandCompleter;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.server.ServerRoot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
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
        var passportCommandCompleter = new PassportCommandCompleter(this, db, server);
        passportCommand.setTabCompleter(passportCommandCompleter);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public String isAttestationDisabledForPlayer(Player player) {
        for (var serviceClass : Bukkit.getServicesManager().getKnownServices()) {
            if (serviceClass.getName().equals("omg.lol.jplexer.race.RaceCSApi")) {
                var racecsService = Bukkit.getServicesManager().getRegistration(serviceClass);

                if (racecsService != null) {
                    try {
                        var api = racecsService.getProvider();
                        var isParticipantMethod = api.getClass().getMethod("isParticipant", String.class);
                        boolean isParticipating = (boolean) isParticipantMethod.invoke(api, player.getName());

                        if (isParticipating) {
                            return "Please continue. Passport processing has been disabled because you are participating in the AirCS Race.";
                        }
                    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                        getLogger().log(java.util.logging.Level.SEVERE, "RaceCS API mismatch", e);
                    }
                }
            }
        }

        return null;
    }
}
