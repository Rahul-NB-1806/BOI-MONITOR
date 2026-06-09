package com.boi.monitor.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Firestore model for UPI transaction records.
 * Maps to: upi_transactions/{documentId}
 */
@IgnoreExtraProperties
public class UpiTransaction {

    public static final String TYPE_UPI_CREDIT = "UPI_CREDIT";
    public static final String COLLECTION      = "upi_transactions";

    @DocumentId
    private String documentId;

    private long amount;
    private String transactionType;      // UPI_CREDIT
    private String accountSuffix;        // last 4 digits
    private String referenceNumber;      // UPI Ref no
    private String debitedAccount;       // sender account
    private String transactionDate;
    private boolean voiceAnnounced;      // track if TTS was fired

    @ServerTimestamp
    private Timestamp timestamp;

    // Required for Firestore
    public UpiTransaction() {}

    public UpiTransaction(long amount, String transactionType, String accountSuffix,
                          String referenceNumber, String debitedAccount,
                          String transactionDate) {
        this.amount          = amount;
        this.transactionType = transactionType;
        this.accountSuffix   = accountSuffix;
        this.referenceNumber = referenceNumber;
        this.debitedAccount  = debitedAccount;
        this.transactionDate = transactionDate;
        this.voiceAnnounced  = false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getDocumentId()      { return documentId; }
    public long getAmount()          { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getAccountSuffix()   { return accountSuffix; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getDebitedAccount()  { return debitedAccount; }
    public String getTransactionDate() { return transactionDate; }
    public boolean isVoiceAnnounced()  { return voiceAnnounced; }
    public Timestamp getTimestamp()    { return timestamp; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDocumentId(String documentId)         { this.documentId = documentId; }
    public void setAmount(long amount)                 { this.amount = amount; }
    public void setTransactionType(String type)          { this.transactionType = type; }
    public void setAccountSuffix(String suffix)          { this.accountSuffix = suffix; }
    public void setReferenceNumber(String ref)           { this.referenceNumber = ref; }
    public void setDebitedAccount(String acc)            { this.debitedAccount = acc; }
    public void setTransactionDate(String date)          { this.transactionDate = date; }
    public void setVoiceAnnounced(boolean announced)     { this.voiceAnnounced = announced; }
    public void setTimestamp(Timestamp timestamp)        { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "UpiTransaction{amount=" + amount +
               ", type=" + transactionType + ", ref=" + referenceNumber + "}";
    }
}
