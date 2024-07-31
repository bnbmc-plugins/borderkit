package net.bnbdiscord.borderkit.commands;

import de.rapha149.signgui.SignGUI;
import net.bnbdiscord.borderkit.Passport;
import net.bnbdiscord.borderkit.PassportSigningState;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

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
            default -> {
                commandSender.sendMessage("Invalid Arguments");
                yield true;
            }
        };
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

        var item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.WRITTEN_BOOK) {
            commandSender.sendMessage("Hold the template that you want to sign and try again.");
            return false;
        }
        var state = new PassportSigningState(plugin, player, item);
        signingStates.put(player.getUniqueId(), state);
        state.openMenu();

        return true;
    }
}
