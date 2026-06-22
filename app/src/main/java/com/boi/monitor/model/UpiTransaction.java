package com.boi.monitor.model;

public class UpiTransaction {

    public static final String TYPE_UPI_CREDIT = "UPI_CREDIT";

    private String documentId;
    private String userId;
    private long amount;
    private String transactionType;
    private String accountSuffix;
    private String referenceNumber;
    private String debitedAccount;
    private String transactionDate;
    private boolean voiceAnnounced;
    private String timestamp;

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

    public String getDocumentId()      { return documentId; }
    public String getUserId()          { return userId; }
    public long getAmount()            { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getAccountSuffix()   { return accountSuffix; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getDebitedAccount()  { return debitedAccount; }
    public String getTransactionDate() { return transactionDate; }
    public boolean isVoiceAnnounced()  { return voiceAnnounced; }
    public String getTimestamp()       { return timestamp; }

    public void setDocumentId(String documentId)         { this.documentId = documentId; }
    public void setUserId(String userId)                 { this.userId = userId; }
    public void setAmount(long amount)                   { this.amount = amount; }
    public void setTransactionType(String type)          { this.transactionType = type; }
    public void setAccountSuffix(String suffix)          { this.accountSuffix = suffix; }
    public void setReferenceNumber(String ref)           { this.referenceNumber = ref; }
    public void setDebitedAccount(String acc)            { this.debitedAccount = acc; }
    public void setTransactionDate(String date)          { this.transactionDate = date; }
    public void setVoiceAnnounced(boolean announced)     { this.voiceAnnounced = announced; }
    public void setTimestamp(String timestamp)           { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "UpiTransaction{amount=" + amount +
               ", type=" + transactionType + ", ref=" + referenceNumber + "}";
    }
}
