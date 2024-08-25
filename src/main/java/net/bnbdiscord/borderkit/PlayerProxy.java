package net.bnbdiscord.borderkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Date;
import java.util.Set;

public class PlayerProxy implements ProxyObject {
    private final Player player;
    private final String jurisdiction;

    public PlayerProxy(Player player, String jurisdiction) {
        this.player = player;
        this.jurisdiction = jurisdiction;
    }

    private static final Set<String> PROPERTIES = Set.of("sendError", "send");

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "sendError" -> (ProxyExecutable) arguments -> {
                player.sendMessage(Component.text().append(Component.text(jurisdiction + "   ").color(TextColor.color(200, 200, 200)))
                        .append(Component.text(arguments[0].asString()).color(TextColor.color(255, 0, 0))));
                return null;
            };
            case "send" -> (ProxyExecutable) arguments -> {
                player.sendMessage(Component.text().append(Component.text(jurisdiction + "   ").color(TextColor.color(200, 200, 200)))
                        .append(Component.text(arguments[0].asString()).color(TextColor.color(0, 200, 0))));
                return null;
            };
            default -> throw new UnsupportedOperationException();
        };
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
