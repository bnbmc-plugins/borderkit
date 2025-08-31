package net.bnbdiscord.borderkit.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import express.DynExpress;
import express.Express;
import express.http.RequestMethod;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.MediaType;
import express.utils.Status;
import net.bnbdiscord.borderkit.Passport;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServerRoot {
    private final Plugin plugin;
    private final DatabaseManager db;
    private final Algorithm tokenAlgorithm;
    private final Map<String, PreBeamData> prebeams = new HashMap<>();
    private final Map<String, Beam> beams = new HashMap<>();

    record Beam(Response res, PreBeamData prebeam) {}

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
            if (path.startsWith("/api/")) return;
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

    public PreBeamData getPreBeamData(String beamCode) {
        var beam = beams.get(beamCode);
        if (beam == null) return null;
        return beam.prebeam;
    }

    public Set<String> activeBeamCodes() {
        return beams.keySet();
    }

    public boolean completeBeam(String beamCode, Passport passport, Player player) {
        var beam = beams.remove(beamCode);
        if (beam == null) {
            return false;
        }

        var res = beam.res;
        var prebeam = beam.prebeam;

        if (player.getName().equals(prebeam.playerName) && passport.getPassportNumber().equals(prebeam.passportNumber)) {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .registerTypeAdapter(Passport.class, new Passport.GsonSerializer())
                    .create();
            var passportData = gson.toJson(passport, Passport.class);

            res.setContentType("application/json");
            res.setStatus(Status._200);
            res.send(passportData);
        } else {
            res.setHeader("X-BorderKit-Beam-Error", "bad-details");
            res.sendStatus(Status._401);
        }

        return true;
    }

    static class RulesetData {
        String name;
        String language;
        String code;
    }

    public static class PreBeamData {
        public String passportNumber;
        public String playerName;
        public String serviceName;
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

        @DynExpress(context = "/api/beam", method = RequestMethod.OPTIONS)
        public void preflightBeam(Request req, Response res) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "POST");
            res.setHeader("Access-Control-Expose-Headers", "X-BorderKit-Beam-Code, X-BorderKit-Beam-Command");
            res.sendStatus(Status._204);
        }

        @DynExpress(context = "/api/beam", method = RequestMethod.POST)
        public void startBeam(Request req, Response res) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "POST");
            res.setHeader("Access-Control-Expose-Headers", "X-BorderKit-Beam-Code, X-BorderKit-Beam-Command");

            var n = 10_000_000 + new java.util.Random().nextInt(90_000_000);
            var beamCode = Integer.toString(n);

            Gson gson = new Gson();
            var prebeam = gson.fromJson(new InputStreamReader(req.getBody()), PreBeamData.class);

            prebeams.put(beamCode, prebeam);

            // Remove the prebeam if the client doesn't respond within 60 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> prebeams.remove(beamCode), 20 * 60);

            res.setHeader("X-BorderKit-Beam-Code", beamCode);
            res.setHeader("X-BorderKit-Beam-Command", "/passport beam " + beamCode);
            res.setStatus(Status._204);
            res.send();
        }

        @DynExpress(context = "/api/beam/:beamCode", method = RequestMethod.GET)
        public void readBeam(Request req, Response res) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET");
            res.setHeader("Access-Control-Expose-Headers", "X-BorderKit-Beam-Error");

            plugin.getLogger().warning("Reading beam code");

            var beamCode = req.getParam("beamCode");
            if (beamCode == null) {
                plugin.getLogger().warning("No beam code provided");
                res.sendStatus(Status._404);
                return;
            }

            var prebeam = prebeams.remove(beamCode);
            if (prebeam == null) {
                plugin.getLogger().warning("No prebeam found for beam code " + beamCode);
                res.sendStatus(Status._404);
                return;
            }

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                var beam = beams.remove(beamCode);
                if (beam == null) {
                    return;
                }

                // Too bad, the beam timed out
                res.setHeader("X-BorderKit-Beam-Error", "timeout");
                res.sendStatus(Status._408);
            }, 20 * 60);

            beams.put(beamCode, new Beam(res, prebeam));
            // Continue without closing the connection
        }
    }
}
