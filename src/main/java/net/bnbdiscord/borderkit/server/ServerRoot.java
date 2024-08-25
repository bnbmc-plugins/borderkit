package net.bnbdiscord.borderkit.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import express.DynExpress;
import express.Express;
import express.http.RequestMethod;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.MediaType;
import express.utils.Status;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.database.Jurisdiction;
import net.bnbdiscord.borderkit.database.Ruleset;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ServerRoot {
    private final Plugin plugin;
    private final DatabaseManager db;
    private final Algorithm tokenAlgorithm;

    public ServerRoot(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;

        byte[] tokenSecret = new byte[32];
        new Random().nextBytes(tokenSecret);
        this.tokenAlgorithm = Algorithm.HMAC256(tokenSecret);

        var app = new Express();
        app.use(new AuthMiddleware(plugin, tokenAlgorithm));
        app.use(new JurisdictionMiddleware(db, plugin, tokenAlgorithm));
        app.bind(new Bindings());
        app.get("*", (req, res) -> {
            var path = req.getPath();
            if (path.equals("/")) path = "/index.html";

            try {
                var resource = ServerRoot.class.getResource("/frontend/dist" + path);
                if (resource == null) {
                    res.sendStatus(Status._404);
                    return;
                }

                var connection = resource.openConnection();
                res.streamFrom(connection.getContentLength(), connection.getInputStream(), MediaType.getByExtension(path.substring(path.lastIndexOf(".") + 1)));
            } catch (IOException e) {
                res.sendStatus(Status._404);
            }
        });
        app.listen(() -> plugin.getLogger().info("BorderKit server listening on port " + plugin.getConfig().getInt("port")), plugin.getConfig().getInt("port"));
    }

    public String rootUrl() {
        return plugin.getConfig().getString("root");
    }

    public String tokenFor(CommandSender commandSender, Jurisdiction jurisdiction) {
        String subject;

        if (commandSender instanceof Player) {
            subject = ((Player) commandSender).getUniqueId().toString();
        } else if (commandSender instanceof ConsoleCommandSender) {
            subject = "CONSOLE";
        } else {
            return null;
        }

        return JWT.create()
                .withIssuer("BKT")
                .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6)))
                .withClaim("sub", subject)
                .withClaim("jurisdiction", jurisdiction.getCode())
                .sign(tokenAlgorithm);
    }

    static class RulesetData {
        String name;
        String language;
        String code;
    }

    class Bindings {
        @DynExpress(context = "/api/rulesets", method = RequestMethod.GET)
        public void getRulesets(Request req, Response res) throws SQLException {
            var jurisdiction = (Jurisdiction) req.getMiddlewareContent("jurisdiction");
            if (jurisdiction == null) {
                res.sendStatus(Status._401);
                return;
            }

            var obj = new JsonObject();
            for (var ruleset : db.getRulesetDao().queryForEq("jurisdiction_id", jurisdiction.getCode())) {
                var objRuleset = new JsonObject();
                objRuleset.addProperty("name", ruleset.getName());
                objRuleset.addProperty("language", ruleset.getLanguage());
                objRuleset.addProperty("code", ruleset.getCode());
                obj.add(ruleset.getName(), objRuleset);
            }

            var gson = new Gson();
            res.send(gson.toJson(obj));
        }

        @DynExpress(context = "/api/rulesets", method = RequestMethod.POST)
        public void setRuleset(Request req, Response res) {
            try {
                var jurisdiction = (Jurisdiction) req.getMiddlewareContent("jurisdiction");
                if (jurisdiction == null) {
                    res.sendStatus(Status._401);
                    return;
                }

                Gson gson = new Gson();
                var ruleset = gson.fromJson(new InputStreamReader(req.getBody()), RulesetData.class);

                var dbRuleset = new Ruleset();
                dbRuleset.setJurisdiction(jurisdiction);
                dbRuleset.setName(ruleset.name);
                dbRuleset.setLanguage(ruleset.language);
                dbRuleset.setCode(ruleset.code);

                var existingRuleset = db.getRulesetDao().queryForFieldValuesArgs(Map.of(
                        "jurisdiction_id", jurisdiction.getCode(),
                        "name", ruleset.name
                ));
                if (!existingRuleset.isEmpty()) {
                    dbRuleset.setId(existingRuleset.get(0).getId());
                }

                db.getRulesetDao().createOrUpdate(dbRuleset);
                res.sendStatus(Status._204);
            } catch (SQLException e) {
                e.printStackTrace();
                res.sendStatus(Status._500);
            }
        }
    }
}
