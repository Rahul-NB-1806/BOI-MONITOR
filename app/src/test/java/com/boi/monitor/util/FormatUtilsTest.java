package com.boi.monitor.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class FormatUtilsTest {

    // ── formatCurrencyFromPaise ────────────────────────────────────────────────

    @Test
    public void formatCurrencyFromPaise_convertsCorrectly() {
        String result = FormatUtils.formatCurrencyFromPaise(150050L);
        assertTrue(result.contains("1,500"));
        assertTrue(result.contains(".50"));
    }

    @Test
    public void formatCurrencyFromPaise_zeroPaise() {
        String result = FormatUtils.formatCurrencyFromPaise(0L);
        assertTrue(result.contains("0"));
    }

    @Test
    public void formatCurrencyFromPaise_largeAmount() {
        String result = FormatUtils.formatCurrencyFromPaise(10000000000L);
        assertTrue(result.contains("10,00,00,000") || result.contains("100,000,000"));
    }

    @Test
    public void formatCurrencyFromPaise_exactRupee() {
        String result = FormatUtils.formatCurrencyFromPaise(500000L);
        assertTrue(result.contains("5,000"));
    }

    // ── formatCurrency (double) ────────────────────────────────────────────────

    @Test
    public void formatCurrency_formatsDouble() {
        String result = FormatUtils.formatCurrency(1500.50);
        assertTrue(result.contains("1,500") || result.contains("₹"));
    }

    // ── truncate ───────────────────────────────────────────────────────────────

    @Test
    public void truncate_shortText_unchanged() {
        assertEquals("Hello", FormatUtils.truncate("Hello", 10));
    }

    @Test
    public void truncate_exactLength_unchanged() {
        assertEquals("12345", FormatUtils.truncate("12345", 5));
    }

    @Test
    public void truncate_longText_appendsEllipsis() {
        String result = FormatUtils.truncate("This is a very long text", 10);
        assertEquals("This is...", result);
    }

    @Test
    public void truncate_null_returnsEmpty() {
        assertEquals("", FormatUtils.truncate(null, 10));
    }

    @Test
    public void truncate_emptyText_returnsEmpty() {
        assertEquals("", FormatUtils.truncate("", 10));
    }

    @Test
    public void truncate_maxLenLessThan3_returnsEmpty() {
        String result = FormatUtils.truncate("Hello World", 2);
        assertTrue(result.length() <= 2);
    }

    // ── chequeStatusLabel ──────────────────────────────────────────────────────

    @Test
    public void chequeStatusLabel_cleared() {
        assertEquals("✓ Cleared", FormatUtils.chequeStatusLabel("CLEARED"));
    }

    @Test
    public void chequeStatusLabel_returned() {
        assertEquals("✗ Returned", FormatUtils.chequeStatusLabel("RETURNED"));
    }

    @Test
    public void chequeStatusLabel_presented() {
        assertEquals("⏳ Presented", FormatUtils.chequeStatusLabel("PRESENTED"));
    }

    @Test
    public void chequeStatusLabel_null_returnsUnknown() {
        assertEquals("UNKNOWN", FormatUtils.chequeStatusLabel(null));
    }

    @Test
    public void chequeStatusLabel_unknownStatus_returnsAsIs() {
        assertEquals("UNKNOWN_STATUS", FormatUtils.chequeStatusLabel("UNKNOWN_STATUS"));
    }

    @Test
    public void chequeStatusLabel_caseInsensitive() {
        assertEquals("✓ Cleared", FormatUtils.chequeStatusLabel("cleared"));
    }

    @Test
    public void chequeStatusLabel_mixedCase() {
        assertEquals("✓ Cleared", FormatUtils.chequeStatusLabel("Cleared"));
    }

    // ── formatDate ─────────────────────────────────────────────────────────────

    @Test
    public void formatDate_null_returnsDash() {
        assertEquals("—", FormatUtils.formatDate(null));
    }
}
