package net.bnbdiscord.borderkit;

import com.google.gson.Gson;
import net.bnbdiscord.borderkit.jsSupport.AsyncThenable;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpResponseProxy implements ProxyObject {
    private final Plugin plugin;
    @org.jetbrains.annotations.NotNull
    private final HttpURLConnection connection;

    public HttpResponseProxy(Plugin plugin, HttpURLConnection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    private static final Set<String> PROPERTIES = Set.of("ok", "status", "statusText", "text", "json");

    @Override
    public Object getMember(String key) {
        try {
            return switch (key) {
                case "ok" -> connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299;
                case "status" -> connection.getResponseCode();
                case "statusText" -> connection.getResponseMessage();
                case "text" -> (ProxyExecutable) (arguments) -> new AsyncThenable(plugin) {
                    @Override
                    public Object doWork() {
                        try {
                            var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                            char[] buffer = new char[1024];
                            var sb = new StringBuilder();
                            for (int read; (read = reader.read(buffer)) > 0; ) {
                                sb.append(buffer, 0, read);
                            }
                            return sb.toString();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                case "json" -> (ProxyExecutable) (arguments) -> new AsyncThenable(plugin) {
                    @Override
                    public Object doWork() {
                        try {
                            var reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                            char[] buffer = new char[1024];
                            var sb = new StringBuilder();
                            for (int read; (read = reader.read(buffer)) > 0; ) {
                                sb.append(buffer, 0, read);
                            }
                            var jsonString = sb.toString();
                            var gson = new Gson();
                            if (jsonString.startsWith("[")) {
                                return ProxyArray.fromArray(gson.fromJson(jsonString, List.class).toArray());
                            } else {
                                return ProxyObject.fromMap(gson.fromJson(jsonString, Map.class));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                default -> null;
            };
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Object getMemberKeys() {
        return PROPERTIES.toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return PROPERTIES.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        // Not allowed
        throw new UnsupportedOperationException();
    }
}
