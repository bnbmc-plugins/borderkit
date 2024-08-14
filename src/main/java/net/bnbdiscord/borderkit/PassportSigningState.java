package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.database.DatabaseManager;
import net.bnbdiscord.borderkit.database.Jurisdiction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class PassportSigningState {
    private final Plugin plugin;
    private final Player player;
    private int page = 0;

    public ItemStack getTemplate() {
        return template;
    }

    private final ItemStack template;

    public Jurisdiction getJurisdiction() {
        return jurisdiction;
    }

    private DatabaseManager db;
    private final Jurisdiction jurisdiction;

    private final Field[] fields = new Field[] {
            new Field("Family Names", "familyNames", (value) -> value.isEmpty() ? "Enter a name" : ""),
            new Field("Given Names", "givenNames"),
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
            }),
            new Field("Date of Birth", "dateOfBirth", (value) -> {
                var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                try {
                    var parsedDate = LocalDate.parse(value, formatter);
                    if (parsedDate.isAfter(LocalDate.now())) {
                        return "Date of birth must be before today";
                    }
                } catch (DateTimeParseException e) {
                    return "Enter a date in the form YYYY-MM-DD";
                }

                return "";
            }),
            new Field("Place of Birth Line 1", "placeOfBirth", (value) -> value.isEmpty() ? "Enter the place of birth" : ""),
            new Field("Place of Birth Line 2", "placeOfBirth2"),
            new Field("Nationality", "nationality", (value) -> {
                try {
                    if (db.getJurisdictionDao().queryForEq("code", value).isEmpty()) {
                        return "Enter a valid jurisdiction code";
                    }

                    return "";
                } catch (SQLException e) {
                    return "SQL Error";
                }
            }),
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

    public PassportSigningState(Plugin plugin, Player player, ItemStack template, DatabaseManager db, Jurisdiction jurisdiction) {
        this.plugin = plugin;
        this.player = player;
        this.template = template;
        this.db = db;
        this.jurisdiction = jurisdiction;

        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        setFieldValue("expiry", ZonedDateTime.now(ZoneOffset.UTC).plusYears(10).format(formatter));
        setFieldValue("nationality", jurisdiction.getCode());
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

    public Jurisdiction getNationality() {
        var nationality = fieldValue("nationality");
        try {
            return db.getJurisdictionDao().queryForEq("code", nationality).get(0);
        } catch (SQLException e) {
            return null;
        }
    }

    public void openMenu() {
        var book = new ItemStack(Material.WRITTEN_BOOK);
        var meta = (BookMeta) book.getItemMeta();

        var heading = Component.text()
                .append(Component.text("ISSUE A NEW"))
                .appendNewline()
                .append(Component.text(jurisdiction.getCode() + " PASSPORT").decorate(TextDecoration.BOLD))
                .appendNewline()
                .appendNewline();

        var errors = Arrays.stream(fields).map(Field::error).filter(error -> !error.isEmpty());

        int visaPages = 0;
        for (var i = 1; i <= ((BookMeta) template.getItemMeta()).getPageCount(); i++) {
            if (isVisaPage(i)) visaPages++;
        }

        meta.addPages(
                (switch (page) {
                    case 0 -> heading.append(Component.text("Fill out each field to issue a new %s passport.".formatted(jurisdiction.getCode()))).appendNewline()
                            .appendNewline()
                            .append(Component.text("<!> WARNING:").decorate(TextDecoration.BOLD).color(TextColor.color(255, 0, 0))).append(Component.text(" Closing this book prematurely will cause you to lose all the information you have entered."));
                    case 1 -> heading.append(Component.text()
                            .append(Component.text("VISA PAGES").color(TextColor.color(0, 0, 255)))).appendNewline()
                            .append(Component.text(visaPages));
                    case 2 -> heading.append(fields[0].textComponent())
                            .appendNewline()
                            .append(fields[1].textComponent())
                            .appendNewline()
                            .append(fields[2].textComponent());
                    case 3 -> heading.append(fields[3].textComponent())
                            .appendNewline()
                            .append(fields[4].textComponent())
                            .appendNewline()
                            .append(fields[5].textComponent());
                    case 4 -> heading.append(fields[6].textComponent());
                    case 5 -> Component.text()
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
                        .append(page == 5 ? Component.text("[ -> ]").color(TextColor.color(150, 150, 150)) : Component.text("[ -> ]").color(TextColor.color(0, 0, 255)).clickEvent(ClickEvent.runCommand("/passport nextPage")).hoverEvent(HoverEvent.showText(Component.text("Next Page"))))
                        .build()
        );

        meta.setTitle("Passport Signing");
        meta.setAuthor("BorderKit");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        book.setItemMeta(meta);
        player.openBook(book);
    }

    public Player getPlayer() {
        return player;
    }

    public int biodataStartPage() {
        var meta = (BookMeta) template.getItemMeta();

        List<Integer> biodataPages = new ArrayList<>();
        for (int i = 1; i <= meta.pages().size(); i++) {
            if (((TextComponent)meta.page(i)).content().equals("BIODATA")) {
                biodataPages.add(i);
            }
        }

        for (int i = 0; i < biodataPages.size() - 2; i++) {
            if (biodataPages.get(i + 1) - biodataPages.get(i) == 1 && biodataPages.get(i + 2) - biodataPages.get(i + 1) == 1) {
                return biodataPages.get(i);
            }
        }
        return -1;
    }

    public boolean isVisaPage(int pageNumber) {
        var meta = (BookMeta) template.getItemMeta();

        if (((TextComponent)meta.page(pageNumber)).content().equals("VISAS")) {
            return true;
        }
        return false;
    }
}
