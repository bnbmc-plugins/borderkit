package net.bnbdiscord.borderkit.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import express.filter.Filter;
import express.http.HttpRequestHandler;
import express.http.request.Request;
import express.http.response.Response;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class AuthMiddleware implements HttpRequestHandler, Filter {
    Plugin plugin;
    JWTVerifier verifier;

    public AuthMiddleware(Plugin plugin, Algorithm algorithm) {
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
                    var playerUuid = jwt.getClaim("sub").asString();
                    req.addMiddlewareContent(this, plugin.getServer().getPlayer(UUID.fromString(playerUuid)));
                } catch (JWTVerificationException ignored) {

                }
            }
        }
    }

    @Override
    public String getName() {
        return "player";
    }
}
