package com.boi.monitor.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Firestore model for cheque transaction records.
 * Maps to: cheque_transactions/{documentId}
 */
@IgnoreExtraProperties
public class ChequeTransaction {

    public static final String STATUS_PRESENTED = "PRESENTED";
    public static final String STATUS_CLEARED    = "CLEARED";
    public static final String STATUS_RETURNED   = "RETURNED";

    public static final String COLLECTION = "cheque_transactions";

    @DocumentId
    private String documentId;

    private String chequeNumber;
    private long amount;
    private String status;             // PRESENTED | CLEARED | RETURNED
    private long availableBalance;   // Only set on CLEARED
    private String transactionDate;

    @ServerTimestamp
    private Timestamp timestamp;

    // Required empty constructor for Firestore deserialization
    public ChequeTransaction() {}

    public ChequeTransaction(String chequeNumber, long amount, String status,
                             long availableBalance, String transactionDate) {
        this.chequeNumber       = chequeNumber;
        this.amount             = amount;
        this.status             = status;
        this.availableBalance   = availableBalance;
        this.transactionDate    = transactionDate;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getDocumentId()      { return documentId; }
    public String getChequeNumber()    { return chequeNumber; }
    public long getAmount()          { return amount; }
    public String getStatus()          { return status; }
    public long getAvailableBalance(){ return availableBalance; }
    public String getTransactionDate() { return transactionDate; }
    public Timestamp getTimestamp()    { return timestamp; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDocumentId(String documentId)           { this.documentId = documentId; }
    public void setChequeNumber(String chequeNumber)       { this.chequeNumber = chequeNumber; }
    public void setAmount(long amount)                   { this.amount = amount; }
    public void setStatus(String status)                   { this.status = status; }
    public void setAvailableBalance(long availableBalance){ this.availableBalance = availableBalance; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public void setTimestamp(Timestamp timestamp)          { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChequeTransaction{cheque=" + chequeNumber +
               ", amount=" + amount + ", status=" + status + "}";
    }
}
