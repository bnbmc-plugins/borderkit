package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.exceptions.AttestationException;
import net.bnbdiscord.borderkit.exceptions.InvalidRulesetException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Player;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.sql.SQLException;
import java.util.Map;

import static net.bnbdiscord.borderkit.Utils.setCommandBlockStrength;

public class Attestation {
    String ruleset;
    String globalRuleset;

    public Attestation(DatabaseManager db, String jurisdictionCode, String ruleset) throws InvalidRulesetException, SQLException {
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
        public Value runNextFunction();
    }

    private Value evaluateRuleset(String rulesetCode, Passport passport, Player player, RulesetEvaluationNextFunction nextFunction) {
        try (var context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.newBuilder()
                        .allowArrayAccess(true)
                        .build())
                .build()) {
            context.eval("js", rulesetCode);
            var handlerFunction = context.getBindings("js").getMember("handler");
            return handlerFunction.execute(passport, new PlayerProxy(player), (ProxyExecutable) arguments -> nextFunction.runNextFunction());
        }
    }

    public int attest(Passport passport, Player player) throws AttestationException {
        try {
            var result = evaluateRuleset(globalRuleset, passport, player, () -> evaluateRuleset(ruleset, passport, player, () -> Value.asValue(true)));

            int distance;
            if (result.isNumber()) {
                distance = result.asInt();
            } else if (result.isBoolean()) {
                if (result.asBoolean()) {
                    distance = 3;
                } else {
                    throw new AttestationException("Attestation failed");
                }
            } else {
                player.sendMessage(Component.text("BorderKit: There was a problem verifying your passport. Please visit a Border Force officer for manual processing.").color(TextColor.color(255, 0, 0)));
                throw new AttestationException("Return value was not a boolean or number");
            }

            return distance;
        } catch (PolyglotException e) {
            throw new AttestationException("An exception was thrown from the handler code. " + e.getMessage());
        }
    }
}
