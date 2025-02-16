package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.exceptions.AttestationException;
import net.bnbdiscord.borderkit.exceptions.InvalidRulesetException;
import net.bnbdiscord.borderkit.jsSupport.Thenable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

public class Attestation {
    private final Plugin plugin;
    private final String jurisdictionCode;
    String ruleset;
    String globalRuleset;

    public Attestation(Plugin plugin, DatabaseManager db, String jurisdictionCode, String ruleset) throws InvalidRulesetException, SQLException {
        this.plugin = plugin;
        this.jurisdictionCode = jurisdictionCode;
        var rulesets = db.getRulesetDao().queryForFieldValues(Map.of("jurisdiction_id", jurisdictionCode, "name", ruleset));
        if (rulesets.isEmpty()) {
            throw new InvalidRulesetException();
        }
        this.ruleset = rulesets.get(0).getCode();

        var globalRuleset = db.getRulesetDao().queryForFieldValues(Map.of("jurisdiction_id", jurisdictionCode, "name", "global")).stream().findFirst();
        this.globalRuleset = globalRuleset.isPresent() ? globalRuleset.get().getCode() : """
                function handler(passport, player, next) {
                    if (passport?.isExpired) return false;
                    return next()
                }""";
    }

    private interface RulesetEvaluationNextFunction {
        public Object runNextFunction();
    }

    private void evaluateRuleset(String rulesetCode, Passport passport, Player player, RulesetEvaluationNextFunction nextFunction, Consumer<Value> callback, Consumer<Exception> onError) {
        try {
            var context = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.newBuilder()
                            .allowArrayAccess(true)
                            .build())
                    .build();
            context.getBindings("js").putMember("fetch", new HttpFetchProxy(plugin));

            context.eval("js", rulesetCode);
            var handlerFunction = context.getBindings("js").getMember("handler");
            var handlerReturnValue = handlerFunction.execute(passport, new PlayerProxy(player, jurisdictionCode), (ProxyExecutable) arguments -> nextFunction.runNextFunction());
            if (handlerReturnValue.hasMember("then")) {
                handlerReturnValue.invokeMember("then", (ProxyExecutable) (retval) -> {
                    callback.accept(retval[0]);
                    Bukkit.getScheduler().runTask(plugin, () -> context.close(true));
                    return null;
                });
            } else {
                callback.accept(handlerReturnValue);
                Bukkit.getScheduler().runTask(plugin, () -> context.close(true));
            }
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    public void attest(Passport passport, Player player, Consumer<Integer> callback, Consumer<AttestationException> onError) {
        try {
            var nextFunction = new Thenable() {
                @Override
                protected void then(Value onResolve, Value onReject) {
                    evaluateRuleset(ruleset, passport, player, () -> Value.asValue(true), onResolve::executeVoid, onReject::executeVoid);
                }
            };

            evaluateRuleset(globalRuleset, passport, player, () -> nextFunction, result -> {
                int distance;
                if (result.isNumber()) {
                    distance = result.asInt();
                } else if (result.isBoolean()) {
                    if (result.asBoolean()) {
                        distance = 3;
                    } else {
                        onError.accept(new AttestationException("Attestation failed"));
                        return;
                    }
                } else {
                    player.sendMessage(Component.text("BorderKit: There was a problem verifying your passport. Please visit a Border Force officer for manual processing.").color(TextColor.color(255, 0, 0)));
                    onError.accept(new AttestationException("Return value was not a boolean or number"));
                    return;
                }

                callback.accept(distance);
            }, e -> onError.accept(new AttestationException("An exception was thrown from the handler code. " + e.getMessage())));
        } catch (Exception e) {
            onError.accept(new AttestationException("An exception was thrown from the handler code. " + e.getMessage()));
        }
    }
}
