package net.bnbdiscord.borderkit.jsSupport.helpers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;

public abstract class Thenable implements ProxyObject {
    private static final Set<String> PROPERTIES = Set.of("then");
    protected abstract void then(Value onResolve, Value onReject);

    @Override
    public Object getMember(String key) {
        if (key.equals("then")) {
            return (ProxyExecutable) (arguments) -> {
                if (arguments.length == 1) {
                    then(arguments[0], Value.asValue((ProxyExecutable) (arguments1) -> null));
                }

                if (arguments.length == 2) {
                    then(arguments[0], arguments[1]);
                }

                return null;
            };
        }
        return null;
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

