package net.bnbdiscord.borderkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;

public class Passport {
    private final BookMeta meta;
    private final NamespacedKey holderGivenNameKey;
    private final NamespacedKey holderFamilyNameKey;
    private final NamespacedKey passportNumberKey;

    public Passport(Plugin plugin, ItemStack book) {
        this.meta = (BookMeta) book.getItemMeta();
        this.holderGivenNameKey = new NamespacedKey(plugin, "passportHolderGiven");
        this.holderFamilyNameKey = new NamespacedKey(plugin, "passportHolderFamily");
        this.passportNumberKey = new NamespacedKey(plugin, "passportNumber");
    }

    private static String replaceFromIndex(String str1, int index, String str2) {
        if (index < 0 || index > str1.length()){
            throw new StringIndexOutOfBoundsException("Index is out of bounds");
        }

        var before = str1.substring(0, index);
        var after = str1.substring(index);
        return before + str2 + after.substring((Math.min(after.length(), str2.length())));
    }

    private static final String CHECK_DIGIT_MAP = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Integer[] WEIGHTS = new Integer[]{7, 3, 1};

    public static int calculateCheckDigit(String str, int from, int to, int start) {
        int checkSum = start;
        for (int i = from; i <= to; i++) {
            char c = str.charAt(i);
            if (c == '<') continue;
            int weight = WEIGHTS[(i + 44) % WEIGHTS.length];
            int checkDigitValue = CHECK_DIGIT_MAP.indexOf(c);
            checkSum += weight * checkDigitValue;
        }
        return checkSum % 10;
    }

    private static String appendCheckDigit(String str, int from, int to) {
        return replaceFromIndex(str, to + 1, calculateCheckDigit(str, from, to, 0) + "");
    }

    private static String romanise(String str) {
        return str.replace(' ', '<')
                .replace('-', '<')
                .replace("'", "")
                .replace("Å", "AA")
                .replace("Æ", "AE")
                .replace("Ä", "AE")
                .replace("Ö", "OE")
                .replace("Ø", "OE")
                .replace("Ü", "UE")
                .replace("ß", "SS")
                .replace("þ", "TH")
                .replace("Œ", "OE")
                .replaceAll("[^A-Za-z0-9<]", "");
    }

    public ItemStack signPassportTemplate(PassportSigningState state) {
        var random = new Random();
        var passportNumber = ((char) (random.nextInt(26) + 'A')) + "" + ((char) (random.nextInt(26) + 'A')) + String.format("%06d", random.nextInt(1000000));

        var issueDate = ZonedDateTime.now(ZoneOffset.UTC);
        var expiryDate = issueDate.plusYears(10);
        var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(Locale.ENGLISH);
        var mrzFormatter = DateTimeFormatter.ofPattern("yyMMdd").withLocale(Locale.ENGLISH);
        var issueDateString = issueDate.format(formatter);

        var givenNames = state.fieldValue("givenNames");
        var familyNames = state.fieldValue("familyNames");
        var placeOfBirth = state.fieldValue("placeOfBirth");

        var issuingCountryCode = "TAY";

        var mrz1 = "P<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
        var mrz2 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";

        mrz1 = replaceFromIndex(mrz1, 2, issuingCountryCode);
        mrz1 = replaceFromIndex(mrz1, 5, (romanise(familyNames) + "<<" + romanise(givenNames)));
        mrz2 = replaceFromIndex(mrz2, 0, passportNumber);
        mrz2 = appendCheckDigit(mrz2, 0, 8);
        mrz2 = replaceFromIndex(mrz2, 10, issuingCountryCode);
        mrz2 = replaceFromIndex(mrz2, 13, mrzFormatter.format(issueDate)); // TODO: Date of birth
        mrz2 = appendCheckDigit(mrz2, 13, 18);
        mrz2 = replaceFromIndex(mrz2, 21, mrzFormatter.format(expiryDate));
        mrz2 = appendCheckDigit(mrz2, 21, 26);
        mrz2 = replaceFromIndex(mrz2, 42, "0");
        mrz2 = replaceFromIndex(mrz2, 43, calculateCheckDigit(mrz2, 0, 9, calculateCheckDigit(mrz2, 13, 19, calculateCheckDigit(mrz2, 21, 42, 0))) + "");

        meta.page(2, Component.text()
                .append(Component.text("BIOMETRIC PAGE").color(TextColor.color(100, 100, 255))).appendNewline()
                .appendNewline()
                .append(Component.text("NAMES").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text("FAMILY").color(TextColor.color(200, 200, 200))).appendNewline()
                .append(Component.text(familyNames).hoverEvent(HoverEvent.showText(Component.text(familyNames)))).appendNewline()
                .append(Component.text("GIVEN").color(TextColor.color(200, 200, 200))).appendNewline()
                .append(Component.text(givenNames).hoverEvent(HoverEvent.showText(Component.text(givenNames)))).appendNewline()
                .appendNewline()
                .append(Component.text("DOCUMENT NUMBER").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(passportNumber).hoverEvent(HoverEvent.showText(Component.text(passportNumber)))).appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(0, 14))).appendNewline()
                .append(Component.text(mrz2.substring(0, 14))).appendNewline()
                .build());
        meta.page(3, Component.text()
                .append(Component.text("PLACE OF BIRTH").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(placeOfBirth).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth)))).appendNewline()
                .appendNewline()
                .append(Component.text("DATE OF ISSUE").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(issueDateString).hoverEvent(HoverEvent.showText(Component.text(issueDateString)))).appendNewline()
                .appendNewline()
                .append(Component.text("PLACE OF BIRTH").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(placeOfBirth).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth)))).appendNewline()
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(14, 28))).appendNewline()
                .append(Component.text(mrz2.substring(14, 28))).appendNewline()
                .build());
        meta.page(4, Component.text()
                .append(Component.text("PLACE OF BIRTH").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(placeOfBirth).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth)))).appendNewline()
                .appendNewline()
                .append(Component.text("DATE OF ISSUE").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(issueDateString).hoverEvent(HoverEvent.showText(Component.text(issueDateString)))).appendNewline()
                .appendNewline()
                .append(Component.text("PLACE OF BIRTH").color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(placeOfBirth).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth)))).appendNewline()
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(28, 44))).appendNewline()
                .append(Component.text(mrz2.substring(28, 44))).appendNewline()
                .build());

        meta.getPersistentDataContainer().set(passportNumberKey, PersistentDataType.STRING, passportNumber);
        meta.getPersistentDataContainer().set(holderGivenNameKey, PersistentDataType.STRING, givenNames);
        meta.getPersistentDataContainer().set(holderFamilyNameKey, PersistentDataType.STRING, familyNames);

        var newBook = new ItemStack(Material.WRITTEN_BOOK);
        newBook.setItemMeta(meta);
        return newBook;
    }

    public String holder() {
        return meta.getPersistentDataContainer().get(holderGivenNameKey, PersistentDataType.STRING);
    }
}
