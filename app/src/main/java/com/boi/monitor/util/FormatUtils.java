package com.boi.monitor.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.Timestamp;

/**
 * FormatUtils
 *
 * Stateless helpers for consistent number and date formatting
 * across adapters and ViewModels.
 */
public final class FormatUtils {

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private FormatUtils() {} // static only

    /** Format a double as Indian Rupee currency: ₹1,50,000.00 */
    public static String formatCurrency(double amount) {
        return getCurrencyFormat().format(amount);
    }

    /** Convert paise (long) to formatted currency string: ₹1,500.50 */
    public static String formatCurrencyFromPaise(long paise) {
        double rupees = paise / 100.0;
        return getCurrencyFormat().format(rupees);
    }

    /** Format a java.util.Date for display */
    public static String formatDate(Date date) {
        if (date == null) return "—";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private static NumberFormat getCurrencyFormat() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    /**
     * Truncate a string for display in list items.
     * e.g. long raw notification text → "BOI UPI - Your a/c no. XXX..."
     */
    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        if (maxLen < 3) return text.substring(0, maxLen);
        return text.substring(0, maxLen - 3) + "...";
    }

    /**
     * Convert a cheque status string to a human-readable label.
     */
    public static String chequeStatusLabel(String status) {
        if (status == null) return "UNKNOWN";
        switch (status.toUpperCase(Locale.ROOT)) {
            case "CLEARED":   return "✓ Cleared";
            case "RETURNED":  return "✗ Returned";
            case "PRESENTED": return "⏳ Presented";
            default:          return status;
        }
    }
    public static String formatDateTime(
            Timestamp timestamp) {

        if (timestamp == null)
            return "";

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "dd-MM-yyyy hh:mm a",
                        Locale.getDefault());

        return sdf.format(
                timestamp.toDate()
        );
    }
}
