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
        // Replace Rp with Rp for consistency or ensure it's correct
        String formatted = formatRupiah.format(amount);
        // Sometimes the formatter adds space or uses "IDR", let's make it standard "Rp"
        if (formatted.contains("IDR")) {
            formatted = formatted.replace("IDR", "Rp");
        }
        // Remove .00 if it's there
        if (formatted.endsWith(",00")) {
            formatted = formatted.substring(0, formatted.length() - 3);
        }
        return formatted;
    }
}
