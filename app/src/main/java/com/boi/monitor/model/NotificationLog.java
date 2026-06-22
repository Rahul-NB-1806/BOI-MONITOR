package com.boi.monitor.model;

public class NotificationLog {

    public static final String TYPE_CHEQUE_CLEARED    = "CHEQUE_CLEARED";
    public static final String TYPE_CHEQUE_RETURNED   = "CHEQUE_RETURNED";
    public static final String TYPE_CHEQUE_PRESENTED  = "CHEQUE_PRESENTED";
    public static final String TYPE_UPI_CREDIT        = "UPI_CREDIT";
    public static final String TYPE_UNRECOGNIZED      = "UNRECOGNIZED";

    private String documentId;
    private String userId;
    private String packageName;
    private String notificationType;
    private boolean processed;
    private String processingError;
    private String rawTextStored;
    private String timestamp;

    public NotificationLog() {}

    public NotificationLog(String packageName, String notificationType,
                           boolean processed, String processingError) {
        this.packageName       = packageName;
        this.notificationType  = notificationType;
        this.processed         = processed;
        this.processingError   = processingError;
    }

    public String getDocumentId()        { return documentId; }
    public String getUserId()            { return userId; }
    public String getPackageName()       { return packageName; }
    public String getNotificationType()  { return notificationType; }
    public boolean isProcessed()         { return processed; }
    public String getProcessingError()   { return processingError; }
    public String getRawTextStored()     { return rawTextStored; }
    public String getTimestamp()         { return timestamp; }

    public void setDocumentId(String id)          { this.documentId = id; }
    public void setUserId(String userId)          { this.userId = userId; }
    public void setPackageName(String name)       { this.packageName = name; }
    public void setNotificationType(String type)  { this.notificationType = type; }
    public void setProcessed(boolean p)           { this.processed = p; }
    public void setProcessingError(String err)    { this.processingError = err; }
    public void setRawTextStored(String txt)      { this.rawTextStored = txt; }
    public void setTimestamp(String t)            { this.timestamp = t; }
}
