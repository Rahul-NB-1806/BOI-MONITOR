package com.boi.monitor.model;

public class ChequeTransaction {

    public static final String STATUS_PRESENTED = "PRESENTED";
    public static final String STATUS_CLEARED    = "CLEARED";
    public static final String STATUS_RETURNED   = "RETURNED";

    private String documentId;
    private String userId;
    private String chequeNumber;
    private long amount;
    private String status;
    private long availableBalance;
    private String transactionDate;
    private String timestamp;

    public ChequeTransaction() {}

    public ChequeTransaction(String chequeNumber, long amount, String status,
                             long availableBalance, String transactionDate) {
        this.chequeNumber       = chequeNumber;
        this.amount             = amount;
        this.status             = status;
        this.availableBalance   = availableBalance;
        this.transactionDate    = transactionDate;
    }

    public String getDocumentId()      { return documentId; }
    public String getUserId()          { return userId; }
    public String getChequeNumber()    { return chequeNumber; }
    public long getAmount()            { return amount; }
    public String getStatus()          { return status; }
    public long getAvailableBalance()  { return availableBalance; }
    public String getTransactionDate() { return transactionDate; }
    public String getTimestamp()       { return timestamp; }

    public void setDocumentId(String documentId)           { this.documentId = documentId; }
    public void setUserId(String userId)                   { this.userId = userId; }
    public void setChequeNumber(String chequeNumber)       { this.chequeNumber = chequeNumber; }
    public void setAmount(long amount)                     { this.amount = amount; }
    public void setStatus(String status)                   { this.status = status; }
    public void setAvailableBalance(long availableBalance) { this.availableBalance = availableBalance; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public void setTimestamp(String timestamp)              { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChequeTransaction{cheque=" + chequeNumber +
               ", amount=" + amount + ", status=" + status + "}";
    }
}
