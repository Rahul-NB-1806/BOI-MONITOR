package com.boi.monitor.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {

    private static final NumberFormat CURRENCY_FMT =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private FormatUtils() {}

    public static String formatCurrency(double amount) {
        return getCurrencyFormat().format(amount);
    }

    public static String formatCurrencyFromPaise(long paise) {
        double rupees = paise / 100.0;
        return getCurrencyFormat().format(rupees);
    }

    public static String formatDate(Date date) {
        if (date == null) return "\u2014";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    private static NumberFormat getCurrencyFormat() {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        if (maxLen < 3) return text.substring(0, maxLen);
        return text.substring(0, maxLen - 3) + "...";
    }

    public static String chequeStatusLabel(String status) {
        if (status == null) return "UNKNOWN";
        switch (status.toUpperCase(Locale.ROOT)) {
            case "CLEARED":   return "\u2713 Cleared";
            case "RETURNED":  return "\u2717 Returned";
            case "PRESENTED": return "\u23F3 Presented";
            default:          return status;
        }
    }

    public static String formatDateTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFmt.parse(timestamp);
            if (date == null) return timestamp;
            return sdf.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }
}
