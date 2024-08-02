package net.bnbdiscord.borderkit.commands;

import de.rapha149.signgui.SignGUI;
import net.bnbdiscord.borderkit.Passport;
import net.bnbdiscord.borderkit.PassportSigningState;
import net.bnbdiscord.borderkit.exceptions.PassportNotFoundException;
import net.bnbdiscord.borderkit.exceptions.PassportSearchException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.jetbrains.annotations.NotNull;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PassportCommand implements CommandExecutor {
    private final Plugin plugin;
    private final NamespacedKey key;

    public Dictionary<UUID, PassportSigningState> signingStates = new Hashtable<>();

    public PassportCommand(Plugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "key");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        return switch (strings[0]) {
            case "sign" -> signPassport(commandSender, strings);
            case "signContinue" -> signContinue(commandSender, strings);
            case "nextPage" -> nextPage(commandSender, strings);
            case "prevPage" -> prevPage(commandSender, strings);
            case "query" -> query(commandSender, strings);
            case "attest" -> attest(commandSender, strings);
            default -> {
                commandSender.sendMessage("Invalid Arguments");
                yield true;
            }
        };
    }

    private boolean attest(CommandSender commandSender, String[] strings) {
        if (strings.length != 2) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        var playerName = strings[1];
        var player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            commandSender.sendMessage("Invalid Player");
            return false;
        }

        try {
            var passport = Passport.forPlayer(plugin, player);

            try (var context = Context.newBuilder("js")
                    .allowHostAccess(HostAccess.newBuilder()
                            .allowArrayAccess(true)
                            .build())
                    .build()) {
                context.eval("js", "function handler(passport) { return JSON.stringify(passport); }");
                var result = context.getBindings("js").getMember("handler").execute(passport);
                if (result.isString()) {
                    var resultStr = result.toString();
                    if (resultStr.isEmpty()) {
                        // TODO: Approve attestation
                        return true;
                    } else {
                        commandSender.sendMessage(Component.text(resultStr).color(TextColor.color(255, 0, 0)));
                        // TODO: Fail attestation
                        return false;
                    }
                } else {
                    commandSender.sendMessage("Return value was not a string");
                    player.sendMessage(Component.text("BorderKit: There was a problem verifying your passport. Please visit a Border Force officer for manual processing.").color(TextColor.color(255, 0, 0)));

                    // TODO: Fail attestation
                    return false;
                }
            }
        } catch (PassportSearchException e) {
            if (e instanceof PassportNotFoundException) {
                commandSender.sendMessage("The player is not holding a valid ePassport+");
                player.sendMessage(Component.text("BorderKit: Please ensure your ePassport+ is in your inventory and try again").color(TextColor.color(255, 0, 0)));
            } else {
                commandSender.sendMessage("The player is holding more than one valid ePassport+");
                player.sendMessage(Component.text("BorderKit: Please ensure your inventory contains only one ePassport+ and try again").color(TextColor.color(255, 0, 0)));
            }

            // TODO: Fail attestation
            return false;
        }
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

    private boolean signPassport(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command can only be run by a player");
            return false;
        }

        if (strings.length != 2) {
            commandSender.sendMessage("Invalid Arguments");
            return false;
        }

        var jurisdiction = strings[1].toUpperCase();
        if (jurisdiction.length() != 3) {
            commandSender.sendMessage("Invalid issuing authority");
            return false;
        }
        if (!jurisdiction.equals("XXX")) {
            commandSender.sendMessage("You do not have permission to issue passports for %s. If you believe this is incorrect, please contact a server administrator.".formatted(jurisdiction));
            return false;
        }

        var item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITTEN_BOOK) {
            commandSender.sendMessage("Hold the template that you want to sign and try again.");
            return false;
        }

        var state = new PassportSigningState(plugin, player, item, jurisdiction);
        signingStates.put(player.getUniqueId(), state);
        state.openMenu();

        return true;
    }
}
