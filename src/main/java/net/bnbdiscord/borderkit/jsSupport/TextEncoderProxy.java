package net.bnbdiscord.borderkit.jsSupport;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class TextEncoderProxy implements ProxyInstantiable {
    @Override
    public Object newInstance(Value... arguments) {
        return new TextEncoderInstanceProxy();
    }
}

class TextEncoderInstanceProxy implements ProxyObject {
    private static final Set<String> PROPERTIES = Set.of("encode", "encoding");

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "encode" -> (ProxyExecutable) (arguments) -> {
                if (arguments.length < 1) {
                    return null;
                }

                return ByteBuffer.wrap(arguments[0].asString().getBytes(StandardCharsets.UTF_8));
            };
            case "encoding" -> "utf-8";
            default -> null;
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