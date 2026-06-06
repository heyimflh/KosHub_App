package com.koshub.psdku.utils;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Helper class for currency formatting.
 */
public class CurrencyHelper {
    
    /**
     * Formats a double amount to Rupiah (IDR) format.
     * Example: 1000000 -> Rp1.000.000
     * 
     * @param amount The amount to format
     * @return Formatted string
     */
    public static String formatRupiah(double amount) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        String formatted = formatRupiah.format(amount);
        if (formatted.contains("IDR")) {
            formatted = formatted.replace("IDR", "Rp");
        }
        if (formatted.endsWith(",00")) {
            formatted = formatted.substring(0, formatted.length() - 3);
        }
        return formatted;
    }

    /**
     * Formats a double amount to compact Rupiah format.
     * Example: 4102102 -> Rp4,1 jt
     * 
     * @param amount The amount to format
     * @return Formatted compact string
     */
    public static String formatRupiahCompact(double amount) {
        double abs = Math.abs(amount);
        if (abs >= 1_000_000_000) {
            return "Rp" + compactOneDecimal(amount / 1_000_000_000) + " M";
        } else if (abs >= 1_000_000) {
            return "Rp" + compactOneDecimal(amount / 1_000_000) + " jt";
        } else if (abs >= 1_000) {
            return "Rp" + compactOneDecimal(amount / 1_000) + " rb";
        } else {
            return formatRupiah(amount);
        }
    }

    private static String compactOneDecimal(double value) {
        String text = String.format(new Locale("in", "ID"), "%.1f", value);
        if (text.endsWith(",0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }
}
