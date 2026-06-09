package com.boi.monitor.parser;

import android.util.Log;

import com.boi.monitor.model.NotificationLog;
import com.boi.monitor.model.ParsedNotification;
import com.boi.monitor.model.ParsedNotification.NotificationType;
import com.boi.monitor.util.Constants;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NotificationParser
 *
 * Stateless utility that converts a raw BOI notification string into a
 * strongly-typed {@link ParsedNotification}.
 */
public class NotificationParser {

    private static final String TAG = "NotificationParser";

    // ── Patterns ──────────────────────────────────────────────────────────────

    // Cheque Cleared
    private static final Pattern PATTERN_CHEQUE_CLEARED = Pattern.compile(
            "(?:Cheque|Chq)\\s*No\\.?\\s*(\\d+).*?Rs\\.?\\s*([\\d,]+(?:\\.\\d+)?).*?Debited\\(Clearing\\)" +
                    ".*?on\\s+([\\d\\-/]+).*?Avl\\s*Bal\\s*Rs\\.?\\s*([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Cheque Returned
    private static final Pattern PATTERN_CHEQUE_RETURNED = Pattern.compile(
            "(?:Cheque|Chq)\\.?\\s*No\\.?\\s*(\\d+).*?(?:Rs\\.?|amt)\\s*([\\d,]+(?:\\.\\d+)?).*?RETURNED",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Cheque Presented
    private static final Pattern PATTERN_CHEQUE_PRESENTED = Pattern.compile(
            "(?:Cheque|Chq)\\.?\\s*No\\.?\\s*(\\d+).*?(?:Rs\\.?|amt)\\s*([\\d,]+(?:\\.\\d+)?).*?presented\\s+in\\s+clearing",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // UPI Credit - Added support for both / and - in date, and ₹ symbol
    private static final Pattern PATTERN_UPI_CREDIT = Pattern.compile(
        "a/c\\s+no\\.\\s*([\\dX]+)\\s+is\\s+credited\\s+for\\s+(?:Rs\\.?|\u20B9)\\s*([\\d,]+(?:\\.\\d+)?)\\s+on\\s+([\\d\\-/]+)" +
        ".*?debited\\s+from\\s+a/c\\s+no\\.\\s*([\\dX]+).*?UPI\\s+Ref\\s+no\\.?\\s*([\\w]+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern PATTERN_FAVOURING = Pattern.compile(
        "Fvg\\.\\s*(.+?)\\s*,\\s*RETURNED",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern AMOUNT_CLEANUP = Pattern.compile("[,\\s]");

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean passesFilter(String text) {
        if (text == null || text.isEmpty()) return false;
        String normalized = text.toUpperCase(Locale.ROOT);
        return normalized.contains(Constants.FILTER_BOI_KEYWORD.toUpperCase(Locale.ROOT))
                && normalized.contains(Constants.FILTER_ACCOUNT_SUFFIX.toUpperCase(Locale.ROOT));
    }

    public static ParsedNotification parse(String rawText) {
        ParsedNotification result = new ParsedNotification(rawText);
        if (rawText == null || rawText.isEmpty()) return result;
        rawText = rawText.trim();

        if (tryParseChequeCleared(rawText, result))   return result;
        if (tryParseChequeReturned(rawText, result))  return result;
        if (tryParseChequePresented(rawText, result)) return result;
        if (tryParseUpiCredit(rawText, result))       return result;

        result.setType(NotificationType.UNRECOGNIZED);
        return result;
    }

    // ── Private Parsers ───────────────────────────────────────────────────────

    private static boolean tryParseChequeCleared(String text, ParsedNotification out) {
        Matcher m = PATTERN_CHEQUE_CLEARED.matcher(text);
        if (!m.find()) return false;
        try {
            out.setType(NotificationType.CHEQUE_CLEARED);
            out.setChequeNumber(m.group(1).trim());
            out.setAmount(parseAmount(m.group(2)));
            out.setTransactionDate(m.group(3).trim());
            out.setAvailableBalance(parseAmount(m.group(4)));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Parse failed for type: CHEQUE_CLEARED", e);
            FirebaseCrashlytics.getInstance().recordException(new RuntimeException("Parse failed for type: CHEQUE_CLEARED"));
            return false;
        }
    }

    private static boolean tryParseChequeReturned(String text, ParsedNotification out) {
        Matcher m = PATTERN_CHEQUE_RETURNED.matcher(text);
        if (!m.find()) return false;
        try {
            out.setType(NotificationType.CHEQUE_RETURNED);
            out.setChequeNumber(m.group(1).trim());
            out.setAmount(parseAmount(m.group(2)));
            Matcher favM = PATTERN_FAVOURING.matcher(text);
            if (favM.find()) out.setFavouringParty(favM.group(1).trim());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Parse failed for type: CHEQUE_RETURNED", e);
            FirebaseCrashlytics.getInstance().recordException(new RuntimeException("Parse failed for type: CHEQUE_RETURNED"));
            return false;
        }
    }

    private static boolean tryParseChequePresented(String text, ParsedNotification out) {
        Matcher m = PATTERN_CHEQUE_PRESENTED.matcher(text);
        if (!m.find()) return false;
        try {
            out.setType(NotificationType.CHEQUE_PRESENTED);
            out.setChequeNumber(m.group(1).trim());
            out.setAmount(parseAmount(m.group(2)));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Parse failed for type: CHEQUE_PRESENTED", e);
            FirebaseCrashlytics.getInstance().recordException(new RuntimeException("Parse failed for type: CHEQUE_PRESENTED"));
            return false;
        }
    }

    private static boolean tryParseUpiCredit(String text, ParsedNotification out) {
        Matcher m = PATTERN_UPI_CREDIT.matcher(text);
        if (!m.find()) return false;
        try {
            out.setType(NotificationType.UPI_CREDIT);
            String fullAccount = m.group(1).trim();
            out.setAccountSuffix(fullAccount.length() >= 4 ? fullAccount.substring(fullAccount.length() - 4) : fullAccount);
            out.setAmount(parseAmount(m.group(2)));
            out.setTransactionDate(m.group(3).trim());
            out.setDebitedAccount(m.group(4).trim());
            out.setReferenceNumber(m.group(5).trim());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Parse failed for type: UPI_CREDIT", e);
            FirebaseCrashlytics.getInstance().recordException(new RuntimeException("Parse failed for type: UPI_CREDIT"));
            return false;
        }
    }

    private static long parseAmount(String raw) {
        if (raw == null) return 0L;
        String cleaned = AMOUNT_CLEANUP.matcher(raw.trim()).replaceAll("");
        try { return Math.round(Double.parseDouble(cleaned) * 100.0); } catch (Exception e) { return 0L; }
    }

    public static String toLogType(ParsedNotification.NotificationType type) {
        switch (type) {
            case CHEQUE_CLEARED:   return NotificationLog.TYPE_CHEQUE_CLEARED;
            case CHEQUE_RETURNED:  return NotificationLog.TYPE_CHEQUE_RETURNED;
            case CHEQUE_PRESENTED: return NotificationLog.TYPE_CHEQUE_PRESENTED;
            case UPI_CREDIT:       return NotificationLog.TYPE_UPI_CREDIT;
            default:               return NotificationLog.TYPE_UNRECOGNIZED;
        }
    }
}
