package com.boi.monitor.model;

import com.boi.monitor.model.ParsedNotification.NotificationType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ParsedNotificationTest {

    // ── isValid ────────────────────────────────────────────────────────────────

    @Test
    public void isValid_chequeCleared_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_CLEARED);
        assertTrue(pn.isValid());
    }

    @Test
    public void isValid_chequeReturned_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_RETURNED);
        assertTrue(pn.isValid());
    }

    @Test
    public void isValid_chequePresented_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_PRESENTED);
        assertTrue(pn.isValid());
    }

    @Test
    public void isValid_upiCredit_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UPI_CREDIT);
        assertTrue(pn.isValid());
    }

    @Test
    public void isValid_unrecognized_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UNRECOGNIZED);
        assertFalse(pn.isValid());
    }

    @Test
    public void isValid_defaultConstructor_isFalse() {
        ParsedNotification pn = new ParsedNotification("");
        assertFalse(pn.isValid());
    }

    // ── isUpiCredit ────────────────────────────────────────────────────────────

    @Test
    public void isUpiCredit_upiCredit_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UPI_CREDIT);
        assertTrue(pn.isUpiCredit());
    }

    @Test
    public void isUpiCredit_chequeCleared_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_CLEARED);
        assertFalse(pn.isUpiCredit());
    }

    @Test
    public void isUpiCredit_chequeReturned_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_RETURNED);
        assertFalse(pn.isUpiCredit());
    }

    @Test
    public void isUpiCredit_chequePresented_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_PRESENTED);
        assertFalse(pn.isUpiCredit());
    }

    @Test
    public void isUpiCredit_unrecognized_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UNRECOGNIZED);
        assertFalse(pn.isUpiCredit());
    }

    // ── isCheque ───────────────────────────────────────────────────────────────

    @Test
    public void isCheque_chequeCleared_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_CLEARED);
        assertTrue(pn.isCheque());
    }

    @Test
    public void isCheque_chequeReturned_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_RETURNED);
        assertTrue(pn.isCheque());
    }

    @Test
    public void isCheque_chequePresented_returnsTrue() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.CHEQUE_PRESENTED);
        assertTrue(pn.isCheque());
    }

    @Test
    public void isCheque_upiCredit_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UPI_CREDIT);
        assertFalse(pn.isCheque());
    }

    @Test
    public void isCheque_unrecognized_returnsFalse() {
        ParsedNotification pn = new ParsedNotification("");
        pn.setType(NotificationType.UNRECOGNIZED);
        assertFalse(pn.isCheque());
    }
}
