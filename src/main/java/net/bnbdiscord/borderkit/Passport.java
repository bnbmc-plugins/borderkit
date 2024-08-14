package net.bnbdiscord.borderkit;

import net.bnbdiscord.borderkit.exceptions.NoBiodataPageException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class Passport implements ProxyObject {
    private final BookMeta meta;
    private final NamespacedKey passportKey;
    private final NamespacedKey holderGivenNameKey;
    private final NamespacedKey holderFamilyNameKey;
    private final NamespacedKey passportNumberKey;
    private final NamespacedKey dateOfBirthKey;
    private final NamespacedKey placeOfBirthKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey authorityKey;
    private final NamespacedKey signerKey;
    private final NamespacedKey nationalityKey;
    private final NamespacedKey versionKey;

    public Passport(Plugin plugin, ItemStack book) {
        this.meta = (BookMeta) book.getItemMeta();
        this.passportKey = new NamespacedKey(plugin, "passport");
        this.holderGivenNameKey = new NamespacedKey(plugin, "passportHolderGiven");
        this.holderFamilyNameKey = new NamespacedKey(plugin, "passportHolderFamily");
        this.passportNumberKey = new NamespacedKey(plugin, "passportNumber");
        this.expiryKey = new NamespacedKey(plugin, "expiry");
        this.authorityKey = new NamespacedKey(plugin, "authority");
        this.nationalityKey = new NamespacedKey(plugin, "nationality");
        this.dateOfBirthKey = new NamespacedKey(plugin, "dateOfBirth");
        this.placeOfBirthKey = new NamespacedKey(plugin, "placeOfBirth");
        this.versionKey = new NamespacedKey(plugin, "version");
        this.signerKey = new NamespacedKey(plugin, "signer");
    }

    public static void forPlayer(Plugin plugin, Player player, Consumer<Passport> callback) {
        List<ItemStack> passports = new ArrayList<>();

        for (var itemStack : player.getEnderChest()) {
            if (isValidPassport(plugin, itemStack)) {
                passports.add(itemStack);
            }
        }
        for (var itemStack : player.getInventory()) {
            if (isValidPassport(plugin, itemStack)) {
                passports.add(itemStack);
            }
        }

        if (passports.isEmpty()) {
            callback.accept(null);
        } else if (passports.size() > 1) {
            PassportSelector.selectPassport(player, plugin, callback);
        } else {
            callback.accept(new Passport(plugin, passports.get(0)));
        }
    }

    public static boolean isValidPassport(Plugin plugin, ItemStack itemStack) {
        var passportKey = new NamespacedKey(plugin, "passport");
        if (itemStack != null && itemStack.getItemMeta() instanceof BookMeta bookMeta && itemStack.getType() == Material.WRITTEN_BOOK) {
            if (Boolean.TRUE.equals(bookMeta.getPersistentDataContainer().get(passportKey, PersistentDataType.BOOLEAN))) {
                return true;
            }
        }
        return false;
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
        int biodataStartPage = state.biodataStartPage();

        var random = new Random();
        var passportNumber = ((char) (random.nextInt(26) + 'A')) + "" + ((char) (random.nextInt(26) + 'A')) + String.format("%06d", random.nextInt(1000000));

        var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").withLocale(Locale.ENGLISH);
        var mrzFormatter = DateTimeFormatter.ofPattern("yyMMdd").withLocale(Locale.ENGLISH);
        var inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var givenNames = state.fieldValue("givenNames");
        var familyNames = state.fieldValue("familyNames");
        var placeOfBirth = state.fieldValue("placeOfBirth");
        var placeOfBirth2 = state.fieldValue("placeOfBirth2");

        var issueDate = ZonedDateTime.now(ZoneOffset.UTC);
        var expiryDate = LocalDate.parse(state.fieldValue("expiry"), inputFormatter).atStartOfDay(ZoneOffset.UTC);
        var issueDateString = issueDate.format(formatter).toUpperCase();
        var expiryDateString = expiryDate.format(formatter).toUpperCase();

        var dateOfBirth = LocalDate.parse(state.fieldValue("dateOfBirth"), inputFormatter).atStartOfDay(ZoneOffset.UTC);
        var dateOfBirthString = dateOfBirth.format(formatter).toUpperCase();

        var issuingCountryCode = state.getJurisdiction().getCode();
        var issuingCountryName = state.getJurisdiction().getName().toUpperCase();

        var nationalityCountryCode = state.getNationality().getCode();
        var nationalityCountryName = state.getNationality().getName().toUpperCase();

        var mrz1 = "P<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
        var mrz2 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";

        mrz1 = replaceFromIndex(mrz1, 2, issuingCountryCode);
        mrz1 = replaceFromIndex(mrz1, 5, (romanise(familyNames) + "<<" + romanise(givenNames)));
        mrz2 = replaceFromIndex(mrz2, 0, passportNumber);
        mrz2 = appendCheckDigit(mrz2, 0, 8);
        mrz2 = replaceFromIndex(mrz2, 10, issuingCountryCode);
        mrz2 = replaceFromIndex(mrz2, 13, mrzFormatter.format(dateOfBirth));
        mrz2 = appendCheckDigit(mrz2, 13, 18);
        mrz2 = replaceFromIndex(mrz2, 21, mrzFormatter.format(expiryDate));
        mrz2 = appendCheckDigit(mrz2, 21, 26);
        mrz2 = replaceFromIndex(mrz2, 42, "0");
        mrz2 = replaceFromIndex(mrz2, 43, calculateCheckDigit(mrz2, 0, 9, calculateCheckDigit(mrz2, 13, 19, calculateCheckDigit(mrz2, 21, 42, 0))) + "");

        meta.page(biodataStartPage, Component.text()
                .append(Component.text(state.getJurisdiction().getBiometricPageString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getNamesString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(state.getJurisdiction().getFamilyString()).color(TextColor.color(200, 200, 200))).appendNewline()
                .append(Component.text(familyNames).hoverEvent(HoverEvent.showText(Component.text(familyNames)))).appendNewline()
                .append(Component.text(givenNames.isEmpty() ? "" : state.getJurisdiction().getGivenString()).color(TextColor.color(200, 200, 200))).appendNewline()
                .append(Component.text(givenNames).hoverEvent(HoverEvent.showText(Component.text(givenNames)))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getDocumentNumberString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(passportNumber).hoverEvent(HoverEvent.showText(Component.text(passportNumber)))).appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(0, 14))).appendNewline()
                .append(Component.text(mrz2.substring(0, 14))).appendNewline()
                .build());
        meta.page(biodataStartPage + 1, Component.text()
                .append(Component.text(state.getJurisdiction().getPlaceOfBirthString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(placeOfBirth).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth).appendNewline().append(Component.text(placeOfBirth2))))).appendNewline()
                .append(Component.text(placeOfBirth2).hoverEvent(HoverEvent.showText(Component.text(placeOfBirth).appendNewline().append(Component.text(placeOfBirth2))))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getDateOfBirthString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(dateOfBirthString).hoverEvent(HoverEvent.showText(Component.text(dateOfBirthString)))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getAuthorityCodeString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(issuingCountryName).hoverEvent(HoverEvent.showText(Component.text(issuingCountryName)))).appendNewline()
                .append(Component.text(issuingCountryCode).hoverEvent(HoverEvent.showText(Component.text(issuingCountryCode)))).appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(14, 28))).appendNewline()
                .append(Component.text(mrz2.substring(14, 28))).appendNewline()
                .build());
        meta.page(biodataStartPage + 2, Component.text()
                .append(Component.text(state.getJurisdiction().getDateOfIssueString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(issueDateString).hoverEvent(HoverEvent.showText(Component.text(issueDateString)))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getDateOfExpiryString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(expiryDateString).hoverEvent(HoverEvent.showText(Component.text(expiryDateString)))).appendNewline()
                .appendNewline()
                .append(Component.text(state.getJurisdiction().getNationalityString()).color(TextColor.color(100, 100, 255))).appendNewline()
                .append(Component.text(nationalityCountryName).hoverEvent(HoverEvent.showText(Component.text(nationalityCountryName)))).appendNewline()
                .append(Component.text(nationalityCountryCode).hoverEvent(HoverEvent.showText(Component.text(nationalityCountryCode)))).appendNewline()
                .appendNewline()
                .appendNewline()
                .append(Component.text(mrz1.substring(28, 44))).appendNewline()
                .append(Component.text(mrz2.substring(28, 44))).appendNewline()
                .build());

        meta.getPersistentDataContainer().set(passportKey, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(passportNumberKey, PersistentDataType.STRING, passportNumber);
        meta.getPersistentDataContainer().set(holderGivenNameKey, PersistentDataType.STRING, givenNames);
        meta.getPersistentDataContainer().set(holderFamilyNameKey, PersistentDataType.STRING, familyNames);
        meta.getPersistentDataContainer().set(authorityKey, PersistentDataType.STRING, issuingCountryCode);
        meta.getPersistentDataContainer().set(nationalityKey, PersistentDataType.STRING, nationalityCountryCode);
        meta.getPersistentDataContainer().set(placeOfBirthKey, PersistentDataType.STRING, (placeOfBirth + " " + placeOfBirth2).trim());
        meta.getPersistentDataContainer().set(expiryKey, PersistentDataType.LONG, expiryDate.toEpochSecond());
        meta.getPersistentDataContainer().set(dateOfBirthKey, PersistentDataType.LONG, dateOfBirth.toEpochSecond());
        meta.getPersistentDataContainer().set(signerKey, PersistentDataType.STRING, state.getPlayer().getName());
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, 0);
        meta.setTitle(meta.getTitle().replace("%g", givenNames).replace("%f", familyNames).replace("%i", issuingCountryCode));

        var newBook = new ItemStack(Material.WRITTEN_BOOK);
        newBook.setItemMeta(meta);
        return newBook;
    }

    public boolean isValidPassport() {
        return Boolean.TRUE.equals(meta.getPersistentDataContainer().get(passportKey, PersistentDataType.BOOLEAN));
    }

    public String getPassportNumber() {
        return meta.getPersistentDataContainer().get(passportNumberKey, PersistentDataType.STRING);
    }

    public String getGivenName() {
        return meta.getPersistentDataContainer().get(holderGivenNameKey, PersistentDataType.STRING);
    }

    public String getFamilyName() {
        return meta.getPersistentDataContainer().get(holderFamilyNameKey, PersistentDataType.STRING);
    }

    public String getIssuingAuthority() {
        return meta.getPersistentDataContainer().get(authorityKey, PersistentDataType.STRING);
    }

    public ZonedDateTime getExpiryDate() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(meta.getPersistentDataContainer().get(expiryKey, PersistentDataType.LONG)), ZoneOffset.UTC);
    }

    public ZonedDateTime getDateOfBirth() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(meta.getPersistentDataContainer().get(dateOfBirthKey, PersistentDataType.LONG)), ZoneOffset.UTC);
    }

    public String getPlaceOfBirth() {
        return meta.getPersistentDataContainer().get(placeOfBirthKey, PersistentDataType.STRING);
    }

    public String getNationality() {
        return meta.getPersistentDataContainer().get(nationalityKey, PersistentDataType.STRING);
    }

    public int getVersion() {
        return meta.getPersistentDataContainer().get(versionKey, PersistentDataType.INTEGER);
    }

    public boolean isExpired() {
        return getExpiryDate().isBefore(ZonedDateTime.now());
    }

    private static final Set<String> PROPERTIES = Set.of("givenName", "familyName", "issuingAuthority", "expiryDate", "dateOfBirth", "placeOfBirth", "isExpired", "nationality");

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "givenName" -> getGivenName();
            case "familyName" -> getFamilyName();
            case "issuingAuthority" -> getIssuingAuthority();
            case "expiryDate" ->  Date.from(getExpiryDate().toInstant());
            case "dateOfBirth" -> Date.from(getDateOfBirth().toInstant());
            case "placeOfBirth" -> getPlaceOfBirth();
            case "nationality" -> getNationality();
            case "isExpired" -> isExpired();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object getMemberKeys() {
        return PROPERTIES.toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return PROPERTIES.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        // Not allowed
        throw new UnsupportedOperationException();
    }
}
