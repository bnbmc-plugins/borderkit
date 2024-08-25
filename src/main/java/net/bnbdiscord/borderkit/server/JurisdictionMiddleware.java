package net.bnbdiscord.borderkit.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import express.filter.Filter;
import express.http.HttpRequestHandler;
import express.http.request.Request;
import express.http.response.Response;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class JurisdictionMiddleware implements HttpRequestHandler, Filter {
    private final DatabaseManager db;
    Plugin plugin;
    JWTVerifier verifier;

    public JurisdictionMiddleware(DatabaseManager db, Plugin plugin, Algorithm algorithm) {
        this.db = db;
        this.plugin = plugin;
        this.verifier = JWT.require(algorithm)
                .withIssuer("BKT")
                .build();
    }

    @Override
    public void handle(Request req, Response res) {
        if (req.hasAuthorization()) {
            var auth = req.getAuthorization().get(0);
            if (auth.getType().equals("Bearer")) {
                try {
                    var jwt = verifier.verify(auth.getData());
                    var jurisdiction = jwt.getClaim("jurisdiction").asString();

                    var jurisdictions = db.getJurisdictionDao().queryForEq("code", jurisdiction);
                    if (!jurisdictions.isEmpty()) {
                        req.addMiddlewareContent(this, jurisdictions.get(0));
                    }
                } catch (JWTVerificationException | SQLException ignored) {

                }
            }
        }
    }

    @Override
    public String getName() {
        return "jurisdiction";
    }
}
