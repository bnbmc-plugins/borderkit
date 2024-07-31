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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.function.Function;

public class PassportSigningState {
    private final Plugin plugin;
    private final Player player;
    private int page = 0;

    public ItemStack getTemplate() {
        return template;
    }

    private final ItemStack template;

    public String getJurisdiction() {
        return jurisdiction;
    }

    private final String jurisdiction;

    private Field[] fields = new Field[] {
            new Field("Given Names", "givenNames"),
            new Field("Family Names", "familyNames"),
            new Field("Place of Birth", "placeOfBirth"),
            new Field("Date of Birth", "dateOfBirth", (value) -> {
                var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                try {
                    LocalDate.parse(value, formatter);
                } catch (DateTimeParseException e) {
                    return "Enter a date in the form YYYY-MM-DD";
                }

                return "";
            }),
            new Field("Expiry", "expiry", (value) -> {
                var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                try {
                    var parsedDate = LocalDate.parse(value, formatter);
                    if (parsedDate.isBefore(LocalDate.now().plusMonths(1))) {
                        return "Minimum expiry is 1 month";
                    }
                } catch (DateTimeParseException e) {
                    return "Enter a date in the form YYYY-MM-DD";
                }

                return "";
            })
    };

    public void flip(int i) {
        page += i;
    }

    private static class Field {
        public Field(String name, String editCommand) {
            this(name, editCommand, (x) -> "");
        }

        public Field(String name, String editCommand, Function<String, String> validator) {
            this.name = name;
            this.editCommand = editCommand;
            this.validator = validator;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value.toUpperCase();
        }

        public Component textComponent() {
            var error = error();
            var hoverComponent = Component.text();
            var textColor = TextColor.color(0, 0, 255);
            if (!error.isEmpty()) {
                textColor = TextColor.color(255, 0, 0);
                hoverComponent = hoverComponent.append(Component.text(error).color(textColor)).appendNewline();
            }
            hoverComponent = hoverComponent.append(Component.text("Click to edit"));

            return Component.text()
                    .append(Component.text(name).color(textColor).clickEvent(ClickEvent.runCommand("/passport signContinue %s".formatted(editCommand))).hoverEvent(HoverEvent.showText(hoverComponent)))
                    .appendNewline()
                    .append(Component.text(value.isBlank() ? "_____" : value))
                    .appendNewline()
                    .build();
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
        private final Function<String, String> validator;

        public String error() {
            return validator.apply(getValue());
        }
    }

    public PassportSigningState(Plugin plugin, Player player, ItemStack template, String jurisdiction) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.jurisdiction = jurisdiction;

        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        setFieldValue("expiry", ZonedDateTime.now(ZoneOffset.UTC).plusYears(10).format(formatter));
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

        var heading = Component.text()
                .append(Component.text("ISSUE A NEW"))
                .appendNewline()
                .append(Component.text(jurisdiction + " PASSPORT").decorate(TextDecoration.BOLD))
                .appendNewline()
                .appendNewline();

        var errors = Arrays.stream(fields).map(Field::error).filter(error -> !error.isEmpty());

        meta.addPages(
                (switch (page) {
                    case 0 -> heading.append(Component.text("Fill out each field to issue a new %s passport.".formatted(jurisdiction))).appendNewline()
                            .appendNewline()
                            .append(Component.text("<!> WARNING:").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0))).append(Component.text(" Closing this book prematurely will cause you to lose all the information you have entered."));
                    case 1 -> heading.append(fields[0].textComponent())
                            .appendNewline()
                            .append(fields[1].textComponent())
                            .appendNewline()
                            .append(fields[2].textComponent());
                    case 2 -> heading.append(fields[3].textComponent())
                            .appendNewline()
                            .append(fields[4].textComponent());
                    case 3 -> Component.text()
                            .append(Component.text("SIGN PASSPORT").decorate(TextDecoration.BOLD)).appendNewline()
                            .append(Component.text("Review the information that you have entered before you sign this passport. Once it has been signed, it cannot be edited.")).appendNewline()
                            .appendNewline()
                            .append(
                                    errors.findAny().isEmpty() ?
                                            Component.text("[Sign Passport]").decorate(TextDecoration.BOLD).color(TextColor.color(0, 200, 0)).hoverEvent(HoverEvent.showText(Component.text("Complete data entry and issue this passport now"))).clickEvent(ClickEvent.runCommand("/passport signContinue")) :
                                            Component.text("[Sign Passport]").decorate(TextDecoration.BOLD).color(TextColor.color(150, 150, 150)).hoverEvent(HoverEvent.showText(Component.text("Correct errors before signing")))
                            );
                    default -> throw new IllegalStateException("Unexpected value: " + page);
                })
                        .appendNewline()
                        .appendNewline()
                        .append(page == 0 ? Component.text("[ <- ]").color(TextColor.color(150, 150, 150)) : Component.text("[ <- ]").color(TextColor.color(0, 0, 255)).clickEvent(ClickEvent.runCommand("/passport prevPage")).hoverEvent(HoverEvent.showText(Component.text("Previous Page"))))
                        .appendSpace()
                        .append(page == 3 ? Component.text("[ -> ]").color(TextColor.color(150, 150, 150)) : Component.text("[ -> ]").color(TextColor.color(0, 0, 255)).clickEvent(ClickEvent.runCommand("/passport nextPage")).hoverEvent(HoverEvent.showText(Component.text("Next Page"))))
                        .build()
        );

        meta.setTitle("Passport Signing");
        meta.setAuthor("BorderKit");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        book.setItemMeta(meta);
        player.openBook(book);
    }
}
