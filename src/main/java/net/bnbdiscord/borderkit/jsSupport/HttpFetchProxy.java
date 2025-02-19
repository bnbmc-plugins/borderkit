package net.bnbdiscord.borderkit.jsSupport;

import net.bnbdiscord.borderkit.jsSupport.helpers.AsyncThenable;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class HttpFetchProxy implements ProxyExecutable {
    private final Plugin plugin;

    public HttpFetchProxy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Object execute(Value... arguments) {
        return new AsyncThenable(plugin) {
            @Override
            public Object doWork() {
                try {
                    if (arguments.length < 1) {
                        // Problem
                        throw new UnsupportedOperationException();
                    }

                    var url = new URL(arguments[0].asString());
                    var connection = (HttpURLConnection) url.openConnection();

                    var followRedirects = true;
                    var method = "GET";
                    if (arguments.length > 1) {
                        var options = arguments[1];
                        if (options.hasMember("followRedirects")) {
                            followRedirects = options.getMember("followRedirects").asBoolean();
                        }

                        if (options.hasMember("method")) {
                            method = options.getMember("method").asString();
                        }
                    }

                    connection.setRequestProperty("User-Agent", "borderkit/1.0");
                    connection.setInstanceFollowRedirects(followRedirects);
                    connection.setRequestMethod(method);

                    if (connection instanceof HttpsURLConnection) {
                        var httpsConnection = (HttpsURLConnection) connection;
                    }

                    connection.getResponseCode();
                    return new HttpResponseProxy(plugin, connection);
                } catch (IOException e) {
                    var response = new HashMap<String, Object>();
                    response.put("error", true);
                    return ProxyObject.fromMap(response);
                }
            }
        };

    }
}

