package net.bnbdiscord.borderkit.commands;

import de.rapha149.signgui.SignGUI;
import net.bnbdiscord.borderkit.Passport;
import net.bnbdiscord.borderkit.PassportSigningState;
import net.bnbdiscord.borderkit.PlayerProxy;
import net.bnbdiscord.borderkit.PlayerTracker;
import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.database.Jurisdiction;
import net.bnbdiscord.borderkit.database.Ruleset;
import net.bnbdiscord.borderkit.exceptions.AttestationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static net.bnbdiscord.borderkit.Utils.setCommandBlockStrength;

public class PassportCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final NamespacedKey key;
    private final DatabaseManager db;

    public Dictionary<UUID, PassportSigningState> signingStates = new Hashtable<>();

    public PassportCommand(Plugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "key");
        this.db = db;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        try {
            return switch (strings[0]) {
                case "sign" -> signPassport(commandSender, strings);
                case "signContinue" -> signContinue(commandSender, strings);
                case "nextPage" -> nextPage(commandSender, strings);
                case "prevPage" -> prevPage(commandSender, strings);
                case "query" -> query(commandSender, strings);
                case "attest" -> attest(commandSender, strings);
                case "jurisdiction" -> jurisdiction(commandSender, strings);
                case "ruleset" -> ruleset(commandSender, strings);
                default -> {
                    commandSender.sendMessage("Invalid Arguments");
                    yield true;
                }
            };
        } catch (SQLException e) {
            commandSender.sendMessage("SQL Error");
            return false;
        }
    }

    private boolean ruleset(CommandSender commandSender, String[] strings) throws SQLException {
        if (strings.length < 3) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        var code = strings[2];
        var jurisdictions = db.getJurisdictionDao().queryForEq("code", code);
        if (jurisdictions.isEmpty()) {
            commandSender.sendMessage("That jurisdiction does not exist");
            return false;
        }

        var jurisdiction = jurisdictions.get(0);
        if (!commandSender.hasPermission("borderkit.jurisdiction." + code.toLowerCase())) {
            commandSender.sendMessage("You don't have permission to manage rulesets for " + jurisdiction.getName());
            return false;
        }

        String name = String.join(" ", Arrays.copyOfRange(strings, 3, strings.length));
        switch (strings[1]) {
            case "set":
                if (strings.length < 4) {
                    commandSender.sendMessage("Invalid Arguments");
                    return false;
                }

                String programCode = null;
                if (commandSender instanceof Player player) {
                    var item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.WRITABLE_BOOK || item.getType() == Material.WRITTEN_BOOK) {
                        var meta = (BookMeta) item.getItemMeta();
                        programCode = String.join("\n", meta.pages().stream().map(page -> ((TextComponent) page).content()).toList());
                    }
                }

                if (programCode == null) {
                    commandSender.sendMessage("No code provided. Please hold a book with code.");
                    return false;
                }

                var ruleset = new Ruleset();
                ruleset.setJurisdiction(jurisdiction);
                ruleset.setName(name);
                ruleset.setLanguage("js");
                ruleset.setCode(programCode);

                var existingRuleset = db.getRulesetDao().queryForFieldValuesArgs(Map.of(
                        "jurisdiction_id", jurisdiction,
                        "name", name,
                        "language", "js"
                ));
                if (!existingRuleset.isEmpty()) {
                    ruleset.setId(existingRuleset.get(0).getId());
                }

                db.getRulesetDao().createOrUpdate(ruleset);

                commandSender.sendMessage("Added " + name + " to " + jurisdiction.getName());
                return true;
            case "remove":
                if (strings.length != 4) {
                    commandSender.sendMessage("Invalid Arguments");
                    return false;
                }

                var rulesets = db.getRulesetDao().queryForFieldValuesArgs(Map.of(
                        "jurisdiction_id", jurisdiction,
                        "name", name,
                        "language", "js"
                ));
                if (rulesets.isEmpty()) {
                    commandSender.sendMessage("No ruleset by that name exists");
                    return false;
                }

                db.getRulesetDao().delete(rulesets.get(0));
                commandSender.sendMessage("Removed " + rulesets.get(0).getName());
                return true;
        }

        return false;
    }

    private boolean jurisdiction(CommandSender commandSender, String[] strings) throws SQLException {
        if (strings.length < 2) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        if (!commandSender.hasPermission("borderkit.jurisdiction")) {
            commandSender.sendMessage("You don't have permissions to use this command.");
        }

        String code;
        String name;
        var jurisdiction = new Jurisdiction();
        switch (strings[1]) {
            case "add":
                if (strings.length < 4) {
                    commandSender.sendMessage("Invalid Arguments");
                    return false;
                }

                code = strings[2];
                name = String.join(" ", Arrays.copyOfRange(strings, 3, strings.length));
                jurisdiction.setCode(code.toUpperCase());
                jurisdiction.setName(name);
                db.getJurisdictionDao().create(jurisdiction);
                commandSender.sendMessage("Added " + code.toUpperCase() + " as " + name);
                return true;
            case "update":
                if (strings.length < 4) {
                    commandSender.sendMessage("Invalid Arguments");
                    return false;
                }

                code = strings[2];
                name = String.join(" ", Arrays.copyOfRange(strings, 3, strings.length));
                jurisdiction.setCode(code.toUpperCase());
                jurisdiction.setName(name);
                db.getJurisdictionDao().update(jurisdiction);
                commandSender.sendMessage("Updated " + code.toUpperCase() + " as " + name);
                return true;
            case "remove":
                if (strings.length != 3) {
                    commandSender.sendMessage("Invalid Arguments");
                    return false;
                }

                code = strings[2];
                var jurisdictions = db.getJurisdictionDao().queryForEq("code", code);
                if (jurisdictions.isEmpty()) {
                    commandSender.sendMessage("That jurisdiction does not exist");
                    return false;
                }

                db.getJurisdictionDao().delete(jurisdictions.get(0));
                commandSender.sendMessage("Removed " + jurisdictions.get(0).getName() + " (" + code.toUpperCase() + ")");
                return true;
        }

        return false;
    }

    private interface RulesetEvaluationNextFunction {
        public Optional<Boolean> runNextFunction();
    }

    private Value evaluateRuleset(String rulesetCode, Passport passport, Player player, RulesetEvaluationNextFunction nextFunction) {
        try (var context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.newBuilder()
                        .allowArrayAccess(true)
                        .build())
                .build()) {
            context.eval("js", rulesetCode);
            var handlerFunction = context.getBindings("js").getMember("handler");
            return handlerFunction.execute(passport, new PlayerProxy(player), (ProxyExecutable) arguments -> nextFunction.runNextFunction().orElse(null));
        }
    }

    private boolean attest(CommandSender commandSender, String[] strings) throws SQLException {
        setCommandBlockStrength(plugin, commandSender, 0);
        if (strings.length != 4) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        var jurisdictionCode = strings[1];
        var rulesetName = strings[2];
        var rulesets = db.getRulesetDao().queryForFieldValues(Map.of("jurisdiction_id", jurisdictionCode, "name", rulesetName));
        if (rulesets.isEmpty()) {
            commandSender.sendMessage("Invalid Ruleset");
            return false;
        }

        var globalRuleset = db.getRulesetDao().queryForFieldValues(Map.of("jurisdiction_id", jurisdictionCode, "name", "global")).stream().findFirst();
        var globalRulesetCode = globalRuleset.isPresent() ? globalRuleset.get().getCode() : """
                function handler(passport, player, next) {
                    if (passport?.isExpired) return false;
                    return next()
                }""";

        var playerName = strings[3];
        var player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            commandSender.sendMessage("Invalid Player");
            return false;
        }

        Passport.forPlayer(plugin, player, passport -> {
            try {
                try {
                    var result = evaluateRuleset(globalRulesetCode, passport, player, () -> {
                        var innerResult = evaluateRuleset(rulesets.get(0).getCode(), passport, player, () -> Optional.of(true));
                        if (!innerResult.isBoolean()) return Optional.empty();
                        return Optional.of(innerResult.asBoolean());
                    });
                    if (result.isBoolean()) {
                        if (result.asBoolean()) {
                            if (setCommandBlockStrength(plugin, commandSender, 15)) {
                                new PlayerTracker(plugin, (BlockCommandSender) commandSender, player);
                            } else {
                                commandSender.sendMessage(Component.text()
                                        .append(Component.text("Attestation succeeded for the passport held by " + player.getName()).color(TextColor.color(0, 200, 0))).appendNewline()
                                        .append(Component.text("Ruleset: ").decorate(TextDecoration.BOLD)).append(Component.text(rulesets.get(0).getName()))
                                );
                            }
                        } else {
                            throw new AttestationException();
                        }
                    } else {
                        commandSender.sendMessage("Return value was not a boolean");
                        player.sendMessage(Component.text("BorderKit: There was a problem verifying your passport. Please visit a Border Force officer for manual processing.").color(TextColor.color(255, 0, 0)));

                        throw new AttestationException();
                    }
                } catch (PolyglotException e) {
                    commandSender.sendMessage("An exception was thrown from the handler code. " + e.getMessage());
                    throw new AttestationException();
                }
            } catch (AttestationException e) {
                if (!setCommandBlockStrength(plugin, commandSender, 0)) {
                    commandSender.sendMessage(Component.text()
                            .append(Component.text("Attestation failed for the passport held by " + player.getName()).color(TextColor.color(255, 0, 0))).appendNewline()
                            .append(Component.text("Ruleset: ").decorate(TextDecoration.BOLD)).append(Component.text(rulesets.get(0).getName()))
                    );
                }
            }
        });
        return true;
    }

    private boolean query(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        var item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITTEN_BOOK) {
            commandSender.sendMessage("Hold the passport that you want to query and try again.");
            return false;
        }

        var passport = new Passport(plugin, item);
        if (!passport.isValidPassport()) {
            commandSender.sendMessage("Hold the passport that you want to query and try again.");
            return false;
        }

        var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(Locale.ENGLISH);

        player.sendMessage(Component.text()
                .append(Component.text("Passport Information").decorate(TextDecoration.BOLD).color(TextColor.color(255, 150, 0))).appendNewline()
                .append(Component.text("Passport Number:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getPassportNumber())).appendNewline()
                .append(Component.text("Issuing Authority:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getIssuingAuthority())).appendNewline()
                .append(Component.text("Family Name:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getFamilyName())).appendNewline()
                .append(Component.text("Given Name:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getGivenName())).appendNewline()
                .append(Component.text("Date of Birth:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getDateOfBirth().format(formatter))).appendNewline()
                .append(Component.text("Place of Birth:").decorate(TextDecoration.BOLD)).appendSpace().append(Component.text(passport.getPlaceOfBirth())).appendNewline()
                .append(Component.text("Expiry Date:").decorate(TextDecoration.BOLD)).appendSpace().append(
                        passport.isExpired() ? Component.text(passport.getExpiryDate().format(formatter) + " (Expired)").color(TextColor.color(255, 0, 0)) : Component.text(passport.getExpiryDate().format(formatter))
                ).appendNewline()
                .build()
        );

        return false;
    }

    private boolean prevPage(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        var state = signingStates.get(player.getUniqueId());
        if (state == null) {
            commandSender.sendMessage("Internal error. Please try again.");
            return false;
        }

        state.flip(-1);
        state.openMenu();
        return true;
    }

    private boolean nextPage(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        var state = signingStates.get(player.getUniqueId());
        if (state == null) {
            commandSender.sendMessage("Internal error. Please try again.");
            return false;
        }

        state.flip(1);
        state.openMenu();
        return true;
    }

    private boolean signContinue(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        var state = signingStates.get(player.getUniqueId());
        if (state == null) {
            commandSender.sendMessage("Internal error. Please try again.");
            return false;
        }

        if (strings.length == 1) {
            // Sign the passport
            var passport = new Passport(plugin, state.getTemplate());
            player.getInventory().addItem(passport.signPassportTemplate(state));
            commandSender.sendMessage("The passport has been signed.");
            return true;
        }

        var field = strings[1];

        var fieldName = state.fieldName(field);
        if (fieldName.isEmpty()) {
            commandSender.sendMessage("Internal error. Please try again.");
            return false;
        }

        var fieldValue = state.fieldValue(field);

        SignGUI.builder().setLines(fieldValue, "-----------------", "Enter value for", fieldName.toUpperCase()).setHandler((p, result) -> {
            state.setFieldValue(field, result.getLine(0));
            state.openMenu();

            return Collections.emptyList();
        }).build().open(player);

        return false;
    }

    private boolean signPassport(CommandSender commandSender, String[] strings) throws SQLException {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        if (strings.length != 2) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        var jurisdictionCode = strings[1].toUpperCase();
        if (jurisdictionCode.length() != 3) {
            commandSender.sendMessage("Invalid issuing authority");
            return false;
        }
        if (!player.hasPermission("borderkit.passport.sign." + jurisdictionCode.toLowerCase())) {
            commandSender.sendMessage("You do not have permission to issue passports for %s. If you believe this is incorrect, please contact a server administrator.".formatted(jurisdictionCode));
            return false;
        }
        var jurisdiction = db.getJurisdictionDao().queryForEq("code", jurisdictionCode);
        if (jurisdiction.isEmpty()) {
            commandSender.sendMessage("Invalid issuing authority");
            return false;
        }

        var item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITTEN_BOOK) {
            commandSender.sendMessage("Hold the template that you want to sign and try again.");
            return false;
        }

        var state = new PassportSigningState(plugin, player, item, jurisdiction.get(0));
        if (state.biodataStartPage() == -1) {
            commandSender.sendMessage("The template is invalid because the template requires three biodata pages, marked with the word \"BIODATA\" on each page.");
            return false;
        }
        signingStates.put(player.getUniqueId(), state);
        state.openMenu();

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            return List.of();
        }

        try {
            var listType = switch (strings[0]) {
                case "sign" -> {
                    yield "jurisdiction";
                }
//                case "signContinue" -> signContinue(commandSender, strings);
//                case "nextPage" -> nextPage(commandSender, strings);
//                case "prevPage" -> prevPage(commandSender, strings);
//                case "query" -> query(commandSender, strings);
//                case "attest" -> attest(commandSender, strings);
//                case "jurisdiction" -> jurisdiction(commandSender, strings);
//                case "ruleset" -> ruleset(commandSender, strings);
                default -> {
                    commandSender.sendMessage("Invalid Arguments");
                    yield null;
                }
            };

            return switch (listType) {
//                case "sign" -> List.of("add", "update", "remove");
                case "jurisdiction" -> db.getJurisdictionDao().queryForAll().stream().map(Jurisdiction::getCode).toList();
                default -> List.of();
            };
        } catch (SQLException e) {
            return List.of();
        }
    }
}
