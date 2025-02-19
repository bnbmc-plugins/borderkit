package net.bnbdiscord.borderkit.jsSupport;

import net.bnbdiscord.borderkit.jsSupport.helpers.Thenable;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public class SubtleCryptoProxy implements ProxyObject {
    private static final Set<String> PROPERTIES = Set.of("digest");

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "digest" -> (ProxyExecutable) (arguments) -> {
                if (arguments.length < 2) {
                    return null;
                }
                return digest(arguments[0].asString(), arguments[1].as(ByteBuffer.class));
            };
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

    public Thenable digest(String algorithm, ByteBuffer input) {
        return new Thenable() {
            @Override
            protected void then(Value onResolve, Value onReject) {
                try {
                    var digestEngine = MessageDigest.getInstance(algorithm);
                    digestEngine.update(input);
                    var result = digestEngine.digest();

                    onResolve.execute(ByteBuffer.wrap(result));
                } catch (NoSuchAlgorithmException e) {
                    onReject.execute(new RuntimeException(e));
                }
            }
        };
    }
}
