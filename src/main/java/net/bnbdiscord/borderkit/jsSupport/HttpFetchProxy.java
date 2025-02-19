package net.bnbdiscord.borderkit.jsSupport;

import net.bnbdiscord.borderkit.jsSupport.helpers.AsyncThenable;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
                    byte[] output = null;
                    var headers = new HashMap<String, String>();
                    if (arguments.length > 1) {
                        var options = arguments[1];
                        if (options.hasMember("redirect")) {
                            var redirect = options.getMember("redirect");
                            followRedirects = redirect.asString().equals("follow");
                        }

                        if (options.hasMember("method")) {
                            method = options.getMember("method").asString();
                        }

                        if (options.hasMember("headers")) {
                            var jsHeaders = options.getMember("headers");
                            for (var header : jsHeaders.getMemberKeys()) {
                                headers.put(header, jsHeaders.getMember(header).asString());
                            }
                        }

                        if (options.hasMember("body")) {
                            var body = options.getMember("body");
                            if (body.isString()) {
                                output = StandardCharsets.UTF_8.encode(body.asString()).array();
                            } else if (body.hasBufferElements()) {
                                output = body.as(ByteBuffer.class).array();
                            }
                        }
                    }

                    connection.setInstanceFollowRedirects(followRedirects);
                    connection.setRequestMethod(method);
                    connection.setRequestProperty("User-Agent", "borderkit/1.0");
                    for (var header : headers.keySet()) {
                        if (header.equals("Host")) continue;
                        connection.setRequestProperty(header, headers.get(header));
                    }

                    if (connection instanceof HttpsURLConnection) {
                        var httpsConnection = (HttpsURLConnection) connection;
                    }

                    if (output != null) {
                        connection.setRequestProperty("Content-Length", String.valueOf(output.length));
                        connection.setDoOutput(true);
                        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                            outputStream.write(output);
                        }
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

