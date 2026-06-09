package com.boi.monitor.model;

/**
 * Intermediate data class holding the result of parsing a raw notification string.
 * Created by NotificationParser; consumed by FirebaseDataModule.
 */
public class ParsedNotification {

    public enum NotificationType {
        CHEQUE_CLEARED,
        CHEQUE_RETURNED,
        CHEQUE_PRESENTED,
        UPI_CREDIT,
        UNRECOGNIZED
    }

    private NotificationType type;
    private String           rawText;

    // Cheque fields
    private String chequeNumber;
    private long amount;
    private long availableBalance;
    private String transactionDate;
    private String favouringParty;

    // UPI fields
    private String referenceNumber;
    private String accountSuffix;
    private String debitedAccount;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ParsedNotification(String rawText) {
        this.rawText = rawText;
        this.type    = NotificationType.UNRECOGNIZED;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public NotificationType getType()         { return type; }
    public void setType(NotificationType t)   { this.type = t; }

    public String getRawText()                { return rawText; }
    public void setRawText(String t)          { this.rawText = t; }

    public String getChequeNumber()           { return chequeNumber; }
    public void setChequeNumber(String n)     { this.chequeNumber = n; }

    public long getAmount()                 { return amount; }
    public void setAmount(long a)           { this.amount = a; }

    public long getAvailableBalance()       { return availableBalance; }
    public void setAvailableBalance(long b) { this.availableBalance = b; }

    public String getTransactionDate()        { return transactionDate; }
    public void setTransactionDate(String d)  { this.transactionDate = d; }

    public String getFavouringParty()         { return favouringParty; }
    public void setFavouringParty(String p)   { this.favouringParty = p; }

    public String getReferenceNumber()        { return referenceNumber; }
    public void setReferenceNumber(String r)  { this.referenceNumber = r; }

    public String getAccountSuffix()          { return accountSuffix; }
    public void setAccountSuffix(String s)    { this.accountSuffix = s; }

    public String getDebitedAccount()         { return debitedAccount; }
    public void setDebitedAccount(String d)   { this.debitedAccount = d; }

    // ── Helper ────────────────────────────────────────────────────────────────

    public boolean isValid() {
        return type != NotificationType.UNRECOGNIZED;
    }

    public boolean isUpiCredit() {
        return type == NotificationType.UPI_CREDIT;
    }

    private long notificationTime;

    public long getNotificationTime() {
        return notificationTime;
    }

    public void setNotificationTime(long notificationTime) {
        this.notificationTime = notificationTime;
    }

    public boolean isCheque() {
        return type == NotificationType.CHEQUE_CLEARED
            || type == NotificationType.CHEQUE_RETURNED
            || type == NotificationType.CHEQUE_PRESENTED;
    }

    @Override
    public String toString() {
        return "ParsedNotification{type=" + type + ", cheque=" + chequeNumber
               + ", amount=" + amount + "}";
    }
}
