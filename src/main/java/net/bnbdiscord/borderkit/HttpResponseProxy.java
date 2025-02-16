package net.bnbdiscord.borderkit;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;

public class HttpResponseProxy implements ProxyObject {
    private static final Set<String> PROPERTIES = Set.of("");

    @Override
    public Object getMember(String key) {
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return null;
    }

    @Override
    public boolean hasMember(String key) {
        return false;
    }

    @Override
    public void putMember(String key, Value value) {

    }
}
