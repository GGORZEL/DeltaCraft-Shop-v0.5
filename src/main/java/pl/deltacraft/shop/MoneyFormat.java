package pl.deltacraft.shop;

import java.util.Locale;

public final class MoneyFormat {
    private MoneyFormat() {}

    public static String format(long cents) {
        double dollars = cents / 100.0;
        double abs = Math.abs(dollars);

        if (abs >= 1_000_000_000) {
            return shortValue(dollars / 1_000_000_000) + " mld";
        }
        if (abs >= 1_000_000) {
            return shortValue(dollars / 1_000_000) + " mln";
        }
        if (abs >= 1_000) {
            return shortValue(dollars / 1_000) + " tys.";
        }
        return String.format(Locale.ROOT, "$%.2f", dollars);
    }

    private static String shortValue(double value) {
        String number = String.format(
            Locale.ROOT, "%.2f", value
        );
        number = number.replaceAll("0+$", "")
                       .replaceAll("\\.$", "");
        return "$" + number.replace('.', ',');
    }
}
