package net.bnbdiscord.borderkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import javax.naming.NameNotFoundException;

public class PassportSigningState {
    private final Plugin plugin;
    private final Player player;

    public ItemStack getTemplate() {
        return template;
    }

    private final ItemStack template;

    private Field[] fields = new Field[] {
            new Field("Given Names", "givenNames"),
            new Field("Family Names", "familyNames"),
            new Field("Place of Birth", "placeOfBirth"),
            new Field("Expiry", "expiry")
    };

    private static class Field {
        public Field(String name, String editCommand) {
            this.name = name;
            this.editCommand = editCommand;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value.toUpperCase();
        }

        public Component textComponent() {
            return Component
                    .text(name).color(TextColor.color(0, 0, 255)).clickEvent(ClickEvent.runCommand("/passport signContinue %s".formatted(editCommand))).hoverEvent(HoverEvent.showText(Component.text("Click to edit")))
                    .appendNewline()
                    .append(Component.text(value.isBlank() ? "_____" : value))
                    .appendNewline();
        }

        private String value = "";

        public String getName() {
            return name;
        }

        private final String name;

        public String getEditCommand() {
            return editCommand;
        }

        private final String editCommand;
    }

    public PassportSigningState(Plugin plugin, Player player, ItemStack template) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
    }

    public void setFieldValue(String field, String value) {
        for (Field f : fields) {
            if (f.getEditCommand().equals(field)) {
                f.setValue(value);
            }
        }
    }

    public String fieldName(String field) {
        for (Field f : fields) {
            if (f.getEditCommand().equals(field)) {
                return f.getName();
            }
        }
        return "";
    }

    public String fieldValue(String field) {
        for (Field f : fields) {
            if (f.getEditCommand().equals(field)) {
                return f.getValue();
            }
        }
        return "";
    }

    public void openMenu() {
        var book = new ItemStack(Material.WRITTEN_BOOK);
        var meta = (BookMeta) book.getItemMeta();

        var heading = Component.text("ISSUE A NEW")
                .appendNewline()
                .append(Component.text("TAY PASSPORT").decorate(TextDecoration.BOLD))
                .appendNewline()
                .appendNewline();

        meta.addPages(
                heading.append(fields[0].textComponent())
                        .appendNewline()
                        .append(fields[1].textComponent())
                        .appendNewline()
                        .append(fields[2].textComponent())
                        .appendNewline()
                        .append(Component.text("PTO")),
                heading.append(fields[3].textComponent())
        );
        meta.setTitle("Passport Signing");
        meta.setAuthor("BorderKit");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        book.setItemMeta(meta);
        player.openBook(book);
    }
}
