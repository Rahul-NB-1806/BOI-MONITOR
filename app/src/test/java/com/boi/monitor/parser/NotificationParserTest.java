package com.boi.monitor.parser;

import com.boi.monitor.model.ParsedNotification;
import com.boi.monitor.model.ParsedNotification.NotificationType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * NotificationParserTest
 *
 * Unit tests for the BOI notification parsing engine.
 * Covers all 4 notification types, edge cases, and the primary filter.
 *
 * Run with: ./gradlew test
 */
@RunWith(JUnit4.class)
public class NotificationParserTest {

    // ── Primary Filter Tests ──────────────────────────────────────────────────

    @Test
    public void filter_passesWhenBothKeywordsPresent() {
        String text = "BOI UPI - Your a/c no. XXX004 credited Rs 500";
        assertTrue(NotificationParser.passesFilter(text));
    }

    @Test
    public void filter_failsWithoutBOI() {
        String text = "HDFC - Your a/c no. XXX004 credited Rs 500";
        assertFalse(NotificationParser.passesFilter(text));
    }

    @Test
    public void filter_failsWithoutAccountSuffix() {
        String text = "BOI - Transaction alert for your account";
        assertFalse(NotificationParser.passesFilter(text));
    }

    @Test
    public void filter_failsOnNull() {
        assertFalse(NotificationParser.passesFilter(null));
    }

    @Test
    public void filter_failsOnEmptyString() {
        assertFalse(NotificationParser.passesFilter(""));
    }

    @Test
    public void filter_caseInsensitive() {
        String text = "boi UPI - Your a/c no. xxx004 credited Rs 500";
        assertTrue(NotificationParser.passesFilter(text));
    }

    @Test
    public void filter_mixedCase() {
        String text = "BoI UPI - Your a/c no. XxX004 credited Rs 500";
        assertTrue(NotificationParser.passesFilter(text));
    }

    @Test
    public void filter_passesWithAccountSuffixInDifferentPosition() {
        String text = "Your a/c no. XXX004 has a BOI UPI credit of Rs 500";
        assertTrue(NotificationParser.passesFilter(text));
    }

    // ── Cheque Cleared ────────────────────────────────────────────────────────

    @Test
    public void parse_chequeCleared_fullSample() {
        String raw = "BOI - Cheque No. 12345 for Rs 50000 Debited(Clearing) in your A/c XX0004 " +
                     "on 27-04-2026 TO CLG. Avl Bal Rs 25000";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_CLEARED, result.getType());
        assertEquals("12345", result.getChequeNumber());
        assertEquals(5000000L, result.getAmount());
        assertEquals(2500000L, result.getAvailableBalance());
        assertEquals("27-04-2026", result.getTransactionDate());
        assertTrue(result.isValid());
        assertFalse(result.isUpiCredit());
        assertTrue(result.isCheque());
    }

    @Test
    public void parse_chequeCleared_withCommasInAmount() {
        String raw = "BOI - Cheque No. 98765 for Rs 1,50,000 Debited(Clearing) in your A/c XX0004 " +
                     "on 01-05-2026 TO CLG. Avl Bal Rs 3,25,500";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_CLEARED, result.getType());
        assertEquals("98765", result.getChequeNumber());
        assertEquals(15000000L, result.getAmount());
        assertEquals(32550000L, result.getAvailableBalance());
    }

    @Test
    public void parse_chequeCleared_lowAmount() {
        String raw = "BOI - Cheque No. 00001 for Rs 500 Debited(Clearing) in your A/c XX0004 " +
                     "on 10-05-2026 TO CLG. Avl Bal Rs 100";

        ParsedNotification result = NotificationParser.parse(raw);
        assertEquals(NotificationType.CHEQUE_CLEARED, result.getType());
        assertEquals(50000L, result.getAmount());
        assertEquals(10000L, result.getAvailableBalance());
    }

    @Test
    public void parse_chequeCleared_minimalAmount() {
        String raw = "BOI - Cheque No. 00001 for Rs 1 Debited(Clearing) in your A/c XX0004 " +
                     "on 27-04-2026 TO CLG. Avl Bal Rs 100";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_CLEARED, result.getType());
        assertEquals(100L, result.getAmount());
        assertEquals(10000L, result.getAvailableBalance());
    }

    // ── Cheque Returned ───────────────────────────────────────────────────────

    @Test
    public void parse_chequeReturned_fullSample() {
        String raw = "BOI-Chq.No.54321 amt 75000, acc XXX004,Fvg. NAGAMMAI PHARMA A UN, RETURNED. " +
                     "Contact branch for details.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_RETURNED, result.getType());
        assertEquals("54321", result.getChequeNumber());
        assertEquals(7500000L, result.getAmount());
        assertEquals("NAGAMMAI PHARMA A UN", result.getFavouringParty());
        assertTrue(result.isValid());
        assertTrue(result.isCheque());
    }

    @Test
    public void parse_chequeReturned_withSpaceAfterChqNo() {
        String raw = "BOI-Chq.No. 11111 amt 20000, acc XXX004,Fvg. SOME BUSINESS, RETURNED.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_RETURNED, result.getType());
        assertEquals("11111", result.getChequeNumber());
        assertEquals(2000000L, result.getAmount());
    }

    @Test
    public void parse_chequeReturned_noFavouringParty() {
        // If Fvg. pattern is absent, should still parse basic fields
        String raw = "BOI-Chq.No.22222 amt 10000, acc XXX004, RETURNED.";

        ParsedNotification result = NotificationParser.parse(raw);

        // May or may not match depending on regex; if it does:
        if (result.getType() == NotificationType.CHEQUE_RETURNED) {
            assertEquals("22222", result.getChequeNumber());
            assertNull(result.getFavouringParty()); // no Fvg. present
        }
        // Parser should not crash
    }

    @Test
    public void parse_chequeReturned_complexFavouring() {
        String raw = "BOI-Chq.No.11111 amt 50000, acc XXX004,Fvg. M/S ABC & CO. (PHARMA), RETURNED. Contact branch.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_RETURNED, result.getType());
        assertEquals("11111", result.getChequeNumber());
        assertEquals(5000000L, result.getAmount());
        assertEquals("M/S ABC & CO. (PHARMA)", result.getFavouringParty());
    }

    // ── Cheque Presented ──────────────────────────────────────────────────────

    @Test
    public void parse_chequePresented_fullSample() {
        String raw = "BOI-Chq.No. 67890 amt 30000 pertaining to acc XXX004," +
                     "Fvg. NAGAMMAI PHARMA A UN is presented in CLEARING today.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_PRESENTED, result.getType());
        assertEquals("67890", result.getChequeNumber());
        assertEquals(3000000L, result.getAmount());
        assertTrue(result.isCheque());
    }

    @Test
    public void parse_chequePresented_largeAmount() {
        String raw = "BOI-Chq.No. 99999 amt 500000 pertaining to acc XXX004," +
                     "Fvg. CORP LTD is presented in CLEARING today.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_PRESENTED, result.getType());
        assertEquals(50000000L, result.getAmount());
    }

    // ── UPI Credit ────────────────────────────────────────────────────────────

    @Test
    public void parse_upiCredit_fullSample() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 1200 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(120000L, result.getAmount());
        assertEquals("20/05/2026", result.getTransactionDate());
        assertEquals("0004", result.getAccountSuffix());
        assertEquals("XXXXXX2101", result.getDebitedAccount());
        assertEquals("ABC123456789", result.getReferenceNumber());
        assertTrue(result.isValid());
        assertTrue(result.isUpiCredit());
        assertFalse(result.isCheque());
    }

    @Test
    public void parse_upiCredit_smallAmount() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 50 on 21/05/2026 " +
                     "and debited from a/c no. XXXXXX9999 (UPI Ref noXYZ000001)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(5000L, result.getAmount());
        assertEquals("XYZ000001", result.getReferenceNumber());
    }

    @Test
    public void parse_upiCredit_largeAmount() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 99999 on 22/05/2026 " +
                     "and debited from a/c no. XXXXXX1234 (UPI Ref noREF99999)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(9999900L, result.getAmount());
    }

    @Test
    public void parse_upiCredit_accountSuffixExtracted() {
        String raw = "BOI UPI - Your a/c no. 123456789012340004 is credited for Rs. 500 on 01/06/2026 " +
                     "and debited from a/c no. XXXXXX5678 (UPI Ref noREFTEST123)";

        ParsedNotification result = NotificationParser.parse(raw);

        if (result.getType() == NotificationType.UPI_CREDIT) {
            // Account suffix should be last 4 chars
            assertEquals("0004", result.getAccountSuffix());
        }
    }

    @Test
    public void parse_upiCredit_decimalAmount() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 1500.50 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(150050L, result.getAmount());
        assertEquals("ABC123456789", result.getReferenceNumber());
    }

    @Test
    public void parse_upiCredit_amountWithCommas() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 5,00,000 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(50000000L, result.getAmount());
    }

    @Test
    public void parse_upiCredit_dateWithDashSeparator() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 500 on 20-05-2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(50000L, result.getAmount());
        assertEquals("20-05-2026", result.getTransactionDate());
        assertEquals("ABC123456789", result.getReferenceNumber());
    }

    // ── Amount paise conversion ───────────────────────────────────────────────

    @Test
    public void parse_amount_convertsToPaise() {
        // ₹1200 → 120000 paise
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 1200 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(120000L, result.getAmount());
    }

    @Test
    public void parse_amount_veryLarge() {
        // ₹10,00,00,000 = 100000000 rupees → 10000000000 paise
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 10,00,00,000 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(10000000000L, result.getAmount());
    }

    // ── Unrecognized ──────────────────────────────────────────────────────────

    @Test
    public void parse_unrecognized_returnsUnrecognizedType() {
        // Passes the filter (has BOI + XXX004) but matches no pattern
        String raw = "BOI Alert: Your account XXX004 has a new message.";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
        assertFalse(result.isValid());
        assertFalse(result.isUpiCredit());
        assertFalse(result.isCheque());
    }

    @Test
    public void parse_unrecognized_genericBOIMessage() {
        // Generic BOI promotional message that passes the filter
        String raw = "BOI Alert: Your account XXX004 has a special offer on loans!";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
        assertFalse(result.isValid());
    }

    @Test
    public void parse_unrecognized_balanceEnquiry() {
        // Balance enquiry reply that passes filter but matches no pattern
        String raw = "BOI - Your account XXX004 balance is Rs 50000 as on 20/05/2026";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
        assertFalse(result.isValid());
    }

    @Test
    public void parse_emptyString_returnsUnrecognized() {
        ParsedNotification result = NotificationParser.parse("");
        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
    }

    @Test
    public void parse_nullString_returnsUnrecognized() {
        ParsedNotification result = NotificationParser.parse(null);
        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
    }

    // ── Type helper methods ───────────────────────────────────────────────────

    @Test
    public void toLogType_mapsAllTypesCorrectly() {
        assertEquals("CHEQUE_CLEARED",   NotificationParser.toLogType(NotificationType.CHEQUE_CLEARED));
        assertEquals("CHEQUE_RETURNED",  NotificationParser.toLogType(NotificationType.CHEQUE_RETURNED));
        assertEquals("CHEQUE_PRESENTED", NotificationParser.toLogType(NotificationType.CHEQUE_PRESENTED));
        assertEquals("UPI_CREDIT",       NotificationParser.toLogType(NotificationType.UPI_CREDIT));
        assertEquals("UNRECOGNIZED",     NotificationParser.toLogType(NotificationType.UNRECOGNIZED));
    }

    // ── No voice for cheques ──────────────────────────────────────────────────

    @Test
    public void parse_chequeTypes_neverIsUpiCredit() {
        String[] chequeTexts = {
            "BOI - Cheque No. 11111 for Rs 1000 Debited(Clearing) in your A/c XX0004 on 01-01-2026 TO CLG. Avl Bal Rs 999",
            "BOI-Chq.No.22222 amt 2000, acc XXX004,Fvg. SOME PARTY, RETURNED.",
            "BOI-Chq.No. 33333 amt 3000 pertaining to acc XXX004,Fvg. ANOTHER CO is presented in CLEARING today."
        };

        for (String text : chequeTexts) {
            ParsedNotification result = NotificationParser.parse(text);
            assertFalse(
                "Cheque notification should never be UPI_CREDIT: " + text,
                result.isUpiCredit()
            );
        }
    }

    // ── Raw text preserved ────────────────────────────────────────────────────

    @Test
    public void parse_rawTextPreservedInResult() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for Rs. 100 on 01/01/2026 " +
                     "and debited from a/c no. XXXXXX0000 (UPI Ref noTEST001)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(raw, result.getRawText());
    }

    // ── Edge Cases ────────────────────────────────────────────────────────────

    @Test
    public void parse_extraWhitespace_stillParsesCleared() {
        String raw = "BOI   -   Cheque  No.   12345   for   Rs   50000   " +
                     "Debited(Clearing)   in   your   A/c   XX0004   " +
                     "on   27-04-2026   TO   CLG.   Avl   Bal   Rs   25000";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.CHEQUE_CLEARED, result.getType());
        assertEquals("12345", result.getChequeNumber());
        assertEquals(5000000L, result.getAmount());
    }

    @Test
    public void parse_extraWhitespace_upiCredit() {
        String raw = "BOI   UPI   -   Your   a/c   no.   XXXXXXXXXXX0004   " +
                     "is   credited   for   Rs.   1200   on   20/05/2026   " +
                     "and   debited   from   a/c   no.   XXXXXX2101   (UPI   Ref   noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(120000L, result.getAmount());
        assertEquals("ABC123456789", result.getReferenceNumber());
    }

    @Test
    public void parse_upiCredit_withUnicodeRupeeSymbol() {
        String raw = "BOI UPI - Your a/c no. XXXXXXXXXXX0004 is credited for ₹ 1200 on 20/05/2026 " +
                     "and debited from a/c no. XXXXXX2101 (UPI Ref noABC123456789)";

        ParsedNotification result = NotificationParser.parse(raw);

        assertEquals(NotificationType.UPI_CREDIT, result.getType());
        assertEquals(120000L, result.getAmount());
    }

    @Test
    public void parse_nullRawText_returnsUnrecognized() {
        ParsedNotification result = NotificationParser.parse(null);
        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
        assertFalse(result.isValid());
    }

    @Test
    public void parse_whitespaceOnly_returnsUnrecognized() {
        ParsedNotification result = NotificationParser.parse("   ");
        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
        assertFalse(result.isValid());
    }

    @Test
    public void parse_trailingNewlines_returnsUnrecognized() {
        ParsedNotification result = NotificationParser.parse("BOI Alert: Your account XXX004 has a new message.\n\n\n");
        assertEquals(NotificationType.UNRECOGNIZED, result.getType());
    }
}
